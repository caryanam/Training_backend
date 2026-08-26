package com.training.serviceImpl;

import com.training.dto.request.CreateFollowupReportDTO;
import com.training.dto.responce.FollowupReportResponseDTO;
import com.training.entity.Executer;
import com.training.entity.FollowupReport;
import com.training.entity.Student;
import com.training.entity.StudentLead;
import com.training.exception.ResourceNotFoundException;
import com.training.repo.ExecuterRepository;
import com.training.repo.FollowupReportRepo;
import com.training.repo.StudentLeadRepository;
import com.training.repo.StudentRepository;
import com.training.service.FollowupReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowupReportServiceImpl implements FollowupReportService {

    private final FollowupReportRepo followupReportRepo;
    private final StudentLeadRepository leadRepository;
    private final ExecuterRepository executerRepository;
    private final StudentRepository studentRepository;

    @Override
    public FollowupReportResponseDTO createFollowupReport(Long leadId, CreateFollowupReportDTO dto, String executorEmail) {
        StudentLead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        
        Executer executor = executerRepository.findByUserEmail(executorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Executor not found"));

        if (lead.getAssignedExecutor() == null || !lead.getAssignedExecutor().getId().equals(executor.getId())) {
            throw new RuntimeException("You are not assigned to this lead.");
        }

        FollowupReport report = FollowupReport.builder()
                .lead(lead)
                .executor(executor)
                .rating(dto.getRating())
                .interested(dto.getInterested())
                .expectedJoiningDate(dto.getExpectedJoiningDate())
                .demoDiscussion(dto.getDemoDiscussion())
                .projectCapability(dto.getProjectCapability())
                .additionalComments(dto.getAdditionalComments())
                .build();

        FollowupReport saved = followupReportRepo.save(report);
        return mapToDTO(saved);
    }

    @Override
    public List<FollowupReportResponseDTO> getFollowupsByLeadId(Long leadId) {
        return followupReportRepo.findByLeadIdOrderByCreatedAtDesc(leadId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FollowupReportResponseDTO> getFollowupsForStudent(String studentEmail) {
        Student student = studentRepository.findByUserEmail(studentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        
        return followupReportRepo.findByLeadStudentIdOrderByCreatedAtDesc(student.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FollowupReportResponseDTO> getAllFollowups() {
        return followupReportRepo.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private FollowupReportResponseDTO mapToDTO(FollowupReport report) {
                return FollowupReportResponseDTO.builder()
                .id(report.getId())
                .leadId(report.getLead().getId())
                .studentId(report.getLead().getStudent() != null ? report.getLead().getStudent().getId() : null)
                .studentCode(report.getLead().getStudent() != null ? report.getLead().getStudent().getStudentCode() : null)
                .leadName(report.getLead().getFullName())
                .executorId(report.getExecutor().getExecutorCode())
                .executorName(report.getExecutor().getUser().getFullName())
                .rating(report.getRating())
                .interested(report.getInterested())
                .expectedJoiningDate(report.getExpectedJoiningDate())
                .demoDiscussion(report.getDemoDiscussion())
                .projectCapability(report.getProjectCapability())
                .additionalComments(report.getAdditionalComments())
                .createdAt(report.getCreatedAt())
                .build();
    }
}



