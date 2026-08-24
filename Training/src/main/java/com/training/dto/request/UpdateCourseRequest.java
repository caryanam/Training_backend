package com.training.dto.request;

import com.training.enums.CourseCategory;
import com.training.enums.CourseStatus;
import com.training.enums.PlanDuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class UpdateCourseRequest {

    private String title;

    private CourseCategory category;

    private CourseStatus status;

    private String description;

    @Valid
    private List<CoursePlanUpdateRequest> plans;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoursePlanUpdateRequest {

        @NotNull(message = "Plan duration is required")
        private PlanDuration duration;

        @NotNull(message = "Plan price is required")
        @Positive(message = "Plan price must be greater than zero")
        private BigDecimal price;
    }
}
