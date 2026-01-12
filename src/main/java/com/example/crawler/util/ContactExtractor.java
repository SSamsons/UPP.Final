package com.example.crawler.util;

import com.example.crawler.model.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ContactExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(ContactExtractor.class);
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(\\+7|8)[\\s\\-\\(\\)]?\\d{3}[\\s\\-\\(\\)]?\\d{3}[\\s\\-\\(\\)]?\\d{2}[\\s\\-\\(\\)]?\\d{2}"
    );
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b"
    );
    
    private static final Pattern COMPANY_NAME_PATTERN = Pattern.compile(
        "<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ADDRESS_PATTERN_RU = Pattern.compile(
        "г\\.\\s*[А-Яа-я]+[,]?\\s*ул\\.\\s*[А-Яа-я]+[,]?\\s*д\\.\\s*\\d+"
    );
    
    private static final Pattern ADDRESS_PATTERN_EN = Pattern.compile(
        "\\d+\\s+[A-Za-z0-9\\s]+(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Lane|Ln|Drive|Dr)[,\\s]+[A-Za-z\\s]+,\\s*[A-Z]{2}\\s+\\d{5}"
    );
    
    public Company extractContacts(String htmlContent, String sourceUrl) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            logger.debug("Empty HTML content provided for URL: {}", sourceUrl);
            return null;
        }
        
        if (sourceUrl == null || sourceUrl.trim().isEmpty()) {
            logger.warn("Source URL is null or empty");
            return null;
        }
        
        try {
            String companyName = extractCompanyName(htmlContent);
            List<String> phones = extractPhones(htmlContent);
            List<String> emails = extractEmails(htmlContent);
            String address = extractAddress(htmlContent);
            
            if (companyName == null || companyName.trim().isEmpty()) {
                companyName = generateDefaultCompanyName(sourceUrl);
            }
            
            String website = extractWebsite(sourceUrl);
            if (website == null || website.trim().isEmpty()) {
                logger.warn("Could not extract website from URL: {}", sourceUrl);
                website = sourceUrl;
            }
            
            return new Company(companyName, website, phones, emails, address, sourceUrl);
        } catch (Exception e) {
            logger.error("Error extracting contacts from URL: {}", sourceUrl, e);
            return null;
        }
    }
    
    private String extractCompanyName(String html) {
        if (html == null) {
            return null;
        }
        
        try {
            Matcher matcher = COMPANY_NAME_PATTERN.matcher(html);
            if (matcher.find()) {
                String name = matcher.group(1).trim();
                // Remove common suffixes that might be in title tags
                name = name.replaceAll("\\s*[-|]\\s*.*$", "").trim();
                return name.length() > 0 ? name : null;
            }
        } catch (Exception e) {
            logger.debug("Error extracting company name", e);
        }
        return null;
    }
    
    private List<String> extractPhones(String html) {
        if (html == null) {
            return new ArrayList<>();
        }
        
        java.util.Set<String> phoneSet = new java.util.LinkedHashSet<>();
        try {
            Matcher matcher = PHONE_PATTERN.matcher(html);
            
            while (matcher.find()) {
                String phone = normalizePhone(matcher.group());
                if (isValidPhone(phone)) {
                    phoneSet.add(phone);
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting phones", e);
        }
        
        return new ArrayList<>(phoneSet);
    }
    
    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        // Remove all non-digit characters except +
        String normalized = phone.replaceAll("[\\s\\-\\(\\)]", "");
        // Normalize +7 and 8 prefixes
        if (normalized.startsWith("8") && normalized.length() == 11) {
            normalized = "+7" + normalized.substring(1);
        }
        return normalized;
    }
    
    private boolean isValidPhone(String phone) {
        if (phone == null || phone.length() < 10) {
            return false;
        }
        // Russian phone: +7XXXXXXXXXX or 8XXXXXXXXXX
        return phone.matches("(\\+7|8)\\d{10}");
    }
    
    private List<String> extractEmails(String html) {
        if (html == null) {
            return new ArrayList<>();
        }
        
        java.util.Set<String> emailSet = new java.util.LinkedHashSet<>();
        try {
            Matcher matcher = EMAIL_PATTERN.matcher(html);
            
            while (matcher.find()) {
                String email = matcher.group().toLowerCase().trim();
                if (isValidEmail(email)) {
                    emailSet.add(email);
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting emails", e);
        }
        
        return new ArrayList<>(emailSet);
    }
    
    private boolean isValidEmail(String email) {
        if (email == null || email.length() < 5) {
            return false;
        }
        // Basic email validation
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$") &&
               !email.startsWith(".") && 
               !email.contains("..") &&
               email.indexOf("@") > 0 &&
               email.indexOf("@") < email.length() - 1;
    }
    
    private String extractAddress(String html) {
        if (html == null) {
            return null;
        }
        
        try {
            // Try Russian address pattern first
            Matcher matcherRu = ADDRESS_PATTERN_RU.matcher(html);
            if (matcherRu.find()) {
                return matcherRu.group().trim();
            }
            
            // Try English address pattern
            Matcher matcherEn = ADDRESS_PATTERN_EN.matcher(html);
            if (matcherEn.find()) {
                return matcherEn.group().trim();
            }
        } catch (Exception e) {
            logger.debug("Error extracting address", e);
        }
        
        return null;
    }
    
    private String extractWebsite(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            java.net.URL uri = new java.net.URL(url);
            String protocol = uri.getProtocol();
            String host = uri.getHost();
            
            if (protocol != null && host != null) {
                return protocol + "://" + host;
            }
        } catch (java.net.MalformedURLException e) {
            logger.debug("Malformed URL: {}", url, e);
        } catch (Exception e) {
            logger.debug("Error extracting website from URL: {}", url, e);
        }
        
        return url;
    }
    
    private String generateDefaultCompanyName(String sourceUrl) {
        try {
            java.net.URL url = new java.net.URL(sourceUrl);
            String host = url.getHost();
            if (host != null && host.startsWith("www.")) {
                host = host.substring(4);
            }
            return "Company from " + host;
        } catch (Exception e) {
            return "Unknown Company from " + sourceUrl;
        }
    }
}


