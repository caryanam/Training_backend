package com.training;

import com.training.dto.responce.LiveLectureJoinResponseDTO;
import com.training.dto.responce.LiveLectureStartResponseDTO;
import com.training.dto.responce.LiveLectureStatusResponseDTO;
import com.training.entity.*;
import com.training.enums.EnrollmentStatus;
import com.training.enums.LiveSessionStatus;
import com.training.enums.ParticipantStatus;
import com.training.enums.Role;
import com.training.exception.BusinessException;
import com.training.exception.UnauthorizedException;
import com.training.repo.*;
import com.training.service.LiveKitTokenService;
import com.training.serviceImpl.LiveLectureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LiveLectureServiceTest {

    @Mock
    private LectureRepository lectureRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FacultyRepository facultyRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private LiveLectureSessionRepository sessionRepository;
    @Mock
    private LiveLectureParticipantRepository participantRepository;
    @Mock
    private LiveLectureEventRepository eventRepository;
    @Mock
    private LiveKitTokenService liveKitTokenService;

    @InjectMocks
    private LiveLectureServiceImpl liveLectureService;

    private User facultyUser;
    private Faculty faculty;
    private User studentUser;
    private Course course;
    private Lecture lecture;
    private LiveLectureSession liveSession;

    @BeforeEach
    void setUp() {
        facultyUser = User.builder()
                .id(1L)
                .email("faculty@codex.com")
                .fullName("Prof. Alan")
                .role(Role.FACULTY)
                .build();

        faculty = Faculty.builder()
                .id(10L)
                .facultyCode("FAC-101")
                .user(facultyUser)
                .build();

        studentUser = User.builder()
                .id(2L)
                .email("student@codex.com")
                .fullName("John Doe")
                .role(Role.STUDENT)
                .build();

        course = Course.builder()
                .id(100L)
                .courseCode("course-100")
                .name("Spring Boot & WebRTC Mastery")
                .faculty(faculty)
                .build();

        lecture = Lecture.builder()
                .id(500L)
                .lectureCode("lecture-500")
                .title("Advanced WebSockets & SFU")
                .course(course)
                .faculty(faculty)
                .build();

        liveSession = LiveLectureSession.builder()
                .id(999L)
                .lecture(lecture)
                .roomName("room-lec-500-123456")
                .status(LiveSessionStatus.LIVE)
                .startedBy(facultyUser)
                .participantCount(0)
                .startedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Assigned faculty can start live lecture successfully")
    void testFacultyStartsLiveLecture_Success() {
        when(userRepository.findByEmail("faculty@codex.com")).thenReturn(Optional.of(facultyUser));
        when(lectureRepository.findByLectureCode("lecture-500")).thenReturn(Optional.of(lecture));
        when(facultyRepository.findByUser(facultyUser)).thenReturn(Optional.of(faculty));
        when(sessionRepository.findFirstByLectureAndStatus(lecture, LiveSessionStatus.LIVE)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(LiveLectureSession.class))).thenReturn(liveSession);
        when(liveKitTokenService.createFacultyToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("mock-faculty-livekit-jwt");
        when(liveKitTokenService.getLivekitUrl()).thenReturn("wss://livekit.internal");

        LiveLectureStartResponseDTO response = liveLectureService.startLiveLecture("lecture-500", "faculty@codex.com");

        assertNotNull(response);
        assertEquals("LIVE", response.getStatus());
        assertEquals("mock-faculty-livekit-jwt", response.getToken());
        assertEquals("Advanced WebSockets & SFU", response.getLectureTitle());
        verify(sessionRepository, times(1)).save(any(LiveLectureSession.class));
    }

    @Test
    @DisplayName("Unassigned faculty starting lecture throws UnauthorizedException")
    void testUnassignedFacultyCannotStartLecture() {
        Faculty otherFaculty = Faculty.builder().id(99L).user(facultyUser).build();
        when(userRepository.findByEmail("faculty@codex.com")).thenReturn(Optional.of(facultyUser));
        when(lectureRepository.findByLectureCode("lecture-500")).thenReturn(Optional.of(lecture));
        when(facultyRepository.findByUser(facultyUser)).thenReturn(Optional.of(otherFaculty));

        assertThrows(UnauthorizedException.class, () ->
                liveLectureService.startLiveLecture("lecture-500", "faculty@codex.com"));
    }

    @Test
    @DisplayName("Enrolled student can join active live lecture and receives subscribe-only token")
    void testEnrolledStudentJoinsLiveLecture_Success() {
        Enrollment activeEnrollment = Enrollment.builder()
                .student(studentUser)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusMonths(3))
                .build();

        when(userRepository.findByEmail("student@codex.com")).thenReturn(Optional.of(studentUser));
        when(lectureRepository.findByLectureCode("lecture-500")).thenReturn(Optional.of(lecture));
        when(enrollmentRepository.findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(
                studentUser, course, EnrollmentStatus.ACTIVE)).thenReturn(Optional.of(activeEnrollment));
        when(sessionRepository.findFirstByLectureAndStatus(lecture, LiveSessionStatus.LIVE)).thenReturn(Optional.of(liveSession));
        when(participantRepository.findFirstBySessionAndStudentAndStatus(
                liveSession, studentUser, ParticipantStatus.ACTIVE)).thenReturn(Optional.empty());
        when(participantRepository.countBySessionAndStatus(liveSession, ParticipantStatus.ACTIVE)).thenReturn(1L);
        when(liveKitTokenService.createStudentToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("mock-student-subscribe-jwt");
        when(liveKitTokenService.getLivekitUrl()).thenReturn("wss://livekit.internal");

        LiveLectureJoinResponseDTO response = liveLectureService.joinLiveLecture("lecture-500", "student@codex.com");

        assertNotNull(response);
        assertEquals("LIVE", response.getStatus());
        assertEquals("mock-student-subscribe-jwt", response.getToken());
        assertEquals("John Doe", response.getStudentName());
        verify(participantRepository, times(1)).save(any(LiveLectureParticipant.class));
    }

    @Test
    @DisplayName("Non-enrolled student is rejected from joining live lecture")
    void testNonEnrolledStudentCannotJoin() {
        when(userRepository.findByEmail("student@codex.com")).thenReturn(Optional.of(studentUser));
        when(lectureRepository.findByLectureCode("lecture-500")).thenReturn(Optional.of(lecture));
        when(enrollmentRepository.findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(
                studentUser, course, EnrollmentStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () ->
                liveLectureService.joinLiveLecture("lecture-500", "student@codex.com"));
    }

    @Test
    @DisplayName("Student cannot join when lecture is not currently live")
    void testStudentCannotJoinNotLiveLecture() {
        Enrollment activeEnrollment = Enrollment.builder()
                .student(studentUser)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusMonths(1))
                .build();

        when(userRepository.findByEmail("student@codex.com")).thenReturn(Optional.of(studentUser));
        when(lectureRepository.findByLectureCode("lecture-500")).thenReturn(Optional.of(lecture));
        when(enrollmentRepository.findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(
                studentUser, course, EnrollmentStatus.ACTIVE)).thenReturn(Optional.of(activeEnrollment));
        when(sessionRepository.findFirstByLectureAndStatus(lecture, LiveSessionStatus.LIVE)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () ->
                liveLectureService.joinLiveLecture("lecture-500", "student@codex.com"));
    }

    @Test
    @DisplayName("Faculty ends live lecture successfully")
    void testFacultyEndsLiveLecture_Success() {
        when(userRepository.findByEmail("faculty@codex.com")).thenReturn(Optional.of(facultyUser));
        when(lectureRepository.findByLectureCode("lecture-500")).thenReturn(Optional.of(lecture));
        when(facultyRepository.findByUser(facultyUser)).thenReturn(Optional.of(faculty));
        when(sessionRepository.findFirstByLectureAndStatus(lecture, LiveSessionStatus.LIVE)).thenReturn(Optional.of(liveSession));

        LiveLectureStatusResponseDTO response = liveLectureService.endLiveLecture("lecture-500", "faculty@codex.com");

        assertNotNull(response);
        assertFalse(response.isLive());
        assertEquals(LiveSessionStatus.ENDED, liveSession.getStatus());
        verify(sessionRepository, times(1)).save(liveSession);
    }
}
