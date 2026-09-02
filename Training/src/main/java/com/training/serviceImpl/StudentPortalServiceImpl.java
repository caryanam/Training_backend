package com.training.serviceImpl;

import com.training.dto.responce.*;
import com.training.entity.*;
import com.training.enums.EnrollmentStatus;
import com.training.exception.ResourceNotFoundException;
import com.training.exception.UnauthorizedException;
import com.training.repo.*;
import com.training.service.StudentPortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentPortalServiceImpl implements StudentPortalService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureRepository lectureRepository;
    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StudentCourseResponseDTO> getEnrolledCourses(String studentEmail) {
        User student = resolveStudent(studentEmail);
        List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentAndStatus(student, EnrollmentStatus.ACTIVE);

        return activeEnrollments.stream()
                .filter(e -> e.getCourse() != null)
                .filter(e -> e.getExpiryDate() == null || !LocalDate.now().isAfter(e.getExpiryDate()))
                .map(enrollment -> {
                    Course course = enrollment.getCourse();
                    int lectureCount = (int) lectureRepository.countByCourse(course);
                    Faculty faculty = course.getFaculty();

                    List<CoursePlanResponseDTO> planDTOs = course.getPlans() != null
                            ? course.getPlans().stream().map(p -> CoursePlanResponseDTO.builder()
                                .id(p.getId())
                                .duration(p.getDuration())
                                .durationLabel(p.getDuration() != null ? p.getDuration().name().replace("_", " ") : "N/A")
                                .price(p.getPrice())
                                .currency(p.getCurrency())
                                .build()).collect(Collectors.toList())
                            : Collections.emptyList();

                    return StudentCourseResponseDTO.builder()
                            .courseId(course.getId())
                            .courseCode(course.getCourseCode())
                            .courseName(course.getName())
                            .category(course.getCategory() != null ? course.getCategory().name() : null)
                            .description(course.getDescription())
                            .facultyName(faculty != null && faculty.getUser() != null ? faculty.getUser().getFullName() : null)
                            .facultyEmail(faculty != null && faculty.getUser() != null ? faculty.getUser().getEmail() : null)
                            .enrollmentStatus(enrollment.getStatus().name())
                            .startDate(enrollment.getStartDate())
                            .expiryDate(enrollment.getExpiryDate())
                            .lectureCount(lectureCount)
                            .enrollmentCode(enrollment.getEnrollmentCode())
                            .plans(planDTOs)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentCourseResponseDTO getEnrolledCourseDetail(String studentEmail, String courseId) {
        User student = resolveStudent(studentEmail);
        Course course = findCourseByIdOrCode(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        Optional<Enrollment> activeEnrollment = enrollmentRepository
                .findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(student, course, EnrollmentStatus.ACTIVE);

        if (activeEnrollment.isEmpty()) {
            throw new UnauthorizedException("You are not enrolled in this course. Access denied.");
        }

        Enrollment enrollment = activeEnrollment.get();
        if (enrollment.getExpiryDate() != null && LocalDate.now().isAfter(enrollment.getExpiryDate())) {
            throw new UnauthorizedException("Your enrollment has expired. Please renew your plan.");
        }

        Faculty faculty = course.getFaculty();
        int lectureCount = (int) lectureRepository.countByCourse(course);

        List<CoursePlanResponseDTO> planDTOs = course.getPlans() != null
                ? course.getPlans().stream().map(p -> CoursePlanResponseDTO.builder()
                    .id(p.getId())
                    .duration(p.getDuration())
                    .durationLabel(p.getDuration() != null ? p.getDuration().name().replace("_", " ") : "N/A")
                    .price(p.getPrice())
                    .currency(p.getCurrency())
                    .build()).collect(Collectors.toList())
                : Collections.emptyList();

        return StudentCourseResponseDTO.builder()
                .courseId(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getName())
                .category(course.getCategory() != null ? course.getCategory().name() : null)
                .description(course.getDescription())
                .facultyName(faculty != null && faculty.getUser() != null ? faculty.getUser().getFullName() : null)
                .facultyEmail(faculty != null && faculty.getUser() != null ? faculty.getUser().getEmail() : null)
                .enrollmentStatus(enrollment.getStatus().name())
                .startDate(enrollment.getStartDate())
                .expiryDate(enrollment.getExpiryDate())
                .lectureCount(lectureCount)
                .enrollmentCode(enrollment.getEnrollmentCode())
                .plans(planDTOs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentLectureResponseDTO> getEnrolledLectures(String studentEmail) {
        User student = resolveStudent(studentEmail);
        List<Course> enrolledCourses = getActiveEnrolledCourses(student);

        if (enrolledCourses.isEmpty()) {
            return Collections.emptyList();
        }

        List<Lecture> lectures = lectureRepository.findByCourseIn(enrolledCourses);

        return lectures.stream().map(lec -> {
            Course course = lec.getCourse();
            return StudentLectureResponseDTO.builder()
                    .lectureId(lec.getLectureCode() != null ? lec.getLectureCode() : String.valueOf(lec.getId()))
                    .title(lec.getTitle())
                    .description(lec.getDescription())
                    .lectureDate(lec.getLectureDate())
                    .startTime(lec.getStartTime())
                    .endTime(lec.getEndTime())
                    .lectureUrl(lec.getLectureUrl())
                    .recordingUrl(lec.getRecordingUrl())
                    .isDownloadable(lec.getIsDownloadable())
                    .courseId(course.getId())
                    .courseCode(course.getCourseCode())
                    .courseName(course.getName())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentMeetingResponseDTO> getUpcomingMeetings(String studentEmail) {
        User student = resolveStudent(studentEmail);
        List<Course> enrolledCourses = getActiveEnrolledCourses(student);

        if (enrolledCourses.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        List<Lecture> lectures = lectureRepository.findByCourseIn(enrolledCourses);

        return lectures.stream()
                .filter(lec -> lec.getLectureDate() == null || !lec.getLectureDate().isBefore(today))
                .sorted(Comparator.comparing(Lecture::getLectureDate)
                        .thenComparing(lec -> lec.getStartTime() != null ? lec.getStartTime() : java.time.LocalTime.MIN))
                .map(lec -> {
                    Course course = lec.getCourse();
                    return StudentMeetingResponseDTO.builder()
                            .lectureId(lec.getLectureCode() != null ? lec.getLectureCode() : String.valueOf(lec.getId()))
                            .title(lec.getTitle())
                            .description(lec.getDescription())
                            .meetingDate(lec.getLectureDate())
                            .startTime(lec.getStartTime())
                            .endTime(lec.getEndTime())
                            .meetingLink(lec.getLectureUrl())
                            .courseId(course.getId())
                            .courseCode(course.getCourseCode())
                            .courseName(course.getName())
                            .status("SCHEDULED")
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DummyPaymentResponseDTO> getMyPayments(String studentEmail) {
        User student = resolveStudent(studentEmail);
        List<Payment> payments = paymentRepository.findByStudent(student);

        return payments.stream().map(payment -> {
            Enrollment enrollment = enrollmentRepository
                    .findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(student, payment.getCourse(), EnrollmentStatus.ACTIVE)
                    .orElse(null);

            return DummyPaymentResponseDTO.builder()
                    .paymentId(payment.getId())
                    .transactionId(payment.getTransactionId())
                    .studentId(String.valueOf(student.getId()))
                    .studentEmail(student.getEmail())
                    .courseId(payment.getCourse() != null ? payment.getCourse().getCourseCode() : null)
                    .courseName(payment.getCourse() != null ? payment.getCourse().getName() : null)
                    .planId(payment.getPlan() != null ? payment.getPlan().getId() : null)
                    .amount(payment.getAmount())
                    .currency(payment.getPlan() != null ? payment.getPlan().getCurrency() : "INR")
                    .status(payment.getPaymentStatus().name())
                    .enrollmentId(enrollment != null ? enrollment.getEnrollmentCode() : null)
                    .startDate(enrollment != null ? enrollment.getStartDate() : null)
                    .expiryDate(enrollment != null ? enrollment.getExpiryDate() : null)
                    .enrollmentStatus(enrollment != null ? enrollment.getStatus().name() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private User resolveStudent(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found: " + email));
    }

    private List<Course> getActiveEnrolledCourses(User student) {
        return enrollmentRepository.findByStudentAndStatus(student, EnrollmentStatus.ACTIVE)
                .stream()
                .filter(e -> e.getExpiryDate() == null || !LocalDate.now().isAfter(e.getExpiryDate()))
                .map(Enrollment::getCourse)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Optional<Course> findCourseByIdOrCode(String key) {
        if (key == null) return Optional.empty();
        Optional<Course> byCode = courseRepository.findByCourseCode(key.trim());
        if (byCode.isPresent()) return byCode;
        try {
            Long id = Long.parseLong(key.trim().replace("course-", "").replace("COURSE-", ""));
            return courseRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
