package com.training.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {

    @NotBlank(message = "Identifier (email or mobile) is required")
    @com.fasterxml.jackson.annotation.JsonAlias("email")
    private String identifier;

    @NotBlank(message = "password is required")
    private String password;
}
