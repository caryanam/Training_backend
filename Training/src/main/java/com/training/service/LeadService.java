package com.training.service;

import com.training.dto.request.AssignExecutorDTO;
import com.training.dto.responce.LeadAssignResponseDTO;
import com.training.dto.responce.LeadResponseDTO;

import java.util.List;

public interface LeadService {
    LeadResponseDTO updateLeadStatus(String leadId, String status);
    List<LeadResponseDTO> getLeads(String status, String search, String executorId, String executorEmail);
    LeadAssignResponseDTO assignExecutor(String leadId, AssignExecutorDTO dto);
}
