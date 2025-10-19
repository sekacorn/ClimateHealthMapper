package com.climate.visualizer.utils;

import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.HashMap;
import java.util.Map;

/**
 * System Resource Monitoring Utility
 *
 * Provides low-level system resource monitoring capabilities
 * for CPU, memory, and other hardware metrics.
 */
@Slf4j
public class ResourceMonitor {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final CentralProcessor processor;
    private final GlobalMemory memory;

    private long[] previousTicks;

    public ResourceMonitor() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.processor = hardware.getProcessor();
        this.memory = hardware.getMemory();
        this.previousTicks = processor.getSystemCpuLoadTicks();
    }

    /**
     * Get CPU usage percentage
     */
    public double getCpuUsage() {
        long[] currentTicks = processor.getSystemCpuLoadTicks();
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(previousTicks) * 100;
        previousTicks = currentTicks;
        return Math.min(100.0, Math.max(0.0, cpuLoad));
    }

    /**
     * Get memory usage information
     */
    public Map<String, Long> getMemoryUsage() {
        Map<String, Long> memoryInfo = new HashMap<>();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;

        memoryInfo.put("total", total);
        memoryInfo.put("used", used);
        memoryInfo.put("available", available);
        memoryInfo.put("usagePercent", (used * 100) / total);

        return memoryInfo;
    }

    /**
     * Get number of available CPU cores
     */
    public int getAvailableCores() {
        return processor.getLogicalProcessorCount();
    }

    /**
     * Get physical CPU cores
     */
    public int getPhysicalCores() {
        return processor.getPhysicalProcessorCount();
    }

    /**
     * Get CPU model name
     */
    public String getCpuModel() {
        return processor.getProcessorIdentifier().getName();
    }

    /**
     * Get system load average
     */
    public double[] getSystemLoadAverage() {
        return processor.getSystemLoadAverage(3);
    }

    /**
     * Check if system is under heavy load
     */
    public boolean isUnderHeavyLoad() {
        double cpuUsage = getCpuUsage();
        Map<String, Long> memory = getMemoryUsage();
        long memoryUsagePercent = memory.get("usagePercent");

        return cpuUsage > 80 || memoryUsagePercent > 85;
    }

    /**
     * Get recommended thread pool size based on current load
     */
    public int getRecommendedThreadPoolSize() {
        double cpuUsage = getCpuUsage();
        int cores = getAvailableCores();

        if (cpuUsage < 30) {
            return cores * 2; // Low load - use more threads
        } else if (cpuUsage < 60) {
            return cores; // Moderate load - use one thread per core
        } else {
            return Math.max(2, cores / 2); // High load - reduce threads
        }
    }

    /**
     * Estimate available memory for new tasks
     */
    public long getAvailableMemoryForTasks() {
        long available = memory.getAvailable();
        // Reserve 20% for system operations
        return (long) (available * 0.8);
    }

    /**
     * Get comprehensive resource snapshot
     */
    public Map<String, Object> getResourceSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();

        snapshot.put("timestamp", System.currentTimeMillis());
        snapshot.put("cpuUsage", getCpuUsage());
        snapshot.put("cpuCores", getAvailableCores());
        snapshot.put("cpuModel", getCpuModel());
        snapshot.put("memory", getMemoryUsage());
        snapshot.put("loadAverage", getSystemLoadAverage());
        snapshot.put("underHeavyLoad", isUnderHeavyLoad());

        return snapshot;
    }

    /**
     * Log current resource usage
     */
    public void logResourceUsage() {
        Map<String, Object> snapshot = getResourceSnapshot();
        log.info("Resource Usage - CPU: {}%, Memory: {}%, Cores: {}",
                 snapshot.get("cpuUsage"),
                 ((Map<String, Long>) snapshot.get("memory")).get("usagePercent"),
                 snapshot.get("cpuCores"));
    }

    /**
     * Wait for resources to become available
     */
    public boolean waitForResources(long timeoutMs, double maxCpuUsage, double maxMemoryUsage) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            double cpuUsage = getCpuUsage();
            Map<String, Long> memory = getMemoryUsage();
            double memoryUsagePercent = memory.get("usagePercent");

            if (cpuUsage < maxCpuUsage && memoryUsagePercent < maxMemoryUsage) {
                return true;
            }

            try {
                Thread.sleep(1000); // Wait 1 second before checking again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false; // Timeout
    }
}
