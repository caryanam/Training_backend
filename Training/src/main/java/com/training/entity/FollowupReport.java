package com.training.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "followup_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowupReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private StudentLead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executor_id", nullable = false)
    private Executer executor;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false)
    private Boolean interested;

    private LocalDate expectedJoiningDate;

    @Column(columnDefinition = "TEXT")
    private String demoDiscussion;

    @Column(columnDefinition = "TEXT")
    private String projectCapability;

    @Column(columnDefinition = "TEXT")
    private String additionalComments;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
