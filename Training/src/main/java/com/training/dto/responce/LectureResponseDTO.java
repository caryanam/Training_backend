package com.training.dto.responce;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LectureResponseDTO {
    private String lectureId;
    private String courseId;
    private String courseName;
    private String title;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lectureDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String lectureUrl;
    private String recordingUrl;
    private Boolean isDownloadable;
}
