package com.training.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleDemoDTO {

    @NotNull(message = "demoDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate demoDate;

    @NotNull(message = "startTime is required")
    @JsonFormat(pattern = "HH:mm")
    @JsonAlias({"startTime", "demoTime"})
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @NotBlank(message = "meetLink is required")
    @JsonAlias({"meetLink", "meetingLink"})
    private String meetLink;

    private String notes;
}
