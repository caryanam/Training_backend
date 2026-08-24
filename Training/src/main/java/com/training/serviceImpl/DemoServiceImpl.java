package com.training.serviceImpl;

import com.training.dto.request.AddParticipantsDTO;
import com.training.dto.request.CompleteDemoDTO;
import com.training.dto.request.CreateGroupDemoDTO;
import com.training.dto.request.RescheduleDemoDTO;
import com.training.dto.request.ScheduleDemoDTO;
import com.training.dto.responce.CompleteDemoResponseDTO;
import com.training.dto.responce.DemoResponseDTO;
import com.training.dto.responce.DemoSessionResponseDTO;
import com.training.entity.Course;
import com.training.entity.DemoParticipant;
import com.training.entity.DemoSession;
import com.training.entity.StudentLead;
import com.training.enums.AttendanceStatus;
import com.training.enums.DemoStatus;
import com.training.enums.LeadStatus;
import com.training.exception.BadRequestException;
import com.training.exception.ResourceNotFoundException;
import com.training.repo.CourseRepository;
import com.training.repo.DemoParticipantRepository;
import com.training.repo.DemoSessionRepository;
import com.training.repo.StudentLeadRepository;
import com.training.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemoServiceImpl implements DemoService {

    private final DemoSessionRepository demoSessionRepository;
    private final DemoParticipantRepository demoParticipantRepository;
    private final StudentLeadRepository studentLeadRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public DemoSessionResponseDTO createGroupDemo(CreateGroupDemoDTO dto, String executorIdentifier) {
        if (dto.getDemoDate() == null) {
            throw new BadRequestException("Please select a demo date.");
        }
        if (dto.getDemoDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Demo date cannot be in the past.");
        }

        LocalTime startTime = dto.getParsedStartTime();
        LocalTime endTime = dto.getParsedEndTime();
        if (startTime == null) {
            throw new BadRequestException("Start time is required.");
        }
        if (endTime != null && !endTime.isAfter(startTime)) {
            throw new BadRequestException("End time must be after start time.");
        }
        if (endTime == null) {
            endTime = startTime.plusHours(1);
        }

        String meetLink = dto.getMeetLink();
        if (meetLink == null || meetLink.trim().isEmpty()) {
            throw new BadRequestException("Please enter a valid Google Meet link.");
        }
        meetLink = meetLink.trim();
        if (!meetLink.toLowerCase().contains("meet.google.com") && !meetLink.toLowerCase().startsWith("http")) {
            throw new BadRequestException("Please enter a valid Google Meet URL.");
        }

        if (dto.getStudentIds() == null || dto.getStudentIds().isEmpty()) {
            throw new BadRequestException("At least one student must be selected for the group demo session.");
        }

        Course course = null;
        if (dto.getCourseId() != null && !dto.getCourseId().isEmpty()) {
            course = findCourseByIdOrCode(dto.getCourseId()).orElse(null);
        }
        String courseName = course != null ? course.getName() : (dto.getCourseName() != null ? dto.getCourseName() : "Full Stack Web Development");

        // 1. Create ONE DemoSession
        DemoSession session = DemoSession.builder()
                .course(course)
                .courseName(courseName)
                .demoDate(dto.getDemoDate())
                .startTime(startTime)
                .endTime(endTime)
                .demoTime(startTime)
                .meetLink(meetLink)
                .meetingLink(meetLink)
                .notes(dto.getNotes())
                .status(DemoStatus.SCHEDULED)
                .createdBy(executorIdentifier)
                .build();
        session = demoSessionRepository.save(session);

        String demoCode = "demo-" + (7700 + session.getId());
        session.setDemoCode(demoCode);

        // 2. Add Participants into existing collection inside same transaction
        if (dto.getStudentIds() != null) {
            for (String studentIdStr : dto.getStudentIds()) {
                Optional<StudentLead> leadOpt = findLeadByIdOrCode(studentIdStr);
                if (leadOpt.isPresent()) {
                    StudentLead lead = leadOpt.get();

                    if (!demoParticipantRepository.existsByDemoSessionIdAndLeadId(session.getId(), lead.getId())) {
                        DemoParticipant participant = DemoParticipant.builder()
                                .demoSession(session)
                                .lead(lead)
                                .attendanceStatus(AttendanceStatus.NOT_MARKED)
                                .build();

                        session.getParticipants().add(participant);

                        lead.setStatus(LeadStatus.DEMO_SCHEDULED);
                        studentLeadRepository.save(lead);
                    }
                }
            }
        }

        session = demoSessionRepository.save(session);

        return mapToSessionDTO(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemoSessionResponseDTO> getExecutorGroupDemos(String statusStr, LocalDate date, String courseId, String executorIdentifier) {
        DemoStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !statusStr.equalsIgnoreCase("all")) {
            try {
                status = DemoStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid enum
            }
        }

        List<DemoSession> sessions = demoSessionRepository.findSessionsWithFilters(status, date);
        return sessions.stream().map(this::mapToSessionDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemoSessionResponseDTO> getStudentUpcomingGroupDemos(String studentEmailOrId) {
        if (studentEmailOrId == null || studentEmailOrId.trim().isEmpty()) {
            return List.of();
        }
        Optional<StudentLead> leadOpt = findLeadByIdOrCode(studentEmailOrId);
        if (leadOpt.isEmpty()) return List.of();

        List<DemoSession> sessions = demoSessionRepository.findUpcomingSessionsForStudent(leadOpt.get().getId());
        return sessions.stream().map(this::mapToSessionDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemoSessionResponseDTO> getStudentGroupDemoHistory(String studentEmailOrId) {
        if (studentEmailOrId == null || studentEmailOrId.trim().isEmpty()) {
            return List.of();
        }
        Optional<StudentLead> leadOpt = findLeadByIdOrCode(studentEmailOrId);
        if (leadOpt.isEmpty()) return List.of();

        List<DemoSession> sessions = demoSessionRepository.findSessionHistoryForStudent(leadOpt.get().getId());
        return sessions.stream().map(this::mapToSessionDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DemoSessionResponseDTO addParticipants(String sessionIdStr, AddParticipantsDTO dto) {
        DemoSession session = findSessionByIdOrCode(sessionIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Demo session not found: " + sessionIdStr));

        if (dto.getStudentIds() != null) {
            for (String studentIdStr : dto.getStudentIds()) {
                Optional<StudentLead> leadOpt = findLeadByIdOrCode(studentIdStr);
                if (leadOpt.isPresent()) {
                    StudentLead lead = leadOpt.get();
                    if (!demoParticipantRepository.existsByDemoSessionIdAndLeadId(session.getId(), lead.getId())) {
                        DemoParticipant participant = DemoParticipant.builder()
                                .demoSession(session)
                                .lead(lead)
                                .attendanceStatus(AttendanceStatus.NOT_MARKED)
                                .build();
                        
                        session.getParticipants().add(participant);

                        lead.setStatus(LeadStatus.DEMO_SCHEDULED);
                        studentLeadRepository.save(lead);
                    }
                }
            }
        }

        session = demoSessionRepository.save(session);
        return mapToSessionDTO(session);
    }

    @Override
    @Transactional
    public DemoSessionResponseDTO removeParticipant(String sessionIdStr, String studentIdStr) {
        DemoSession session = findSessionByIdOrCode(sessionIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Demo session not found: " + sessionIdStr));

        Optional<StudentLead> leadOpt = findLeadByIdOrCode(studentIdStr);
        if (leadOpt.isPresent()) {
            Long leadId = leadOpt.get().getId();
            session.getParticipants().removeIf(p -> p.getLead() != null && p.getLead().getId().equals(leadId));
            demoParticipantRepository.deleteByDemoSessionIdAndLeadId(session.getId(), leadId);
        }

        session = demoSessionRepository.save(session);
        return mapToSessionDTO(session);
    }

    @Override
    @Transactional
    public DemoSessionResponseDTO editGroupDemo(String sessionIdStr, CreateGroupDemoDTO dto) {
        DemoSession session = findSessionByIdOrCode(sessionIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Demo session not found: " + sessionIdStr));

        if (dto.getDemoDate() != null) {
            if (dto.getDemoDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("Demo date cannot be in the past.");
            }
            session.setDemoDate(dto.getDemoDate());
        }

        if (dto.getParsedStartTime() != null) {
            session.setStartTime(dto.getParsedStartTime());
            session.setDemoTime(dto.getParsedStartTime());
        }

        if (dto.getParsedEndTime() != null) {
            if (session.getStartTime() != null && !dto.getParsedEndTime().isAfter(session.getStartTime())) {
                throw new BadRequestException("End time must be after start time.");
            }
            session.setEndTime(dto.getParsedEndTime());
        }

        if (dto.getMeetLink() != null && !dto.getMeetLink().trim().isEmpty()) {
            session.setMeetLink(dto.getMeetLink().trim());
            session.setMeetingLink(dto.getMeetLink().trim());
        }

        if (dto.getNotes() != null) {
            session.setNotes(dto.getNotes());
        }

        session.setStatus(DemoStatus.RESCHEDULED);
        session = demoSessionRepository.save(session);

        return mapToSessionDTO(session);
    }

    @Override
    @Transactional
    public DemoSessionResponseDTO cancelGroupDemo(String sessionIdStr) {
        DemoSession session = findSessionByIdOrCode(sessionIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Demo session not found: " + sessionIdStr));

        session.setStatus(DemoStatus.CANCELLED);
        session = demoSessionRepository.save(session);

        return mapToSessionDTO(session);
    }

    // --- Backwards Compatibility Implementation ---
    @Override
    @Transactional
    public DemoResponseDTO scheduleDemo(ScheduleDemoDTO dto) {
        String leadId = dto.getLeadId() != null ? dto.getLeadId() : dto.getStudentId();
        String startTimeStr = dto.getStartTime() != null ? dto.getStartTime().toString() : "11:00";
        String endTimeStr = dto.getEndTime() != null ? dto.getEndTime().toString() : "12:00";

        CreateGroupDemoDTO groupDto = CreateGroupDemoDTO.builder()
                .courseId(dto.getCourseId())
                .demoDate(dto.getDemoDate())
                .startTime(startTimeStr)
                .endTime(endTimeStr)
                .meetLink(dto.getMeetLink())
                .notes(dto.getNotes())
                .studentIds(List.of(leadId))
                .build();

        DemoSessionResponseDTO groupRes = createGroupDemo(groupDto, "executor");
        return mapSessionToSingleDTO(groupRes);
    }

    @Override
    @Transactional
    public CompleteDemoResponseDTO completeDemo(String demoIdStr, CompleteDemoDTO dto) {
        DemoSession session = findSessionByIdOrCode(demoIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Demo session not found: " + demoIdStr));

        session.setStatus(DemoStatus.COMPLETED);
        session.setFeedback(dto.getFeedback());
        session.setMarkInterested(dto.getMarkInterested());
        session = demoSessionRepository.save(session);

        return CompleteDemoResponseDTO.builder()
                .demoId(session.getDemoCode() != null ? session.getDemoCode() : "demo-" + session.getId())
                .demoStatus(session.getStatus().name())
                .leadStatus("COMPLETED")
                .build();
    }

    @Override
    public List<DemoResponseDTO> getExecutorDemos(String status, LocalDate date, String studentId, String executorEmailOrId) {
        List<DemoSessionResponseDTO> sessions = getExecutorGroupDemos(status, date, null, executorEmailOrId);
        return sessions.stream().map(this::mapSessionToSingleDTO).collect(Collectors.toList());
    }

    @Override
    public List<DemoResponseDTO> getUpcomingStudentDemos(String studentEmailOrId) {
        List<DemoSessionResponseDTO> sessions = getStudentUpcomingGroupDemos(studentEmailOrId);
        return sessions.stream().map(this::mapSessionToSingleDTO).collect(Collectors.toList());
    }

    @Override
    public List<DemoResponseDTO> getStudentDemoHistory(String studentEmailOrId) {
        List<DemoSessionResponseDTO> sessions = getStudentGroupDemoHistory(studentEmailOrId);
        return sessions.stream().map(this::mapSessionToSingleDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DemoResponseDTO rescheduleDemo(String demoIdStr, RescheduleDemoDTO dto) {
        String startTimeStr = dto.getStartTime() != null ? dto.getStartTime().toString() : "11:00";
        String endTimeStr = dto.getEndTime() != null ? dto.getEndTime().toString() : "12:00";

        CreateGroupDemoDTO groupDto = CreateGroupDemoDTO.builder()
                .demoDate(dto.getDemoDate())
                .startTime(startTimeStr)
                .endTime(endTimeStr)
                .meetLink(dto.getMeetLink())
                .notes(dto.getNotes())
                .build();

        DemoSessionResponseDTO groupRes = editGroupDemo(demoIdStr, groupDto);
        return mapSessionToSingleDTO(groupRes);
    }

    @Override
    @Transactional
    public DemoResponseDTO cancelDemo(String demoIdStr) {
        DemoSessionResponseDTO groupRes = cancelGroupDemo(demoIdStr);
        return mapSessionToSingleDTO(groupRes);
    }

    // --- Helper Mappers ---
    private DemoSessionResponseDTO mapToSessionDTO(DemoSession session) {
        String sessionCode = session.getDemoCode() != null ? session.getDemoCode() : "demo-" + session.getId();
        String courseIdStr = session.getCourse() != null ? String.valueOf(session.getCourse().getId()) : null;

        List<DemoSessionResponseDTO.ParticipantDTO> participantDTOs = new ArrayList<>();
        if (session.getParticipants() != null) {
            for (DemoParticipant p : session.getParticipants()) {
                StudentLead lead = p.getLead();
                String leadCode = lead != null ? (lead.getLeadCode() != null ? lead.getLeadCode() : "lead-" + lead.getId()) : "";
                String studentName = lead != null ? lead.getFullName() : "Student";
                String studentEmail = lead != null ? lead.getEmail() : "";
                String studentPhone = lead != null ? lead.getPhone() : "";

                participantDTOs.add(DemoSessionResponseDTO.ParticipantDTO.builder()
                        .participantId(String.valueOf(p.getId()))
                        .leadId(leadCode)
                        .studentId(leadCode)
                        .studentName(studentName)
                        .studentEmail(studentEmail)
                        .studentPhone(studentPhone)
                        .attendanceStatus(p.getAttendanceStatus() != null ? p.getAttendanceStatus().name() : "NOT_MARKED")
                        .joinedAt(p.getJoinedAt())
                        .build());
            }
        }

        return DemoSessionResponseDTO.builder()
                .id(String.valueOf(session.getId()))
                .sessionId(String.valueOf(session.getId()))
                .demoCode(sessionCode)
                .courseId(courseIdStr)
                .courseName(session.getCourseName() != null ? session.getCourseName() : "Full Stack Web Development")
                .demoDate(session.getDemoDate())
                .startTime(session.getStartTime() != null ? session.getStartTime() : session.getDemoTime())
                .endTime(session.getEndTime())
                .meetLink(session.getMeetLink() != null ? session.getMeetLink() : session.getMeetingLink())
                .notes(session.getNotes())
                .status(session.getStatus() != null ? session.getStatus().name() : "SCHEDULED")
                .totalParticipants(participantDTOs.size())
                .participants(participantDTOs)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private DemoResponseDTO mapSessionToSingleDTO(DemoSessionResponseDTO session) {
        String studentName = session.getParticipants() != null && !session.getParticipants().isEmpty()
                ? session.getParticipants().get(0).getStudentName() : "Group Participants";
        String studentEmail = session.getParticipants() != null && !session.getParticipants().isEmpty()
                ? session.getParticipants().get(0).getStudentEmail() : "";

        return DemoResponseDTO.builder()
                .id(session.getId())
                .demoId(session.getDemoCode())
                .leadId(session.getDemoCode())
                .studentId(session.getDemoCode())
                .studentName(studentName)
                .studentEmail(studentEmail)
                .courseId(session.getCourseId())
                .courseName(session.getCourseName())
                .demoDate(session.getDemoDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .meetLink(session.getMeetLink())
                .notes(session.getNotes())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .build();
    }

    private Optional<DemoSession> findSessionByIdOrCode(String key) {
        if (key == null || key.trim().isEmpty()) return Optional.empty();
        String cleanKey = key.trim();
        Optional<DemoSession> byCode = demoSessionRepository.findByDemoCode(cleanKey);
        if (byCode.isPresent()) return byCode;

        try {
            Long id = Long.parseLong(cleanKey.replace("demo-", "").replace("session-", ""));
            return demoSessionRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<StudentLead> findLeadByIdOrCode(String key) {
        if (key == null || key.trim().isEmpty()) return Optional.empty();
        String cleanKey = key.trim();
        Optional<StudentLead> byCode = studentLeadRepository.findByLeadCode(cleanKey);
        if (byCode.isPresent()) return byCode;

        List<StudentLead> byEmail = studentLeadRepository.findLeadsWithFilters(null, cleanKey);
        if (!byEmail.isEmpty()) return Optional.of(byEmail.get(0));

        try {
            Long id = Long.parseLong(cleanKey.replace("lead-", "").replace("STU-", "").replace("stu-", ""));
            return studentLeadRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Course> findCourseByIdOrCode(String key) {
        if (key == null || key.trim().isEmpty()) return Optional.empty();
        Optional<Course> byCode = courseRepository.findByCourseCode(key);
        if (byCode.isPresent()) return byCode;

        try {
            Long id = Long.parseLong(key.replace("course-", ""));
            return courseRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
