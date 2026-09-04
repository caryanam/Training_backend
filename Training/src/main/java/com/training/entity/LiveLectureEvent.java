package com.training.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_lecture_events", indexes = {
        @Index(name = "idx_live_evt_session", columnList = "session_id"),
        @Index(name = "idx_live_evt_type", columnList = "event_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveLectureEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private LiveLectureSession session;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    private String actorEmail;

    private String actorRole;

    @Column(length = 2000)
    private String details;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
