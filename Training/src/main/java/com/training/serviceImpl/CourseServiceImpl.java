package com.training.serviceImpl;

import com.training.dto.request.CreateCourseRequest;
import com.training.dto.request.UpdateCourseRequest;
import com.training.dto.responce.CoursePlanResponseDTO;
import com.training.dto.responce.CourseResponseDTO;
import com.training.entity.Course;
import com.training.entity.CoursePlan;
import com.training.enums.CourseStatus;
import com.training.enums.EnrollmentStatus;
import com.training.enums.PlanDuration;
import com.training.exception.BadRequestException;
import com.training.exception.DuplicateResourceException;
import com.training.exception.ResourceNotFoundException;
import com.training.repo.CoursePlanRepository;
import com.training.repo.CourseRepository;
import com.training.repo.EnrollmentRepository;
import com.training.repo.FacultyRepository;
import com.training.repo.LectureRepository;
import com.training.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final FacultyRepository facultyRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Cacheable(value = "courses")
    public List<CourseResponseDTO> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public CourseResponseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        return toDTO(course);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public CourseResponseDTO createCourse(CreateCourseRequest dto) {

        if (dto.getTitle() == null || dto.getTitle().trim().length() < 3) {
            throw new BadRequestException("Course title must be at least 3 characters");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().length() < 10) {
            throw new BadRequestException("Description must be at least 10 characters");
        }

        // 1. Duplicate title check
        if (courseRepository.existsByName(dto.getTitle())) {
            throw new DuplicateResourceException("A course with this title already exists: " + dto.getTitle());
        }

        // 2. Duplicate plan duration check
        validateNoDuplicateDurations(dto.getPlans().stream()
                .map(p -> p.getDuration())
                .collect(Collectors.toList()));

        // 3. Build and save Course
        com.training.entity.Faculty faculty = findFacultyByIdOrCode(dto.getFacultyId());
        Course course = Course.builder()
                .name(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .status(dto.getStatus() != null ? dto.getStatus() : CourseStatus.ACTIVE)
                .faculty(faculty)
                .plans(new ArrayList<>())
                .curriculum(new ArrayList<>())
                .build();

        course = courseRepository.save(course);

        String courseCode = "COURSE-" + (1000 + course.getId());
        course.setCourseCode(courseCode);
        course = courseRepository.save(course);

        // 4. Build and save Plans atomically (within same transaction)
        for (CreateCourseRequest.CoursePlanRequest planReq : dto.getPlans()) {
            CoursePlan plan = CoursePlan.builder()
                    .course(course)
                    .duration(planReq.getDuration())
                    .price(planReq.getPrice())
                    .currency("INR")
                    .build();
            coursePlanRepository.save(plan);
        }

        return toDTO(courseRepository.findById(course.getId()).orElse(course));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public CourseResponseDTO updateCourse(Long id, UpdateCourseRequest dto) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        // Title uniqueness check (excluding current record)
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            if (courseRepository.existsByNameAndIdNot(dto.getTitle(), id)) {
                throw new DuplicateResourceException("A course with this title already exists: " + dto.getTitle());
            }
            course.setName(dto.getTitle());
        }

        if (dto.getDescription() != null) {
            course.setDescription(dto.getDescription());
        }
        if (dto.getCategory() != null) {
            course.setCategory(dto.getCategory());
        }
        if (dto.getStatus() != null) {
            course.setStatus(dto.getStatus());
        }
        if (dto.getFacultyId() != null && !dto.getFacultyId().trim().isEmpty()) {
            com.training.entity.Faculty faculty = findFacultyByIdOrCode(dto.getFacultyId());
            course.setFaculty(faculty);
        }

        course = courseRepository.save(course);

        // Replace plans if provided
        if (dto.getPlans() != null && !dto.getPlans().isEmpty()) {
            validateNoDuplicateDurations(dto.getPlans().stream()
                    .map(UpdateCourseRequest.CoursePlanUpdateRequest::getDuration)
                    .collect(Collectors.toList()));

            coursePlanRepository.deleteAllByCourse(course);
            coursePlanRepository.flush();

            for (UpdateCourseRequest.CoursePlanUpdateRequest planReq : dto.getPlans()) {
                CoursePlan plan = CoursePlan.builder()
                        .course(course)
                        .duration(planReq.getDuration())
                        .price(planReq.getPrice())
                        .currency("INR")
                        .build();
                coursePlanRepository.save(plan);
            }
        }

        return toDTO(courseRepository.findById(course.getId()).orElse(course));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        // Soft-delete if enrollments or lectures exist to preserve historical data
        boolean hasEnrollments = enrollmentRepository.existsByCourse(course);
        long lectureCount = lectureRepository.countByCourse(course);

        if (hasEnrollments || lectureCount > 0) {
            course.setStatus(CourseStatus.INACTIVE);
            courseRepository.save(course);
        } else {
            // Safe to hard-delete — no historical data
            coursePlanRepository.deleteAllByCourse(course);
            courseRepository.delete(course);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: Course → DTO
    // ─────────────────────────────────────────────────────────────────────────
    private CourseResponseDTO toDTO(Course course) {
        List<CoursePlan> plans = coursePlanRepository.findByCourse(course);

        long lectureCount = lectureRepository.countByCourse(course);
        long activeStudents = enrollmentRepository.countByCourseAndStatus(course, EnrollmentStatus.ACTIVE);

        List<CoursePlanResponseDTO> planDTOs = plans.stream()
                .map(p -> CoursePlanResponseDTO.builder()
                        .id(p.getId())
                        .duration(p.getDuration())
                        .durationLabel(toDurationLabel(p.getDuration()))
                        .price(p.getPrice())
                        .currency(p.getCurrency())
                        .build())
                .collect(Collectors.toList());

        com.training.entity.Faculty faculty = course.getFaculty();
        String facId = faculty != null ? (faculty.getFacultyCode() != null ? faculty.getFacultyCode() : "FAC-" + faculty.getId()) : "FAC-2001";
        String facName = faculty != null && faculty.getUser() != null ? faculty.getUser().getFullName() : "Dr. Rajesh Sharma";
        String facEmail = faculty != null && faculty.getUser() != null ? faculty.getUser().getEmail() : "faculty@codex.com";
        String facPhone = faculty != null && faculty.getUser() != null ? faculty.getUser().getPhone() : "9876543210";

        return CourseResponseDTO.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .title(course.getName())
                .description(course.getDescription())
                .category(course.getCategory())
                .status(course.getStatus())
                .lectureCount((int) lectureCount)
                .activeStudentCount((int) activeStudents)
                .facultyId(facId)
                .facultyName(facName)
                .facultyEmail(facEmail)
                .facultyPhone(facPhone)
                .plans(planDTOs)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: Enum → Human-readable label
    // ─────────────────────────────────────────────────────────────────────────
        private com.training.entity.Faculty findFacultyByIdOrCode(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        String cleanKey = key.trim();
        java.util.Optional<com.training.entity.Faculty> byCode = facultyRepository.findByFacultyCode(cleanKey);
        if (byCode.isPresent()) return byCode.get();

        java.util.Optional<com.training.entity.Faculty> byEmail = facultyRepository.findByUserEmail(cleanKey);
        if (byEmail.isPresent()) return byEmail.get();

        try {
            Long id = Long.parseLong(cleanKey.replace("FAC-", "").replace("fac-prof-", ""));
            return facultyRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toDurationLabel(PlanDuration duration) {
        if (duration == null) return "";
        return switch (duration) {
            case ONE_MONTH -> "1 Month";
            case TWO_MONTHS -> "2 Months";
            case THREE_MONTHS -> "3 Months";
            case SIX_MONTHS -> "6 Months";
            case TWELVE_MONTHS -> "12 Months";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: Validate no duplicate durations in plan list
    // ─────────────────────────────────────────────────────────────────────────
    private void validateNoDuplicateDurations(List<PlanDuration> durations) {
        Set<PlanDuration> seen = new HashSet<>();
        for (PlanDuration d : durations) {
            if (!seen.add(d)) {
                throw new BadRequestException("Duplicate plan duration submitted: " + d
                        + ". Each duration (ONE_MONTH, TWO_MONTHS, THREE_MONTHS) can only appear once per course.");
            }
        }
    }
}



