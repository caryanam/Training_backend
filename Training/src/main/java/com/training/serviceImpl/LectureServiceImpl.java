package com.training.serviceImpl;

import com.training.dto.request.CreateLectureDTO;
import com.training.dto.responce.LectureAccessResponseDTO;
import com.training.dto.responce.LectureResponseDTO;
import com.training.entity.*;
import com.training.enums.EnrollmentStatus;
import com.training.enums.Role;
import com.training.exception.ResourceNotFoundException;
import com.training.exception.UnauthorizedException;
import com.training.repo.*;
import com.training.service.LectureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LectureServiceImpl implements LectureService {

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public LectureResponseDTO createLecture(CreateLectureDTO dto) {
        return createLecture(dto, null);
    }

    @Override
    @Transactional
    public LectureResponseDTO createLecture(CreateLectureDTO dto, String authenticatedEmail) {
        Course course = findCourseByIdOrCode(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + dto.getCourseId()));

        Faculty faculty = null;

        // Verify authenticated user's role and course ownership if faculty
        if (authenticatedEmail != null) {
            Optional<User> userOpt = userRepository.findByEmail(authenticatedEmail);
            if (userOpt.isPresent()) {
                User authUser = userOpt.get();
                if (authUser.getRole() == Role.FACULTY) {
                    faculty = facultyRepository.findByUser(authUser).orElse(null);

                    // Validate faculty assignment on the course
                    if (faculty != null && course.getFaculty() != null && !course.getFaculty().getId().equals(faculty.getId())) {
                        throw new UnauthorizedException("You are not assigned to manage this course: " + course.getName());
                    }
                    // Auto-assign faculty if unassigned
                    if (course.getFaculty() == null && faculty != null) {
                        course.setFaculty(faculty);
                        courseRepository.save(course);
                    }
                }
            }
        }

        if (faculty == null && dto.getFacultyId() != null && !dto.getFacultyId().isEmpty()) {
            faculty = findFacultyByIdOrCode(dto.getFacultyId()).orElse(null);
        }

        if (faculty == null && course.getFaculty() != null) {
            faculty = course.getFaculty();
        }

        Lecture lecture = Lecture.builder()
                .course(course)
                .faculty(faculty)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .lectureDate(dto.getLectureDate())
                .startTime(dto.getParsedStartTime())
                .endTime(dto.getParsedEndTime())
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
    @Transactional(readOnly = true)
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
            return buildLectureAccessResponse(true, "Access granted", lecture, course);
        }

        // Student Access Validation
        Optional<Enrollment> activeEnrollment = enrollmentRepository
                .findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(user, course, EnrollmentStatus.ACTIVE);

        if (activeEnrollment.isEmpty()) {
            return buildLectureAccessResponse(false, "No active enrollment found for this course. Please purchase a plan.", lecture, course);
        }

        Enrollment enrollment = activeEnrollment.get();
        LocalDate today = LocalDate.now();

        if (enrollment.getExpiryDate() != null && today.isAfter(enrollment.getExpiryDate())) {
            return buildLectureAccessResponse(false, "Your course access has expired. Please renew your plan.", lecture, course);
        }

        return buildLectureAccessResponse(true, "Access granted", lecture, course);
    }

    private LectureAccessResponseDTO buildLectureAccessResponse(boolean hasAccess, String reason, Lecture lecture, Course course) {
        Faculty faculty = (lecture != null && lecture.getFaculty() != null)
                ? lecture.getFaculty()
                : (course != null ? course.getFaculty() : null);

        String facultyName = faculty != null && faculty.getUser() != null ? faculty.getUser().getFullName() : null;
        String facultyEmail = faculty != null && faculty.getUser() != null ? faculty.getUser().getEmail() : null;
        String facultyCode = faculty != null ? faculty.getFacultyCode() : null;
        String facultyDepartment = faculty != null ? faculty.getDepartment() : null;

        return LectureAccessResponseDTO.builder()
                .hasAccess(hasAccess)
                .reason(reason)
                .lectureId(lecture != null ? (lecture.getLectureCode() != null ? lecture.getLectureCode() : String.valueOf(lecture.getId())) : null)
                .title(lecture != null ? lecture.getTitle() : null)
                .description(lecture != null ? lecture.getDescription() : null)
                .lectureDate(lecture != null ? lecture.getLectureDate() : null)
                .startTime(lecture != null ? lecture.getStartTime() : null)
                .endTime(lecture != null ? lecture.getEndTime() : null)
                .lectureUrl(hasAccess && lecture != null ? lecture.getLectureUrl() : null)
                .meetingLink(hasAccess && lecture != null ? lecture.getLectureUrl() : null)
                .recordingUrl(hasAccess && lecture != null ? lecture.getRecordingUrl() : null)
                .isDownloadable(lecture != null ? lecture.getIsDownloadable() : false)
                .courseId(course != null ? course.getId() : null)
                .courseCode(course != null ? course.getCourseCode() : null)
                .courseName(course != null ? course.getName() : null)
                .facultyName(facultyName)
                .facultyCode(facultyCode)
                .facultyEmail(facultyEmail)
                .facultyDepartment(facultyDepartment)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LectureResponseDTO> getFacultyLectures(String facultyEmail) {
        if (facultyEmail == null || facultyEmail.isBlank()) {
            return lectureRepository.findAll().stream().map(this::mapToLectureDTO).toList();
        }

        User user = userRepository.findByEmail(facultyEmail).orElse(null);
        if (user == null) {
            return lectureRepository.findAll().stream().map(this::mapToLectureDTO).toList();
        }

        List<Lecture> lectures;
        if (user.getRole() == Role.ADMIN) {
            lectures = lectureRepository.findAll();
        } else if (user.getRole() == Role.FACULTY) {
            Optional<Faculty> facultyOpt = facultyRepository.findByUser(user);
            if (facultyOpt.isPresent()) {
                Faculty faculty = facultyOpt.get();
                List<Course> courses = courseRepository.findByFaculty(faculty);
                if (!courses.isEmpty()) {
                    lectures = lectureRepository.findByCourseIn(courses);
                } else {
                    lectures = lectureRepository.findAll();
                }
            } else {
                lectures = lectureRepository.findAll();
            }
        } else {
            lectures = lectureRepository.findAll();
        }

        return lectures.stream().map(this::mapToLectureDTO).toList();
    }

    private LectureResponseDTO mapToLectureDTO(Lecture l) {
        Course course = l.getCourse();
        return LectureResponseDTO.builder()
                .lectureId(l.getLectureCode() != null ? l.getLectureCode() : "lecture-" + l.getId())
                .courseId(course != null ? (course.getCourseCode() != null ? course.getCourseCode() : "course-" + course.getId()) : null)
                .courseName(course != null ? course.getName() : null)
                .title(l.getTitle())
                .description(l.getDescription())
                .lectureDate(l.getLectureDate())
                .startTime(l.getStartTime())
                .endTime(l.getEndTime())
                .lectureUrl(l.getLectureUrl())
                .recordingUrl(l.getRecordingUrl())
                .isDownloadable(l.getIsDownloadable())
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
