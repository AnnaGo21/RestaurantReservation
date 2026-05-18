package org.example.reservations.calendar;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DailyCalendarDto(
        LocalDate date,
        Map<Long, TableScheduleDto> tableSchedules
) {
}
