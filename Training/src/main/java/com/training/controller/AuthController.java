package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.LoginRequestDTO;
import com.training.dto.request.RegisterStudentDTO;
import com.training.dto.request.ResendOtpDTO;
import com.training.dto.request.VerifyOtpDTO;
import com.training.dto.responce.LoginResponseDTO;
import com.training.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping({"/register", "/register/initiate"})
    public ResponseEntity<ApiResponse<String>> registerStudent(
            @Valid @RequestBody RegisterStudentDTO dto) {
        String message = authService.registerStudent(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        message,
                        null
                ));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(
            @Valid @RequestBody VerifyOtpDTO dto) {
        String message = authService.verifyRegistrationOtp(dto);
        return ResponseEntity
                .ok(new ApiResponse<>(
                        true,
                        message,
                        null
                ));
    }

    @PostMapping("/register/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(
            @Valid @RequestBody ResendOtpDTO dto) {
        String message = authService.resendRegistrationOtp(dto);
        return ResponseEntity
                .ok(new ApiResponse<>(
                        true,
                        message,
                        null
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO dto) {
        LoginResponseDTO data = authService.login(dto);
        return ResponseEntity
                .ok(new ApiResponse<>(
                        true,
                        "Login successful.",
                        data
                ));
    }
}
