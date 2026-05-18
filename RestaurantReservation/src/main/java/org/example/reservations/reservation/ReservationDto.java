package org.example.reservations.reservation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {
    private Long id;
    private Long restaurantId;
    private Long tableId;
    private Long guestId;
    private String guestName;
    private String guestPhone;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer partySize;
    private String notes;
    private ReservationStatus status;
    private Boolean reminderSent;
    private LocalDateTime checkedInAt;
}
