package org.example.reservations.reservation;

import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationDto toDto(Reservation reservation) {
        return new ReservationDto(
                reservation.getId(),
                reservation.getRestaurant().getId(),
                reservation.getRestaurantTable().getId(),
                reservation.getGuest().getId(),
                reservation.getGuest().getFullName(),
                reservation.getGuest().getPhone(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPartySize(),
                reservation.getNotes(),
                reservation.getStatus(),
                reservation.getReminderSent(),
                reservation.getCheckedInAt()
        );
    }
}
