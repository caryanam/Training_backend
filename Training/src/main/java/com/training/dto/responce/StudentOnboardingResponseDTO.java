package com.training.dto.responce;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentOnboardingResponseDTO {
    private String onboardingCode;
    private String studentId;
    private String profileId;
    private String leadId;
    private String fullName;
    private String email;
    private String phone;
    private String courseId;
    private String courseName;
    private Long planId;
    private String planDuration;
    private BigDecimal amount;
    private String enrollmentId;
    private Long paymentId;
    private String transactionId;
    private String facultyId;
    private String facultyName;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private String paymentStatus;
    private String enrollmentStatus;
    private Boolean syllabusExplained;
    private Boolean scheduleExplained;
    private Boolean validityExplained;
    private String onboardedBy;
    private String message;
}
