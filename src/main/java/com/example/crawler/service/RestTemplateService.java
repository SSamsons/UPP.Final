package com.example.crawler.service;

import com.example.crawler.config.CrawlerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RestTemplateService {
    
    private static final Logger logger = LoggerFactory.getLogger(RestTemplateService.class);
    private final RestTemplate restTemplate;
    private final CrawlerConfig crawlerConfig;
    
    public RestTemplateService(CrawlerConfig crawlerConfig) {
        this.crawlerConfig = crawlerConfig;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(crawlerConfig.getTimeout());
        factory.setReadTimeout(crawlerConfig.getTimeout());
        this.restTemplate = new RestTemplate(factory);
    }
    
    public String fetchHtmlContent(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", crawlerConfig.getUserAgent());
            headers.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            logger.debug("Timeout or connection error fetching URL: {}", url, e);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.debug("HTTP client error for URL: {} - Status: {}", url, e.getStatusCode(), e);
        } catch (Exception e) {
            logger.debug("Error fetching URL with RestTemplate: {}", url, e);
        }
        
        return null;
    }
}


