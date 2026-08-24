package com.training.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteDemoResponseDTO {
    private String demoId;
    private String demoStatus;
    private String leadStatus;
}
