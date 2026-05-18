package org.example.reservations.calendar;

import lombok.RequiredArgsConstructor;
import org.example.reservations.reservation.Reservation;
import org.example.reservations.reservation.ReservationRepository;
import org.example.reservations.table.RestaurantTable;
import org.example.reservations.table.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;

    @Transactional(readOnly = true)
    public DailyCalendarDto getDailyCalendar(Long restaurantId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<RestaurantTable> tables = tableRepository.findByRestaurantId(restaurantId);
        List<Reservation> reservations = reservationRepository.findByRestaurantIdAndStartTimeBetween(
                restaurantId,
                startOfDay,
                endOfDay
        );

        Map<Long, TableScheduleDto> tableSchedules = new LinkedHashMap<>();

        for (RestaurantTable table : tables) {
            List<TimeSlotDto> tableReservations = reservations.stream()
                    .filter(r -> r.getRestaurantTable().getId().equals(table.getId()))
                    .map(this::toTimeSlotDto)
                    .collect(Collectors.toList());

            TableScheduleDto schedule = new TableScheduleDto(
                    table.getId(),
                    table.getLabel(),
                    table.getCapacity(),
                    tableReservations
            );

            tableSchedules.put(table.getId(), schedule);
        }

        return new DailyCalendarDto(date, tableSchedules);
    }

    @Transactional(readOnly = true)
    public WeeklyCalendarDto getWeeklyCalendar(Long restaurantId, LocalDate startDate) {
        // Calculate week start (Monday) and end (Sunday)
        LocalDate weekStart = startDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyCalendarDto> days = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            DailyCalendarDto dailyView = getDailyCalendar(restaurantId, date);
            days.add(dailyView);
        }

        return new WeeklyCalendarDto(weekStart, weekEnd, days);
    }

    private TimeSlotDto toTimeSlotDto(Reservation reservation) {
        return new TimeSlotDto(
                reservation.getId(),
                reservation.getRestaurantTable().getId(),
                reservation.getRestaurantTable().getLabel(),
                reservation.getGuest().getFullName(),
                reservation.getGuest().getPhone(),
                reservation.getPartySize(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getNotes()
        );
    }
}
