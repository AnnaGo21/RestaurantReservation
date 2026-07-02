package org.example.reservations.reservation;

import org.example.reservations.guest.Guest;
import org.example.reservations.guest.GuestRepository;
import org.example.reservations.restaurant.Restaurant;
import org.example.reservations.table.RestaurantTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoShowSchedulerTest {

    @Mock ReservationRepository reservationRepository;
    @Mock GuestRepository guestRepository;

    NoShowScheduler scheduler;

    Restaurant restaurant;

    @BeforeEach
    void setUp() {
        scheduler = new NoShowScheduler(reservationRepository, guestRepository);
        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setGracePeriodMinutes(15);
    }

    private Reservation reservation(ReservationStatus status, LocalDateTime start, Guest guest) {
        Reservation r = new Reservation();
        r.setRestaurant(restaurant);
        RestaurantTable t = new RestaurantTable();
        t.setId(1L);
        r.setRestaurantTable(t);
        r.setGuest(guest);
        r.setStartTime(start);
        r.setEndTime(start.plusMinutes(90));
        r.setPartySize(2);
        r.setStatus(status);
        return r;
    }

    private Guest guest() {
        Guest g = new Guest();
        g.setId(1L);
        g.setNoShowCount(2);
        g.setTotalVisits(5);
        return g;
    }

    @Test
    void marksConfirmedReservationAsNoShowWhenGracePeriodElapsed() {
        Guest g = guest();
        int noShowsBefore = g.getNoShowCount();
        // Started 60 minutes ago, grace = 15 → well past deadline.
        Reservation stale = reservation(ReservationStatus.CONFIRMED,
                LocalDateTime.now().minusMinutes(60), g);

        when(reservationRepository.findAll()).thenReturn(List.of(stale));

        scheduler.markExpiredReservationsAsNoShow();

        assertThat(stale.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
        assertThat(g.getNoShowCount()).isEqualTo(noShowsBefore + 1);
        verify(reservationRepository).save(stale);
        verify(guestRepository).save(g);
    }

    @Test
    void doesNotFireWhenStillWithinGracePeriod() {
        Guest g = guest();
        int before = g.getNoShowCount();
        // Started 5 minutes ago; grace = 15 → deadline has NOT passed.
        Reservation inGrace = reservation(ReservationStatus.CONFIRMED,
                LocalDateTime.now().minusMinutes(5), g);

        when(reservationRepository.findAll()).thenReturn(List.of(inGrace));

        scheduler.markExpiredReservationsAsNoShow();

        assertThat(inGrace.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(g.getNoShowCount()).isEqualTo(before);
        verify(reservationRepository, never()).save(inGrace);
        verify(guestRepository, never()).save(g);
    }

    @Test
    void ignoresReservationsInTerminalStates() {
        Guest g = guest();
        int before = g.getNoShowCount();
        LocalDateTime longAgo = LocalDateTime.now().minusHours(3);

        Reservation completed = reservation(ReservationStatus.COMPLETED, longAgo, g);
        Reservation cancelled = reservation(ReservationStatus.CANCELLED, longAgo, g);
        Reservation noShow    = reservation(ReservationStatus.NO_SHOW, longAgo, g);
        Reservation seated    = reservation(ReservationStatus.SEATED, longAgo, g);
        Reservation pending   = reservation(ReservationStatus.PENDING, longAgo, g);

        when(reservationRepository.findAll())
                .thenReturn(List.of(completed, cancelled, noShow, seated, pending));

        scheduler.markExpiredReservationsAsNoShow();

        assertThat(completed.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(noShow.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
        assertThat(seated.getStatus()).isEqualTo(ReservationStatus.SEATED);
        assertThat(pending.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(g.getNoShowCount()).isEqualTo(before);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void processesOnlyRestaurantsOwnGracePeriod() {
        // Different restaurants can have different grace periods; scheduler must use each row's own.
        Restaurant strictRestaurant = new Restaurant();
        strictRestaurant.setId(2L);
        strictRestaurant.setGracePeriodMinutes(5);

        Restaurant lenient = new Restaurant();
        lenient.setId(3L);
        lenient.setGracePeriodMinutes(120);

        Guest g1 = guest();
        Guest g2 = guest();

        Reservation strictRes = reservation(ReservationStatus.CONFIRMED,
                LocalDateTime.now().minusMinutes(10), g1);
        strictRes.setRestaurant(strictRestaurant);

        Reservation lenientRes = reservation(ReservationStatus.CONFIRMED,
                LocalDateTime.now().minusMinutes(10), g2);
        lenientRes.setRestaurant(lenient);

        when(reservationRepository.findAll()).thenReturn(List.of(strictRes, lenientRes));

        scheduler.markExpiredReservationsAsNoShow();

        assertThat(strictRes.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
        assertThat(lenientRes.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(reservationRepository, times(1)).save(strictRes);
        verify(reservationRepository, never()).save(lenientRes);
    }

}
