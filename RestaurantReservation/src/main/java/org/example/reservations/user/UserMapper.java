package org.example.reservations.user;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public AppUserDto toDto(AppUser user) {
        return new AppUserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getRestaurant() != null ? user.getRestaurant().getId() : null
        );
    }
}
