package org.example.reservations.calendar;

import lombok.RequiredArgsConstructor;
import org.example.reservations.auth.SecurityUtils;
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return calendarService.getDailyCalendar(SecurityUtils.currentRestaurantId(), date);
    }

    @GetMapping("/weekly")
    @PreAuthorize("isAuthenticated()")
    public WeeklyCalendarDto getWeeklyCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        return calendarService.getWeeklyCalendar(SecurityUtils.currentRestaurantId(), startDate);
    }
}
