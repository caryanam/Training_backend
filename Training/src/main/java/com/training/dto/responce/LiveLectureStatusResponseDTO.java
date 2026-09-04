package com.training.dto.responce;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LiveLectureStatusResponseDTO {
    private boolean isLive;
    private Long sessionId;
    private String lectureId;
    private String lectureTitle;
    private String courseName;
    private String roomName;
    private String facultyName;
    private Integer participantCount;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
