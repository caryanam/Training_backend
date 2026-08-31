package com.training.payment.dummy;

import com.training.enums.PaymentStatus;
import com.training.payment.PaymentGateway;
import com.training.payment.dto.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component("dummyPaymentGateway")
public class DummyPaymentGateway implements PaymentGateway {

    public static final String PROVIDER_NAME = "DUMMY";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String transactionId = "DUMMY_TXN_" + datePart + "_" + randomSuffix;
        String dummyOrderId = "DUMMY_ORD_" + randomSuffix;

        return PaymentInitiationResponse.builder()
                .transactionId(transactionId)
                .provider(PROVIDER_NAME)
                .providerOrderId(dummyOrderId)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .status(PaymentStatus.PENDING)
                .message("Dummy payment order initiated successfully")
                .build();
    }

    @Override
    public PaymentVerificationResponse verifyPayment(PaymentVerificationRequest request) {
        PaymentStatus finalStatus = request.getDesiredStatus() != null 
                ? request.getDesiredStatus() 
                : PaymentStatus.SUCCESS;

        boolean verified = (finalStatus == PaymentStatus.SUCCESS);
        String providerPaymentId = "DUMMY_PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        String message = switch (finalStatus) {
            case SUCCESS -> "Dummy payment verified successfully.";
            case FAILED -> "Dummy payment simulated failure.";
            case CANCELLED -> "Dummy payment was cancelled by the user.";
            case PENDING -> "Dummy payment is still pending.";
        };

        return PaymentVerificationResponse.builder()
                .transactionId(request.getTransactionId())
                .status(finalStatus)
                .verified(verified)
                .amount(request.getExpectedAmount())
                .providerPaymentId(providerPaymentId)
                .message(message)
                .build();
    }
}
