package com.contactcrawler.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class CrawlJob {
    private String jobId;
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private int totalPages;
    private int processedPages;
    private int foundOrganizations;
    private String errorMessage;

    public enum JobStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    public CrawlJob() {
        this.jobId = UUID.randomUUID().toString();
        this.status = JobStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.totalPages = 0;
        this.processedPages = 0;
        this.foundOrganizations = 0;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getProcessedPages() {
        return processedPages;
    }

    public void setProcessedPages(int processedPages) {
        this.processedPages = processedPages;
    }

    public int getFoundOrganizations() {
        return foundOrganizations;
    }

    public void setFoundOrganizations(int foundOrganizations) {
        this.foundOrganizations = foundOrganizations;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
