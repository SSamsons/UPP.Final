package com.contactcrawler.repository;

import com.contactcrawler.model.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    @Query("SELECT DISTINCT o FROM Organization o LEFT JOIN FETCH o.phones LEFT JOIN FETCH o.emails LEFT JOIN FETCH o.addresses WHERE " +
           "LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.website) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "EXISTS (SELECT 1 FROM o.phones p WHERE p LIKE CONCAT('%', :search, '%')) OR " +
           "EXISTS (SELECT 1 FROM o.emails e WHERE e LIKE CONCAT('%', :search, '%')) OR " +
           "EXISTS (SELECT 1 FROM o.addresses a WHERE LOWER(a) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Organization> searchOrganizations(@Param("search") String search, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Organization o LEFT JOIN FETCH o.phones LEFT JOIN FETCH o.emails WHERE :phone MEMBER OF o.phones")
    Optional<Organization> findByPhone(@Param("phone") String phone);

    @Query("SELECT DISTINCT o FROM Organization o LEFT JOIN FETCH o.phones LEFT JOIN FETCH o.emails WHERE :email MEMBER OF o.emails")
    Optional<Organization> findByEmail(@Param("email") String email);

    List<Organization> findByNameContainingIgnoreCase(String name);

    Page<Organization> findAllByOrderByNameAsc(Pageable pageable);
    
    Page<Organization> findAllByOrderByCrawledAtDesc(Pageable pageable);
}
