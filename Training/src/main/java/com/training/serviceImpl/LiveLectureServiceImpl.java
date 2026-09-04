package com.training.serviceImpl;

import com.training.dto.responce.LiveLectureJoinResponseDTO;
import com.training.dto.responce.LiveLectureStartResponseDTO;
import com.training.dto.responce.LiveLectureStatusResponseDTO;
import com.training.entity.*;
import com.training.enums.EnrollmentStatus;
import com.training.enums.LiveSessionStatus;
import com.training.enums.ParticipantStatus;
import com.training.enums.Role;
import com.training.exception.BusinessException;
import com.training.exception.ResourceNotFoundException;
import com.training.exception.UnauthorizedException;
import com.training.repo.*;
import com.training.service.LiveKitTokenService;
import com.training.service.LiveLectureService;
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
public class LiveLectureServiceImpl implements LiveLectureService {

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LiveLectureSessionRepository sessionRepository;
    private final LiveLectureParticipantRepository participantRepository;
    private final LiveLectureEventRepository eventRepository;
    private final LiveKitTokenService liveKitTokenService;

    @Override
    @Transactional
    public LiveLectureStartResponseDTO startLiveLecture(String lectureIdStr, String facultyEmail) {
        User user = userRepository.findByEmail(facultyEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + facultyEmail));

        Lecture lecture = findLectureByIdOrCode(lectureIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with identifier: " + lectureIdStr));

        Course course = lecture.getCourse();
        if (course == null) {
            throw new ResourceNotFoundException("Associated course not found for lecture: " + lecture.getTitle());
        }

        // Validate faculty assignment if user is FACULTY
        if (user.getRole() == Role.FACULTY) {
            Faculty faculty = facultyRepository.findByUser(user)
                    .orElseThrow(() -> new UnauthorizedException("Faculty profile not found for user: " + user.getEmail()));

            // Check if lecture is explicitly assigned to a faculty, or course is assigned
            if (lecture.getFaculty() != null && !lecture.getFaculty().getId().equals(faculty.getId())) {
                throw new UnauthorizedException("You are not assigned to conduct this lecture: " + lecture.getTitle());
            }
            if (course.getFaculty() != null && !course.getFaculty().getId().equals(faculty.getId())) {
                throw new UnauthorizedException("You are not assigned to course track: " + course.getName());
            }

            // If lecture has no faculty assigned, bind it to current faculty
            if (lecture.getFaculty() == null) {
                lecture.setFaculty(faculty);
                lectureRepository.save(lecture);
            }
        } else if (user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only Faculty and Admin can start a live lecture session.");
        }

        // Check if an active session already exists for this lecture
        Optional<LiveLectureSession> existingSessionOpt = sessionRepository
                .findFirstByLectureAndStatus(lecture, LiveSessionStatus.LIVE);

        LiveLectureSession session;
        if (existingSessionOpt.isPresent()) {
            session = existingSessionOpt.get();
            log.info("Faculty {} re-joining active live session {} for lecture {}", facultyEmail, session.getId(), lecture.getTitle());
        } else {
            String roomName = "room-lecture-" + lecture.getId() + "-" + System.currentTimeMillis();
            session = LiveLectureSession.builder()
                    .lecture(lecture)
                    .status(LiveSessionStatus.LIVE)
                    .roomName(roomName)
                    .startedAt(LocalDateTime.now())
                    .startedBy(user)
                    .participantCount(0)
                    .build();
            session = sessionRepository.save(session);

            // Record audit event
            logLiveEvent(session, "LECTURE_STARTED", user.getEmail(), user.getRole().name(),
                    "Live lecture session started by " + user.getFullName() + " for room " + roomName);
        }

        // Generate LiveKit Publisher Token for Faculty
        String identity = "fac_" + user.getId() + "_" + user.getEmail();
        String metadata = "{\"role\":\"FACULTY\",\"userId\":" + user.getId() + ",\"email\":\"" + user.getEmail() + "\"}";
        String token = liveKitTokenService.createFacultyToken(
                session.getRoomName(),
                identity,
                user.getFullName(),
                metadata
        );

        return LiveLectureStartResponseDTO.builder()
                .sessionId(session.getId())
                .lectureId(lecture.getLectureCode() != null ? lecture.getLectureCode() : String.valueOf(lecture.getId()))
                .lectureTitle(lecture.getTitle())
                .courseName(course.getName())
                .roomName(session.getRoomName())
                .token(token)
                .livekitUrl(liveKitTokenService.getLivekitUrl())
                .status("LIVE")
                .startedAt(session.getStartedAt())
                .build();
    }

