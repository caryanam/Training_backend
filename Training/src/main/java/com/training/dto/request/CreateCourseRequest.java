package com.training.dto.request;

import com.training.enums.CourseCategory;
import com.training.enums.CourseStatus;
import com.training.enums.PlanDuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCourseRequest {

    @NotBlank(message = "Course title is required")
    @Size(min = 3, message = "Course title must be at least 3 characters")
    private String title;

    @NotNull(message = "Category is required")
    private CourseCategory category;

    @NotBlank(message = "Description is required")
    @Size(min = 10, message = "Description must be at least 10 characters")
    private String description;

    private String facultyId;

    @Builder.Default
    private CourseStatus status = CourseStatus.ACTIVE;

    @NotEmpty(message = "At least one pricing plan is required")
    @Valid
    private List<CoursePlanRequest> plans;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoursePlanRequest {

        @NotNull(message = "Plan duration is required")
        private PlanDuration duration;

        @NotNull(message = "Plan price is required")
        @Positive(message = "Plan price must be greater than zero")
        private BigDecimal price;
    }
}
