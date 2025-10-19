package com.climate.visualizer.utils;

import com.climate.visualizer.service.ResourceMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Dynamic Multithreading Manager
 *
 * Manages thread pools dynamically based on system resources
 * and workload to optimize visualization processing.
 */
@Component
@Slf4j
public class MultithreadingManager {

    private final ResourceMonitorService resourceMonitorService;
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutor;

    private int currentThreadPoolSize;
    private static final int MIN_THREADS = 2;
    private static final int MAX_THREADS = 32;

    @Autowired
    public MultithreadingManager(ResourceMonitorService resourceMonitorService) {
        this.resourceMonitorService = resourceMonitorService;
        this.currentThreadPoolSize = calculateOptimalThreadCount();
        this.executorService = createThreadPool(currentThreadPoolSize);
        this.scheduledExecutor = Executors.newScheduledThreadPool(1);

        // Adjust thread pool size periodically
        scheduledExecutor.scheduleAtFixedRate(
            this::adjustThreadPoolSize,
            30, 30, TimeUnit.SECONDS
        );

        log.info("Multithreading manager initialized with {} threads", currentThreadPoolSize);
    }

    /**
     * Execute a task asynchronously
     */
    public <T> CompletableFuture<T> executeAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executorService);
    }

    /**
     * Execute a runnable task asynchronously
     */
    public CompletableFuture<Void> executeAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executorService);
    }

    /**
     * Execute multiple tasks in parallel and wait for all to complete
     */
    public <T> List<T> executeAllAndWait(List<Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {

        try {
            List<Future<T>> futures = executorService.invokeAll(tasks, timeout, unit);

            return futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        log.error("Error executing task", e);
                        return null;
                    }
                })
                .filter(result -> result != null)
                .toList();

        } catch (InterruptedException e) {
            log.error("Task execution interrupted", e);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    /**
     * Execute batch processing with resource-aware parallelism
     */
    public <T, R> List<R> processBatch(
            List<T> items,
            java.util.function.Function<T, R> processor,
            int batchSize) {

        int optimalParallelism = getOptimalParallelism();
        log.info("Processing {} items with parallelism level: {}", items.size(), optimalParallelism);

        List<R> results = new CopyOnWriteArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            List<T> batch = items.subList(i, end);

            CompletableFuture<Void> future = executeAsync(() -> {
                for (T item : batch) {
                    try {
                        R result = processor.apply(item);
                        results.add(result);
                    } catch (Exception e) {
                        log.error("Error processing item in batch", e);
                    }
                }
            });

            futures.add(future);

            // Limit concurrent batches based on optimal parallelism
            if (futures.size() >= optimalParallelism) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                futures.clear();
            }
        }

        // Wait for remaining batches
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        return results;
    }

    /**
     * Get optimal parallelism level based on current resources
     */
    public int getOptimalParallelism() {
        return resourceMonitorService.getRecommendedThreadCount();
    }

    /**
     * Calculate optimal thread count based on system resources
     */
    private int calculateOptimalThreadCount() {
        int recommendedThreads = resourceMonitorService.getRecommendedThreadCount();
        return Math.max(MIN_THREADS, Math.min(MAX_THREADS, recommendedThreads));
    }

    /**
     * Create thread pool with specified size
     */
    private ExecutorService createThreadPool(int size) {
        return new ThreadPoolExecutor(
            size, // core pool size
            size, // maximum pool size
            60L, TimeUnit.SECONDS, // keep-alive time
            new LinkedBlockingQueue<>(),
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "ClimateViz-Worker-" + counter++);
                    thread.setDaemon(false);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // Rejection policy
        );
    }

    /**
     * Adjust thread pool size based on current resource usage
     */
    private void adjustThreadPoolSize() {
        try {
            int optimalSize = calculateOptimalThreadCount();

            if (optimalSize != currentThreadPoolSize) {
                log.info("Adjusting thread pool size from {} to {}", currentThreadPoolSize, optimalSize);

                // Create new executor with optimal size
                ExecutorService oldExecutor = executorService;
                executorService = createThreadPool(optimalSize);
                currentThreadPoolSize = optimalSize;

                // Gracefully shutdown old executor
                oldExecutor.shutdown();
                try {
                    if (!oldExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        oldExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    oldExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            log.error("Error adjusting thread pool size", e);
        }
    }

    /**
     * Get current thread pool statistics
     */
    public Map<String, Object> getThreadPoolStats() {
        Map<String, Object> stats = new HashMap<>();

        if (executorService instanceof ThreadPoolExecutor tpe) {
            stats.put("poolSize", tpe.getPoolSize());
            stats.put("activeThreads", tpe.getActiveCount());
            stats.put("queueSize", tpe.getQueue().size());
            stats.put("completedTasks", tpe.getCompletedTaskCount());
            stats.put("totalTasks", tpe.getTaskCount());
        }

        stats.put("currentThreadPoolSize", currentThreadPoolSize);
        stats.put("optimalParallelism", getOptimalParallelism());

        return stats;
    }

    /**
     * Shutdown thread pools gracefully
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down multithreading manager");

        scheduledExecutor.shutdown();
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Thread pool did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Multithreading manager shut down successfully");
    }
}
