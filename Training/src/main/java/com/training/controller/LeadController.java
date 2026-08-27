package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.AssignExecutorDTO;
import com.training.dto.responce.LeadAssignResponseDTO;
import com.training.dto.responce.LeadResponseDTO;
import com.training.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeadResponseDTO>>> getLeads(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String executorId,
            @RequestParam(required = false) String executorEmail) {
        List<LeadResponseDTO> leads = leadService.getLeads(status, search, executorId, executorEmail);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Student leads fetched successfully.",
                leads
        ));
    }

    @PutMapping("/{leadId}/assign")
    public ResponseEntity<ApiResponse<LeadAssignResponseDTO>> assignExecutor(
            @PathVariable String leadId,
            @Valid @RequestBody AssignExecutorDTO dto) {
        LeadAssignResponseDTO data = leadService.assignExecutor(leadId, dto);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Executor assigned successfully.",
                data
        ));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_EXECUTOR')")
    @PutMapping("/{leadId}/status")
    public ResponseEntity<ApiResponse<LeadResponseDTO>> updateLeadStatus(
            @PathVariable String leadId,
            @RequestParam String status) {
        LeadResponseDTO data = leadService.updateLeadStatus(leadId, status);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lead status updated successfully.",
                data
        ));
    }
}
