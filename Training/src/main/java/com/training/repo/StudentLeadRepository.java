package com.training.repo;

import com.training.entity.StudentLead;
import com.training.enums.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentLeadRepository extends JpaRepository<StudentLead, Long> {
    Optional<StudentLead> findByLeadCode(String leadCode);

    long countByStatus(LeadStatus status);

    @Query("SELECT l FROM StudentLead l WHERE " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:search IS NULL OR LOWER(l.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(l.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(l.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<StudentLead> findLeadsWithFilters(@Param("status") LeadStatus status, @Param("search") String search);
}
