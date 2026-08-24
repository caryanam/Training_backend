package com.training.service;

import com.training.dto.request.CreateExecutorDTO;
import com.training.dto.request.CreateFacultyDTO;
import com.training.dto.responce.AdminDashboardStatsDTO;
import com.training.dto.responce.ExecutorResponseDTO;
import com.training.dto.responce.FacultyResponseDTO;

import java.util.List;

public interface AdminService {
    List<FacultyResponseDTO> getAllFaculty();
    List<ExecutorResponseDTO> getAllExecutors();
    FacultyResponseDTO createFaculty(CreateFacultyDTO dto);
    ExecutorResponseDTO createExecutor(CreateExecutorDTO dto);
    ExecutorResponseDTO updateExecutor(String executorId, CreateExecutorDTO dto);
    ExecutorResponseDTO updateExecutorStatus(String executorId, String status);
    void deleteExecutor(String executorId);
    AdminDashboardStatsDTO getDashboardStats();
}
