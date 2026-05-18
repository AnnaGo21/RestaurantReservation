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
        LocalDateTime in24Hours = now.plusHours(24);
        LocalDateTime in23Hours = now.plusHours(23);

        // Find reservations happening in 23-24 hours that haven't received reminders
        List<Reservation> upcomingReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .filter(r -> !r.getReminderSent())
                .filter(r -> r.getStartTime().isAfter(in23Hours) && r.getStartTime().isBefore(in24Hours))
                .toList();

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
