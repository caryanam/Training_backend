package com.training.serviceImpl;

import com.training.dto.request.AssignExecutorDTO;
import com.training.dto.responce.LeadAssignResponseDTO;
import com.training.dto.responce.LeadResponseDTO;
import com.training.entity.Executer;
import com.training.entity.StudentLead;
import com.training.enums.LeadStatus;
import com.training.exception.ResourceNotFoundException;
import com.training.repo.ExecuterRepository;
import com.training.repo.StudentLeadRepository;
import com.training.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final StudentLeadRepository studentLeadRepository;
    private final ExecuterRepository executerRepository;

    @Override
    public List<LeadResponseDTO> getLeads(String statusStr, String search, String executorId, String executorEmail) {
        LeadStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !statusStr.equalsIgnoreCase("all")) {
            try {
                status = LeadStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid status enum
            }
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
                    .status(lead.getStatus().name())
                    .assignedExecutor(executorName)
                    .assignedExecutorId(assignedExeCode)
                    .assignedExecutorEmail(assignedExeEmail)
                    .followupDate(lead.getFollowupDate())
                    .lastActivity(lead.getLastActivity())
                    .createdAt(lead.getCreatedAt())
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
                .status(lead.getStatus().name())
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

        if (statusStr != null) {
            try {
                lead.setStatus(LeadStatus.valueOf(statusStr.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid enum
            }
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
                .status(lead.getStatus().name())
                .assignedExecutor(executorName)
                .assignedExecutorId(assignedExeCode)
                .assignedExecutorEmail(assignedExeEmail)
                .followupDate(lead.getFollowupDate())
                .lastActivity(lead.getLastActivity())
                .createdAt(lead.getCreatedAt())
                .build();
    }
}

