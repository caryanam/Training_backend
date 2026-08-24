package com.training.service;

import com.training.dto.request.LoginRequestDTO;
import com.training.dto.request.RegisterStudentDTO;
import com.training.dto.responce.LoginResponseDTO;
import com.training.dto.responce.RegisterStudentResponseDTO;

public interface AuthService {
    RegisterStudentResponseDTO registerStudent(RegisterStudentDTO dto);
    LoginResponseDTO login(LoginRequestDTO dto);
}
