package com.training.dto.responce;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class FollowupReportResponseDTO {
    private Long id;
    private Long leadId;
    private Long studentId;
    private String studentCode;
    private String leadName;
    private String executorId;
    private String executorName;
    private Integer rating;
    private Boolean interested;
    private LocalDate expectedJoiningDate;
    private String demoDiscussion;
    private String projectCapability;
    private String additionalComments;
    private LocalDateTime createdAt;
}

