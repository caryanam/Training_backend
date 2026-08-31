package com.training.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentOnboardingDTO {

    private String leadId;

    private String fullName;

    private String email;

    private String phone;

    private String education;

    private String city;

    @NotBlank(message = "courseId is required")
    private String courseId;

    @NotBlank(message = "planId is required")
    private String planId;

    @Builder.Default
    private Boolean syllabusExplained = false;

    @Builder.Default
    private Boolean scheduleExplained = false;

    @Builder.Default
    private Boolean validityExplained = false;

    private String facultyId;

    private String paymentId;

    private String transactionId;

    @Builder.Default
    private Boolean directEnrollment = false;

    private String notes;
}
