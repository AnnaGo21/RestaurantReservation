package org.example.reservations.reservation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull Long restaurantId,
        @NotNull Long tableId,
        @NotNull String guestName,
        @NotNull String guestPhone,
        String guestEmail,
        @NotNull @Future LocalDateTime startTime,
        @NotNull @Min(1) Integer partySize,
        String notes
) {
}
