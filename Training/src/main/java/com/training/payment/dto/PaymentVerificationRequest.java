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
public class PaymentVerificationRequest {
    private String transactionId;
    private String providerOrderId;
    private String providerPaymentId;
    private String providerSignature;
    private PaymentStatus desiredStatus;
    private BigDecimal expectedAmount;
}
