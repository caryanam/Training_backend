package com.training.service;

import com.training.dto.request.ReportSecurityEventDTO;
import com.training.dto.responce.LectureSecurityEventResponseDTO;
import com.training.dto.responce.SecurityPolicyStatusDTO;

import java.util.List;

public interface LectureSecurityService {

    SecurityPolicyStatusDTO recordSecurityEvent(ReportSecurityEventDTO dto, String authenticatedEmail);

    List<LectureSecurityEventResponseDTO> getLectureSecurityEvents(String lectureId, String facultyEmail);

    SecurityPolicyStatusDTO getStudentPolicyStatus(String lectureId, String studentEmail);
}
