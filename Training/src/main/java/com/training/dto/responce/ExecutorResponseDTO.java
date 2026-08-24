package com.training.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutorResponseDTO {
    private String profileId;
    private String executorId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String status;
}
