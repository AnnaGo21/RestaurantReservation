package org.example.reservations.reservation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.reservations.auth.SecurityUtils;
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return reservationService.getAllReservationsByRestaurant(
                SecurityUtils.currentRestaurantId(),
                date.atStartOfDay(),
                date.atTime(LocalTime.MAX)
        );
    }

    @GetMapping("/weekly")
    public List<ReservationDto> getWeeklyReservations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        return reservationService.getAllReservationsByRestaurant(
                SecurityUtils.currentRestaurantId(),
                weekStart.atStartOfDay(),
                weekStart.plusDays(7).atStartOfDay()
        );
    }

    @GetMapping("/available-tables")
    public List<Long> getAvailableTables(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        return reservationService.getAvailableTables(SecurityUtils.currentRestaurantId(), startTime, endTime);
    }

    @PatchMapping("/{id}/status")
    public ReservationDto updateStatus(@PathVariable Long id, @RequestParam ReservationStatus status) {
        return reservationService.updateReservationStatus(id, status);
    }

    @PatchMapping("/{id}/cancel")
    public ReservationDto cancelReservation(@PathVariable Long id) {
        return reservationService.cancelReservation(id);
    }

    @PostMapping("/walk-in")
    public ReservationDto createWalkIn(@Valid @RequestBody WalkInRequest request) {
        return reservationService.createWalkIn(request);
    }

    @PatchMapping("/{id}/move")
    public ReservationDto moveReservation(
            @PathVariable Long id,
            @RequestBody MoveReservationRequest request
    ) {
        return reservationService.moveReservation(id, request);
    }

    @PatchMapping("/{id}/check-in")
    public ReservationDto checkIn(@PathVariable Long id) {
        return reservationService.checkIn(id);
    }
}
