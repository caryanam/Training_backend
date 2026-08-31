package com.training.serviceImpl;

import com.training.dto.request.AssignExecutorDTO;
import com.training.dto.responce.LeadAssignResponseDTO;
import com.training.dto.responce.LeadResponseDTO;
import com.training.entity.Enrollment;
import com.training.entity.Executer;
import com.training.entity.Payment;
import com.training.entity.StudentLead;
import com.training.entity.User;
import com.training.enums.EnrollmentStatus;
import com.training.enums.LeadStatus;
import com.training.enums.PaymentStatus;
import com.training.exception.ResourceNotFoundException;
import com.training.repo.EnrollmentRepository;
import com.training.repo.ExecuterRepository;
import com.training.repo.PaymentRepository;
import com.training.repo.StudentLeadRepository;
import com.training.repo.UserRepository;
import com.training.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final StudentLeadRepository studentLeadRepository;
    private final ExecuterRepository executerRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public List<LeadResponseDTO> getLeads(String statusStr, String search, String executorId, String executorEmail) {
        LeadStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !statusStr.equalsIgnoreCase("all")) {
            status = LeadStatus.fromString(statusStr);
        }

        String searchFilter = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        List<StudentLead> leads = studentLeadRepository.findLeadsWithFilters(status, searchFilter);

        // Filter by executor if specified by ID or Email
        if ((executorId != null && !executorId.trim().isEmpty()) || (executorEmail != null && !executorEmail.trim().isEmpty())) {
            final String targetExeId = executorId != null ? executorId.trim().toLowerCase() : "";
            final String targetEmail = executorEmail != null ? executorEmail.trim().toLowerCase() : "";

            leads = leads.stream().filter(l -> {
                if (l.getAssignedExecutor() == null) return false;
                Executer exe = l.getAssignedExecutor();
                String code = exe.getExecutorCode() != null ? exe.getExecutorCode().toLowerCase() : "";
                String idStr = String.valueOf(exe.getId()).toLowerCase();
                String userProfId = exe.getUser() != null ? String.valueOf(exe.getUser().getId()).toLowerCase() : "";
                String userEmail = (exe.getUser() != null && exe.getUser().getEmail() != null) ? exe.getUser().getEmail().toLowerCase() : "";
                String userName = (exe.getUser() != null && exe.getUser().getFullName() != null) ? exe.getUser().getFullName().toLowerCase() : "";

                boolean matchEmail = !targetEmail.isEmpty() && targetEmail.equals(userEmail);
                boolean matchId = !targetExeId.isEmpty() && (
                    targetExeId.equals(code) ||
                    targetExeId.equals(idStr) ||
                    targetExeId.equals("exe-prof-" + userProfId) ||
                    targetExeId.equals("exe-" + idStr) ||
                    targetExeId.equals(userEmail) ||
                    targetExeId.equals(userName)
                );

                return matchEmail || matchId;
            }).collect(Collectors.toList());
        }

        return leads.stream().map(lead -> {
            String studentCode = lead.getStudent() != null ? lead.getStudent().getStudentCode() : null;
            String profileId = (lead.getStudent() != null && lead.getStudent().getUser() != null)
                    ? String.valueOf(lead.getStudent().getUser().getId()) : null;
            String executorName = (lead.getAssignedExecutor() != null && lead.getAssignedExecutor().getUser() != null)
                    ? lead.getAssignedExecutor().getUser().getFullName() : null;
            String assignedExeCode = lead.getAssignedExecutor() != null
                    ? lead.getAssignedExecutor().getExecutorCode() : null;
            String assignedExeEmail = (lead.getAssignedExecutor() != null && lead.getAssignedExecutor().getUser() != null)
                    ? lead.getAssignedExecutor().getUser().getEmail() : null;

            User studentUser = (lead.getStudent() != null && lead.getStudent().getUser() != null)
                    ? lead.getStudent().getUser()
                    : userRepository.findByEmail(lead.getEmail()).orElse(null);

            Enrollment activeEnrollment = null;
            if (studentUser != null) {
                List<Enrollment> enrollments = enrollmentRepository.findByStudentOrderByExpiryDateDesc(studentUser);
                activeEnrollment = enrollments.stream()
                        .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE && (e.getExpiryDate() == null || !LocalDate.now().isAfter(e.getExpiryDate())))
                        .findFirst()
                        .orElse(null);

                // Auto-sync status: If active enrollment exists, lead status must be ENROLLED
                if (activeEnrollment != null && lead.getStatus() != LeadStatus.ENROLLED) {
                    lead.setStatus(LeadStatus.ENROLLED);
                    studentLeadRepository.save(lead);
                } else if (activeEnrollment == null && lead.getStatus() != LeadStatus.ENROLLED) {
                    List<Payment> payments = paymentRepository.findByStudent(studentUser);
                    boolean hasSuccessPayment = payments.stream()
                            .anyMatch(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS);
                    if (hasSuccessPayment) {
                        lead.setStatus(LeadStatus.ENROLLED);
                        studentLeadRepository.save(lead);
                    }
                }
            }

            String enrollmentStatusStr = "NOT_ACTIVATED";
            String enrollmentCode = null;
            LocalDate startDate = null;
            LocalDate expiryDate = null;
            String courseValidity = "Not Activated";

            if (activeEnrollment != null) {
                enrollmentStatusStr = "ACTIVE";
                enrollmentCode = activeEnrollment.getEnrollmentCode();
                startDate = activeEnrollment.getStartDate();
                expiryDate = activeEnrollment.getExpiryDate();
                courseValidity = (expiryDate != null) ? "Valid until " + expiryDate : "Active";
            } else if (lead.getStatus() == LeadStatus.ENROLLED) {
                enrollmentStatusStr = "ACTIVE";
                courseValidity = "Active";
            }

            return LeadResponseDTO.builder()
                    .id(lead.getId())
                    .leadId(lead.getLeadCode() != null ? lead.getLeadCode() : "lead-" + lead.getId())
                    .studentId(studentCode)
                    .profileId(profileId)
                    .fullName(lead.getFullName())
                    .email(lead.getEmail())
                    .phone(lead.getPhone())
                    .interestedCourse(lead.getInterestedCourse())
                    .education(lead.getEducation())
                    .city(lead.getCity())
                    .status(lead.getStatus() != null ? lead.getStatus().getValue() : LeadStatus.NEW.getValue())
                    .assignedExecutor(executorName)
                    .assignedExecutorId(assignedExeCode)
                    .assignedExecutorEmail(assignedExeEmail)
                    .followupDate(lead.getFollowupDate())
                    .lastActivity(lead.getLastActivity())
                    .createdAt(lead.getCreatedAt())
                    .enrollmentStatus(enrollmentStatusStr)
                    .enrollmentId(enrollmentCode)
                    .startDate(startDate)
                    .expiryDate(expiryDate)
                    .courseValidity(courseValidity)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public LeadAssignResponseDTO assignExecutor(String leadIdStr, AssignExecutorDTO dto) {
        StudentLead lead = findLeadByIdOrCode(leadIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadIdStr));

        Executer executor = findExecutorByIdOrCode(dto.getExecutorId())
                .orElseThrow(() -> new ResourceNotFoundException("Executor not found: " + dto.getExecutorId()));

        lead.setAssignedExecutor(executor);
        lead.setStatus(LeadStatus.ASSIGNED);
        lead = studentLeadRepository.save(lead);

        String executorName = executor.getUser() != null ? executor.getUser().getFullName() : null;
        String executorId = executor.getExecutorCode() != null ? executor.getExecutorCode() : "exe-" + executor.getId();

        return LeadAssignResponseDTO.builder()
                .leadId(lead.getLeadCode() != null ? lead.getLeadCode() : "lead-" + lead.getId())
                .status(lead.getStatus().getValue())
                .executorId(executorId)
                .executorName(executorName)
                .build();
    }

    private Optional<StudentLead> findLeadByIdOrCode(String key) {
        Optional<StudentLead> byCode = studentLeadRepository.findByLeadCode(key);
        if (byCode.isPresent()) return byCode;

        try {
            Long id = Long.parseLong(key.replace("lead-", ""));
            return studentLeadRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Executer> findExecutorByIdOrCode(String key) {
        Optional<Executer> byCode = executerRepository.findByExecutorCode(key);
        if (byCode.isPresent()) return byCode;

        try {
            Long id = Long.parseLong(key.replace("EXE-", "").replace("exe-rec-", "").replace("exe-", ""));
            return executerRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public LeadResponseDTO updateLeadStatus(String leadIdStr, String statusStr) {
        StudentLead lead = findLeadByIdOrCode(leadIdStr)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadIdStr));

        if (statusStr != null && !statusStr.trim().isEmpty()) {
            lead.setStatus(LeadStatus.fromString(statusStr));
        }
        lead = studentLeadRepository.save(lead);

        String studentCode = lead.getStudent() != null ? lead.getStudent().getStudentCode() : null;
        String profileId = (lead.getStudent() != null && lead.getStudent().getUser() != null)
                    ? String.valueOf(lead.getStudent().getUser().getId()) : null;
        String executorName = (lead.getAssignedExecutor() != null && lead.getAssignedExecutor().getUser() != null)
                ? lead.getAssignedExecutor().getUser().getFullName() : null;
        String assignedExeCode = lead.getAssignedExecutor() != null
                ? lead.getAssignedExecutor().getExecutorCode() : null;
        String assignedExeEmail = (lead.getAssignedExecutor() != null && lead.getAssignedExecutor().getUser() != null)
                ? lead.getAssignedExecutor().getUser().getEmail() : null;

        return LeadResponseDTO.builder()
                .id(lead.getId())
                .leadId(lead.getLeadCode() != null ? lead.getLeadCode() : "lead-" + lead.getId())
                .studentId(studentCode)
                .profileId(profileId)
                .fullName(lead.getFullName())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .interestedCourse(lead.getInterestedCourse())
                .education(lead.getEducation())
                .city(lead.getCity())
                .status(lead.getStatus() != null ? lead.getStatus().getValue() : LeadStatus.NEW.getValue())
                .assignedExecutor(executorName)
                .assignedExecutorId(assignedExeCode)
                .assignedExecutorEmail(assignedExeEmail)
                .followupDate(lead.getFollowupDate())
                .lastActivity(lead.getLastActivity())
                .createdAt(lead.getCreatedAt())
                .build();
    }
}
