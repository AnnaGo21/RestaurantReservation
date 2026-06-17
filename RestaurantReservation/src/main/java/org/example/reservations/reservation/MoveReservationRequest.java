package org.example.reservations.reservation;

import java.time.LocalDateTime;

public record MoveReservationRequest(
        Long tableId,
        LocalDateTime startTime
) {}
