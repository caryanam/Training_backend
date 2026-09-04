package com.training.dto.request;

import com.training.enums.SecurityEventType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSecurityEventDTO {

    private String lectureId;

    private Long sessionId;

    @NotNull(message = "Event type is required")
    private SecurityEventType eventType;

    private String metadata;

    private LocalDateTime timestamp;
}
