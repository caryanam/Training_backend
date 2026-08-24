package com.training.serviceImpl;

import com.training.dto.request.VerifyPaymentDTO;
import com.training.dto.responce.PaymentResponseDTO;
import com.training.entity.*;
import com.training.enums.EnrollmentStatus;
import com.training.enums.LeadStatus;
import com.training.enums.PaymentStatus;
import com.training.exception.ResourceNotFoundException;
import com.training.repo.*;
import com.training.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CoursePlanRepository coursePlanRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentLeadRepository studentLeadRepository;

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

        // Generate Transaction ID
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
                .build();
        payment = paymentRepository.save(payment);

        // Calculate enrollment dates based on PlanDuration enum
        LocalDate startDate = LocalDate.now();
        int months = 3; // default
        if (plan.getDuration() != null) {
            months = switch (plan.getDuration()) {
                case ONE_MONTH -> 1;
                case TWO_MONTHS -> 2;
                case THREE_MONTHS -> 3;
            };
        }
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

        // Update lead status if exists for student email
        String userEmail = studentUser.getEmail();
        studentLeadRepository.findLeadsWithFilters(null, userEmail).stream()
                .filter(l -> l.getEmail().equalsIgnoreCase(userEmail))
                .findFirst()
                .ifPresent(lead -> {
                    lead.setStatus(LeadStatus.ENROLLED);
                    studentLeadRepository.save(lead);
                });

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

    private Optional<User> findUserByIdOrProfileId(String key) {
        try {
            Long id = Long.parseLong(key.replace("student-", "").replace("profile-", ""));
            return userRepository.findById(id);
        } catch (NumberFormatException e) {
            return userRepository.findByEmail(key);
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

    private Optional<CoursePlan> findPlanByIdOrCode(String key) {
        try {
            Long id = Long.parseLong(key.replace("plan-", "").replace("PLAN-", ""));
            return coursePlanRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
