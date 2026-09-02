package com.training.dto.responce;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentCourseResponseDTO {
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String category;
    private String description;
    private String facultyName;
    private String facultyEmail;
    private String enrollmentStatus;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    private int lectureCount;
    private String enrollmentCode;
    private List<CoursePlanResponseDTO> plans;
}
