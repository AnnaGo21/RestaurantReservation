package org.example.reservations.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for reading user data (safe to expose)
 * Never includes password_hash or sensitive data
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppUserDto {
    private Long id;
    private String fullName;
    private String email;
    private String role;  // Read-only, can't be modified by user
    private Long restaurantId;
}
