package com.training.repo;

import com.training.entity.Lecture;
import com.training.entity.LectureSecurityEvent;
import com.training.entity.LiveLectureSession;
import com.training.entity.User;
import com.training.enums.SecurityEventSeverity;
import com.training.enums.SecurityEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LectureSecurityEventRepository extends JpaRepository<LectureSecurityEvent, Long> {

    List<LectureSecurityEvent> findBySessionOrderByTimestampDesc(LiveLectureSession session);

    List<LectureSecurityEvent> findByLectureOrderByTimestampDesc(Lecture lecture);

    @Query("SELECT e FROM LectureSecurityEvent e WHERE e.lecture = :lecture AND e.timestamp >= :since ORDER BY e.timestamp DESC")
    List<LectureSecurityEvent> findRecentEventsByLecture(
            @Param("lecture") Lecture lecture,
            @Param("since") LocalDateTime since
    );

    long countByLectureAndStudentAndSeverityIn(
            Lecture lecture,
            User student,
            List<SecurityEventSeverity> severities
    );

    long countByLectureAndStudentAndEventType(
            Lecture lecture,
            User student,
            SecurityEventType eventType
    );

    Optional<LectureSecurityEvent> findFirstByLectureAndStudentAndEventTypeOrderByTimestampDesc(
            Lecture lecture,
            User student,
            SecurityEventType eventType
    );
}
