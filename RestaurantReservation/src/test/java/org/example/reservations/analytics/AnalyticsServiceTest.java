package org.example.reservations.analytics;

import org.example.reservations.guest.Guest;
import org.example.reservations.reservation.Reservation;
import org.example.reservations.reservation.ReservationRepository;
import org.example.reservations.reservation.ReservationStatus;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.table.RestaurantTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock ReservationRepository reservationRepository;

    AnalyticsService service;

    Restaurant restaurant;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(reservationRepository);
        restaurant = new Restaurant();
        restaurant.setId(1L);
    }

    private Reservation res(LocalDateTime start, int partySize, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setRestaurant(restaurant);
        RestaurantTable t = new RestaurantTable();
        t.setId(1L);
        r.setRestaurantTable(t);
        Guest g = new Guest();
        g.setId(1L);
        r.setGuest(g);
        r.setStartTime(start);
        r.setEndTime(start.plusMinutes(90));
        r.setPartySize(partySize);
        r.setStatus(status);
        return r;
    }

    @Test
    void computesPeakHoursByStartHour() {
        // Two at 19:00, one at 12:00
        List<Reservation> data = List.of(
                res(LocalDateTime.of(2026, 7, 6, 19, 0), 2, ReservationStatus.COMPLETED),
                res(LocalDateTime.of(2026, 7, 6, 19, 45), 4, ReservationStatus.COMPLETED),
                res(LocalDateTime.of(2026, 7, 7, 12, 0), 2, ReservationStatus.COMPLETED)
        );
        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(data);

        AnalyticsResponse r = service.getAnalytics(1L,
                LocalDateTime.of(2026, 7, 6, 0, 0),
                LocalDateTime.of(2026, 7, 13, 0, 0));

        assertThat(r.peakHours()).containsEntry(19, 2L).containsEntry(12, 1L);
    }

    @Test
    void computesBusiestDaysOfWeek() {
        LocalDateTime monday = LocalDateTime.of(2026, 7, 6, 19, 0);   // Monday
        LocalDateTime tuesday = LocalDateTime.of(2026, 7, 7, 19, 0);  // Tuesday

        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of(
                        res(monday, 2, ReservationStatus.COMPLETED),
                        res(monday.plusHours(1), 2, ReservationStatus.COMPLETED),
                        res(tuesday, 2, ReservationStatus.COMPLETED)
                ));

        AnalyticsResponse r = service.getAnalytics(1L,
                LocalDateTime.of(2026, 7, 6, 0, 0),
                LocalDateTime.of(2026, 7, 13, 0, 0));

        assertThat(r.busiestDays()).containsEntry(DayOfWeek.MONDAY.name(), 2L);
        assertThat(r.busiestDays()).containsEntry(DayOfWeek.TUESDAY.name(), 1L);
    }

    @Test
    void computesAveragePartySize() {
        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of(
                        res(LocalDateTime.now(), 2, ReservationStatus.COMPLETED),
                        res(LocalDateTime.now(), 4, ReservationStatus.COMPLETED),
                        res(LocalDateTime.now(), 6, ReservationStatus.COMPLETED)
                ));

        AnalyticsResponse r = service.getAnalytics(1L, LocalDateTime.now(), LocalDateTime.now());

        assertThat(r.averagePartySize()).isEqualTo(4.0);
    }

    @Test
    void computesStatusBreakdownAndNoShowPercentage() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 6, 19, 0);
        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of(
                        res(t, 2, ReservationStatus.COMPLETED),
                        res(t, 2, ReservationStatus.COMPLETED),
                        res(t, 2, ReservationStatus.CANCELLED),
                        res(t, 2, ReservationStatus.NO_SHOW)
                ));

        AnalyticsResponse r = service.getAnalytics(1L, t, t.plusDays(1));

        assertThat(r.totalReservations()).isEqualTo(4);
        assertThat(r.completedReservations()).isEqualTo(2);
        assertThat(r.cancelledReservations()).isEqualTo(1);
        assertThat(r.noShows()).isEqualTo(1);
        assertThat(r.noShowPercentage()).isEqualTo(25.0, offset(0.001));
    }

    @Test
    void noShowPercentageIsZeroWhenNoReservations() {
        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        AnalyticsResponse r = service.getAnalytics(1L, LocalDateTime.now(), LocalDateTime.now());

        assertThat(r.noShowPercentage()).isEqualTo(0.0);
        assertThat(r.averagePartySize()).isEqualTo(0.0);
        assertThat(r.totalReservations()).isZero();
    }

    @Test
    void dailyTrendsGroupPerCalendarDay() {
        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of(
                        res(LocalDateTime.of(2026, 7, 6, 12, 0), 2, ReservationStatus.COMPLETED),
                        res(LocalDateTime.of(2026, 7, 6, 20, 0), 2, ReservationStatus.COMPLETED),
                        res(LocalDateTime.of(2026, 7, 7, 12, 0), 2, ReservationStatus.COMPLETED)
                ));

        AnalyticsResponse r = service.getAnalytics(1L,
                LocalDateTime.of(2026, 7, 6, 0, 0),
                LocalDateTime.of(2026, 7, 8, 0, 0));

        assertThat(r.dailyTrends()).containsEntry(java.time.LocalDate.of(2026, 7, 6), 2L);
        assertThat(r.dailyTrends()).containsEntry(java.time.LocalDate.of(2026, 7, 7), 1L);
    }
}
