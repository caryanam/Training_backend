package com.training.payment;

import com.training.payment.dto.*;

/**
 * Pluggable Payment Gateway abstraction.
 * Allows seamless switching between DummyPaymentGateway, Razorpay, PhonePe, Stripe, etc.
 */
public interface PaymentGateway {

    /**
     * Identifies the provider name (e.g., "DUMMY", "RAZORPAY", "STRIPE").
     */
    String getProviderName();

    /**
     * Initiates a payment transaction and generates provider details / order ID.
     */
    PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request);

    /**
     * Verifies the completion / callback / simulation of a payment transaction.
     */
    PaymentVerificationResponse verifyPayment(PaymentVerificationRequest request);
}
