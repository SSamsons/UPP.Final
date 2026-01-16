package com.contactcrawler.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Service
public class WebClientService {
    
    private WebClient webClient;
    
    @Value("${crawler.timeout.ms:10000}")
    private int timeoutMs;
    
    @Value("${crawler.user.agent:Mozilla/5.0}")
    private String userAgent;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
    }

    public Mono<String> fetchHtml(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof java.util.concurrent.TimeoutException))
                .onErrorReturn("");
    }
}
