package com.training.repo;

import com.training.entity.Lecture;
import com.training.entity.LiveLectureSession;
import com.training.enums.LiveSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiveLectureSessionRepository extends JpaRepository<LiveLectureSession, Long> {
    Optional<LiveLectureSession> findFirstByLectureAndStatus(Lecture lecture, LiveSessionStatus status);
    Optional<LiveLectureSession> findFirstByLectureOrderByCreatedAtDesc(Lecture lecture);
    Optional<LiveLectureSession> findByRoomName(String roomName);
    List<LiveLectureSession> findByLecture(Lecture lecture);
    boolean existsByLectureAndStatus(Lecture lecture, LiveSessionStatus status);
}
