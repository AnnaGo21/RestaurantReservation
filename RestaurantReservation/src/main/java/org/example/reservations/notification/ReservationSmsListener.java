package org.example.reservations.notification;

import org.example.reservations.reservation.ReservationConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationSmsListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationSmsListener.class);

    private final SmsService smsService;

    public ReservationSmsListener(SmsService smsService) {
        this.smsService = smsService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationConfirmed(ReservationConfirmedEvent e) {
        try {
            smsService.sendConfirmation(
                    e.guestPhone(),
                    e.guestName(),
                    e.restaurantName(),
                    e.formattedDateTime()
            );
        } catch (Exception ex) {
            log.error("SMS confirmation failed for {}: {}", e.guestPhone(), ex.getMessage());
        }
    }
}
