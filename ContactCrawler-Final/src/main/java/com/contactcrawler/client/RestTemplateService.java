package com.contactcrawler.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Service
public class RestTemplateService {
    
    private RestTemplate restTemplate;
    
    @Value("${crawler.timeout.ms:10000}")
    private int timeoutMs;
    
    @Value("${crawler.user.agent:Mozilla/5.0}")
    private String userAgent;

    @javax.annotation.PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        
        this.restTemplate = new RestTemplate(factory);
    }

    public String fetchHtml(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            return response.getBody() != null ? response.getBody() : "";
        } catch (Exception e) {
            System.err.println("Error fetching URL with RestTemplate: " + url + " - " + e.getMessage());
            return "";
        }
    }
}
