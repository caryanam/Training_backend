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
public class LectureAccessResponseDTO {
    private boolean hasAccess;
    private String reason;
    private String lectureId;
    private String title;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lectureDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String lectureUrl;
    private String meetingLink;
    private String recordingUrl;
    private Boolean isDownloadable;

    private Long courseId;
    private String courseCode;
    private String courseName;

    private String facultyName;
    private String facultyCode;
    private String facultyEmail;
    private String facultyDepartment;
}
