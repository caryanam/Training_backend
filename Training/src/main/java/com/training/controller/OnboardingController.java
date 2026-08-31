package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.StudentOnboardingDTO;
import com.training.dto.responce.StudentOnboardingResponseDTO;
import com.training.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * POST /api/v1/onboarding/students
     * Production-grade student onboarding workflow.
     * Restricted strictly to ADMIN and EXECUTOR roles.
     */
    @PostMapping("/students")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EXECUTOR')")
    public ResponseEntity<ApiResponse<StudentOnboardingResponseDTO>> onboardStudent(
            @Valid @RequestBody StudentOnboardingDTO dto,
            Authentication authentication) {
        String executorEmail = authentication != null ? authentication.getName() : null;
        StudentOnboardingResponseDTO data = onboardingService.onboardStudent(dto, executorEmail);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Student onboarded successfully.",
                        data
                ));
    }
}
