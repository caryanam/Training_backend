package com.training.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFacultyDTO {

    @NotBlank(message = "fullName is required")
    @Size(min = 2, message = "Full name must be at least 2 characters")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String phoneNumber;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Size(min = 2, message = "Department must be at least 2 characters")
    private String department;
}
