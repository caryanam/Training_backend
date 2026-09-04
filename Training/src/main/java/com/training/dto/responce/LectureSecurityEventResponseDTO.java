package com.training.dto.responce;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureSecurityEventResponseDTO {

    private Long id;
    private String lectureId;
    private String lectureTitle;
    private Long studentId;
    private String studentName;
    private String studentIdentifier;
    private String studentEmail;
    private String eventType;
    private String severity;
    private String metadata;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private int violationCount;
    private boolean sessionTerminated;
}
