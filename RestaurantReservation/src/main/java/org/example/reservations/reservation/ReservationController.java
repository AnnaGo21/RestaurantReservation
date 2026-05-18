package org.example.reservations.reservation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ReservationDto createReservation(@Valid @RequestBody ReservationRequest request) {
        return reservationService.createReservation(request);
    }

    @GetMapping("/{id}")
    public ReservationDto getReservationById(@PathVariable Long id) {
        return reservationService.getReservationById(id);
    }

    @GetMapping("/daily")
    public List<ReservationDto> getDailyReservations(
            @RequestParam Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return reservationService.getAllReservationsByRestaurant(
                restaurantId,
                date.atStartOfDay(),
                date.atTime(LocalTime.MAX)
        );
    }

    @GetMapping("/weekly")
    public List<ReservationDto> getWeeklyReservations(
            @RequestParam Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        return reservationService.getAllReservationsByRestaurant(
                restaurantId,
                weekStart.atStartOfDay(),
                weekStart.plusDays(7).atStartOfDay()
        );
    }

    @GetMapping("/available-tables")
    public List<Long> getAvailableTables(
            @RequestParam Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        return reservationService.getAvailableTables(restaurantId, startTime, endTime);
    }

    @PatchMapping("/{id}/status")
    public ReservationDto updateStatus(@PathVariable Long id, @RequestParam ReservationStatus status) {
        return reservationService.updateReservationStatus(id, status);
    }

    @PatchMapping("/{id}/cancel")
    public ReservationDto cancelReservation(@PathVariable Long id) {
        return reservationService.cancelReservation(id);
    }
}
