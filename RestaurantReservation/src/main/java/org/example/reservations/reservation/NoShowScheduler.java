package org.example.reservations.reservation;

import org.example.reservations.guest.GuestRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class NoShowScheduler {

    private final ReservationRepository reservationRepository;
    private final GuestRepository guestRepository;

    public NoShowScheduler(
            ReservationRepository reservationRepository,
            GuestRepository guestRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.guestRepository = guestRepository;
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void markExpiredReservationsAsNoShow() {
        LocalDateTime now = LocalDateTime.now();

        var activeReservations = reservationRepository.findAll().stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
                .filter(reservation -> reservation.getStartTime()
                        .plusMinutes(reservation.getRestaurant().getGracePeriodMinutes())
                        .isBefore(now))
                .toList();

        for (Reservation reservation : activeReservations) {
            if (!reservation.getStatus().canTransitionTo(ReservationStatus.NO_SHOW)) {
                continue;
            }
            reservation.setStatus(ReservationStatus.NO_SHOW);

            var guest = reservation.getGuest();
            guest.setNoShowCount(guest.getNoShowCount() + 1);

            guestRepository.save(guest);
            reservationRepository.save(reservation);
        }
    }
}
