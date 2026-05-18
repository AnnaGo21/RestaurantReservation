package org.example.reservations.dashboard;

import lombok.RequiredArgsConstructor;
import org.example.reservations.reservation.Reservation;
import org.example.reservations.reservation.ReservationDto;
import org.example.reservations.reservation.ReservationRepository;
import org.example.reservations.reservation.ReservationStatus;
import org.example.reservations.table.RestaurantTable;
import org.example.reservations.table.RestaurantTableRepository;
import org.example.reservations.table.TableDto;
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

    @Transactional(readOnly = true)
    public DashboardDto getDashboardData(Long restaurantId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> todayReservations = reservationRepository
                .findByRestaurantIdAndStartTimeBetween(restaurantId, todayStart, todayEnd);

        List<ReservationDto> todayReservationDtos = todayReservations.stream()
                .map(this::toReservationDto)
                .collect(Collectors.toList());

        List<ReservationDto> upcomingReservations = todayReservations.stream()
                .filter(r -> r.getStartTime().isAfter(now))
                .map(this::toReservationDto)
                .collect(Collectors.toList());

        List<Long> occupiedTableIds = todayReservations.stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED
                        && r.getStatus() != ReservationStatus.COMPLETED
                        && r.getStatus() != ReservationStatus.NO_SHOW)
                .filter(r -> r.getStartTime().isBefore(now) && r.getEndTime().isAfter(now))
                .map(r -> r.getRestaurantTable().getId())
                .collect(Collectors.toList());

        List<RestaurantTable> allTables = tableRepository.findByRestaurantId(restaurantId);

        List<TableDto> occupiedTables = allTables.stream()
                .filter(t -> occupiedTableIds.contains(t.getId()))
                .map(this::toTableDto)
                .collect(Collectors.toList());

        List<TableDto> freeTables = allTables.stream()
                .filter(t -> !occupiedTableIds.contains(t.getId()))
                .map(this::toTableDto)
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

    private ReservationDto toReservationDto(Reservation reservation) {
        return new ReservationDto(
                reservation.getId(),
                reservation.getRestaurant().getId(),
                reservation.getRestaurantTable().getId(),
                reservation.getGuest().getId(),
                reservation.getGuest().getFullName(),
                reservation.getGuest().getPhone(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPartySize(),
                reservation.getNotes(),
                reservation.getStatus(),
                reservation.getReminderSent(),
                reservation.getCheckedInAt()
        );
    }

    private TableDto toTableDto(RestaurantTable table) {
        return new TableDto(
                table.getId(),
                table.getLabel(),
                table.getCapacity(),
                table.getStatus(),
                table.getPositionX(),
                table.getPositionY(),
                table.getRestaurant().getId()
        );
    }
}
