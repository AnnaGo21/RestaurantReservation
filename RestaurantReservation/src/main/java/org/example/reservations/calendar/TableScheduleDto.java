package org.example.reservations.calendar;

import java.util.List;

public record TableScheduleDto(
        Long tableId,
        String tableName,
        int capacity,
        List<TimeSlotDto> reservations
) {
}
