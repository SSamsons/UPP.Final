package com.example.crawler.service;

import com.example.crawler.config.CrawlerConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;

@Service
public class WebClientService {
    
    private final WebClient webClient;
    private final CrawlerConfig crawlerConfig;
    
    public WebClientService(CrawlerConfig crawlerConfig) {
        this.crawlerConfig = crawlerConfig;
        int timeoutMs = crawlerConfig.getTimeout();
        
        this.webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, crawlerConfig.getUserAgent())
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }
    
    public Mono<String> fetchHtmlContent(String url) {
        if (url == null || url.trim().isEmpty()) {
            return Mono.empty();
        }
        
        try {
            int timeoutMs = crawlerConfig.getTimeout();
            URI uri = URI.create(url);
            
            return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(1, Duration.ofMillis(500))
                    .filter(throwable -> throwable instanceof java.util.concurrent.TimeoutException 
                        || throwable instanceof java.net.SocketTimeoutException))
                .onErrorResume(e -> Mono.empty());
        } catch (IllegalArgumentException e) {
            // Invalid URL format
            return Mono.empty();
        }
    }
}


