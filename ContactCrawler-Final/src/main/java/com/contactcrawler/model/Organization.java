package com.contactcrawler.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_website", columnList = "website"),
    @Index(name = "idx_name", columnList = "name"),
    @Index(name = "idx_crawled_at", columnList = "crawled_at")
})
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String website;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "organization_phones", 
                     joinColumns = @JoinColumn(name = "organization_id"),
                     indexes = @Index(name = "idx_phone", columnList = "phone"))
    @Column(name = "phone")
    private Set<String> phones = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "organization_emails", 
                     joinColumns = @JoinColumn(name = "organization_id"),
                     indexes = @Index(name = "idx_email", columnList = "email"))
    @Column(name = "email")
    private Set<String> emails = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "organization_addresses", 
                     joinColumns = @JoinColumn(name = "organization_id"))
    @Column(name = "address", length = 500)
    private Set<String> addresses = new HashSet<>();

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;

    @Column(name = "description", length = 2000)
    private String description;

    public Organization() {
        this.crawledAt = LocalDateTime.now();
    }

    public Organization(String name, String website) {
        this();
        this.name = name;
        this.website = website;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Set<String> getPhones() {
        return phones;
    }

    public void setPhones(Set<String> phones) {
        this.phones = phones;
    }

    public Set<String> getEmails() {
        return emails;
    }

    public void setEmails(Set<String> emails) {
        this.emails = emails;
    }

    public Set<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<String> addresses) {
        this.addresses = addresses;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public LocalDateTime getCrawledAt() {
        return crawledAt;
    }

    public void setCrawledAt(LocalDateTime crawledAt) {
        this.crawledAt = crawledAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
