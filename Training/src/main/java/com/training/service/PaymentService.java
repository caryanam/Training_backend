package com.training.service;

import com.training.dto.request.VerifyPaymentDTO;
import com.training.dto.responce.PaymentResponseDTO;

public interface PaymentService {
    PaymentResponseDTO verifyPayment(VerifyPaymentDTO dto, String authenticatedEmail);
}
