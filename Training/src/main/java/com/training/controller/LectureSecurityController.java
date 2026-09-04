package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.ReportSecurityEventDTO;
import com.training.dto.responce.LectureSecurityEventResponseDTO;
import com.training.dto.responce.SecurityPolicyStatusDTO;
import com.training.service.LectureSecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lectures/{lectureId}/security")
@RequiredArgsConstructor
public class LectureSecurityController {

    private final LectureSecurityService securityService;

    /**
     * POST /api/v1/lectures/{lectureId}/security/events
     * Authenticated student reports browser security event (screen share, visibility, focus, fullscreen).
     * Student identity is derived strictly from authentication context.
     */
    @PostMapping("/events")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<SecurityPolicyStatusDTO>> reportSecurityEvent(
            @PathVariable String lectureId,
            @Valid @RequestBody ReportSecurityEventDTO dto,
            Authentication authentication) {
        dto.setLectureId(lectureId);
        String studentEmail = authentication.getName();
        SecurityPolicyStatusDTO data = securityService.recordSecurityEvent(dto, studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Security event recorded.", data));
    }

    /**
     * GET /api/v1/lectures/{lectureId}/security/events
     * Faculty/Admin fetches real-time security events log for this lecture.
     */
    @GetMapping("/events")
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<LectureSecurityEventResponseDTO>>> getLectureSecurityEvents(
            @PathVariable String lectureId,
            Authentication authentication) {
        String facultyEmail = authentication.getName();
        List<LectureSecurityEventResponseDTO> data = securityService.getLectureSecurityEvents(lectureId, facultyEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lecture security events retrieved.", data));
    }

    /**
     * GET /api/v1/lectures/{lectureId}/security/policy
     * Retrieves current student violation count and standing for this lecture.
     */
    @GetMapping("/policy")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<SecurityPolicyStatusDTO>> getStudentPolicyStatus(
            @PathVariable String lectureId,
            Authentication authentication) {
        String studentEmail = authentication.getName();
        SecurityPolicyStatusDTO data = securityService.getStudentPolicyStatus(lectureId, studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Policy status retrieved.", data));
    }
}
