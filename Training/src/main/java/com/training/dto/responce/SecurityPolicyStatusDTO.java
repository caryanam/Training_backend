package com.training.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityPolicyStatusDTO {

    private int violationCount;
    private boolean isSuspended;
    private String warningLevel; // "NONE", "WARNING", "STRONG_WARNING", "TERMINATED"
    private String message;
    private String actionRequired;
}
