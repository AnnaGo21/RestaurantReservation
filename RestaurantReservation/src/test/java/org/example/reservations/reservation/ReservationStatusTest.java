package org.example.reservations.reservation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "PENDING,   CONFIRMED",
            "PENDING,   CANCELLED",
            "CONFIRMED, SEATED",
            "CONFIRMED, COMPLETED",
            "CONFIRMED, NO_SHOW",
            "CONFIRMED, CANCELLED",
            "SEATED,    COMPLETED",
            "SEATED,    CANCELLED"
    })
    void allowedTransitions(ReservationStatus from, ReservationStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            // Backwards / same-state / skip-over
            "PENDING,   SEATED",
            "PENDING,   COMPLETED",
            "PENDING,   NO_SHOW",
            "PENDING,   PENDING",
            "CONFIRMED, PENDING",
            "CONFIRMED, CONFIRMED",
            "SEATED,    PENDING",
            "SEATED,    CONFIRMED",
            "SEATED,    NO_SHOW",
            "SEATED,    SEATED",
            // Terminal states cannot leave
            "COMPLETED, PENDING",
            "COMPLETED, CONFIRMED",
            "COMPLETED, SEATED",
            "COMPLETED, CANCELLED",
            "COMPLETED, NO_SHOW",
            "CANCELLED, PENDING",
            "CANCELLED, CONFIRMED",
            "CANCELLED, SEATED",
            "CANCELLED, COMPLETED",
            "CANCELLED, NO_SHOW",
            "NO_SHOW,   PENDING",
            "NO_SHOW,   CONFIRMED",
            "NO_SHOW,   SEATED",
            "NO_SHOW,   COMPLETED",
            "NO_SHOW,   CANCELLED"
    })
    void forbiddenTransitions(ReservationStatus from, ReservationStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }
}
