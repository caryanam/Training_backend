package com.training.dto.responce;

import com.training.enums.PlanDuration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePlanResponseDTO {

    private Long id;
    private PlanDuration duration;
    private String durationLabel;
    private BigDecimal price;
    private String currency;
}
