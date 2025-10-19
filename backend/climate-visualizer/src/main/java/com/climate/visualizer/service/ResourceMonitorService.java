package com.climate.visualizer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for monitoring system resources
 *
 * Tracks CPU, memory, and GPU usage to optimize
 * visualization processing and prevent resource exhaustion.
 */
@Service
@Slf4j
public class ResourceMonitorService {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final CentralProcessor processor;
    private final GlobalMemory memory;

    private long[] prevTicks;
    private double currentCpuUsage = 0.0;
    private long currentMemoryUsage = 0L;
    private long totalMemory = 0L;

    public ResourceMonitorService() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.processor = hardware.getProcessor();
        this.memory = hardware.getMemory();
        this.prevTicks = processor.getSystemCpuLoadTicks();
        this.totalMemory = memory.getTotal();

        log.info("Resource monitor initialized - CPU: {}, Memory: {} GB",
                 processor.getProcessorIdentifier().getName(),
                 totalMemory / (1024 * 1024 * 1024));
    }

    /**
     * Get current system resources
     */
    public Map<String, Object> getSystemResources() {
        Map<String, Object> resources = new HashMap<>();

        // CPU information
        Map<String, Object> cpu = new HashMap<>();
        cpu.put("model", processor.getProcessorIdentifier().getName());
        cpu.put("cores", processor.getLogicalProcessorCount());
        cpu.put("physicalCores", processor.getPhysicalProcessorCount());
        cpu.put("usage", getCurrentCpuUsage());
        cpu.put("maxFrequency", processor.getMaxFreq() / 1_000_000_000.0); // GHz

        resources.put("cpu", cpu);

        // Memory information
        Map<String, Object> mem = new HashMap<>();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;

        mem.put("total", formatBytes(totalMemory));
        mem.put("used", formatBytes(usedMemory));
        mem.put("available", formatBytes(availableMemory));
        mem.put("usagePercent", (usedMemory * 100.0) / totalMemory);

        resources.put("memory", mem);

        // GPU information
        List<GraphicsCard> graphicsCards = hardware.getGraphicsCards();
        if (!graphicsCards.isEmpty()) {
            Map<String, Object> gpu = new HashMap<>();
            GraphicsCard primaryGpu = graphicsCards.get(0);

            gpu.put("name", primaryGpu.getName());
            gpu.put("vendor", primaryGpu.getVendor());
            gpu.put("vram", formatBytes(primaryGpu.getVRam()));
            gpu.put("available", true);

            resources.put("gpu", gpu);
        } else {
            resources.put("gpu", Map.of("available", false));
        }

        // System load
        double[] loadAverage = processor.getSystemLoadAverage(3);
        resources.put("loadAverage", Map.of(
            "1min", loadAverage[0],
            "5min", loadAverage[1],
            "15min", loadAverage[2]
        ));

        // Resource availability
        resources.put("status", assessResourceStatus(
            getCurrentCpuUsage(),
            (usedMemory * 100.0) / totalMemory));

        return resources;
    }

    /**
     * Get current CPU usage percentage
     */
    public double getCurrentCpuUsage() {
        long[] ticks = processor.getSystemCpuLoadTicks();
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = ticks;
        currentCpuUsage = cpuLoad;
        return cpuLoad;
    }

    /**
     * Get current memory usage percentage
     */
    public double getCurrentMemoryUsage() {
        long available = memory.getAvailable();
        long used = totalMemory - available;
        return (used * 100.0) / totalMemory;
    }

    /**
     * Check if system has sufficient resources for processing
     */
    public boolean hasSufficientResources(String quality) {
        double cpuUsage = getCurrentCpuUsage();
        double memoryUsage = getCurrentMemoryUsage();

        return switch (quality.toLowerCase()) {
            case "low" -> cpuUsage < 80 && memoryUsage < 80;
            case "medium" -> cpuUsage < 60 && memoryUsage < 70;
            case "high" -> cpuUsage < 40 && memoryUsage < 60;
            default -> cpuUsage < 70 && memoryUsage < 75;
        };
    }

    /**
     * Get recommended thread count based on current resources
     */
    public int getRecommendedThreadCount() {
        double cpuUsage = getCurrentCpuUsage();
        int availableCores = processor.getLogicalProcessorCount();

        if (cpuUsage < 30) {
            return availableCores; // Use all cores
        } else if (cpuUsage < 60) {
            return Math.max(2, availableCores / 2); // Use half cores
        } else {
            return Math.max(1, availableCores / 4); // Use quarter cores
        }
    }

    /**
     * Update resource metrics periodically
     */
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void updateMetrics() {
        try {
            currentCpuUsage = getCurrentCpuUsage();
            currentMemoryUsage = totalMemory - memory.getAvailable();

            log.debug("Resource metrics - CPU: {:.2f}%, Memory: {:.2f}%",
                     currentCpuUsage, getCurrentMemoryUsage());

            // Log warnings if resources are high
            if (currentCpuUsage > 80) {
                log.warn("High CPU usage detected: {:.2f}%", currentCpuUsage);
            }
            if (getCurrentMemoryUsage() > 85) {
                log.warn("High memory usage detected: {:.2f}%", getCurrentMemoryUsage());
            }

        } catch (Exception e) {
            log.error("Error updating resource metrics", e);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }

    private String assessResourceStatus(double cpuUsage, double memoryUsage) {
        if (cpuUsage > 85 || memoryUsage > 90) {
            return "CRITICAL";
        } else if (cpuUsage > 70 || memoryUsage > 80) {
            return "HIGH";
        } else if (cpuUsage > 50 || memoryUsage > 60) {
            return "MODERATE";
        } else {
            return "GOOD";
        }
    }

    /**
     * Get memory headroom for processing
     */
    public long getAvailableMemoryBytes() {
        return memory.getAvailable();
    }

    /**
     * Estimate memory required for visualization
     */
    public long estimateMemoryRequired(int dataPoints, String quality) {
        // Rough estimation: base + points * complexity factor
        long baseMemory = 50 * 1024 * 1024; // 50 MB base

        long perPointMemory = switch (quality.toLowerCase()) {
            case "low" -> 1024; // 1 KB per point
            case "high" -> 4096; // 4 KB per point
            default -> 2048; // 2 KB per point (medium)
        };

        return baseMemory + (dataPoints * perPointMemory);
    }
}
