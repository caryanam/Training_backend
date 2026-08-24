package com.training.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGroupDemoDTO {

    private String courseId;

    private String courseName;

    @NotNull(message = "demoDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate demoDate;

    @NotBlank(message = "startTime is required")
    @JsonAlias({"startTime", "demoTime"})
    private String startTime;

    @JsonAlias({"endTime"})
    private String endTime;

    @NotBlank(message = "meetLink is required")
    @JsonAlias({"meetLink", "meetingLink"})
    private String meetLink;

    private String notes;

    @NotEmpty(message = "At least one student must be selected")
    @JsonAlias({"studentIds", "leadIds"})
    private List<String> studentIds;

    public LocalTime getParsedStartTime() {
        if (startTime == null || startTime.trim().isEmpty()) return null;
        return parseTimeStr(startTime);
    }

    public LocalTime getParsedEndTime() {
        if (endTime == null || endTime.trim().isEmpty()) return null;
        return parseTimeStr(endTime);
    }

    private LocalTime parseTimeStr(String str) {
        String clean = str.trim();
        if (clean.length() == 5) {
            return LocalTime.parse(clean, DateTimeFormatter.ofPattern("HH:mm"));
        }
        if (clean.length() == 8) {
            return LocalTime.parse(clean, DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
        try {
            return LocalTime.parse(clean);
        } catch (Exception e) {
            return LocalTime.of(11, 0);
        }
    }
}
