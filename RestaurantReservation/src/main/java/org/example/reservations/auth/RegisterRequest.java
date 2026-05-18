package org.example.reservations.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.reservations.user.Role;

public record RegisterRequest(
        @NotBlank String fullName,
        @Email String email,
        @NotBlank String password,
        @NotNull Role role,
        Long restaurantId
) {
}