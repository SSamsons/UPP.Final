package com.contactcrawler.controller;

import com.contactcrawler.model.CrawlJob;
import com.contactcrawler.service.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {
    
    @Autowired
    private CrawlerService crawlerService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startCrawling(@RequestBody Set<String> urls) {
        CrawlJob job = crawlerService.startCrawling(urls);
        
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getJobId());
        response.put("status", job.getStatus());
        response.put("message", "Crawling started successfully");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<CrawlJob> getJobStatus(@PathVariable String jobId) {
        CrawlJob job = crawlerService.getJobStatus(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(job);
    }

    @GetMapping("/active-jobs")
    public ResponseEntity<Map<String, CrawlJob>> getActiveJobs() {
        return ResponseEntity.ok(crawlerService.getActiveJobs());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeJobs", crawlerService.getActiveJobs().size());
        stats.put("totalJobs", crawlerService.getActiveJobs().values().size());
        return ResponseEntity.ok(stats);
    }
}
