package org.example.reservations.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.reservations.reservation.ReservationDto;
import org.example.reservations.table.TableDto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {
    private List<ReservationDto> todayReservations;
    private List<ReservationDto> upcomingReservations;
    private List<TableDto> occupiedTables;
    private List<TableDto> freeTables;
    private Double noShowPercentage;
    private Integer totalReservationsToday;
}
