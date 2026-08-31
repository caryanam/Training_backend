package com.training.service;

import com.training.dto.request.CompleteDummyPaymentDTO;
import com.training.dto.request.CreateDummyPaymentDTO;
import com.training.dto.request.VerifyPaymentDTO;
import com.training.dto.responce.DummyPaymentResponseDTO;
import com.training.dto.responce.PaymentResponseDTO;

import java.util.List;

public interface PaymentService {
    DummyPaymentResponseDTO createDummyPayment(CreateDummyPaymentDTO dto, String authenticatedEmail);
    DummyPaymentResponseDTO completeDummyPayment(CompleteDummyPaymentDTO dto, String authenticatedEmail);
    PaymentResponseDTO verifyPayment(VerifyPaymentDTO dto, String authenticatedEmail);
    List<DummyPaymentResponseDTO> getAllPayments();
    List<DummyPaymentResponseDTO> getMyPayments(String studentEmail);
}
