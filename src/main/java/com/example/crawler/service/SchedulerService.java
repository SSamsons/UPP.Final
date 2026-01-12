package com.example.crawler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SchedulerService {
    
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    
    private final CrawlerService crawlerService;
    private final ScheduledExecutorService scheduler;
    
    private static final List<String> DEFAULT_SITES = Arrays.asList(
        "https://www.example-business.com",
        "https://www.sample-company.org"
    );
    
    public SchedulerService(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "scheduler-thread");
            t.setDaemon(false);
            return t;
        });
        
        startScheduledCrawling();
    }
    
    private void startScheduledCrawling() {
        scheduler.scheduleAtFixedRate(this::performScheduledCrawl, 0, 30, TimeUnit.MINUTES);
        logger.info("Scheduled crawler initialized with 30-minute interval");
    }
    
    @Scheduled(cron = "0 0 2 * * ?")
    public void performDailyCrawl() {
        logger.info("Executing daily scheduled crawl task");
        try {
            String taskId = crawlerService.startCrawling(DEFAULT_SITES);
            logger.info("Daily crawl task initiated with ID: {}", taskId);
        } catch (Exception e) {
            logger.error("Failed to start daily crawl task", e);
        }
    }
    
    private void performScheduledCrawl() {
        logger.info("Executing periodic scheduled crawl task");
        try {
            String taskId = crawlerService.startCrawling(DEFAULT_SITES);
            logger.info("Periodic crawl task initiated with ID: {}", taskId);
        } catch (Exception e) {
            logger.error("Failed to start periodic crawl task", e);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down scheduler service");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Scheduler did not terminate gracefully, forcing shutdown");
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.error("Scheduler pool did not terminate");
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Scheduler shutdown interrupted", e);
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}


