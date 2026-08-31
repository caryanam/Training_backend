package com.training.payment.dto;

import com.training.entity.Course;
import com.training.entity.CoursePlan;
import com.training.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiationRequest {
    private User student;
    private Course course;
    private CoursePlan plan;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;
    private String description;
}
