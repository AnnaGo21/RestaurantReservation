package org.example.reservations.calendar;

import org.example.reservations.reservation.ReservationStatus;

import java.time.LocalDateTime;

public record TimeSlotDto(
        Long reservationId,
        Long tableId,
        String tableName,
        String guestName,
        String guestPhone,
        int partySize,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ReservationStatus status,
        String notes
) {
}
