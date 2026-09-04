package com.training.entity;

import com.training.enums.SecurityEventSeverity;
import com.training.enums.SecurityEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_security_events", indexes = {
        @Index(name = "idx_sec_evt_session", columnList = "session_id"),
        @Index(name = "idx_sec_evt_lecture", columnList = "lecture_id"),
        @Index(name = "idx_sec_evt_student", columnList = "student_id"),
        @Index(name = "idx_sec_evt_type", columnList = "event_type"),
        @Index(name = "idx_sec_evt_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureSecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private LiveLectureSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "student_identifier")
    private String studentIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SecurityEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private SecurityEventSeverity severity;

    @Column(length = 2000)
    private String metadata;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
