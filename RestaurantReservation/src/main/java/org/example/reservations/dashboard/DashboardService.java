package org.example.reservations.dashboard;

import lombok.RequiredArgsConstructor;
import org.example.reservations.reservation.Reservation;
import org.example.reservations.reservation.ReservationDto;
import org.example.reservations.reservation.ReservationMapper;
import org.example.reservations.reservation.ReservationRepository;
import org.example.reservations.reservation.ReservationStatus;
import org.example.reservations.table.RestaurantTable;
import org.example.reservations.table.RestaurantTableRepository;
import org.example.reservations.table.TableDto;
import org.example.reservations.table.TableMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final ReservationMapper reservationMapper;
    private final TableMapper tableMapper;

    @Transactional(readOnly = true)
    public DashboardDto getDashboardData(Long restaurantId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime now = LocalDateTime.now();

        // All reservations for today regardless of status — full picture for staff
        List<Reservation> todayReservations = reservationRepository
                .findByRestaurantIdAndStartTimeBetween(restaurantId, todayStart, todayEnd);

        List<ReservationDto> todayReservationDtos = todayReservations.stream()
                .map(reservationMapper::toDto)
                .collect(Collectors.toList());

        // Subset of today's reservations that are still upcoming and active (not cancelled/no-show/completed)
        List<ReservationDto> upcomingReservations = todayReservations.stream()
                .filter(r -> r.getStartTime().isAfter(now))
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED
                        && r.getStatus() != ReservationStatus.NO_SHOW
                        && r.getStatus() != ReservationStatus.COMPLETED)
                .map(reservationMapper::toDto)
                .collect(Collectors.toList());

        List<Long> occupiedTableIds = todayReservations.stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED
                        && r.getStatus() != ReservationStatus.COMPLETED
                        && r.getStatus() != ReservationStatus.NO_SHOW)
                .filter(r -> r.getStartTime().isBefore(now) && r.getEndTime().isAfter(now))
                .map(r -> r.getRestaurantTable().getId())
                .toList();

        List<RestaurantTable> allTables = tableRepository.findByRestaurantId(restaurantId);

        List<TableDto> occupiedTables = allTables.stream()
                .filter(t -> occupiedTableIds.contains(t.getId()))
                .map(tableMapper::toDto)
                .collect(Collectors.toList());

        List<TableDto> freeTables = allTables.stream()
                .filter(t -> !occupiedTableIds.contains(t.getId()))
                .map(tableMapper::toDto)
                .collect(Collectors.toList());

        long totalReservations = reservationRepository.countByRestaurantId(restaurantId);
        long noShowCount = reservationRepository.countByRestaurantIdAndStatus(restaurantId, ReservationStatus.NO_SHOW);

        double noShowPercentage = totalReservations > 0
                ? (double) noShowCount / totalReservations * 100
                : 0.0;

        return new DashboardDto(
                todayReservationDtos,
                upcomingReservations,
                occupiedTables,
                freeTables,
                noShowPercentage,
                todayReservations.size()
        );
    }
}
