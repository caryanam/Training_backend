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
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLectureDTO {

    @NotBlank(message = "courseId is required")
    private String courseId;

    private String facultyId;

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    @NotNull(message = "lectureDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lectureDate;

    @JsonAlias({"startTime"})
    private String startTime;

    @JsonAlias({"endTime"})
    private String endTime;

    @NotBlank(message = "lectureUrl is required")
    @JsonAlias({"lectureUrl", "meetingLink", "meetUrl"})
    private String lectureUrl;

    private String recordingUrl;

    @Builder.Default
    private Boolean isDownloadable = false;

    public void setStartTime(LocalTime time) {
        this.startTime = time != null ? time.toString() : null;
    }

    public void setStartTime(String time) {
        this.startTime = time;
    }

    public void setEndTime(LocalTime time) {
        this.endTime = time != null ? time.toString() : null;
    }

    public void setEndTime(String time) {
        this.endTime = time;
    }

    public LocalTime getParsedStartTime() {
        if (startTime == null || startTime.trim().isEmpty()) return LocalTime.of(18, 0);
        return parseTimeStr(startTime);
    }

    public LocalTime getParsedEndTime() {
        if (endTime == null || endTime.trim().isEmpty()) return LocalTime.of(19, 30);
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
            return LocalTime.of(18, 0);
        }
    }

    public static class CreateLectureDTOBuilder {
        private String startTime;
        private String endTime;

        public CreateLectureDTOBuilder startTime(LocalTime time) {
            this.startTime = time != null ? time.toString() : null;
            return this;
        }

        public CreateLectureDTOBuilder startTime(String time) {
            this.startTime = time;
            return this;
        }

        public CreateLectureDTOBuilder endTime(LocalTime time) {
            this.endTime = time != null ? time.toString() : null;
            return this;
        }

        public CreateLectureDTOBuilder endTime(String time) {
            this.endTime = time;
            return this;
        }
    }
}
