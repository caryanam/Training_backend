package com.training;

import com.training.dto.request.ReportSecurityEventDTO;
import com.training.dto.responce.SecurityPolicyStatusDTO;
import com.training.entity.*;
import com.training.enums.EnrollmentStatus;
import com.training.enums.Role;
import com.training.enums.SecurityEventSeverity;
import com.training.enums.SecurityEventType;
import com.training.exception.UnauthorizedException;
import com.training.repo.*;
import com.training.serviceImpl.LectureSecurityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LectureSecurityServiceTest {

    @Mock
    private LectureSecurityEventRepository securityEventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LectureRepository lectureRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private FacultyRepository facultyRepository;
    @Mock
    private LiveLectureSessionRepository sessionRepository;

    @InjectMocks
    private LectureSecurityServiceImpl securityService;

    private User studentUser;
    private User facultyUser;
    private Course course;
    private Lecture lecture;
    private Enrollment activeEnrollment;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(10L)
                .email("student@test.com")
                .fullName("Test Student")
                .role(Role.STUDENT)
                .build();

        facultyUser = User.builder()
                .id(2L)
                .email("faculty@test.com")
                .fullName("Test Faculty")
                .role(Role.FACULTY)
                .build();

        course = Course.builder()
                .id(100L)
                .courseCode("COURSE-100")
                .name("Full Stack Web Development")
                .build();

        lecture = Lecture.builder()
                .id(50L)
                .lectureCode("lecture-50")
                .title("Spring Boot Security")
                .course(course)
                .build();

        activeEnrollment = Enrollment.builder()
                .id(1L)
                .student(studentUser)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusMonths(3))
                .build();
    }

    @Test
    @DisplayName("Should record SCREEN_SHARE_STARTED event and return warning policy")
    void testRecordScreenShareStarted() {
        ReportSecurityEventDTO dto = ReportSecurityEventDTO.builder()
                .lectureId("lecture-50")
                .eventType(SecurityEventType.SCREEN_SHARE_STARTED)
                .metadata("displaySurface:monitor")
                .timestamp(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(lectureRepository.findByLectureCode("lecture-50")).thenReturn(Optional.of(lecture));
        when(enrollmentRepository.findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(studentUser, course, EnrollmentStatus.ACTIVE))
                .thenReturn(Optional.of(activeEnrollment));
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.empty());
        when(securityEventRepository.countByLectureAndStudentAndSeverityIn(eq(lecture), eq(studentUser), anyList()))
                .thenReturn(1L);

        SecurityPolicyStatusDTO status = securityService.recordSecurityEvent(dto, "student@test.com");

        assertNotNull(status);
        assertEquals(1, status.getViolationCount());
        assertFalse(status.isSuspended());
        assertEquals("WARNING", status.getWarningLevel());
        verify(securityEventRepository, times(1)).save(any(LectureSecurityEvent.class));
    }

    @Test
    @DisplayName("Should enforce termination policy when 3 high-severity violations occur")
    void testRepeatedViolationTermination() {
        ReportSecurityEventDTO dto = ReportSecurityEventDTO.builder()
                .lectureId("lecture-50")
                .eventType(SecurityEventType.SCREEN_SHARE_STARTED)
                .timestamp(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(lectureRepository.findByLectureCode("lecture-50")).thenReturn(Optional.of(lecture));
        when(enrollmentRepository.findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(studentUser, course, EnrollmentStatus.ACTIVE))
                .thenReturn(Optional.of(activeEnrollment));
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.empty());
        when(securityEventRepository.countByLectureAndStudentAndSeverityIn(eq(lecture), eq(studentUser), anyList()))
                .thenReturn(3L);

        SecurityPolicyStatusDTO status = securityService.recordSecurityEvent(dto, "student@test.com");

        assertNotNull(status);
        assertEquals(3, status.getViolationCount());
        assertTrue(status.isSuspended());
        assertEquals("TERMINATED", status.getWarningLevel());
    }

    @Test
    @DisplayName("Should reject security event if student is not enrolled")
    void testUnenrolledStudentRejected() {
        ReportSecurityEventDTO dto = ReportSecurityEventDTO.builder()
                .lectureId("lecture-50")
                .eventType(SecurityEventType.SCREEN_SHARE_STARTED)
                .build();

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(lectureRepository.findByLectureCode("lecture-50")).thenReturn(Optional.of(lecture));
        when(enrollmentRepository.findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(studentUser, course, EnrollmentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> securityService.recordSecurityEvent(dto, "student@test.com"));
        verify(securityEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should suppress transient duplicate events occurring within 2 seconds")
    void testDeduplicateTransientEvents() {
        ReportSecurityEventDTO dto = ReportSecurityEventDTO.builder()
                .lectureId("lecture-50")
                .eventType(SecurityEventType.WINDOW_BLUR)
                .timestamp(LocalDateTime.now())
                .build();

        LectureSecurityEvent recent = LectureSecurityEvent.builder()
                .id(1L)
                .eventType(SecurityEventType.WINDOW_BLUR)
                .timestamp(LocalDateTime.now().minusSeconds(1))
                .build();

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(lectureRepository.findByLectureCode("lecture-50")).thenReturn(Optional.of(lecture));
        when(enrollmentRepository.findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(studentUser, course, EnrollmentStatus.ACTIVE))
                .thenReturn(Optional.of(activeEnrollment));
        when(securityEventRepository.findFirstByLectureAndStudentAndEventTypeOrderByTimestampDesc(lecture, studentUser, SecurityEventType.WINDOW_BLUR))
                .thenReturn(Optional.of(recent));

        SecurityPolicyStatusDTO status = securityService.recordSecurityEvent(dto, "student@test.com");

        assertNotNull(status);
        // Ensure no duplicate save occurred
        verify(securityEventRepository, never()).save(any(LectureSecurityEvent.class));
    }
    @Test
    @DisplayName("Should record SCREEN_RECORDING_ATTEMPT event and return warning policy")
    void testRecordScreenRecordingAttempt() {
        ReportSecurityEventDTO dto = ReportSecurityEventDTO.builder()
                .lectureId("lecture-50")
                .eventType(SecurityEventType.SCREEN_RECORDING_ATTEMPT)
                .metadata("key:Ctrl+Alt+R;platform:Win32")
                .timestamp(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(lectureRepository.findByLectureCode("lecture-50")).thenReturn(Optional.of(lecture));
        when(enrollmentRepository.findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(studentUser, course, EnrollmentStatus.ACTIVE))
                .thenReturn(Optional.of(activeEnrollment));
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.empty());
        when(securityEventRepository.countByLectureAndStudentAndSeverityIn(eq(lecture), eq(studentUser), anyList()))
                .thenReturn(1L);

        SecurityPolicyStatusDTO status = securityService.recordSecurityEvent(dto, "student@test.com");

        assertNotNull(status);
        assertEquals(1, status.getViolationCount());
        assertFalse(status.isSuspended());
        assertEquals("WARNING", status.getWarningLevel());
        verify(securityEventRepository, times(1)).save(any(LectureSecurityEvent.class));
    }

    @Test
    @DisplayName("Should record SCREENSHOT_ATTEMPT event and return warning policy")
    void testRecordScreenshotAttempt() {
        ReportSecurityEventDTO dto = ReportSecurityEventDTO.builder()
                .lectureId("lecture-50")
                .eventType(SecurityEventType.SCREENSHOT_ATTEMPT)
                .metadata("key:PrintScreen;platform:Win32")
                .timestamp(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(lectureRepository.findByLectureCode("lecture-50")).thenReturn(Optional.of(lecture));
        when(enrollmentRepository.findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(studentUser, course, EnrollmentStatus.ACTIVE))
                .thenReturn(Optional.of(activeEnrollment));
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.empty());
        when(securityEventRepository.countByLectureAndStudentAndSeverityIn(eq(lecture), eq(studentUser), anyList()))
                .thenReturn(2L);

        SecurityPolicyStatusDTO status = securityService.recordSecurityEvent(dto, "student@test.com");

        assertNotNull(status);
        assertEquals(2, status.getViolationCount());
        assertFalse(status.isSuspended());
        assertEquals("STRONG_WARNING", status.getWarningLevel());
        verify(securityEventRepository, times(1)).save(any(LectureSecurityEvent.class));
    }
}
