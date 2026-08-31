package com.training.dto.responce;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DummyPaymentResponseDTO {
    private Long paymentId;
    private String transactionId;
    private String studentId;
    private String studentEmail;
    private String courseId;
    private String courseName;
    private Long planId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String providerOrderId;
    private String providerPaymentId;
    private String enrollmentId;
    private java.time.LocalDate startDate;
    private java.time.LocalDate expiryDate;
    private String enrollmentStatus;
    private String message;
}
