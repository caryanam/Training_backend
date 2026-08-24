package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.LoginRequestDTO;
import com.training.dto.request.RegisterStudentDTO;
import com.training.dto.responce.LoginResponseDTO;
import com.training.dto.responce.RegisterStudentResponseDTO;
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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterStudentResponseDTO>> registerStudent(
            @Valid @RequestBody RegisterStudentDTO dto) {
        RegisterStudentResponseDTO data = authService.registerStudent(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Registration successful. Admissions executor will contact you for a free demo.",
                        data
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
