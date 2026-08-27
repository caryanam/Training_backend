package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.CreateExecutorDTO;
import com.training.dto.request.CreateFacultyDTO;
import com.training.dto.responce.ExecutorResponseDTO;
import com.training.dto.responce.FacultyResponseDTO;
import com.training.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.LocalDate;
import com.training.service.DemoService;
import com.training.dto.responce.DemoSessionResponseDTO;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final DemoService demoService;

    @PostMapping("/faculty")
    public ResponseEntity<ApiResponse<FacultyResponseDTO>> createFaculty(
            @Valid @RequestBody CreateFacultyDTO dto) {
        FacultyResponseDTO data = adminService.createFaculty(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Faculty account created.",
                        data
                ));
    }

    @PostMapping("/executors")
    public ResponseEntity<ApiResponse<ExecutorResponseDTO>> createExecutor(
            @Valid @RequestBody CreateExecutorDTO dto) {
        ExecutorResponseDTO data = adminService.createExecutor(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Executor account created.",
                        data
                ));
    }

    @PutMapping("/executors/{executorId}")
    public ResponseEntity<ApiResponse<ExecutorResponseDTO>> updateExecutor(
            @PathVariable String executorId,
            @RequestBody CreateExecutorDTO dto) {
        ExecutorResponseDTO data = adminService.updateExecutor(executorId, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Executor account updated successfully.", data));
    }

    @PutMapping("/executors/{executorId}/status")
    public ResponseEntity<ApiResponse<ExecutorResponseDTO>> updateExecutorStatus(
            @PathVariable String executorId,
            @RequestParam String status) {
        ExecutorResponseDTO data = adminService.updateExecutorStatus(executorId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Executor status updated successfully.", data));
    }

    @DeleteMapping("/executors/{executorId}")
    public ResponseEntity<ApiResponse<Void>> deleteExecutor(
            @PathVariable String executorId) {
        adminService.deleteExecutor(executorId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Executor account deleted successfully.", null));
    }

    @GetMapping("/faculty")
    public ResponseEntity<ApiResponse<List<FacultyResponseDTO>>> getAllFaculty() {
        List<FacultyResponseDTO> data = adminService.getAllFaculty();
        return ResponseEntity.ok(new ApiResponse<>(true, "Faculty list fetched successfully.", data));
    }

    @GetMapping("/executors")
    public ResponseEntity<ApiResponse<List<ExecutorResponseDTO>>> getAllExecutors() {
        List<ExecutorResponseDTO> data = adminService.getAllExecutors();
        return ResponseEntity.ok(new ApiResponse<>(true, "Executor list fetched successfully.", data));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<com.training.dto.responce.AdminDashboardStatsDTO>> getDashboardStats() {
        com.training.dto.responce.AdminDashboardStatsDTO data = adminService.getDashboardStats();
        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard stats fetched successfully.", data));
    }

    @GetMapping("/demo-sessions")
    public ResponseEntity<ApiResponse<List<DemoSessionResponseDTO>>> getAllGroupDemosAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DemoSessionResponseDTO> data = demoService.getAllGroupDemosForAdmin(status, date);
        return ResponseEntity.ok(new ApiResponse<>(true, "Demo sessions fetched successfully.", data));
    }
}
