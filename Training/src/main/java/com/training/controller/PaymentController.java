package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.CompleteDummyPaymentDTO;
import com.training.dto.request.CreateDummyPaymentDTO;
import com.training.dto.request.VerifyPaymentDTO;
import com.training.dto.responce.DummyPaymentResponseDTO;
import com.training.dto.responce.PaymentResponseDTO;
import com.training.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * GET /api/v1/payments
     * Fetches all real payments recorded in database.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DummyPaymentResponseDTO>>> getAllPayments() {
        List<DummyPaymentResponseDTO> data = paymentService.getAllPayments();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Fetched all payment transactions successfully.",
                data
        ));
    }

    /**
     * GET /api/v1/payments/student/me
     * Fetches payments for the authenticated student.
     */
    @GetMapping("/student/me")
    public ResponseEntity<ApiResponse<List<DummyPaymentResponseDTO>>> getMyPayments(Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        List<DummyPaymentResponseDTO> data = paymentService.getMyPayments(userEmail);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Fetched student payment records.",
                data
        ));
    }

    /**
     * POST /api/v1/payments/dummy/create
     * Creates a dummy payment transaction with PENDING status.
     * Source of truth for amount is always fetched from database.
     */
    @PostMapping("/dummy/create")
    public ResponseEntity<ApiResponse<DummyPaymentResponseDTO>> createDummyPayment(
            @Valid @RequestBody CreateDummyPaymentDTO dto,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        DummyPaymentResponseDTO data = paymentService.createDummyPayment(dto, userEmail);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Dummy payment order created successfully.",
                        data
                ));
    }

    /**
     * POST /api/v1/payments/dummy/complete
     * Simulates completion of dummy payment (SUCCESS, FAILED, CANCELLED).
     * Prevents duplicate completions and validates payment record.
     */
    @PostMapping("/dummy/complete")
    public ResponseEntity<ApiResponse<DummyPaymentResponseDTO>> completeDummyPayment(
            @Valid @RequestBody CompleteDummyPaymentDTO dto,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        DummyPaymentResponseDTO data = paymentService.completeDummyPayment(dto, userEmail);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                data.getMessage() != null ? data.getMessage() : "Dummy payment completed.",
                data
        ));
    }

    /**
     * POST /api/v1/payments/verify
     * Legacy/direct verification endpoint.
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> verifyPayment(
            @Valid @RequestBody VerifyPaymentDTO dto,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        PaymentResponseDTO data = paymentService.verifyPayment(dto, userEmail);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Payment verified. Course enrollment activated.",
                data
        ));
    }
}
