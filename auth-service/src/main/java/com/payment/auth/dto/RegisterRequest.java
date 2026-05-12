package com.payment.auth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username may only contain letters, digits, underscores, dots, and hyphens")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 128, message = "Email must not exceed 128 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 64, message = "First name must not exceed 64 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 64, message = "Last name must not exceed 64 characters")
    private String lastName;
}
