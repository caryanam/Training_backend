package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.responce.ExecutorPaymentResponseDTO;
import com.training.dto.responce.ExecutorStudentResponseDTO;
import com.training.service.ExecutorPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/executor")
@RequiredArgsConstructor
public class ExecutorPortalController {

    private final ExecutorPortalService executorPortalService;

    /**
     * GET /api/v1/executor/students
     * Returns ONLY students enrolled or assigned to this executor.
     */
    @GetMapping("/students")
    @PreAuthorize("hasAnyAuthority('ROLE_EXECUTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ExecutorStudentResponseDTO>>> getMyStudents(Authentication authentication) {
        String executorEmail = authentication.getName();
        List<ExecutorStudentResponseDTO> data = executorPortalService.getMyStudents(executorEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Executor students retrieved successfully.", data));
    }

    /**
     * GET /api/v1/executor/payments
     * Returns payment records ONLY for students enrolled/assigned by this executor.
     */
    @GetMapping("/payments")
    @PreAuthorize("hasAnyAuthority('ROLE_EXECUTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ExecutorPaymentResponseDTO>>> getMyStudentPayments(Authentication authentication) {
        String executorEmail = authentication.getName();
        List<ExecutorPaymentResponseDTO> data = executorPortalService.getMyStudentPayments(executorEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Executor student payments retrieved successfully.", data));
    }
}
