package com.training.serviceImpl;

import com.training.dto.responce.ExecutorPaymentResponseDTO;
import com.training.dto.responce.ExecutorStudentResponseDTO;
import com.training.entity.*;
import com.training.enums.EnrollmentStatus;
import com.training.enums.PaymentStatus;
import com.training.exception.ResourceNotFoundException;
import com.training.repo.*;
import com.training.service.ExecutorPortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutorPortalServiceImpl implements ExecutorPortalService {

    private final UserRepository userRepository;
    private final ExecuterRepository executerRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OnboardingAuditRepository onboardingAuditRepository;
    private final StudentLeadRepository studentLeadRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ExecutorStudentResponseDTO> getMyStudents(String executorEmail) {
        User executorUser = resolveExecutor(executorEmail);
        Set<User> assignedStudents = getAssignedStudents(executorUser);

        if (assignedStudents.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExecutorStudentResponseDTO> result = new ArrayList<>();

        for (User studentUser : assignedStudents) {
            List<Enrollment> enrollments = enrollmentRepository.findByStudent(studentUser);
            if (enrollments.isEmpty()) {
                // If lead exists without enrollment yet
                result.add(ExecutorStudentResponseDTO.builder()
                        .studentId(studentUser.getId())
                        .studentCode("STU-" + studentUser.getId())
                        .studentName(studentUser.getFullName())
                        .email(studentUser.getEmail())
                        .phone(studentUser.getPhone())
                        .enrollmentStatus("PENDING")
                        .paymentStatus("PENDING")
                        .build());
            } else {
                for (Enrollment enrollment : enrollments) {
                    Course course = enrollment.getCourse();
                    result.add(ExecutorStudentResponseDTO.builder()
                            .studentId(studentUser.getId())
                            .studentCode("STU-" + studentUser.getId())
                            .studentName(studentUser.getFullName())
                            .email(studentUser.getEmail())
                            .phone(studentUser.getPhone())
                            .courseId(course != null ? course.getId() : null)
                            .courseCode(course != null ? course.getCourseCode() : null)
                            .courseName(course != null ? course.getName() : null)
                            .enrollmentStatus(enrollment.getStatus() != null ? enrollment.getStatus().name() : "ACTIVE")
                            .enrolledAt(enrollment.getStartDate())
                            .expiryDate(enrollment.getExpiryDate())
                            .paymentStatus("SUCCESS")
                            .enrollmentCode(enrollment.getEnrollmentCode())
                            .build());
                }
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExecutorPaymentResponseDTO> getMyStudentPayments(String executorEmail) {
        User executorUser = resolveExecutor(executorEmail);
        Set<User> assignedStudents = getAssignedStudents(executorUser);

        if (assignedStudents.isEmpty()) {
            return Collections.emptyList();
        }

        List<Payment> payments = paymentRepository.findByStudentIn(new ArrayList<>(assignedStudents));

        return payments.stream().map(payment -> {
            User student = payment.getStudent();
            Course course = payment.getCourse();
            CoursePlan plan = payment.getPlan();

            Enrollment enrollment = null;
            if (student != null && course != null) {
                enrollment = enrollmentRepository
                        .findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(student, course, EnrollmentStatus.ACTIVE)
                        .orElse(null);
            }

            return ExecutorPaymentResponseDTO.builder()
                    .studentId(student != null ? student.getId() : null)
                    .studentName(student != null ? student.getFullName() : "Unknown")
                    .studentEmail(student != null ? student.getEmail() : null)
                    .courseId(course != null ? course.getId() : null)
                    .courseCode(course != null ? course.getCourseCode() : null)
                    .courseName(course != null ? course.getName() : null)
                    .planName(plan != null && plan.getDuration() != null ? plan.getDuration().name().replace("_", " ") : "Course Plan")
                    .amount(payment.getAmount())
                    .currency(plan != null ? plan.getCurrency() : "INR")
                    .paymentStatus(payment.getPaymentStatus() != null ? payment.getPaymentStatus().name() : PaymentStatus.SUCCESS.name())
                    .transactionId(payment.getTransactionId())
                    .paidAt(payment.getPaymentDate())
                    .enrollmentStatus(enrollment != null ? enrollment.getStatus().name() : null)
                    .enrollmentCode(enrollment != null ? enrollment.getEnrollmentCode() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private User resolveExecutor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Executor user not found: " + email));
    }

    private Set<User> getAssignedStudents(User executorUser) {
        Set<User> students = new HashSet<>();

        // 1. Direct enrollments created by this executor
        List<Enrollment> enrolledByMe = enrollmentRepository.findByEnrolledBy(executorUser);
        for (Enrollment e : enrolledByMe) {
            if (e.getStudent() != null) {
                students.add(e.getStudent());
            }
        }

        // 2. Onboarding audits by this executor
        List<OnboardingAudit> audits = onboardingAuditRepository.findAll().stream()
                .filter(a -> a.getExecutor() != null && a.getExecutor().getId().equals(executorUser.getId()))
                .collect(Collectors.toList());
        for (OnboardingAudit audit : audits) {
            if (audit.getStudent() != null) {
                students.add(audit.getStudent());
            }
        }

        // 3. Student leads assigned to this executor
        Optional<Executer> executerEntity = executerRepository.findByUser(executorUser);
        if (executerEntity.isPresent()) {
            List<StudentLead> leads = studentLeadRepository.findAll().stream()
                    .filter(l -> l.getAssignedExecutor() != null && l.getAssignedExecutor().getId().equals(executerEntity.get().getId()))
                    .collect(Collectors.toList());
            for (StudentLead lead : leads) {
                if (lead.getStudent() != null && lead.getStudent().getUser() != null) {
                    students.add(lead.getStudent().getUser());
                } else if (lead.getEmail() != null) {
                    userRepository.findByEmail(lead.getEmail()).ifPresent(students::add);
                }
            }
        }

        return students;
    }
}
