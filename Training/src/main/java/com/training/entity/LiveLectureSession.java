package com.training.entity;

import com.training.enums.LiveSessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_lecture_sessions", indexes = {
        @Index(name = "idx_live_sess_lecture", columnList = "lecture_id"),
        @Index(name = "idx_live_sess_status", columnList = "status"),
        @Index(name = "idx_live_sess_room", columnList = "room_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveLectureSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private LiveSessionStatus status = LiveSessionStatus.SCHEDULED;

    @Column(name = "room_name", nullable = false, unique = true, length = 150)
    private String roomName;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "started_by_user_id", nullable = false)
    private User startedBy;

    @Column(nullable = false)
    @Builder.Default
    private Integer participantCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = LiveSessionStatus.SCHEDULED;
        }
        if (this.participantCount == null) {
            this.participantCount = 0;
        }
    }
}
