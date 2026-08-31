package com.training.payment.dto;

import com.training.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerificationResponse {
    private String transactionId;
    private PaymentStatus status;
    private boolean verified;
    private BigDecimal amount;
    private String providerPaymentId;
    private String message;
}