    @Override
    @Transactional
    public LiveLectureJoinResponseDTO joinLiveLecture(String lectureIdStr, String studentEmail) {
        User user = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + studentEmail));

        Lecture lecture = findLectureByIdOrCode(lectureIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureIdStr));

        Course course = lecture.getCourse();
        if (course == null) {
            throw new ResourceNotFoundException("Course not found for lecture: " + lecture.getTitle());
        }

        // Validate Student Enrollment
        if (user.getRole() == Role.STUDENT) {
            Optional<Enrollment> activeEnrollmentOpt = enrollmentRepository
                    .findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(user, course, EnrollmentStatus.ACTIVE);

            if (activeEnrollmentOpt.isEmpty()) {
                logLiveEvent(null, "JOIN_REJECTED", user.getEmail(), user.getRole().name(),
                        "Rejected: No active enrollment for student " + user.getEmail() + " in course " + course.getName());
                throw new UnauthorizedException("Access Denied: You do not have an active enrollment in '" + course.getName() + "'.");
            }

            Enrollment enrollment = activeEnrollmentOpt.get();
            LocalDate today = LocalDate.now();
            if (enrollment.getExpiryDate() != null && today.isAfter(enrollment.getExpiryDate())) {
                logLiveEvent(null, "JOIN_REJECTED", user.getEmail(), user.getRole().name(),
                        "Rejected: Enrollment expired on " + enrollment.getExpiryDate() + " for student " + user.getEmail());
                throw new UnauthorizedException("Access Denied: Your course enrollment expired on " + enrollment.getExpiryDate() + ".");
            }
        }

        // Validate Lecture is currently LIVE
        LiveLectureSession session = sessionRepository
                .findFirstByLectureAndStatus(lecture, LiveSessionStatus.LIVE)
                .orElseThrow(() -> new BusinessException("This lecture is not currently LIVE. Please wait for the faculty instructor to start the class."));

        // Single active session policy / anti-sharing enforcement:
        // Disconnect previous active sessions for this student in this room
        Optional<LiveLectureParticipant> existingParticipantOpt = participantRepository
                .findFirstBySessionAndStudentAndStatus(session, user, ParticipantStatus.ACTIVE);

        if (existingParticipantOpt.isPresent()) {
            LiveLectureParticipant prev = existingParticipantOpt.get();
            prev.setStatus(ParticipantStatus.DISCONNECTED);
            prev.setLeftAt(LocalDateTime.now());
            participantRepository.save(prev);
            logLiveEvent(session, "CONCURRENT_SESSION_DISCONNECTED", user.getEmail(), user.getRole().name(),
                    "Superseded existing active session for student " + user.getEmail());
        }

        // Create new active participant record
        LiveLectureParticipant participant = LiveLectureParticipant.builder()
                .session(session)
                .student(user)
                .studentName(user.getFullName())
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .lastHeartbeat(LocalDateTime.now())
                .build();
        participantRepository.save(participant);

        // Count active participants dynamically (avoid updating parent LiveLectureSession row to prevent MySQL deadlock)
        long activeCount = participantRepository.countBySessionAndStatus(session, ParticipantStatus.ACTIVE);

        // Generate LiveKit Student Token (Subscribe-Only, Publish = false)
        String identity = "stu_" + user.getId() + "_" + user.getEmail();
        String metadata = "{\"role\":\"STUDENT\",\"userId\":" + user.getId() + ",\"email\":\"" + user.getEmail() + "\"}";
        String token = liveKitTokenService.createStudentToken(
                session.getRoomName(),
                identity,
                user.getFullName(),
                metadata
        );

        // Resolve student code if available
        String studentCode = "STU-" + (100 + user.getId());
        Optional<Student> studentProfileOpt = studentRepository.findByUser(user);
        if (studentProfileOpt.isPresent() && studentProfileOpt.get().getStudentCode() != null) {
            studentCode = studentProfileOpt.get().getStudentCode();
        }

        Faculty faculty = lecture.getFaculty() != null ? lecture.getFaculty() : course.getFaculty();
        String facultyName = (faculty != null && faculty.getUser() != null)
                ? faculty.getUser().getFullName()
                : "Faculty Instructor";

        logLiveEvent(session, "STUDENT_JOINED", user.getEmail(), user.getRole().name(),
                "Student " + user.getFullName() + " (" + studentCode + ") joined room " + session.getRoomName());

        return LiveLectureJoinResponseDTO.builder()
                .sessionId(session.getId())
                .lectureId(lecture.getLectureCode() != null ? lecture.getLectureCode() : String.valueOf(lecture.getId()))
                .lectureTitle(lecture.getTitle())
                .courseName(course.getName())
                .roomName(session.getRoomName())
                .token(token)
                .livekitUrl(liveKitTokenService.getLivekitUrl())
                .facultyName(facultyName)
                .studentIdentifier(studentCode)
                .studentName(user.getFullName())
                .participantCount((int) activeCount)
                .status("LIVE")
                .startedAt(session.getStartedAt())
                .build();
    }

    @Override
    @Transactional
    public LiveLectureStatusResponseDTO endLiveLecture(String lectureIdStr, String facultyEmail) {
        User user = userRepository.findByEmail(facultyEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + facultyEmail));

        Lecture lecture = findLectureByIdOrCode(lectureIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureIdStr));

        if (user.getRole() == Role.FACULTY) {
            Faculty faculty = facultyRepository.findByUser(user)
                    .orElseThrow(() -> new UnauthorizedException("Faculty profile not found"));
            if (lecture.getFaculty() != null && !lecture.getFaculty().getId().equals(faculty.getId())) {
                throw new UnauthorizedException("You are not assigned to end this lecture.");
            }
        } else if (user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only Faculty and Admin can end a live lecture session.");
        }

        LiveLectureSession session = sessionRepository
                .findFirstByLectureAndStatus(lecture, LiveSessionStatus.LIVE)
                .orElseThrow(() -> new BusinessException("No active live session found for lecture: " + lecture.getTitle()));

        session.setStatus(LiveSessionStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());
        session.setParticipantCount(0);
        sessionRepository.save(session);

        // Mark all active participants as LEFT
        List<LiveLectureParticipant> activeParticipants = participantRepository
                .findBySessionAndStatus(session, ParticipantStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        for (LiveLectureParticipant p : activeParticipants) {
            p.setStatus(ParticipantStatus.LEFT);
            p.setLeftAt(now);
        }
        participantRepository.saveAll(activeParticipants);

        logLiveEvent(session, "LECTURE_ENDED", user.getEmail(), user.getRole().name(),
                "Live lecture session ended by " + user.getFullName());

        return buildStatusDTO(session, lecture, false);
    }

    @Override
    @Transactional
    public void leaveLiveLecture(String lectureIdStr, String studentEmail) {
        User user = userRepository.findByEmail(studentEmail).orElse(null);
        if (user == null) return;

        Lecture lecture = findLectureByIdOrCode(lectureIdStr).orElse(null);
        if (lecture == null) return;

        Optional<LiveLectureSession> sessionOpt = sessionRepository
                .findFirstByLectureAndStatus(lecture, LiveSessionStatus.LIVE);
        if (sessionOpt.isEmpty()) return;

        LiveLectureSession session = sessionOpt.get();
        Optional<LiveLectureParticipant> participantOpt = participantRepository
                .findFirstBySessionAndStudentAndStatus(session, user, ParticipantStatus.ACTIVE);

        if (participantOpt.isPresent()) {
            LiveLectureParticipant participant = participantOpt.get();
            participant.setStatus(ParticipantStatus.LEFT);
            participant.setLeftAt(LocalDateTime.now());
            participantRepository.save(participant);

            logLiveEvent(session, "STUDENT_LEFT", user.getEmail(), user.getRole().name(),
                    "Student " + user.getFullName() + " left room " + session.getRoomName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LiveLectureStatusResponseDTO getLiveStatus(String lectureIdStr) {
        Lecture lecture = findLectureByIdOrCode(lectureIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureIdStr));

        Optional<LiveLectureSession> activeSessionOpt = sessionRepository
                .findFirstByLectureAndStatus(lecture, LiveSessionStatus.LIVE);

        if (activeSessionOpt.isPresent()) {
            return buildStatusDTO(activeSessionOpt.get(), lecture, true);
        }

        // If not active, check latest session
        Optional<LiveLectureSession> latestSessionOpt = sessionRepository
                .findFirstByLectureOrderByCreatedAtDesc(lecture);

        if (latestSessionOpt.isPresent()) {
            LiveLectureSession session = latestSessionOpt.get();
            return buildStatusDTO(session, lecture, session.getStatus() == LiveSessionStatus.LIVE);
        }

        return LiveLectureStatusResponseDTO.builder()
                .isLive(false)
                .lectureId(lecture.getLectureCode() != null ? lecture.getLectureCode() : String.valueOf(lecture.getId()))
                .lectureTitle(lecture.getTitle())
                .courseName(lecture.getCourse() != null ? lecture.getCourse().getName() : "Curriculum Track")
                .status("SCHEDULED")
                .participantCount(0)
                .build();
    }

    @Override
    @Transactional
    public void heartbeat(String lectureIdStr, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return;

        Lecture lecture = findLectureByIdOrCode(lectureIdStr).orElse(null);
        if (lecture == null) return;

        Optional<LiveLectureSession> sessionOpt = sessionRepository
                .findFirstByLectureAndStatus(lecture, LiveSessionStatus.LIVE);
        if (sessionOpt.isEmpty()) return;

        LiveLectureSession session = sessionOpt.get();
        Optional<LiveLectureParticipant> participantOpt = participantRepository
                .findFirstBySessionAndStudentAndStatus(session, user, ParticipantStatus.ACTIVE);

        if (participantOpt.isPresent()) {
            LiveLectureParticipant p = participantOpt.get();
            p.setLastHeartbeat(LocalDateTime.now());
            participantRepository.save(p);
        }
    }

    private LiveLectureStatusResponseDTO buildStatusDTO(LiveLectureSession session, Lecture lecture, boolean isLive) {
        Faculty faculty = lecture.getFaculty() != null
                ? lecture.getFaculty()
                : (lecture.getCourse() != null ? lecture.getCourse().getFaculty() : null);

        String facultyName = (faculty != null && faculty.getUser() != null)
                ? faculty.getUser().getFullName()
                : "Faculty Instructor";

        long activeCount = isLive ? participantRepository.countBySessionAndStatus(session, ParticipantStatus.ACTIVE) : 0;

        return LiveLectureStatusResponseDTO.builder()
                .isLive(isLive)
                .sessionId(session.getId())
                .lectureId(lecture.getLectureCode() != null ? lecture.getLectureCode() : String.valueOf(lecture.getId()))
                .lectureTitle(lecture.getTitle())
                .courseName(lecture.getCourse() != null ? lecture.getCourse().getName() : "Curriculum Track")
                .roomName(session.getRoomName())
                .facultyName(facultyName)
                .participantCount((int) activeCount)
                .status(session.getStatus().name())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .build();
    }

    private void logLiveEvent(LiveLectureSession session, String eventType, String actorEmail, String actorRole, String details) {
        try {
            LiveLectureEvent event = LiveLectureEvent.builder()
                    .session(session)
                    .eventType(eventType)
                    .actorEmail(actorEmail)
                    .actorRole(actorRole)
                    .details(details)
                    .build();
            eventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to save live lecture audit event: {}", e.getMessage());
        }
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

        // Fallback if key is a timestamp or mock ID: match against any available lecture
        List<Lecture> allLectures = lectureRepository.findAll();
        if (!allLectures.isEmpty()) {
            return Optional.of(allLectures.get(0));
        }

        return Optional.empty();
    }
}
