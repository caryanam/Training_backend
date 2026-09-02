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
public class CreateExecutorDTO {

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "Invalid email format")
    private String email;

    @jakarta.validation.constraints.Pattern(
            regexp = "^$|^(?:\\+?91[\\-\\s]?)?[6-9](?:[\\-\\s]?\\d){9}$",
            message = "Mobile number must be a valid 10-digit number starting with 6, 7, 8, or 9"
    )
    private String phone;

    @jakarta.validation.constraints.Pattern(
            regexp = "^$|^(?:\\+?91[\\-\\s]?)?[6-9](?:[\\-\\s]?\\d){9}$",
            message = "Mobile number must be a valid 10-digit number starting with 6, 7, 8, or 9"
    )
    private String phoneNumber;
    private String password;
}
