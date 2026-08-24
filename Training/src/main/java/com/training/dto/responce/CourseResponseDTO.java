package com.training.dto.responce;

import com.training.enums.CourseCategory;
import com.training.enums.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponseDTO {

    private Long id;
    private String courseCode;
    private String title;
    private String description;
    private CourseCategory category;
    private CourseStatus status;
    private int lectureCount;
    private int activeStudentCount;
    private List<CoursePlanResponseDTO> plans;
}
