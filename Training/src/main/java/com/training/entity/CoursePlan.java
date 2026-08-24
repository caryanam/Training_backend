package com.training.entity;

import com.training.enums.PlanDuration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "course_plans",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "duration"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private PlanDuration duration;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Builder.Default
    private String currency = "INR";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
