package com.training.dto.responce;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoSessionResponseDTO {
    private String id;
    private String sessionId;
    private String demoCode;
    private String courseId;
    private String courseName;
    private String executorId;
    private String executorName;
    private String executorEmail;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate demoDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String meetLink;
    private String notes;
    private String status;
    private Integer totalParticipants;
    private List<ParticipantDTO> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParticipantDTO {
        private String participantId;
        private String leadId;
        private String studentId;
        private String studentName;
        private String studentEmail;
        private String studentPhone;
        private String attendanceStatus;
        private LocalDateTime joinedAt;
    }
}
