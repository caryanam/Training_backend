package com.training.repo;

import com.training.entity.LiveLectureParticipant;
import com.training.entity.LiveLectureSession;
import com.training.entity.User;
import com.training.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LiveLectureParticipantRepository extends JpaRepository<LiveLectureParticipant, Long> {
    Optional<LiveLectureParticipant> findFirstBySessionAndStudentAndStatus(
            LiveLectureSession session, User student, ParticipantStatus status);

    List<LiveLectureParticipant> findBySessionAndStudent(LiveLectureSession session, User student);

    List<LiveLectureParticipant> findBySessionAndStatus(LiveLectureSession session, ParticipantStatus status);

    long countBySessionAndStatus(LiveLectureSession session, ParticipantStatus status);

    List<LiveLectureParticipant> findByStatusAndLastHeartbeatBefore(
            ParticipantStatus status, LocalDateTime threshold);
}
