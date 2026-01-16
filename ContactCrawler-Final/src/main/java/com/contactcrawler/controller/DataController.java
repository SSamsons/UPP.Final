package com.contactcrawler.controller;

import com.contactcrawler.model.Organization;
import com.contactcrawler.repository.OrganizationRepository;
import com.contactcrawler.service.DataProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/data")
public class DataController {
    
    @Autowired
    private DataProcessingService dataProcessingService;
    
    @Autowired
    private OrganizationRepository organizationRepository;

    @GetMapping("/answer")
    public ResponseEntity<Map<String, Object>> getAnswer(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Organization> organizations;
        
        if (search != null && !search.trim().isEmpty()) {
            organizations = dataProcessingService.searchOrganizations(search, pageable);
        } else {
            organizations = organizationRepository.findAll(pageable);
        }
        
        // Apply sorting using parallelStream
        List<Organization> sortedList = dataProcessingService.processAndSortOrganizations(
            search, sortBy, ascending
        );
        
        // Apply pagination manually
        int start = page * size;
        int end = Math.min(start + size, sortedList.size());
        List<Organization> paginatedList = sortedList.subList(
            Math.min(start, sortedList.size()), 
            end
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", paginatedList);
        response.put("totalElements", sortedList.size());
        response.put("totalPages", (int) Math.ceil((double) sortedList.size() / size));
        response.put("currentPage", page);
        response.put("pageSize", size);
        response.put("search", search);
        response.put("sortBy", sortBy);
        response.put("ascending", ascending);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/companies")
    public ResponseEntity<Map<String, Object>> getCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<Organization> organizations = dataProcessingService.processAndSortOrganizations(
            search, sortBy, ascending
        );
        
        int start = page * size;
        int end = Math.min(start + size, organizations.size());
        List<Organization> paginatedList = organizations.subList(
            Math.min(start, organizations.size()), 
            end
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", paginatedList);
        response.put("totalElements", organizations.size());
        response.put("totalPages", (int) Math.ceil((double) organizations.size() / size));
        response.put("currentPage", page);
        response.put("pageSize", size);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/companies/phone/{phone}")
    public ResponseEntity<Organization> getByPhone(@PathVariable String phone) {
        Optional<Organization> org = organizationRepository.findByPhone(phone);
        return org.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/companies/email/{email}")
    public ResponseEntity<Organization> getByEmail(@PathVariable String email) {
        Optional<Organization> org = organizationRepository.findByEmail(email);
        return org.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
}
