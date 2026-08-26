package com.training.repo;

import com.training.entity.FollowupReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowupReportRepo extends JpaRepository<FollowupReport, Long> {
    List<FollowupReport> findByLeadIdOrderByCreatedAtDesc(Long leadId);
    List<FollowupReport> findByLeadStudentIdOrderByCreatedAtDesc(Long studentId);
}
