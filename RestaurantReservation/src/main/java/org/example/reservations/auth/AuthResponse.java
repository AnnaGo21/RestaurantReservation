package org.example.reservations.auth;

public record AuthResponse(
        String token,
        String email,
        String role
) {
}
