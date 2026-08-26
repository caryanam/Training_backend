package com.training.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateFollowupReportDTO {
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @NotNull(message = "Interested status is required")
    private Boolean interested;

    private LocalDate expectedJoiningDate;
    private String demoDiscussion;
    private String projectCapability;
    private String additionalComments;
}
