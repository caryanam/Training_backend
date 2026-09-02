package com.training.dto.responce;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutorStudentResponseDTO {
    private Long studentId;
    private String studentCode;
    private String studentName;
    private String email;
    private String phone;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String enrollmentStatus;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate enrolledAt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    private String paymentStatus;
    private String enrollmentCode;
}
