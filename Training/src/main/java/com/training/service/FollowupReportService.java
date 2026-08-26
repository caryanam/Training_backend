package com.training.service;

import com.training.dto.request.CreateFollowupReportDTO;
import com.training.dto.responce.FollowupReportResponseDTO;

import java.util.List;

public interface FollowupReportService {
    FollowupReportResponseDTO createFollowupReport(Long leadId, CreateFollowupReportDTO dto, String executorEmail);
    List<FollowupReportResponseDTO> getFollowupsByLeadId(Long leadId);
    List<FollowupReportResponseDTO> getFollowupsForStudent(String studentEmail);
    List<FollowupReportResponseDTO> getAllFollowups();
}

