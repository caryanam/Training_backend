package com.training.serviceImpl;

import com.training.dto.request.CreateLectureDTO;
import com.training.dto.responce.LectureAccessResponseDTO;
import com.training.dto.responce.LectureResponseDTO;
import com.training.entity.*;
import com.training.enums.EnrollmentStatus;
import com.training.enums.Role;
import com.training.exception.ResourceNotFoundException;
import com.training.repo.*;
import com.training.service.LectureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LectureServiceImpl implements LectureService {

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public LectureResponseDTO createLecture(CreateLectureDTO dto) {
        Course course = findCourseByIdOrCode(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + dto.getCourseId()));

        Faculty faculty = null;
        if (dto.getFacultyId() != null && !dto.getFacultyId().isEmpty()) {
            faculty = findFacultyByIdOrCode(dto.getFacultyId()).orElse(null);
        }

        Lecture lecture = Lecture.builder()
                .course(course)
                .faculty(faculty)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .lectureDate(dto.getLectureDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .lectureUrl(dto.getLectureUrl())
                .recordingUrl(dto.getRecordingUrl())
                .isDownloadable(dto.getIsDownloadable())
                .build();
        lecture = lectureRepository.save(lecture);

        String lectureCode = "lecture-" + (100 + lecture.getId());
        lecture.setLectureCode(lectureCode);
        lecture = lectureRepository.save(lecture);

        return LectureResponseDTO.builder()
                .lectureId(lectureCode)
                .courseId(course.getCourseCode() != null ? course.getCourseCode() : "course-" + course.getId())
                .title(lecture.getTitle())
                .description(lecture.getDescription())
                .lectureDate(lecture.getLectureDate())
                .startTime(lecture.getStartTime())
                .endTime(lecture.getEndTime())
                .lectureUrl(lecture.getLectureUrl())
                .recordingUrl(lecture.getRecordingUrl())
                .isDownloadable(lecture.getIsDownloadable())
                .build();
    }

    @Override
    public LectureAccessResponseDTO getLectureAccess(String lectureIdStr, String userEmail) {
        Lecture lecture = findLectureByIdOrCode(lectureIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureIdStr));

        Course course = lecture.getCourse();
        if (course == null) {
            throw new ResourceNotFoundException("Associated course not found");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        // ADMIN and FACULTY have full access
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.FACULTY) {
            return LectureAccessResponseDTO.builder()
                    .hasAccess(true)
                    .reason("Access granted")
                    .lectureUrl(lecture.getLectureUrl())
                    .recordingUrl(lecture.getRecordingUrl())
                    .build();
        }

        // Student Access Validation
        Optional<Enrollment> activeEnrollment = enrollmentRepository
                .findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(user, course, EnrollmentStatus.ACTIVE);

        if (activeEnrollment.isEmpty()) {
            return LectureAccessResponseDTO.builder()
                    .hasAccess(false)
                    .reason("No active enrollment found for this course. Please purchase a plan.")
                    .lectureUrl(null)
                    .recordingUrl(null)
                    .build();
        }

        Enrollment enrollment = activeEnrollment.get();
        LocalDate today = LocalDate.now();

        if (enrollment.getExpiryDate() != null && today.isAfter(enrollment.getExpiryDate())) {
            return LectureAccessResponseDTO.builder()
                    .hasAccess(false)
                    .reason("Your course access has expired. Please renew your plan.")
                    .lectureUrl(null)
                    .recordingUrl(null)
                    .build();
        }

        return LectureAccessResponseDTO.builder()
                .hasAccess(true)
                .reason("Access granted")
                .lectureUrl(lecture.getLectureUrl())
                .recordingUrl(lecture.getRecordingUrl())
                .build();
    }

    private Optional<Faculty> findFacultyByIdOrCode(String key) {
        Optional<Faculty> byCode = facultyRepository.findByFacultyCode(key);
        if (byCode.isPresent()) return byCode;

        try {
            Long id = Long.parseLong(key.replace("FAC-", "").replace("fac-rec-", "").replace("fac-prof-", "").replace("fac-", ""));
            return facultyRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Course> findCourseByIdOrCode(String key) {
        Optional<Course> byCode = courseRepository.findByCourseCode(key);
        if (byCode.isPresent()) return byCode;

        try {
            Long id = Long.parseLong(key.replace("course-", ""));
            return courseRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Lecture> findLectureByIdOrCode(String key) {
        Optional<Lecture> byCode = lectureRepository.findByLectureCode(key);
        if (byCode.isPresent()) return byCode;

        try {
            Long id = Long.parseLong(key.replace("lecture-", ""));
            return lectureRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
