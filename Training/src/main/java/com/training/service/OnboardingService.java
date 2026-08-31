package com.training.service;

import com.training.dto.request.StudentOnboardingDTO;
import com.training.dto.responce.StudentOnboardingResponseDTO;

public interface OnboardingService {
    StudentOnboardingResponseDTO onboardStudent(StudentOnboardingDTO dto, String executorEmail);
}
