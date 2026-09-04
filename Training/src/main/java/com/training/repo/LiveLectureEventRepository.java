package com.training.repo;

import com.training.entity.LiveLectureEvent;
import com.training.entity.LiveLectureSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveLectureEventRepository extends JpaRepository<LiveLectureEvent, Long> {
    List<LiveLectureEvent> findBySessionOrderByCreatedAtDesc(LiveLectureSession session);
    List<LiveLectureEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);
}
