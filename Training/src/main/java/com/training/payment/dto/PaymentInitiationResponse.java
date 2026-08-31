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
public class PaymentInitiationResponse {
    private String transactionId;
    private String provider;
    private String providerOrderId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String message;
}
