package com.training.service;

import com.training.dto.request.LoginRequestDTO;
import com.training.dto.request.RegisterStudentDTO;
import com.training.dto.responce.LoginResponseDTO;
import com.training.dto.responce.RegisterStudentResponseDTO;
import com.training.dto.request.VerifyOtpDTO;
import com.training.dto.request.ResendOtpDTO;

public interface AuthService {
    String registerStudent(RegisterStudentDTO dto);
    String verifyRegistrationOtp(VerifyOtpDTO dto);
    String resendRegistrationOtp(ResendOtpDTO dto);
    LoginResponseDTO login(LoginRequestDTO dto);
}
