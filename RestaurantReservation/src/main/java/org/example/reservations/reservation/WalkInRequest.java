package org.example.reservations.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WalkInRequest(
        @NotNull Long tableId,
        @NotNull String guestName,
        String guestPhone,
        @NotNull @Min(1) Integer partySize,
        String notes
) {}
