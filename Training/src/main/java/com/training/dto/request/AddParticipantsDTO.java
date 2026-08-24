package com.training.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddParticipantsDTO {

    @NotEmpty(message = "At least one studentId is required")
    @JsonAlias({"studentIds", "leadIds"})
    private List<String> studentIds;
}
