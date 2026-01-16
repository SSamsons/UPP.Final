package com.contactcrawler.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "htmlFetchClient", url = "${feign.htmlFetch.baseUrl:}", fallback = FeignHtmlClientFallback.class)
public interface FeignHtmlClient {
    
    @GetMapping
    String fetchHtml(@RequestParam(value = "url", required = false) String url);
}
