package com.training.service;

import com.training.dto.responce.ExecutorPaymentResponseDTO;
import com.training.dto.responce.ExecutorStudentResponseDTO;

import java.util.List;

public interface ExecutorPortalService {
    List<ExecutorStudentResponseDTO> getMyStudents(String executorEmail);
    List<ExecutorPaymentResponseDTO> getMyStudentPayments(String executorEmail);
}
