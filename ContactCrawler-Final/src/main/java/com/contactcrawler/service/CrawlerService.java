package com.contactcrawler.service;

import com.contactcrawler.client.FeignHtmlClient;
import com.contactcrawler.client.RestTemplateService;
import com.contactcrawler.client.WebClientService;
import com.contactcrawler.model.CrawlJob;
import com.contactcrawler.model.Organization;
import com.contactcrawler.repository.OrganizationRepository;
import com.contactcrawler.util.ContactParser;
import com.contactcrawler.util.LinkExtractor;
import com.contactcrawler.util.TracingUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CrawlerService {
    
    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);
    
    @Autowired
    @Qualifier("crawlerExecutorService")
    private ExecutorService executorService;
    
    @Autowired
    private WebClientService webClientService;
    
    @Autowired
    private RestTemplateService restTemplateService;
    
    @Autowired
    private FeignHtmlClient feignHtmlClient;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private Timer parsingTimer;
    
    @Autowired
    private Timer htmlFetchTimer;
    
    @Autowired
    private Timer databaseSaveTimer;
    
    @Autowired
    private Counter parsingSuccessCounter;
    
    @Autowired
    private Counter parsingErrorCounter;
    
    @Autowired
    private Counter databaseRecordsCounter;
    
    @Autowired
    private Counter pagesCrawledCounter;
    
    @Autowired
    private Counter urlsVisitedCounter;
    
    @Autowired
    private TracingUtil tracingUtil;
    
    @Value("${crawler.max.depth:3}")
    private int maxDepth;
    
    @Value("${crawler.max.pages:200}")
    private int maxPages;
    
    private final ConcurrentHashMap<String, CrawlJob> activeJobs = new ConcurrentHashMap<>();

    public CrawlJob startCrawling(Set<String> startUrls) {
        CrawlJob job = new CrawlJob();
        job.setStatus(CrawlJob.JobStatus.RUNNING);
        activeJobs.put(job.getJobId(), job);
        
        // Reset state for this job
        ConcurrentHashMap<String, Boolean> jobVisitedUrls = new ConcurrentHashMap<>();
        BlockingQueue<String> jobUrlQueue = new LinkedBlockingQueue<>();
        AtomicInteger jobProcessedCount = new AtomicInteger(0);
        AtomicInteger jobFoundOrganizations = new AtomicInteger(0);
        
        // Add start URLs to queue
        for (String url : startUrls) {
            jobUrlQueue.offer(url);
        }
        
        job.setTotalPages(jobUrlQueue.size());
        
        // Start crawling in background with multiple threads
        for (int i = 0; i < Math.min(startUrls.size(), 5); i++) {
            executorService.submit(() -> {
                try {
                    crawlUrls(job, jobVisitedUrls, jobUrlQueue, jobProcessedCount, jobFoundOrganizations, 0);
                } catch (Exception e) {
                    logger.error("Error in crawler thread: {}", e.getMessage(), e);
                }
            });
        }
        
        // Monitor job completion
        executorService.submit(() -> {
            try {
                while (job.getStatus() == CrawlJob.JobStatus.RUNNING) {
                    Thread.sleep(1000);
                    job.setProcessedPages(jobProcessedCount.get());
                    job.setFoundOrganizations(jobFoundOrganizations.get());
                    
                    if (jobUrlQueue.isEmpty() && jobProcessedCount.get() >= job.getTotalPages()) {
                        job.setStatus(CrawlJob.JobStatus.COMPLETED);
                        job.setCompletedAt(java.time.LocalDateTime.now());
                        job.setProcessedPages(jobProcessedCount.get());
                        job.setFoundOrganizations(jobFoundOrganizations.get());
                        break;
                    }
                }
            } catch (Exception e) {
                job.setStatus(CrawlJob.JobStatus.FAILED);
                job.setErrorMessage(e.getMessage());
                job.setCompletedAt(java.time.LocalDateTime.now());
                logger.error("Job monitoring failed: {}", e.getMessage(), e);
            }
        });
        
        return job;
    }

    private void crawlUrls(CrawlJob job, 
                          ConcurrentHashMap<String, Boolean> visitedUrls,
                          BlockingQueue<String> urlQueue,
                          AtomicInteger processedCount,
                          AtomicInteger foundOrganizations,
                          int currentDepth) {
        while (currentDepth <= maxDepth && processedCount.get() < maxPages && job.getStatus() == CrawlJob.JobStatus.RUNNING) {
            String url = urlQueue.poll();
            if (url == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            if (visitedUrls.containsKey(url)) {
                continue;
            }

            visitedUrls.put(url, true);
            processedCount.incrementAndGet();
            pagesCrawledCounter.increment();
            urlsVisitedCounter.increment();

            try {
                // Trace HTML fetch with fallback chain
                String html = tracingUtil.trace("download_webpage_content", () -> 
                    htmlFetchTimer.recordCallable(() -> {
                        String content = webClientService.fetchHtml(url).block();
                        
                        // Fallback to RestTemplate if WebClient fails
                        if (content == null || content.isEmpty()) {
                            content = restTemplateService.fetchHtml(url);
                        }
                        
                        // Fallback to Feign if RestTemplate fails
                        if (content == null || content.isEmpty()) {
                            try {
                                content = feignHtmlClient.fetchHtml(url);
                            } catch (Exception e) {
                                logger.debug("Feign client failed for URL: {}", url, e);
                            }
                        }
                        
                        return content != null ? content : "";
                    })
                );

                if (html != null && !html.isEmpty()) {
                    // Trace parsing
                    ContactParser.ContactData contactData = tracingUtil.trace("extract_contact_information", () -> 
                        parsingTimer.recordCallable(() -> 
                            ContactParser.extractContacts(html, url)
                        )
                    );
                    
                    // Save organization if contacts found
                    if (!contactData.getEmails().isEmpty() || 
                        !contactData.getPhones().isEmpty() || 
                        !contactData.getAddresses().isEmpty()) {
                        
                        parsingSuccessCounter.increment();
                        
                        tracingUtil.trace("persist_organization_data", () -> {
                            Organization org = new Organization();
                            org.setName(contactData.getOrganizationName() != null ? 
                                       contactData.getOrganizationName() : 
                                       extractDomainName(url));
                            org.setWebsite(url);
                            org.setSourceUrl(url);
                            org.setEmails(contactData.getEmails());
                            org.setPhones(contactData.getPhones());
                            org.setAddresses(contactData.getAddresses());
                            org.setDescription(contactData.getDescription());
                            
                            saveOrganizationIfNew(org);
                            foundOrganizations.incrementAndGet();
                        });
                    } else {
                        parsingErrorCounter.increment();
                    }
                    
                    // Trace link extraction
                    Set<String> links = tracingUtil.trace("discover_page_links", () -> 
                        LinkExtractor.extractLinks(html, url)
                    );
                    
                    // Extract links for next depth level
                    if (currentDepth < maxDepth) {
                        for (String link : links) {
                            if (!visitedUrls.containsKey(link) && urlQueue.size() < maxPages) {
                                urlQueue.offer(link);
                            }
                        }
                        job.setTotalPages(Math.max(job.getTotalPages(), urlQueue.size() + processedCount.get()));
                    }
                } else {
                    parsingErrorCounter.increment();
                    logger.warn("Empty HTML content retrieved for URL: {}", url);
                }
                
                // Rate limiting
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Exception e) {
                parsingErrorCounter.increment();
                logger.warn("Error crawling URL: {} - {}", url, e.getMessage(), e);
            }
        }
    }

    @Transactional
    private void saveOrganizationIfNew(Organization org) {
        databaseSaveTimer.record(() -> {
            Optional<Organization> existing = organizationRepository.findByPhone(
                org.getPhones().isEmpty() ? "" : org.getPhones().iterator().next()
            );
            if (existing.isEmpty() || !existing.get().getWebsite().equals(org.getWebsite())) {
                organizationRepository.save(org);
                databaseRecordsCounter.increment();
                logger.info("Saved organization: {}", org.getName());
            }
        });
    }

    private String extractDomainName(String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            String host = urlObj.getHost();
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public CrawlJob getJobStatus(String jobId) {
        return activeJobs.get(jobId);
    }

    public ConcurrentHashMap<String, CrawlJob> getActiveJobs() {
        return activeJobs;
    }
}
