package com.contactcrawler.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    
    @Bean
    public Timer parsingTimer(MeterRegistry registry) {
        return Timer.builder("contact.parser.execution.time")
                .description("Time taken to parse HTML content and extract contacts")
                .register(registry);
    }
    
    @Bean
    public Counter parsingSuccessCounter(MeterRegistry registry) {
        return Counter.builder("contact.parser.successful.extractions")
                .description("Number of successful contact extraction operations")
                .register(registry);
    }
    
    @Bean
    public Counter parsingErrorCounter(MeterRegistry registry) {
        return Counter.builder("contact.parser.failed.extractions")
                .description("Number of failed contact extraction operations")
                .register(registry);
    }
    
    @Bean
    public Counter databaseRecordsCounter(MeterRegistry registry) {
        return Counter.builder("contact.database.organizations.saved")
                .description("Number of organization records saved to database")
                .register(registry);
    }
    
    @Bean
    public Counter pagesCrawledCounter(MeterRegistry registry) {
        return Counter.builder("contact.crawler.pages.processed")
                .description("Total number of web pages processed")
                .register(registry);
    }
    
    @Bean
    public Counter urlsVisitedCounter(MeterRegistry registry) {
        return Counter.builder("contact.crawler.urls.accessed")
                .description("Total number of URLs accessed during crawling")
                .register(registry);
    }
    
    @Bean
    public Timer databaseSaveTimer(MeterRegistry registry) {
        return Timer.builder("contact.database.save.operation.time")
                .description("Time taken to save organization data to database")
                .register(registry);
    }
    
    @Bean
    public Timer htmlFetchTimer(MeterRegistry registry) {
        return Timer.builder("contact.crawler.html.download.time")
                .description("Time taken to download HTML content from web")
                .register(registry);
    }
}
