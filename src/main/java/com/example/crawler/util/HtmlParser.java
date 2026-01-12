package com.example.crawler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HtmlParser {
    
    private static final Logger logger = LoggerFactory.getLogger(HtmlParser.class);
    private static final Pattern LINK_PATTERN = Pattern.compile(
        "<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>", 
        Pattern.CASE_INSENSITIVE
    );
    
    public String fetchHtmlContent(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            logger.warn("Attempted to fetch HTML with null or empty URL");
            return "";
        }
        
        StringBuilder content = new StringBuilder();
        HttpURLConnection connection = null;
        
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "CompanyCrawler/1.0");
            connection.setInstanceFollowRedirects(true);
            
            int status = connection.getResponseCode();
            
            if (status == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
            } else {
                logger.debug("HTTP status {} for URL: {}", status, urlString);
            }
            
        } catch (MalformedURLException e) {
            logger.warn("Malformed URL: {}", urlString, e);
        } catch (java.io.IOException e) {
            logger.debug("IO error fetching URL: {}", urlString, e);
        } catch (Exception e) {
            logger.warn("Unexpected error fetching URL: {}", urlString, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        
        return content.toString();
    }
    
    public List<String> extractLinks(String htmlContent, String baseUrl) {
        if (htmlContent == null || htmlContent.trim().isEmpty() || baseUrl == null) {
            return new ArrayList<>();
        }
        
        java.util.Set<String> linkSet = new java.util.LinkedHashSet<>();
        Matcher matcher = LINK_PATTERN.matcher(htmlContent);
        
        try {
            URL base = new URL(baseUrl);
            String protocol = base.getProtocol();
            String host = base.getHost();
            
            while (matcher.find()) {
                String link = matcher.group(1);
                
                if (link == null || link.trim().isEmpty()) {
                    continue;
                }
                
                // Normalize relative URLs
                if (link.startsWith("/")) {
                    link = protocol + "://" + host + link;
                } else if (link.startsWith("./")) {
                    link = protocol + "://" + host + link.substring(1);
                } else if (!link.startsWith("http://") && !link.startsWith("https://")) {
                    link = protocol + "://" + host + "/" + link;
                }
                
                // Filter out invalid URLs
                if (link.startsWith("http://") || link.startsWith("https://")) {
                    try {
                        new URL(link); // Validate URL format
                        linkSet.add(link);
                    } catch (MalformedURLException e) {
                        logger.debug("Skipping invalid link: {}", link);
                    }
                }
            }
            
        } catch (MalformedURLException e) {
            logger.warn("Invalid base URL for link extraction: {}", baseUrl, e);
        } catch (Exception e) {
            logger.warn("Error extracting links from URL: {}", baseUrl, e);
        }
        
        return new ArrayList<>(linkSet);
    }
}


