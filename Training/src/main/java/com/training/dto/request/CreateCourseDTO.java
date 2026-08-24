package com.training.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCourseDTO {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    private String category;

    private String facultyId;

    private List<CurriculumModuleDTO> curriculum;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CurriculumModuleDTO {
        private String module;
        private String title;
        private Integer lectures;
    }
}
