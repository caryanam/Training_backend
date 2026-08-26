package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.CreateFollowupReportDTO;
import com.training.dto.responce.FollowupReportResponseDTO;
import com.training.service.FollowupReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/followups")
@RequiredArgsConstructor
public class FollowupController {

    private final FollowupReportService followupService;

    @PostMapping("/leads/{leadId}")
    public ResponseEntity<ApiResponse<FollowupReportResponseDTO>> createFollowup(
            @PathVariable Long leadId,
            @Valid @RequestBody CreateFollowupReportDTO dto,
            Authentication authentication) {
        FollowupReportResponseDTO data = followupService.createFollowupReport(leadId, dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Follow-up report submitted.", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FollowupReportResponseDTO>>> getAllFollowups() {
        List<FollowupReportResponseDTO> data = followupService.getAllFollowups();
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched all follow-up reports.", data));
    }

    @GetMapping("/leads/{leadId}")
    public ResponseEntity<ApiResponse<List<FollowupReportResponseDTO>>> getFollowupsByLead(@PathVariable Long leadId) {
        List<FollowupReportResponseDTO> data = followupService.getFollowupsByLeadId(leadId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched follow-up reports.", data));
    }

    @GetMapping("/student/me")
    public ResponseEntity<ApiResponse<List<FollowupReportResponseDTO>>> getMyFollowups(Authentication authentication) {
        List<FollowupReportResponseDTO> data = followupService.getFollowupsForStudent(authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched your follow-up reports.", data));
    }
}

