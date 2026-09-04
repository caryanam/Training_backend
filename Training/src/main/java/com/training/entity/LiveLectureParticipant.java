package com.training.entity;

import com.training.enums.ParticipantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_lecture_participants", indexes = {
        @Index(name = "idx_live_part_session", columnList = "session_id"),
        @Index(name = "idx_live_part_student", columnList = "student_user_id"),
        @Index(name = "idx_live_part_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveLectureParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private LiveLectureSession session;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    private String studentName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    private LocalDateTime lastHeartbeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ParticipantStatus status = ParticipantStatus.ACTIVE;

    @Column(length = 255)
    private String sessionTokenHash;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = ParticipantStatus.ACTIVE;
        }
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
        if (this.lastHeartbeat == null) {
            this.lastHeartbeat = LocalDateTime.now();
        }
    }
}
