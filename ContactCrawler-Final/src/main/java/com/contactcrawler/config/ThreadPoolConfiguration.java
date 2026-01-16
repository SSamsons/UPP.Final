package com.contactcrawler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfiguration {

    @Value("${crawler.thread.pool.size:10}")
    private int threadPoolSize;

    @Bean(name = "crawlerExecutorService")
    public ExecutorService crawlerExecutorService() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "crawler-thread-" + threadNumber.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        };
        
        return Executors.newFixedThreadPool(threadPoolSize, threadFactory);
    }

    @Bean(name = "dataProcessingForkJoinPool")
    public ForkJoinPool dataProcessingForkJoinPool() {
        return new ForkJoinPool(
            Runtime.getRuntime().availableProcessors(),
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null,
            true
        );
    }
}
