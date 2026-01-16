package com.contactcrawler.client;

import org.springframework.stereotype.Component;

@Component
public class FeignHtmlClientFallback implements FeignHtmlClient {
    
    @Override
    public String fetchHtml(String url) {
        System.out.println("Feign client fallback triggered for URL: " + url);
        return "";
    }
}
