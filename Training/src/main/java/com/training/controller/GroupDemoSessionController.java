package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.AddParticipantsDTO;
import com.training.dto.request.CreateGroupDemoDTO;
import com.training.dto.responce.DemoSessionResponseDTO;
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
@RequestMapping("/api/v1/demo-sessions")
@RequiredArgsConstructor
public class GroupDemoSessionController {

    private final DemoService demoService;

    @PostMapping
    public ResponseEntity<ApiResponse<DemoSessionResponseDTO>> createGroupDemo(
            @Valid @RequestBody CreateGroupDemoDTO dto,
            Authentication authentication) {
        String executorId = authentication != null ? authentication.getName() : "executor";
        DemoSessionResponseDTO data = demoService.createGroupDemo(dto, executorId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Group demo session scheduled successfully.",
                        data
                ));
    }

    @GetMapping("/executor")
    public ResponseEntity<ApiResponse<List<DemoSessionResponseDTO>>> getExecutorGroupDemos(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String courseId,
            Authentication authentication) {
        String executorId = authentication != null ? authentication.getName() : null;
        List<DemoSessionResponseDTO> data = demoService.getExecutorGroupDemos(status, date, courseId, executorId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Executor group demo sessions fetched successfully.",
                data
        ));
    }

    @GetMapping("/student/upcoming")
    public ResponseEntity<ApiResponse<List<DemoSessionResponseDTO>>> getStudentUpcomingGroupDemos(
            Authentication authentication) {
        String studentEmail = authentication != null ? authentication.getName() : null;
        List<DemoSessionResponseDTO> data = demoService.getStudentUpcomingGroupDemos(studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Upcoming group demos fetched successfully.",
                data
        ));
    }

    @GetMapping("/student/history")
    public ResponseEntity<ApiResponse<List<DemoSessionResponseDTO>>> getStudentGroupDemoHistory(
            Authentication authentication) {
        String studentEmail = authentication != null ? authentication.getName() : null;
        List<DemoSessionResponseDTO> data = demoService.getStudentGroupDemoHistory(studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Student demo history fetched successfully.",
                data
        ));
    }

    @PostMapping("/{sessionId}/participants")
    public ResponseEntity<ApiResponse<DemoSessionResponseDTO>> addParticipants(
            @PathVariable String sessionId,
            @Valid @RequestBody AddParticipantsDTO dto) {
        DemoSessionResponseDTO data = demoService.addParticipants(sessionId, dto);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Students added to group demo session successfully.",
                data
        ));
    }

    @DeleteMapping("/{sessionId}/participants/{studentId}")
    public ResponseEntity<ApiResponse<DemoSessionResponseDTO>> removeParticipant(
            @PathVariable String sessionId,
            @PathVariable String studentId) {
        DemoSessionResponseDTO data = demoService.removeParticipant(sessionId, studentId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Student removed from group demo session.",
                data
        ));
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<DemoSessionResponseDTO>> editGroupDemo(
            @PathVariable String sessionId,
            @RequestBody CreateGroupDemoDTO dto) {
        DemoSessionResponseDTO data = demoService.editGroupDemo(sessionId, dto);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Group demo session updated successfully.",
                data
        ));
    }

    @PutMapping("/{sessionId}/cancel")
    public ResponseEntity<ApiResponse<DemoSessionResponseDTO>> cancelGroupDemo(
            @PathVariable String sessionId) {
        DemoSessionResponseDTO data = demoService.cancelGroupDemo(sessionId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Group demo session cancelled.",
                data
        ));
    }
}
