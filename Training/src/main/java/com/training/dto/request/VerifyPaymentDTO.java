package com.training.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyPaymentDTO {

    private String studentProfileId;

    @NotBlank(message = "courseId is required")
    private String courseId;

    @NotBlank(message = "planId is required")
    private String planId;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    private String paymentMethod;

    private String providerOrderId;

    private String providerPaymentId;

    private String providerSignature;
}
