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
public class CreateDummyPaymentDTO {

    @NotBlank(message = "studentId is required")
    private String studentId;

    @NotBlank(message = "courseId is required")
    private String courseId;

    @NotBlank(message = "planId is required")
    private String planId;

    private String idempotencyKey;
}
