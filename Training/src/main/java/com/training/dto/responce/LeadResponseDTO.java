package com.training.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadResponseDTO {
    private Long id;
    private String leadId;
    private String studentId;
    private String profileId;
    private String fullName;
    private String email;
    private String phone;
    private String interestedCourse;
    private String education;
    private String city;
    private String status;
    private String assignedExecutor;
    private String assignedExecutorId;
    private String assignedExecutorEmail;
    private LocalDate followupDate;
    private LocalDateTime lastActivity;
    private LocalDateTime createdAt;
    private String enrollmentStatus;
    private String enrollmentId;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private String courseValidity;
}

