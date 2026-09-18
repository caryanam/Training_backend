package com.training.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp, String fullName);
}
