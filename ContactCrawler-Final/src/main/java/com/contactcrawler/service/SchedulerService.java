package com.contactcrawler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SchedulerService {
    
    @Autowired
    private CrawlerService crawlerService;
    
    @Value("${crawler.scheduled.start.urls:https://2gis.ru,https://yandex.ru/maps}")
    private String scheduledStartUrls;
    
    private final ScheduledExecutorService scheduledExecutorService = 
        Executors.newScheduledThreadPool(1);

    // @Scheduled - запуск каждый день в 2:00
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCrawlWithAnnotation() {
        System.out.println("Scheduled crawl started at " + LocalDateTime.now());
        String[] urls = scheduledStartUrls.split(",");
        Set<String> urlSet = Set.of(urls);
        crawlerService.startCrawling(urlSet);
    }

    // ScheduledExecutorService - запуск каждые 30 минут
    public void startPeriodicCrawl() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            System.out.println("Periodic crawl started at " + LocalDateTime.now());
            String[] urls = scheduledStartUrls.split(",");
            Set<String> urlSet = Set.of(urls);
            crawlerService.startCrawling(urlSet);
        }, 0, 30, TimeUnit.MINUTES);
    }

    public void stopPeriodicCrawl() {
        scheduledExecutorService.shutdown();
    }
}
