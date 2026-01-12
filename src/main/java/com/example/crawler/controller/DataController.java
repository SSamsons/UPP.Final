package com.example.crawler.controller;

import com.example.crawler.model.Company;
import com.example.crawler.service.DataProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataController {
    
    private final DataProcessingService dataProcessingService;
    
    public DataController(DataProcessingService dataProcessingService) {
        this.dataProcessingService = dataProcessingService;
    }
    
    @GetMapping("/companies")
    public ResponseEntity<List<Company>> getCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "true") boolean ascending) {
        
        List<Company> companies = dataProcessingService.filterAndSortCompanies(search, sortBy, ascending);
        return ResponseEntity.ok(companies);
    }
    
    @GetMapping("/companies/phone/{phone}")
    public ResponseEntity<List<Company>> getCompaniesByPhone(@PathVariable String phone) {
        List<Company> companies = dataProcessingService.findCompaniesByPhone(phone);
        return ResponseEntity.ok(companies);
    }
    
    @GetMapping("/companies/email/{email}")
    public ResponseEntity<List<Company>> getCompaniesByEmail(@PathVariable String email) {
        List<Company> companies = dataProcessingService.findCompaniesByEmail(email);
        return ResponseEntity.ok(companies);
    }
    
    @GetMapping("/answer")
    public ResponseEntity<Map<String, Object>> getAnswer(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 10;
        }
        
        List<Company> companies = dataProcessingService.filterAndSortCompanies(search, "name", true);
        
        int total = companies.size();
        int start = page * size;
        int end = Math.min(start + size, total);
        
        List<Company> pageContent;
        if (start >= total || start < 0) {
            pageContent = new java.util.ArrayList<>();
        } else {
            pageContent = companies.subList(start, end);
        }
        
        int totalPages = total > 0 ? (int) Math.ceil((double) total / size) : 0;
        
        Map<String, Object> response = Map.of(
            "content", pageContent,
            "page", page,
            "size", size,
            "total", total,
            "totalPages", totalPages
        );
        
        return ResponseEntity.ok(response);
    }
}


