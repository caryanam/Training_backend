package com.training.entity;

import com.training.enums.DemoStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demo_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String demoCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    private Course course;

    private String courseName;

    private LocalDate demoDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalTime demoTime;

    private String meetLink;

    private String meetingLink;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DemoStatus status = DemoStatus.SCHEDULED;

    private String createdBy;

    @OneToMany(mappedBy = "demoSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DemoParticipant> participants = new ArrayList<>();

    private String feedback;

    private Boolean markInterested;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = DemoStatus.SCHEDULED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
