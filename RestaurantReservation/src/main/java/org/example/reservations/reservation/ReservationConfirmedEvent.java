package org.example.reservations.reservation;

public record ReservationConfirmedEvent(
        String guestPhone,
        String guestName,
        String restaurantName,
        String formattedDateTime
) {}
