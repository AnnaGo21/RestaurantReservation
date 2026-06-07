package org.example.reservations.guest;

import org.springframework.stereotype.Component;

@Component
public class GuestMapper {

    public GuestDto toDto(Guest guest) {
        return new GuestDto(
                guest.getId(),
                guest.getFullName(),
                guest.getPhone(),
                guest.getEmail(),
                guest.getTotalVisits(),
                guest.getNoShowCount(),
                guest.getRestaurant().getId()
        );
    }
}
