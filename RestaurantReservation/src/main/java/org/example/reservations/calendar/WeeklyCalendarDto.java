package org.example.reservations.calendar;

import java.time.LocalDate;
import java.util.List;

public record WeeklyCalendarDto(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<DailyCalendarDto> days
) {
}
