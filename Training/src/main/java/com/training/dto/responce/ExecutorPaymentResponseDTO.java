package com.training.dto.responce;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutorPaymentResponseDTO {
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String planName;
    private BigDecimal amount;
    private String currency;
    private String paymentStatus;
    private String transactionId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;

    private String enrollmentStatus;
    private String enrollmentCode;
}
