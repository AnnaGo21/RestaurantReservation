package org.example.reservations.calendar;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/daily")
    @PreAuthorize("isAuthenticated()")
    public DailyCalendarDto getDailyCalendar(
            @RequestParam Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return calendarService.getDailyCalendar(restaurantId, date);
    }

    @GetMapping("/weekly")
    @PreAuthorize("isAuthenticated()")
    public WeeklyCalendarDto getWeeklyCalendar(
            @RequestParam Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        return calendarService.getWeeklyCalendar(restaurantId, startDate);
    }
}
