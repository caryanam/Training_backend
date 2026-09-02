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

    @jakarta.validation.constraints.Pattern(
            regexp = "^$|^(?:\\+?91[\\-\\s]?)?[6-9](?:[\\-\\s]?\\d){9}$",
            message = "Mobile number must be a valid 10-digit number starting with 6, 7, 8, or 9"
    )
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
