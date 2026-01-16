package com.contactcrawler.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class LinkExtractor {
    
    public static Set<String> extractLinks(String html, String baseUrl) {
        Set<String> links = new HashSet<>();
        
        if (html == null || html.isEmpty()) {
            return links;
        }

        try {
            Document doc = Jsoup.parse(html, baseUrl);
            Elements anchorTags = doc.select("a[href]");
            
            for (Element anchor : anchorTags) {
                String href = anchor.attr("abs:href");
                if (href != null && !href.isEmpty()) {
                    try {
                        URL url = new URL(href);
                        // Only HTTP/HTTPS links
                        if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
                            // Remove fragments
                            String cleanUrl = url.getProtocol() + "://" + url.getHost();
                            if (url.getPort() != -1 && url.getPort() != 80 && url.getPort() != 443) {
                                cleanUrl += ":" + url.getPort();
                            }
                            if (url.getPath() != null && !url.getPath().isEmpty()) {
                                cleanUrl += url.getPath();
                            }
                            if (url.getQuery() != null && !url.getQuery().isEmpty()) {
                                cleanUrl += "?" + url.getQuery();
                            }
                            links.add(cleanUrl);
                        }
                    } catch (MalformedURLException e) {
                        // Skip invalid URLs
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting links: " + e.getMessage());
        }
        
        return links;
    }
}
