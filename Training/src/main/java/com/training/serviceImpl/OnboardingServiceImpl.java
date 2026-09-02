package com.training.serviceImpl;

import com.training.dto.request.StudentOnboardingDTO;
import com.training.dto.responce.StudentOnboardingResponseDTO;
import com.training.entity.*;
import com.training.enums.*;
import com.training.exception.BadRequestException;
import com.training.exception.DuplicateResourceException;
import com.training.exception.ResourceNotFoundException;
import com.training.exception.UnauthorizedException;
import com.training.notification.NotificationService;
import com.training.repo.*;
import com.training.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentLeadRepository studentLeadRepository;
    private final CourseRepository courseRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final FacultyRepository facultyRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OnboardingAuditRepository onboardingAuditRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Override
    @Transactional
    @CacheEvict(value = {"dashboardStats", "courses"}, allEntries = true)
    public StudentOnboardingResponseDTO onboardStudent(StudentOnboardingDTO dto, String executorEmail) {
        log.info("[ONBOARDING] Initiating onboarding workflow by executor: {}, payload: {}", executorEmail, dto);

        // 1. Authenticated Executor Validation
        User executorUser = null;
        if (executorEmail != null && !executorEmail.trim().isEmpty()) {
            executorUser = userRepository.findByEmail(executorEmail.trim()).orElse(null);
        }
        if (executorUser == null) {
            throw new UnauthorizedException("Executor authentication required for onboarding.");
        }
        if (executorUser.getRole() != Role.ADMIN && executorUser.getRole() != Role.EXECUTOR) {
            throw new UnauthorizedException("Access denied: Only ADMIN and EXECUTOR roles can onboard students.");
        }

        // 2. Syllabus & Schedule Confirmation Validation
        if (Boolean.FALSE.equals(dto.getDirectEnrollment())) {
            if (Boolean.FALSE.equals(dto.getSyllabusExplained()) 
                    || Boolean.FALSE.equals(dto.getScheduleExplained()) 
                    || Boolean.FALSE.equals(dto.getValidityExplained())) {
                throw new BadRequestException("Syllabus, schedule, and plan validity confirmations are mandatory for student onboarding.");
            }
        }

        // 3. Course Validation
        Course course = findCourseByIdOrCode(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + dto.getCourseId()));

        if (course.getStatus() != null && course.getStatus() != CourseStatus.ACTIVE) {
            throw new BadRequestException("Course is currently inactive: " + course.getName());
        }

        // 4. Plan Validation
        CoursePlan plan = findPlanByIdOrCode(dto.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Course plan not found: " + dto.getPlanId()));

        if (plan.getCourse() != null && !plan.getCourse().getId().equals(course.getId())) {
            throw new BadRequestException("Selected plan (" + plan.getId() + ") does not belong to course (" + course.getName() + ")");
        }

        // 5. Faculty Validation (optional / course-level)
        Faculty faculty = null;
        if (dto.getFacultyId() != null && !dto.getFacultyId().trim().isEmpty()) {
            faculty = findFacultyByIdOrCode(dto.getFacultyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Faculty not found: " + dto.getFacultyId()));
            if ("INACTIVE".equalsIgnoreCase(faculty.getStatus())) {
                throw new BadRequestException("Selected faculty is inactive: " + faculty.getFacultyCode());
            }
        } else if (course.getFaculty() != null) {
            faculty = course.getFaculty();
        }

        // 6. Student & Lead Resolution
        StudentLead lead = null;
        User studentUser = null;
        Student student = null;

        if (dto.getLeadId() != null && !dto.getLeadId().trim().isEmpty()) {
            lead = findLeadByIdOrCode(dto.getLeadId().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + dto.getLeadId()));
            if (lead.getStudent() != null && lead.getStudent().getUser() != null) {
                student = lead.getStudent();
                studentUser = student.getUser();
            } else {
                studentUser = userRepository.findByEmail(lead.getEmail().trim()).orElse(null);
            }
        }

        if (studentUser == null && dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            studentUser = userRepository.findByEmail(dto.getEmail().trim()).orElse(null);
        }

        if (studentUser == null) {
            // New Student creation
            String email = (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) 
                    ? dto.getEmail().trim() 
                    : (lead != null ? lead.getEmail() : null);
            if (email == null || email.trim().isEmpty()) {
                throw new BadRequestException("Email is required for registering new student.");
            }

            if (userRepository.existsByEmail(email)) {
                studentUser = userRepository.findByEmail(email).get();
            } else {
                String fullName = (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) 
                        ? dto.getFullName().trim() 
                        : (lead != null ? lead.getFullName() : "Student");
                if (fullName.length() < 2) {
                    throw new BadRequestException("Full name must be at least 2 characters.");
                }

                String rawPhone = (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) 
                        ? dto.getPhone().trim() 
                        : (lead != null ? lead.getPhone() : null);
                String cleanPhone = validateAndGetPhone(rawPhone);

                String rawPassword = "EduFlow@" + (1000 + (int)(Math.random() * 9000));
                studentUser = User.builder()
                        .fullName(fullName)
                        .email(email)
                        .phone(cleanPhone)
                        .password(passwordEncoder.encode(rawPassword))
                        .role(Role.STUDENT)
                        .status("ACTIVE")
                        .build();
                studentUser = userRepository.save(studentUser);
            }
        }

        // Ensure Student profile exists
        if (student == null) {
            student = studentRepository.findByUser(studentUser).orElse(null);
            if (student == null) {
                String studentCode = "STU-" + (1000 + studentUser.getId());
                student = Student.builder()
                        .user(studentUser)
                        .studentCode(studentCode)
                        .interestedCourse(course.getName())
                        .education(dto.getEducation() != null ? dto.getEducation() : (lead != null ? lead.getEducation() : null))
                        .city(dto.getCity() != null ? dto.getCity() : (lead != null ? lead.getCity() : null))
                        .build();
                student = studentRepository.save(student);
            }
        }

        // Ensure Lead is linked or created
        if (lead == null) {
            final String studentEmail = studentUser.getEmail();
            lead = studentLeadRepository.findLeadsWithFilters(null, studentEmail).stream()
                    .filter(l -> l.getEmail().equalsIgnoreCase(studentEmail))
                    .findFirst()
                    .orElse(null);

            if (lead == null) {
                String leadCode = "lead-" + (9900 + studentUser.getId());
                lead = StudentLead.builder()
                        .leadCode(leadCode)
                        .student(student)
                        .fullName(studentUser.getFullName())
                        .email(studentUser.getEmail())
                        .phone(studentUser.getPhone())
                        .interestedCourse(course.getName())
                        .education(student.getEducation())
                        .city(student.getCity())
                        .status(LeadStatus.NEW)
                        .build();
                lead = studentLeadRepository.save(lead);
            }
        }

        // 7. Payment Resolution & Validation
        Payment payment = null;
        if (dto.getPaymentId() != null && !dto.getPaymentId().trim().isEmpty()) {
            payment = findPaymentByIdOrTxn(dto.getPaymentId().trim(), dto.getTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment record not found: " + dto.getPaymentId()));
        } else if (dto.getTransactionId() != null && !dto.getTransactionId().trim().isEmpty()) {
            payment = paymentRepository.findByTransactionId(dto.getTransactionId().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found: " + dto.getTransactionId()));
        }

        if (payment == null) {
            if (Boolean.TRUE.equals(dto.getDirectEnrollment())) {
                // Direct enrollment waiver payment record
                String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String transactionId = "DUMMY_DIRECT_" + dateStr + "_" + (10000 + (long)(Math.random() * 89999));
                payment = Payment.builder()
                        .transactionId(transactionId)
                        .student(studentUser)
                        .course(course)
                        .plan(plan)
                        .amount(plan.getPrice())
                        .paymentMethod("DIRECT_ADMIN_ENROLLMENT")
                        .paymentStatus(PaymentStatus.SUCCESS)
                        .paymentDate(LocalDateTime.now())
                        .build();
                payment = paymentRepository.save(payment);
            } else {
                throw new BadRequestException("Valid paymentId or transactionId is required for student onboarding.");
            }
        }

        // Validate payment status and association
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Cannot activate enrollment: Payment status is " 
                    + payment.getPaymentStatus() + ". Payment must be in SUCCESS status.");
        }

        // If payment's student was null or different, link to resolved studentUser
        if (payment.getStudent() == null || !payment.getStudent().getId().equals(studentUser.getId())) {
            payment.setStudent(studentUser);
            payment = paymentRepository.save(payment);
        }

        // 8. Duplicate Active Enrollment Check
        Optional<Enrollment> existingActive = enrollmentRepository
                .findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(studentUser, course, EnrollmentStatus.ACTIVE);

        Enrollment enrollment = null;
        if (existingActive.isPresent()) {
            Enrollment active = existingActive.get();
            LocalDate today = LocalDate.now();
            boolean isStillActive = (active.getExpiryDate() == null || !today.isAfter(active.getExpiryDate()));

            if (isStillActive) {
                // If this enrollment was already activated for this same plan during payment completion, reuse it
                boolean isSamePlan = (active.getPlan() != null && active.getPlan().getId().equals(plan.getId()));
                boolean isSameCourse = (active.getCourse() != null && active.getCourse().getId().equals(course.getId()));
                if (isSamePlan && isSameCourse && !Boolean.TRUE.equals(dto.getDirectEnrollment())) {
                    enrollment = active;
                } else {
                    throw new DuplicateResourceException("Student " + studentUser.getEmail() 
                            + " is already actively enrolled in course '" + course.getName() 
                            + "' with enrollment ID " + active.getEnrollmentCode() 
                            + " until " + active.getExpiryDate());
                }
            }
        }

        // 9. Enrollment Activation
        if (enrollment == null) {
            LocalDate startDate = LocalDate.now();
            int months = (plan.getDuration() != null) ? plan.getDuration().getMonths() : 3;
            LocalDate expiryDate = startDate.plusMonths(months);

            String enrollmentCode = "ENR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) 
                    + "-" + (1000 + (long)(Math.random() * 8999));

            enrollment = Enrollment.builder()
                    .enrollmentCode(enrollmentCode)
                    .student(studentUser)
                    .course(course)
                    .plan(plan)
                    .enrolledBy(executorUser)
                    .startDate(startDate)
                    .expiryDate(expiryDate)
                    .status(EnrollmentStatus.ACTIVE)
                    .build();
            enrollment = enrollmentRepository.save(enrollment);
        }

        // 10. Update Lead Status
        lead.setStatus(LeadStatus.ENROLLED);
        lead.setStudent(student);
        lead.setLastActivity(LocalDateTime.now());
        studentLeadRepository.save(lead);

        // 11. Persist Onboarding Audit Record
        String onboardingCode = "ONB-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) 
                + "-" + (1000 + (long)(Math.random() * 8999));

        OnboardingAudit audit = OnboardingAudit.builder()
                .onboardingCode(onboardingCode)
                .student(studentUser)
                .lead(lead)
                .course(course)
                .plan(plan)
                .faculty(faculty)
                .payment(payment)
                .enrollment(enrollment)
                .executor(executorUser)
                .syllabusExplained(dto.getSyllabusExplained())
                .scheduleExplained(dto.getScheduleExplained())
                .validityExplained(dto.getValidityExplained())
                .directEnrollment(dto.getDirectEnrollment())
                .notes(dto.getNotes())
                .build();
        audit = onboardingAuditRepository.save(audit);

        // 12. Dispatch Notifications Safely
        notificationService.notifyStudentEnrollmentActivated(studentUser, course, enrollment);
        if (faculty != null) {
            notificationService.notifyFacultyStudentAssigned(faculty, studentUser, course);
        }
        notificationService.notifyExecutorOnboardingCompleted(executorUser, studentUser, course);

        log.info("[ONBOARDING] Successfully completed student onboarding. Code: {}, Student: {}, Course: {}, Enrollment: {}",
                onboardingCode, studentUser.getEmail(), course.getName(), enrollment != null ? enrollment.getEnrollmentCode() : "N/A");

        String facId = faculty != null ? (faculty.getFacultyCode() != null ? faculty.getFacultyCode() : "FAC-" + faculty.getId()) : null;
        String facName = (faculty != null && faculty.getUser() != null) ? faculty.getUser().getFullName() : null;

        return StudentOnboardingResponseDTO.builder()
                .onboardingCode(onboardingCode)
                .studentId(student.getStudentCode() != null ? student.getStudentCode() : "STU-" + studentUser.getId())
                .profileId(String.valueOf(studentUser.getId()))
                .leadId(lead.getLeadCode() != null ? lead.getLeadCode() : "lead-" + lead.getId())
                .fullName(studentUser.getFullName())
                .email(studentUser.getEmail())
                .phone(studentUser.getPhone())
                .courseId(course.getCourseCode() != null ? course.getCourseCode() : String.valueOf(course.getId()))
                .courseName(course.getName())
                .planId(plan.getId())
                .planDuration(plan.getDuration() != null ? plan.getDuration().name() : null)
                .amount(payment.getAmount())
                .enrollmentId(enrollment.getEnrollmentCode())
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .facultyId(facId)
                .facultyName(facName)
                .startDate(enrollment.getStartDate())
                .expiryDate(enrollment.getExpiryDate())
                .paymentStatus(payment.getPaymentStatus().name())
                .enrollmentStatus(enrollment.getStatus().name())
                .syllabusExplained(audit.getSyllabusExplained())
                .scheduleExplained(audit.getScheduleExplained())
                .validityExplained(audit.getValidityExplained())
                .onboardedBy(executorUser.getFullName() + " (" + executorUser.getEmail() + ")")
                .message("Student onboarded successfully.")
                .build();
    }

    private String validateAndGetPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new BadRequestException("Phone number is required");
        }
        String cleanPhone = phone.trim().replace(" ", "").replace("-", "");
        if (cleanPhone.startsWith("+91")) {
            cleanPhone = cleanPhone.substring(3);
        } else if (cleanPhone.startsWith("91") && cleanPhone.length() == 12) {
            cleanPhone = cleanPhone.substring(2);
        }
        if (!cleanPhone.matches("^[6-9]\\d{9}$")) {
            throw new BadRequestException("Mobile number must be a valid 10-digit number starting with 6, 7, 8, or 9");
        }
        return cleanPhone;
    }

    private Optional<StudentLead> findLeadByIdOrCode(String key) {
        if (key == null) return Optional.empty();
        Optional<StudentLead> byCode = studentLeadRepository.findByLeadCode(key.trim());
        if (byCode.isPresent()) return byCode;

        try {
            Long id = Long.parseLong(key.trim().replace("lead-", ""));
            return studentLeadRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Payment> findPaymentByIdOrTxn(String paymentIdStr, String transactionId) {
        if (paymentIdStr != null && !paymentIdStr.trim().isEmpty()) {
            try {
                Long id = Long.parseLong(paymentIdStr.trim().replace("PAY-", "").replace("pay-", ""));
                Optional<Payment> byId = paymentRepository.findById(id);
                if (byId.isPresent()) return byId;
            } catch (NumberFormatException ignored) {}
        }

        if (transactionId != null && !transactionId.trim().isEmpty()) {
            return paymentRepository.findByTransactionId(transactionId.trim());
        }

        return Optional.empty();
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

    private Optional<CoursePlan> findPlanByIdOrCode(String key) {
        if (key == null) return Optional.empty();
        try {
            Long id = Long.parseLong(key.trim().replace("plan-", "").replace("PLAN-", ""));
            return coursePlanRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Faculty> findFacultyByIdOrCode(String key) {
        if (key == null || key.trim().isEmpty()) return Optional.empty();
        String cleanKey = key.trim();
        Optional<Faculty> byCode = facultyRepository.findByFacultyCode(cleanKey);
        if (byCode.isPresent()) return byCode;

        Optional<Faculty> byEmail = facultyRepository.findByUserEmail(cleanKey);
        if (byEmail.isPresent()) return byEmail;

        try {
            Long id = Long.parseLong(cleanKey.replace("FAC-", "").replace("fac-prof-", "").replace("fac-", ""));
            return facultyRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

