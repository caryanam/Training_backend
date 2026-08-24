package com.training.repo;

import com.training.entity.DemoParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemoParticipantRepository extends JpaRepository<DemoParticipant, Long> {
    List<DemoParticipant> findByDemoSessionId(Long demoSessionId);
    Optional<DemoParticipant> findByDemoSessionIdAndLeadId(Long demoSessionId, Long leadId);
    boolean existsByDemoSessionIdAndLeadId(Long demoSessionId, Long leadId);
    void deleteByDemoSessionIdAndLeadId(Long demoSessionId, Long leadId);
}
