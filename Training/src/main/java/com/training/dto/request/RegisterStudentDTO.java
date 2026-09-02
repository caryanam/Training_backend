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
public class RegisterStudentDTO {

    @NotBlank(message = "fullName is required")
    @Size(min = 2, message = "Full name must be at least 2 characters")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @jakarta.validation.constraints.Pattern(
            regexp = "^(?:\\+?91[\\-\\s]?)?[6-9](?:[\\-\\s]?\\d){9}$",
            message = "Mobile number must be a valid 10-digit number starting with 6, 7, 8, or 9"
    )
    private String phone;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String interestedCourse;

    private String education;

    private String city;
}
