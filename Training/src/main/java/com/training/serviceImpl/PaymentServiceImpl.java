package com.training.serviceImpl;

import com.training.dto.request.CompleteDummyPaymentDTO;
import com.training.dto.request.CreateDummyPaymentDTO;
import com.training.dto.request.VerifyPaymentDTO;
import com.training.dto.responce.DummyPaymentResponseDTO;
import com.training.dto.responce.PaymentResponseDTO;
import com.training.entity.*;
import com.training.enums.CourseStatus;
import com.training.enums.EnrollmentStatus;
import com.training.enums.LeadStatus;
import com.training.enums.PaymentStatus;
import com.training.exception.BadRequestException;
import com.training.exception.ResourceNotFoundException;
import com.training.notification.NotificationService;
import com.training.payment.PaymentGateway;
import com.training.payment.PaymentGatewayFactory;
import com.training.payment.dto.PaymentInitiationRequest;
import com.training.payment.dto.PaymentInitiationResponse;
import com.training.payment.dto.PaymentVerificationRequest;
import com.training.payment.dto.PaymentVerificationResponse;
import com.training.repo.*;
import com.training.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentLeadRepository studentLeadRepository;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<DummyPaymentResponseDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getPaymentDate() == null) return 1;
                    if (b.getPaymentDate() == null) return -1;
                    return b.getPaymentDate().compareTo(a.getPaymentDate());
                })
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DummyPaymentResponseDTO> getMyPayments(String studentEmail) {
        if (studentEmail == null || studentEmail.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return paymentRepository.findAll().stream()
                .filter(p -> p.getStudent() != null && studentEmail.equalsIgnoreCase(p.getStudent().getEmail()))
                .sorted((a, b) -> {
                    if (a.getPaymentDate() == null) return 1;
                    if (b.getPaymentDate() == null) return -1;
                    return b.getPaymentDate().compareTo(a.getPaymentDate());
                })
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private DummyPaymentResponseDTO mapToDTO(Payment payment) {
        return DummyPaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .studentId(payment.getStudent() != null ? String.valueOf(payment.getStudent().getId()) : null)
                .studentEmail(payment.getStudent() != null ? payment.getStudent().getEmail() : null)
                .courseId(payment.getCourse() != null ? payment.getCourse().getCourseCode() : null)
                .courseName(payment.getCourse() != null ? payment.getCourse().getName() : "Full Stack Java & Spring Boot Masterclass")
                .planId(payment.getPlan() != null ? payment.getPlan().getId() : null)
                .amount(payment.getAmount())
                .currency("INR")
                .status(payment.getPaymentStatus() != null ? payment.getPaymentStatus().name() : "SUCCESS")
                .providerOrderId(payment.getProviderOrderId())
                .providerPaymentId(payment.getProviderPaymentId())
                .message("Payment transaction record.")
                .build();
    }

    @Override
    @Transactional
    public DummyPaymentResponseDTO createDummyPayment(CreateDummyPaymentDTO dto, String authenticatedEmail) {
        User studentUser = resolveStudentUser(dto.getStudentId(), authenticatedEmail);

        Course course = findCourseByIdOrCode(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + dto.getCourseId()));

        if (course.getStatus() != null && course.getStatus() != CourseStatus.ACTIVE) {
            throw new BadRequestException("Course is inactive: " + course.getName());
        }

        CoursePlan plan = findPlanByIdOrCode(dto.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Course plan not found: " + dto.getPlanId()));

        if (plan.getCourse() != null && !plan.getCourse().getId().equals(course.getId())) {
            throw new BadRequestException("Selected plan does not belong to course: " + course.getName());
        }

        PaymentGateway gateway = paymentGatewayFactory.getGateway("DUMMY");
        PaymentInitiationRequest request = PaymentInitiationRequest.builder()
                .student(studentUser)
                .course(course)
                .plan(plan)
                .amount(plan.getPrice())
                .currency(plan.getCurrency() != null ? plan.getCurrency() : "INR")
                .idempotencyKey("DUMMY_KEY_" + System.currentTimeMillis())
                .description("Dummy Payment Order")
                .build();

        PaymentInitiationResponse initRes = gateway.initiatePayment(request);

        Payment payment = Payment.builder()
                .transactionId(initRes.getTransactionId())
                .student(studentUser)
                .course(course)
                .plan(plan)
                .amount(plan.getPrice())
                .paymentMethod("DUMMY_PAYMENT")
                .providerOrderId(initRes.getProviderOrderId())
                .paymentStatus(PaymentStatus.PENDING)
                .paymentDate(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);

        return DummyPaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .studentId(String.valueOf(studentUser.getId()))
                .studentEmail(studentUser.getEmail())
                .courseId(course.getCourseCode())
                .courseName(course.getName())
                .planId(plan.getId())
                .amount(payment.getAmount())
                .currency(plan.getCurrency() != null ? plan.getCurrency() : "INR")
                .status(payment.getPaymentStatus().name())
                .providerOrderId(payment.getProviderOrderId())
                .message("Dummy payment order created with PENDING status. Please complete via simulation.")
                .build();
    }

    @Override
    @Transactional
    public DummyPaymentResponseDTO completeDummyPayment(CompleteDummyPaymentDTO dto, String authenticatedEmail) {
        Payment payment = findPaymentByIdOrTxn(dto.getPaymentId(), dto.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Payment is already completed and verified: " + payment.getTransactionId());
        }

        PaymentGateway gateway = paymentGatewayFactory.getGateway("DUMMY");
        PaymentStatus desiredStatus = PaymentStatus.SUCCESS;
        if (dto.getResult() != null) {
            try {
                desiredStatus = PaymentStatus.valueOf(dto.getResult().toUpperCase());
            } catch (Exception ignored) {}
        }

        PaymentVerificationRequest verifyReq = PaymentVerificationRequest.builder()
                .providerOrderId(payment.getProviderOrderId())
                .providerPaymentId("DUMMY_PAY_" + Long.toHexString(System.currentTimeMillis()).toUpperCase())
                .transactionId(payment.getTransactionId())
                .desiredStatus(desiredStatus)
                .expectedAmount(payment.getAmount())
                .build();

        PaymentVerificationResponse verifyRes = gateway.verifyPayment(verifyReq);

        PaymentStatus newStatus = verifyRes.getStatus() != null ? verifyRes.getStatus() : PaymentStatus.SUCCESS;
        payment.setPaymentStatus(newStatus);
        payment.setProviderPaymentId(verifyRes.getProviderPaymentId());
        payment.setPaymentDate(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        Enrollment enrollment = null;
        if (newStatus == PaymentStatus.SUCCESS) {
            LocalDate startDate = LocalDate.now();
            int months = (payment.getPlan() != null && payment.getPlan().getDuration() != null)
                    ? payment.getPlan().getDuration().getMonths() : 3;
            LocalDate expiryDate = startDate.plusMonths(months);

            Optional<Enrollment> existingEnrollment = enrollmentRepository.findByStudentAndCourse(payment.getStudent(), payment.getCourse()).stream().findFirst();
            if (existingEnrollment.isPresent()) {
                enrollment = existingEnrollment.get();
                enrollment.setStatus(EnrollmentStatus.ACTIVE);
                enrollment.setPlan(payment.getPlan());
                enrollment.setStartDate(startDate);
                enrollment.setExpiryDate(expiryDate);
            } else {
                String enrollmentCode = "ENR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + (1000 + (long)(Math.random() * 8999));
                enrollment = Enrollment.builder()
                        .enrollmentCode(enrollmentCode)
                        .student(payment.getStudent())
                        .course(payment.getCourse())
                        .plan(payment.getPlan())
                        .startDate(startDate)
                        .expiryDate(expiryDate)
                        .status(EnrollmentStatus.ACTIVE)
                        .build();
            }
            enrollment = enrollmentRepository.save(enrollment);

            if (payment.getStudent() != null) {
                String studentEmail = payment.getStudent().getEmail();
                studentLeadRepository.findLeadsWithFilters(null, studentEmail).stream()
                        .filter(l -> l.getEmail().equalsIgnoreCase(studentEmail))
                        .findFirst()
                        .ifPresent(lead -> {
                            lead.setStatus(LeadStatus.ENROLLED);
                            studentLeadRepository.save(lead);
                        });
            }

            notificationService.notifyStudentEnrollmentActivated(payment.getStudent(), payment.getCourse(), enrollment);
        }

        return DummyPaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .studentId(payment.getStudent() != null ? String.valueOf(payment.getStudent().getId()) : null)
                .studentEmail(payment.getStudent() != null ? payment.getStudent().getEmail() : null)
                .courseId(payment.getCourse() != null ? payment.getCourse().getCourseCode() : null)
                .courseName(payment.getCourse() != null ? payment.getCourse().getName() : null)
                .planId(payment.getPlan() != null ? payment.getPlan().getId() : null)
                .amount(payment.getAmount())
                .currency(payment.getPlan() != null ? payment.getPlan().getCurrency() : "INR")
                .status(payment.getPaymentStatus().name())
                .providerOrderId(payment.getProviderOrderId())
                .providerPaymentId(payment.getProviderPaymentId())
                .enrollmentId(enrollment != null ? enrollment.getEnrollmentCode() : null)
                .startDate(enrollment != null ? enrollment.getStartDate() : null)
                .expiryDate(enrollment != null ? enrollment.getExpiryDate() : null)
                .enrollmentStatus(enrollment != null ? enrollment.getStatus().name() : null)
                .message(verifyRes.getMessage())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponseDTO verifyPayment(VerifyPaymentDTO dto, String authenticatedEmail) {
        User studentUser = null;
        if (dto.getStudentProfileId() != null && !dto.getStudentProfileId().isEmpty()) {
            studentUser = findUserByIdOrProfileId(dto.getStudentProfileId()).orElse(null);
        }
        if (studentUser == null && authenticatedEmail != null) {
            studentUser = userRepository.findByEmail(authenticatedEmail).orElse(null);
        }
        if (studentUser == null) {
            studentUser = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == com.training.enums.Role.STUDENT)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Student user not found"));
        }

        Course course = findCourseByIdOrCode(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + dto.getCourseId()));

        CoursePlan plan = findPlanByIdOrCode(dto.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Course plan not found: " + dto.getPlanId()));

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String transactionId = "TXN-" + dateStr + "-" + (10000 + (long)(Math.random() * 89999));

        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .student(studentUser)
                .course(course)
                .plan(plan)
                .amount(dto.getAmount())
                .paymentMethod(dto.getPaymentMethod())
                .providerOrderId(dto.getProviderOrderId())
                .providerPaymentId(dto.getProviderPaymentId())
                .providerSignature(dto.getProviderSignature())
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentDate(LocalDateTime.now())
                .build();
        payment = paymentRepository.save(payment);

        LocalDate startDate = LocalDate.now();
        int months = (plan.getDuration() != null) ? plan.getDuration().getMonths() : 3;
        LocalDate expiryDate = startDate.plusMonths(months);

        String enrollmentCode = "enr-" + (5000 + (long)(Math.random() * 4000));
        Enrollment enrollment = Enrollment.builder()
                .enrollmentCode(enrollmentCode)
                .student(studentUser)
                .course(course)
                .plan(plan)
                .startDate(startDate)
                .expiryDate(expiryDate)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        enrollment = enrollmentRepository.save(enrollment);

        String userEmail = studentUser.getEmail();
        studentLeadRepository.findLeadsWithFilters(null, userEmail).stream()
                .filter(l -> l.getEmail().equalsIgnoreCase(userEmail))
                .findFirst()
                .ifPresent(lead -> {
                    lead.setStatus(LeadStatus.ENROLLED);
                    studentLeadRepository.save(lead);
                });

        notificationService.notifyStudentEnrollmentActivated(studentUser, course, enrollment);

        PaymentResponseDTO.EnrollmentDTO enrollmentDTO = PaymentResponseDTO.EnrollmentDTO.builder()
                .enrollmentId(enrollmentCode)
                .startDate(enrollment.getStartDate())
                .expiryDate(enrollment.getExpiryDate())
                .status(enrollment.getStatus().name())
                .build();

        return PaymentResponseDTO.builder()
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .paymentStatus(payment.getPaymentStatus().name())
                .enrollment(enrollmentDTO)
                .build();
    }

    private User resolveStudentUser(String studentId, String authenticatedEmail) {
        if (studentId != null && !studentId.trim().isEmpty()) {
            Optional<User> userOpt = findUserByIdOrProfileId(studentId);
            if (userOpt.isPresent()) return userOpt.get();

            Optional<Student> stuOpt = studentRepository.findByStudentCode(studentId.trim());
            if (stuOpt.isPresent() && stuOpt.get().getUser() != null) {
                return stuOpt.get().getUser();
            }

            Optional<User> userByEmail = userRepository.findByEmail(studentId.trim());
            if (userByEmail.isPresent()) return userByEmail.get();
        }

        if (authenticatedEmail != null && !authenticatedEmail.trim().isEmpty()) {
            Optional<User> userOpt = userRepository.findByEmail(authenticatedEmail.trim());
            if (userOpt.isPresent()) return userOpt.get();
        }

        throw new ResourceNotFoundException("Student user not found for identifier: " + studentId);
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

    private Optional<User> findUserByIdOrProfileId(String key) {
        if (key == null) return Optional.empty();
        try {
            Long id = Long.parseLong(key.replace("student-", "").replace("profile-", "").replace("STU-", "").replace("stu-", ""));
            return userRepository.findById(id);
        } catch (NumberFormatException e) {
            return userRepository.findByEmail(key);
        }
    }

    private Optional<Course> findCourseByIdOrCode(String key) {
        if (key == null) return Optional.empty();
        Optional<Course> byCode = courseRepository.findByCourseCode(key);
        if (byCode.isPresent()) return byCode;

        try {
            Long id = Long.parseLong(key.replace("course-", "").replace("COURSE-", ""));
            return courseRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<CoursePlan> findPlanByIdOrCode(String key) {
        if (key == null) return Optional.empty();
        try {
            Long id = Long.parseLong(key.replace("plan-", "").replace("PLAN-", ""));
            return coursePlanRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
