package org.example.reservations.notification;

import org.example.reservations.reservation.Reservation;
import org.example.reservations.reservation.ReservationRepository;
import org.example.reservations.reservation.ReservationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class SmsReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(SmsReminderScheduler.class);

    private final ReservationRepository reservationRepository;
    private final SmsService smsService;

    public SmsReminderScheduler(
            ReservationRepository reservationRepository,
            SmsService smsService
    ) {
        this.reservationRepository = reservationRepository;
        this.smsService = smsService;
    }

    // Run every 30 minutes
    @Scheduled(fixedRate = 1800000)
    @Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        // Send reminders for any CONFIRMED reservation starting in the next 1–24 hours
        // that hasn't been reminded yet. The 1-hour minimum ensures the guest still has
        // time to act on the reminder. The reminderSent flag prevents duplicate sends.
        LocalDateTime from = now.plusHours(1);
        LocalDateTime until = now.plusHours(24);

        List<Reservation> upcomingReservations = reservationRepository
                .findByStatusAndReminderSentFalseAndStartTimeBetween(ReservationStatus.CONFIRMED, from, until);

        log.info("Found {} reservations needing reminders", upcomingReservations.size());

        for (Reservation reservation : upcomingReservations) {
            try {
                String formattedDateTime = reservation.getStartTime()
                        .format(DateTimeFormatter.ofPattern("MMM dd 'at' HH:mm"));

                smsService.sendReminder(
                        reservation.getGuest().getPhone(),
                        reservation.getGuest().getFullName(),
                        reservation.getRestaurant().getName(),
                        formattedDateTime
                );

                reservation.setReminderSent(true);
                reservationRepository.save(reservation);

                log.info("Reminder sent for reservation #{}", reservation.getId());
            } catch (Exception e) {
                log.error("Failed to send reminder for reservation #{}: {}",
                        reservation.getId(), e.getMessage());
            }
        }
    }
}
