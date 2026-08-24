package com.training.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateLectureDTO {

    @NotBlank(message = "courseId is required")
    private String courseId;

    private String facultyId;

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    @NotNull(message = "lectureDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lectureDate;

    @NotNull(message = "startTime is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @NotNull(message = "endTime is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @NotBlank(message = "lectureUrl is required")
    private String lectureUrl;

    private String recordingUrl;

    @Builder.Default
    private Boolean isDownloadable = false;
}
