package com.training.repo;

import com.training.entity.DemoSession;
import com.training.enums.DemoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DemoSessionRepository extends JpaRepository<DemoSession, Long> {
    Optional<DemoSession> findByDemoCode(String demoCode);

    @Query("SELECT DISTINCT d FROM DemoSession d LEFT JOIN FETCH d.participants p WHERE " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:date IS NULL OR d.demoDate = :date) ORDER BY d.demoDate DESC, d.startTime DESC")
    List<DemoSession> findSessionsWithFilters(
            @Param("status") DemoStatus status,
            @Param("date") LocalDate date);

    @Query("SELECT DISTINCT d FROM DemoSession d JOIN d.participants p WHERE " +
           "p.lead.id = :leadId AND " +
           "d.status IN ('SCHEDULED', 'RESCHEDULED') AND " +
           "d.demoDate >= CURRENT_DATE ORDER BY d.demoDate ASC, d.startTime ASC")
    List<DemoSession> findUpcomingSessionsForStudent(@Param("leadId") Long leadId);

    @Query("SELECT DISTINCT d FROM DemoSession d JOIN d.participants p WHERE " +
           "p.lead.id = :leadId AND " +
           "(d.status IN ('COMPLETED', 'CANCELLED') OR d.demoDate < CURRENT_DATE) ORDER BY d.demoDate DESC")
    List<DemoSession> findSessionHistoryForStudent(@Param("leadId") Long leadId);
}
