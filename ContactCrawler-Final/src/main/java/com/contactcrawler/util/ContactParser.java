package com.contactcrawler.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ContactParser {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(\\+?[0-9]{1,3}[-\\s]?)?\\(?[0-9]{1,4}\\)?[-\\s]?[0-9]{1,4}[-\\s]?[0-9]{1,9}"
    );

    public static ContactData extractContacts(String html, String baseUrl) {
        ContactData data = new ContactData();
        
        if (html == null || html.isEmpty()) {
            return data;
        }

        try {
            Document doc = Jsoup.parse(html, baseUrl);
            
            // Extract emails
            String text = doc.text();
            java.util.regex.Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
            while (emailMatcher.find()) {
                String email = emailMatcher.group().toLowerCase().trim();
                if (email.length() < 50) { // Sanity check
                    data.getEmails().add(email);
                }
            }
            
            // Extract emails from href attributes
            Elements emailLinks = doc.select("a[href^=mailto:]");
            for (Element link : emailLinks) {
                String href = link.attr("href");
                if (href.startsWith("mailto:")) {
                    String email = href.substring(7).split("[?]")[0].toLowerCase().trim();
                    if (EMAIL_PATTERN.matcher(email).matches()) {
                        data.getEmails().add(email);
                    }
                }
            }
            
            // Extract phones
            java.util.regex.Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
            while (phoneMatcher.find()) {
                String phone = normalizePhone(phoneMatcher.group());
                if (phone.length() >= 7 && phone.length() <= 20) {
                    data.getPhones().add(phone);
                }
            }
            
            // Extract phones from href attributes
            Elements phoneLinks = doc.select("a[href^=tel:]");
            for (Element link : phoneLinks) {
                String href = link.attr("href");
                if (href.startsWith("tel:")) {
                    String phone = normalizePhone(href.substring(4).split("[?]")[0]);
                    if (phone.length() >= 7) {
                        data.getPhones().add(phone);
                    }
                }
            }
            
            // Extract addresses (simple heuristic - look for common address keywords)
            Elements addressElements = doc.select("address, [class*='address'], [class*='contact'], [id*='address']");
            for (Element elem : addressElements) {
                String address = elem.text().trim();
                if (address.length() > 10 && address.length() < 500) {
                    if (containsAddressKeywords(address)) {
                        data.getAddresses().add(address);
                    }
                }
            }
            
            // Extract organization name
            String title = doc.title();
            if (title != null && !title.isEmpty()) {
                data.setOrganizationName(extractOrganizationName(title));
            }
            
            // Extract description
            Elements metaDescriptions = doc.select("meta[name=description]");
            if (!metaDescriptions.isEmpty()) {
                data.setDescription(metaDescriptions.first().attr("content"));
            }
            
        } catch (Exception e) {
            // Log error but continue
            System.err.println("Error parsing HTML: " + e.getMessage());
        }
        
        return data;
    }

    private static String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9+]", "").trim();
    }

    private static boolean containsAddressKeywords(String text) {
        String lower = text.toLowerCase();
        return lower.contains("ул.") || lower.contains("улица") || 
               lower.contains("пр.") || lower.contains("проспект") ||
               lower.contains("д.") || lower.contains("дом") ||
               lower.contains("г.") || lower.contains("город") ||
               lower.contains("street") || lower.contains("avenue") ||
               lower.contains("road") || lower.contains("city");
    }

    private static String extractOrganizationName(String title) {
        // Remove common suffixes
        return title.replaceAll("\\s*[-|]\\s*.*$", "")
                   .replaceAll("\\s*—\\s*.*$", "")
                   .trim();
    }

    public static class ContactData {
        private Set<String> emails = new HashSet<>();
        private Set<String> phones = new HashSet<>();
        private Set<String> addresses = new HashSet<>();
        private String organizationName;
        private String description;

        public Set<String> getEmails() {
            return emails;
        }

        public Set<String> getPhones() {
            return phones;
        }

        public Set<String> getAddresses() {
            return addresses;
        }

        public String getOrganizationName() {
            return organizationName;
        }

        public void setOrganizationName(String organizationName) {
            this.organizationName = organizationName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
