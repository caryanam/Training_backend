package com.training.serviceImpl;

import com.training.dto.request.ReportSecurityEventDTO;
import com.training.dto.responce.LectureSecurityEventResponseDTO;
import com.training.dto.responce.SecurityPolicyStatusDTO;
import com.training.entity.*;
import com.training.enums.EnrollmentStatus;
import com.training.enums.Role;
import com.training.enums.SecurityEventSeverity;
import com.training.enums.SecurityEventType;
import com.training.exception.ResourceNotFoundException;
import com.training.exception.UnauthorizedException;
import com.training.repo.*;
import com.training.service.LectureSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LectureSecurityServiceImpl implements LectureSecurityService {

    private final LectureSecurityEventRepository securityEventRepository;
    private final UserRepository userRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final LiveLectureSessionRepository sessionRepository;

    @Override
    @Transactional
    public SecurityPolicyStatusDTO recordSecurityEvent(ReportSecurityEventDTO dto, String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authenticatedEmail));

        Lecture lecture = findLectureByIdOrCode(dto.getLectureId())
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + dto.getLectureId()));

        Course course = lecture.getCourse();
        if (course == null) {
            throw new ResourceNotFoundException("Course not found for lecture: " + lecture.getTitle());
        }

        // Validate student enrollment if role is STUDENT
        if (user.getRole() == Role.STUDENT) {
            Optional<Enrollment> activeEnrollmentOpt = enrollmentRepository
                    .findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(user, course, EnrollmentStatus.ACTIVE);

            if (activeEnrollmentOpt.isEmpty()) {
                throw new UnauthorizedException("Access Denied: No active enrollment for course " + course.getName());
            }

            Enrollment enrollment = activeEnrollmentOpt.get();
            if (enrollment.getExpiryDate() != null && LocalDate.now().isAfter(enrollment.getExpiryDate())) {
                throw new UnauthorizedException("Access Denied: Enrollment expired on " + enrollment.getExpiryDate());
            }
        }

        SecurityEventType eventType = dto.getEventType();
        SecurityEventSeverity severity = resolveSeverity(eventType);

        // Deduplication for frequent transient signals (e.g. rapid blur/focus)
        if (isTransientEvent(eventType)) {
            Optional<LectureSecurityEvent> latestOpt = securityEventRepository
                    .findFirstByLectureAndStudentAndEventTypeOrderByTimestampDesc(lecture, user, eventType);

            if (latestOpt.isPresent()) {
                LocalDateTime lastTime = latestOpt.get().getTimestamp();
                if (lastTime != null && lastTime.isAfter(LocalDateTime.now().minusSeconds(2))) {
                    // Suppress duplicate spam
                    return buildPolicyStatus(lecture, user);
                }
            }
        }

        // Resolve student profile code
        String studentIdentifier = "STU-" + (100 + user.getId());
        Optional<Student> studentOpt = studentRepository.findByUser(user);
        if (studentOpt.isPresent() && studentOpt.get().getStudentCode() != null) {
            studentIdentifier = studentOpt.get().getStudentCode();
        }

        // Optional Live Session association
        LiveLectureSession session = null;
        if (dto.getSessionId() != null) {
            session = sessionRepository.findById(dto.getSessionId()).orElse(null);
        }

        LocalDateTime eventTime = dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now();

        LectureSecurityEvent event = LectureSecurityEvent.builder()
                .session(session)
                .lecture(lecture)
                .student(user)
                .studentName(user.getFullName())
                .studentIdentifier(studentIdentifier)
                .eventType(eventType)
                .severity(severity)
                .metadata(dto.getMetadata())
                .timestamp(eventTime)
                .build();

        securityEventRepository.save(event);

        log.info("Lecture Security Event recorded: [{}] by student {} (ID: {}) on lecture {}",
                eventType, user.getEmail(), studentIdentifier, lecture.getTitle());

        return buildPolicyStatus(lecture, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LectureSecurityEventResponseDTO> getLectureSecurityEvents(String lectureIdStr, String facultyEmail) {
        User user = userRepository.findByEmail(facultyEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + facultyEmail));

        Lecture lecture = findLectureByIdOrCode(lectureIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureIdStr));

        // Validate faculty course ownership
        if (user.getRole() == Role.FACULTY) {
            Faculty faculty = facultyRepository.findByUser(user)
                    .orElseThrow(() -> new UnauthorizedException("Faculty profile not found"));
            if (lecture.getFaculty() != null && !lecture.getFaculty().getId().equals(faculty.getId())) {
                if (lecture.getCourse() != null && lecture.getCourse().getFaculty() != null
                        && !lecture.getCourse().getFaculty().getId().equals(faculty.getId())) {
                    throw new UnauthorizedException("You are not authorized to view security logs for this lecture.");
                }
            }
        } else if (user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only Faculty and Admin can access security logs.");
        }

        List<LectureSecurityEvent> events = securityEventRepository.findByLectureOrderByTimestampDesc(lecture);

        return events.stream().map(e -> {
            long violations = securityEventRepository.countByLectureAndStudentAndSeverityIn(
                    lecture,
                    e.getStudent(),
                    List.of(SecurityEventSeverity.HIGH, SecurityEventSeverity.CRITICAL)
            );
            return LectureSecurityEventResponseDTO.builder()
                    .id(e.getId())
                    .lectureId(lecture.getLectureCode() != null ? lecture.getLectureCode() : String.valueOf(lecture.getId()))
                    .lectureTitle(lecture.getTitle())
                    .studentId(e.getStudent().getId())
                    .studentName(e.getStudentName())
                    .studentIdentifier(e.getStudentIdentifier())
                    .studentEmail(e.getStudent().getEmail())
                    .eventType(e.getEventType().name())
                    .severity(e.getSeverity().name())
                    .metadata(e.getMetadata())
                    .timestamp(e.getTimestamp())
                    .violationCount((int) violations)
                    .sessionTerminated(violations >= 3)
                    .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityPolicyStatusDTO getStudentPolicyStatus(String lectureIdStr, String studentEmail) {
        User user = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + studentEmail));

        Lecture lecture = findLectureByIdOrCode(lectureIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureIdStr));

        return buildPolicyStatus(lecture, user);
    }

    private SecurityPolicyStatusDTO buildPolicyStatus(Lecture lecture, User student) {
        long highSeverityCount = securityEventRepository.countByLectureAndStudentAndSeverityIn(
                lecture,
                student,
                List.of(SecurityEventSeverity.HIGH, SecurityEventSeverity.CRITICAL)
        );

        if (highSeverityCount >= 3) {
            return SecurityPolicyStatusDTO.builder()
                    .violationCount((int) highSeverityCount)
                    .isSuspended(true)
                    .warningLevel("TERMINATED")
                    .message("Lecture access suspended: Multiple security policy violations recorded.")
                    .actionRequired("Contact administration to restore access.")
                    .build();
        } else if (highSeverityCount == 2) {
            return SecurityPolicyStatusDTO.builder()
                    .violationCount((int) highSeverityCount)
                    .isSuspended(false)
                    .warningLevel("STRONG_WARNING")
                    .message("Strong Security Warning: Unauthorized screen sharing is not allowed. A 3rd violation will terminate your live lecture session.")
                    .actionRequired("Stop screen sharing immediately.")
                    .build();
        } else if (highSeverityCount == 1) {
            return SecurityPolicyStatusDTO.builder()
                    .violationCount((int) highSeverityCount)
                    .isSuspended(false)
                    .warningLevel("WARNING")
                    .message("Security Warning: Screen sharing is not allowed during this lecture. This activity has been recorded and the faculty notified.")
                    .actionRequired("Stop screen sharing.")
                    .build();
        }

        return SecurityPolicyStatusDTO.builder()
                .violationCount(0)
                .isSuspended(false)
                .warningLevel("NONE")
                .message("Session in good standing.")
                .actionRequired(null)
                .build();
    }

    private SecurityEventSeverity resolveSeverity(SecurityEventType eventType) {
        return switch (eventType) {
            case SCREEN_SHARE_STARTED, MULTIPLE_SESSION_DETECTED, SESSION_TERMINATED -> SecurityEventSeverity.HIGH;
            case TAB_HIDDEN, WINDOW_BLUR, FULLSCREEN_EXITED, SUSPICIOUS_ACTIVITY -> SecurityEventSeverity.MEDIUM;
            case SCREEN_SHARE_STOPPED, TAB_VISIBLE, WINDOW_FOCUS -> SecurityEventSeverity.LOW;
        };
    }

    private boolean isTransientEvent(SecurityEventType type) {
        return type == SecurityEventType.TAB_HIDDEN ||
                type == SecurityEventType.TAB_VISIBLE ||
                type == SecurityEventType.WINDOW_BLUR ||
                type == SecurityEventType.WINDOW_FOCUS;
    }

    private Optional<Lecture> findLectureByIdOrCode(String key) {
        if (key == null || key.isBlank()) return Optional.empty();

        Optional<Lecture> byCode = lectureRepository.findByLectureCode(key);
        if (byCode.isPresent()) return byCode;

        try {
            String clean = key.replace("lecture-", "").replace("lec-", "").replace("LEC-", "").trim();
            Long id = Long.parseLong(clean);
            Optional<Lecture> byId = lectureRepository.findById(id);
            if (byId.isPresent()) return byId;
        } catch (NumberFormatException ignored) {}

        List<Lecture> allLectures = lectureRepository.findAll();
        if (!allLectures.isEmpty()) {
            return Optional.of(allLectures.get(0));
        }

        return Optional.empty();
    }
}
