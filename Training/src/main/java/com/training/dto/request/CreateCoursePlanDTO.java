package com.training.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCoursePlanDTO {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "durationMonths is required")
    @Positive(message = "durationMonths must be positive")
    private Integer durationMonths;

    @NotNull(message = "price is required")
    private BigDecimal price;

    private BigDecimal discount;

    @Builder.Default
    private String currency = "INR";
}
