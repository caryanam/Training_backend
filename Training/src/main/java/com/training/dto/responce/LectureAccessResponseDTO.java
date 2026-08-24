package com.training.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureAccessResponseDTO {
    private boolean hasAccess;
    private String reason;
    private String lectureUrl;
    private String recordingUrl;
}
