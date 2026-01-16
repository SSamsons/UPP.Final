package com.contactcrawler.service;

import com.contactcrawler.model.Organization;
import com.contactcrawler.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DataProcessingService {
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    @Qualifier("dataProcessingForkJoinPool")
    private ForkJoinPool forkJoinPool;

    public List<Organization> processAndSortOrganizations(String searchTerm, String sortBy, boolean ascending) {
        List<Organization> allOrganizations = organizationRepository.findAll();
        
        Stream<Organization> stream = allOrganizations.parallelStream();
        
        // Filter by search term if provided
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String search = searchTerm.toLowerCase();
            stream = stream.filter(org -> 
                (org.getName() != null && org.getName().toLowerCase().contains(search)) ||
                (org.getWebsite() != null && org.getWebsite().toLowerCase().contains(search)) ||
                org.getPhones().stream().anyMatch(p -> p.contains(search)) ||
                org.getEmails().stream().anyMatch(e -> e.toLowerCase().contains(search)) ||
                org.getAddresses().stream().anyMatch(a -> a.toLowerCase().contains(search))
            );
        }
        
        // Sort
        Comparator<Organization> comparator = getComparator(sortBy);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        stream = stream.sorted(comparator);
        
        return stream.collect(Collectors.toList());
    }

    public List<Organization> processWithForkJoinPool(String searchTerm) {
        List<Organization> allOrganizations = organizationRepository.findAll();
        
        return forkJoinPool.submit(() -> 
            allOrganizations.parallelStream()
                .filter(org -> {
                    if (searchTerm == null || searchTerm.trim().isEmpty()) {
                        return true;
                    }
                    String search = searchTerm.toLowerCase();
                    return (org.getName() != null && org.getName().toLowerCase().contains(search)) ||
                           (org.getWebsite() != null && org.getWebsite().toLowerCase().contains(search)) ||
                           org.getPhones().stream().anyMatch(p -> p.contains(search)) ||
                           org.getEmails().stream().anyMatch(e -> e.toLowerCase().contains(search)) ||
                           org.getAddresses().stream().anyMatch(a -> a.toLowerCase().contains(search));
                })
                .sorted(Comparator.comparing(Organization::getName))
                .collect(Collectors.toList())
        ).join();
    }

    private Comparator<Organization> getComparator(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            return Comparator.comparing(Organization::getName, Comparator.nullsLast(String::compareTo));
        }
        
        switch (sortBy.toLowerCase()) {
            case "name":
                return Comparator.comparing(Organization::getName, Comparator.nullsLast(String::compareTo));
            case "website":
                return Comparator.comparing(Organization::getWebsite, Comparator.nullsLast(String::compareTo));
            case "crawledat":
            case "crawled_at":
                return Comparator.comparing(Organization::getCrawledAt, Comparator.nullsLast(java.time.LocalDateTime::compareTo));
            default:
                return Comparator.comparing(Organization::getName, Comparator.nullsLast(String::compareTo));
        }
    }

    public Page<Organization> searchOrganizations(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return organizationRepository.findAll(pageable);
        }
        return organizationRepository.searchOrganizations(search, pageable);
    }
}
