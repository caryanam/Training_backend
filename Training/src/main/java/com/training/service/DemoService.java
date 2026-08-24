package com.training.service;

import com.training.dto.request.AddParticipantsDTO;
import com.training.dto.request.CompleteDemoDTO;
import com.training.dto.request.CreateGroupDemoDTO;
import com.training.dto.request.RescheduleDemoDTO;
import com.training.dto.request.ScheduleDemoDTO;
import com.training.dto.responce.CompleteDemoResponseDTO;
import com.training.dto.responce.DemoResponseDTO;
import com.training.dto.responce.DemoSessionResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface DemoService {
    // Group Demo Session APIs
    DemoSessionResponseDTO createGroupDemo(CreateGroupDemoDTO dto, String executorIdentifier);
    List<DemoSessionResponseDTO> getExecutorGroupDemos(String status, LocalDate date, String courseId, String executorIdentifier);
    List<DemoSessionResponseDTO> getStudentUpcomingGroupDemos(String studentEmailOrId);
    List<DemoSessionResponseDTO> getStudentGroupDemoHistory(String studentEmailOrId);
    DemoSessionResponseDTO addParticipants(String sessionId, AddParticipantsDTO dto);
    DemoSessionResponseDTO removeParticipant(String sessionId, String studentId);
    DemoSessionResponseDTO editGroupDemo(String sessionId, CreateGroupDemoDTO dto);
    DemoSessionResponseDTO cancelGroupDemo(String sessionId);

    // Backwards Compatibility APIs
    DemoResponseDTO scheduleDemo(ScheduleDemoDTO dto);
    CompleteDemoResponseDTO completeDemo(String demoId, CompleteDemoDTO dto);
    List<DemoResponseDTO> getExecutorDemos(String status, LocalDate date, String studentId, String executorEmailOrId);
    List<DemoResponseDTO> getUpcomingStudentDemos(String studentEmailOrId);
    List<DemoResponseDTO> getStudentDemoHistory(String studentEmailOrId);
    DemoResponseDTO rescheduleDemo(String demoId, RescheduleDemoDTO dto);
    DemoResponseDTO cancelDemo(String demoId);
}
