package org.example.reservations.reservation;

import java.util.Map;
import java.util.Set;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    SEATED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED = Map.of(
            PENDING,   Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(SEATED, COMPLETED, NO_SHOW, CANCELLED),
            SEATED,    Set.of(COMPLETED, CANCELLED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of(),
            NO_SHOW,   Set.of()
    );

    public boolean canTransitionTo(ReservationStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}
