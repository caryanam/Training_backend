package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.CompleteDemoDTO;
import com.training.dto.request.RescheduleDemoDTO;
import com.training.dto.request.ScheduleDemoDTO;
import com.training.dto.responce.CompleteDemoResponseDTO;
import com.training.dto.responce.DemoResponseDTO;
import com.training.service.DemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/demos")
@RequiredArgsConstructor
public class DemoController {

    private final DemoService demoService;

    @PostMapping
    public ResponseEntity<ApiResponse<DemoResponseDTO>> scheduleDemo(
            @Valid @RequestBody ScheduleDemoDTO dto) {
        DemoResponseDTO data = demoService.scheduleDemo(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Demo scheduled successfully",
                        data
                ));
    }

    @GetMapping("/executor")
    public ResponseEntity<ApiResponse<List<DemoResponseDTO>>> getExecutorDemos(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String studentId,
            Authentication authentication) {
        String executorEmailOrId = authentication != null ? authentication.getName() : null;
        List<DemoResponseDTO> data = demoService.getExecutorDemos(status, date, studentId, executorEmailOrId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Executor demos fetched successfully",
                data
        ));
    }

    @GetMapping("/student/upcoming")
    public ResponseEntity<ApiResponse<List<DemoResponseDTO>>> getStudentUpcomingDemos(
            Authentication authentication) {
        String studentEmail = authentication != null ? authentication.getName() : null;
        List<DemoResponseDTO> data = demoService.getUpcomingStudentDemos(studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Upcoming student demos fetched successfully",
                data
        ));
    }

    @GetMapping("/student/history")
    public ResponseEntity<ApiResponse<List<DemoResponseDTO>>> getStudentDemoHistory(
            Authentication authentication) {
        String studentEmail = authentication != null ? authentication.getName() : null;
        List<DemoResponseDTO> data = demoService.getStudentDemoHistory(studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Student demo history fetched successfully",
                data
        ));
    }

    @PutMapping("/{demoId}/reschedule")
    public ResponseEntity<ApiResponse<DemoResponseDTO>> rescheduleDemo(
            @PathVariable String demoId,
            @Valid @RequestBody RescheduleDemoDTO dto) {
        DemoResponseDTO data = demoService.rescheduleDemo(demoId, dto);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Demo rescheduled successfully",
                data
        ));
    }

    @PutMapping("/{demoId}/cancel")
    public ResponseEntity<ApiResponse<DemoResponseDTO>> cancelDemo(
            @PathVariable String demoId) {
        DemoResponseDTO data = demoService.cancelDemo(demoId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Demo cancelled successfully",
                data
        ));
    }

    @PutMapping("/{demoId}/complete")
    public ResponseEntity<ApiResponse<CompleteDemoResponseDTO>> completeDemo(
            @PathVariable String demoId,
            @RequestBody CompleteDemoDTO dto) {
        CompleteDemoResponseDTO data = demoService.completeDemo(demoId, dto);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Demo completed. Student lead updated successfully.",
                data
        ));
    }
}
