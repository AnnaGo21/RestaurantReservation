package org.example.reservations.notification;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${sms.enabled}")
    private boolean smsEnabled;

    @Value("${sms.mock-mode}")
    private boolean mockMode;

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    @PostConstruct
    void init() {
        if (smsEnabled && !mockMode) {
            if (accountSid == null || accountSid.isBlank()) {
                log.warn("SMS enabled but Twilio credentials not configured. Set SMS_MOCK_MODE=true for development.");
                return;
            }
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS initialized");
        } else if (mockMode) {
            log.info("SMS service running in MOCK MODE - messages will be logged only");
        } else {
            log.info("SMS service disabled");
        }
    }

    public void sendConfirmation(String toNumber, String guestName, String restaurantName, String dateTime) {
        String message = String.format(
                "Hi %s! Your reservation at %s is confirmed for %s. See you soon!",
                guestName, restaurantName, dateTime
        );
        sendSms(toNumber, message);
    }

    public void sendReminder(String toNumber, String guestName, String restaurantName, String dateTime) {
        String message = String.format(
                "Reminder: You have a reservation at %s tomorrow at %s. Looking forward to seeing you, %s!",
                restaurantName, dateTime, guestName
        );
        sendSms(toNumber, message);
    }

    private void sendSms(String toNumber, String messageBody) {
        if (!smsEnabled) {
            log.debug("SMS disabled. Would have sent to {}: {}", toNumber, messageBody);
            return;
        }

        if (mockMode) {
            log.info("📱 [MOCK SMS] To: {} | Message: {}", toNumber, messageBody);
            return;
        }

        try {
            Message.creator(
                    new com.twilio.type.PhoneNumber(toNumber),
                    new com.twilio.type.PhoneNumber(fromNumber),
                    messageBody
            ).create();
            log.info("✓ SMS sent to {}", toNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toNumber, e.getMessage());
        }
    }
}
