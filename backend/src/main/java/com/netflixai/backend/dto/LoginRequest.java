package com.netflixai.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email must be 254 characters or fewer")
        String email,

        @NotBlank(message = "Password is required")
        String password,

        boolean rememberMe) {
}
