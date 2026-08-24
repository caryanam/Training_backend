package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.VerifyPaymentDTO;
import com.training.dto.responce.PaymentResponseDTO;
import com.training.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

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
