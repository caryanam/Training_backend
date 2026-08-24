package com.training.entity;

import com.training.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "demo_participants", uniqueConstraints = {
    @UniqueConstraint(name = "uk_session_lead", columnNames = {"demo_session_id", "lead_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demo_session_id", nullable = false)
    private DemoSession demoSession;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lead_id", nullable = false)
    private StudentLead lead;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AttendanceStatus attendanceStatus = AttendanceStatus.NOT_MARKED;

    private LocalDateTime joinedAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.attendanceStatus == null) {
            this.attendanceStatus = AttendanceStatus.NOT_MARKED;
        }
    }
}
