package org.example.reservations.dashboard;

import org.example.reservations.guest.Guest;
import org.example.reservations.reservation.Reservation;
import org.example.reservations.reservation.ReservationMapper;
import org.example.reservations.reservation.ReservationRepository;
import org.example.reservations.reservation.ReservationStatus;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.table.RestaurantTable;
import org.example.reservations.table.RestaurantTableRepository;
import org.example.reservations.table.TableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock RestaurantTableRepository tableRepository;

    // Use real mappers — trivial, and mocking them hides real projection bugs.
    ReservationMapper reservationMapper = new ReservationMapper();
    TableMapper tableMapper = new TableMapper();

    DashboardService service;

    Restaurant restaurant;

    @BeforeEach
    void setUp() {
        service = new DashboardService(reservationRepository, tableRepository, reservationMapper, tableMapper);
        restaurant = new Restaurant();
        restaurant.setId(1L);
    }

    private RestaurantTable table(long id) {
        RestaurantTable t = new RestaurantTable();
        t.setId(id);
        t.setCapacity(4);
        t.setRestaurant(restaurant);
        return t;
    }

    private Reservation res(RestaurantTable t, LocalDateTime start, LocalDateTime end,
                            ReservationStatus status) {
        Reservation r = new Reservation();
        r.setId(start.getNano() + 1L);
        r.setRestaurant(restaurant);
        r.setRestaurantTable(t);
        Guest g = new Guest();
        g.setId(1L);
        g.setFullName("G");
        g.setPhone("+1");
        r.setGuest(g);
        r.setStartTime(start);
        r.setEndTime(end);
        r.setPartySize(2);
        r.setStatus(status);
        r.setReminderSent(false);
        return r;
    }

    @Test
    void todayReservationsIncludeAllStatuses() {
        RestaurantTable t = table(10L);
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> all = List.of(
                res(t, now.minusHours(2), now.minusHours(1), ReservationStatus.COMPLETED),
                res(t, now.plusHours(1),  now.plusHours(2),  ReservationStatus.CONFIRMED),
                res(t, now.minusHours(3), now.minusHours(2), ReservationStatus.CANCELLED),
                res(t, now.minusHours(1), now.plusMinutes(30), ReservationStatus.SEATED)
        );
        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(all);
        when(tableRepository.findByRestaurantId(1L)).thenReturn(List.of(t));
        when(reservationRepository.countByRestaurantId(1L)).thenReturn(4L);
        when(reservationRepository.countByRestaurantIdAndStatus(1L, ReservationStatus.NO_SHOW))
                .thenReturn(0L);

        DashboardDto dto = service.getDashboardData(1L);

        // Full picture for staff — cancelled/completed included
        assertThat(dto.getTodayReservations()).hasSize(4);
        assertThat(dto.getTotalReservationsToday()).isEqualTo(4);
    }

    @Test
    void upcomingReservationsExcludeCancelledCompletedAndPast() {
        RestaurantTable t = table(10L);
        LocalDateTime now = LocalDateTime.now();

        Reservation upcomingActive = res(t, now.plusHours(1), now.plusHours(2), ReservationStatus.CONFIRMED);
        Reservation upcomingCancelled = res(t, now.plusHours(1), now.plusHours(2), ReservationStatus.CANCELLED);
        Reservation past = res(t, now.minusHours(1), now.minusMinutes(1), ReservationStatus.COMPLETED);

        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of(upcomingActive, upcomingCancelled, past));
        when(tableRepository.findByRestaurantId(1L)).thenReturn(List.of(t));
        when(reservationRepository.countByRestaurantId(1L)).thenReturn(3L);
        when(reservationRepository.countByRestaurantIdAndStatus(1L, ReservationStatus.NO_SHOW)).thenReturn(0L);

        DashboardDto dto = service.getDashboardData(1L);

        assertThat(dto.getUpcomingReservations()).hasSize(1);
        assertThat(dto.getUpcomingReservations().get(0).getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void occupiedTablesArePartitionedFromFreeTables() {
        RestaurantTable occupied = table(10L);
        RestaurantTable free = table(11L);
        LocalDateTime now = LocalDateTime.now();

        Reservation ongoing = res(occupied,
                now.minusMinutes(30), now.plusMinutes(30), ReservationStatus.SEATED);

        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of(ongoing));
        when(tableRepository.findByRestaurantId(1L)).thenReturn(List.of(occupied, free));
        when(reservationRepository.countByRestaurantId(1L)).thenReturn(1L);
        when(reservationRepository.countByRestaurantIdAndStatus(1L, ReservationStatus.NO_SHOW)).thenReturn(0L);

        DashboardDto dto = service.getDashboardData(1L);

        assertThat(dto.getOccupiedTables()).extracting("id").containsExactly(10L);
        assertThat(dto.getFreeTables()).extracting("id").containsExactly(11L);
    }

    @Test
    void cancelledReservationDoesNotOccupyTable() {
        RestaurantTable t = table(10L);
        LocalDateTime now = LocalDateTime.now();

        Reservation cancelledWindow = res(t,
                now.minusMinutes(30), now.plusMinutes(30), ReservationStatus.CANCELLED);

        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of(cancelledWindow));
        when(tableRepository.findByRestaurantId(1L)).thenReturn(List.of(t));
        when(reservationRepository.countByRestaurantId(1L)).thenReturn(1L);
        when(reservationRepository.countByRestaurantIdAndStatus(1L, ReservationStatus.NO_SHOW)).thenReturn(0L);

        DashboardDto dto = service.getDashboardData(1L);

        assertThat(dto.getOccupiedTables()).isEmpty();
        assertThat(dto.getFreeTables()).extracting("id").containsExactly(10L);
    }

    @Test
    void noShowPercentageIsAllTimeStableMetric() {
        RestaurantTable t = table(10L);
        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(tableRepository.findByRestaurantId(1L)).thenReturn(List.of(t));
        // All-time counts, not just today
        when(reservationRepository.countByRestaurantId(1L)).thenReturn(200L);
        when(reservationRepository.countByRestaurantIdAndStatus(1L, ReservationStatus.NO_SHOW))
                .thenReturn(30L);

        DashboardDto dto = service.getDashboardData(1L);

        assertThat(dto.getNoShowPercentage()).isEqualTo(15.0, offset(0.001));
    }

    @Test
    void noShowPercentageIsZeroWhenNoReservationsAllTime() {
        RestaurantTable t = table(10L);
        when(reservationRepository.findByRestaurantIdAndStartTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(tableRepository.findByRestaurantId(1L)).thenReturn(List.of(t));
        when(reservationRepository.countByRestaurantId(1L)).thenReturn(0L);
        when(reservationRepository.countByRestaurantIdAndStatus(1L, ReservationStatus.NO_SHOW))
                .thenReturn(0L);

        DashboardDto dto = service.getDashboardData(1L);

        assertThat(dto.getNoShowPercentage()).isEqualTo(0.0);
    }
}
