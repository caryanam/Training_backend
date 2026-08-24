package com.training.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterStudentResponseDTO {
    private String profileId;
    private String studentId;
    private String leadId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String leadStatus;
}
