package org.example.reservations.analytics;

import lombok.RequiredArgsConstructor;
import org.example.reservations.reservation.Reservation;
import org.example.reservations.reservation.ReservationRepository;
import org.example.reservations.reservation.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Long restaurantId, LocalDateTime start, LocalDateTime end) {
        List<Reservation> reservations = reservationRepository.findByRestaurantIdAndStartTimeBetween(
                restaurantId,
                start,
                end
        );

        // Peak Hours - Most popular reservation hours
        Map<Integer, Long> peakHours = reservations.stream()
                .collect(Collectors.groupingBy(
                        reservation -> reservation.getStartTime().getHour(),
                        Collectors.counting()
                ));

        // Busiest Days - Traffic by weekday
        Map<String, Long> busiestDays = reservations.stream()
                .collect(Collectors.groupingBy(
                        reservation -> reservation.getStartTime().getDayOfWeek().name(),
                        Collectors.counting()
                ));

        // Average Party Size
        double averagePartySize = reservations.stream()
                .mapToInt(Reservation::getPartySize)
                .average()
                .orElse(0.0);

        // Total Reservations
        long totalReservations = reservations.size();

        // Status Counts
        long completedReservations = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                .count();

        long cancelledReservations = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CANCELLED)
                .count();

        long noShows = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.NO_SHOW)
                .count();

        // No-show Percentage
        double noShowPercentage = totalReservations > 0
                ? (double) noShows / totalReservations * 100
                : 0.0;

        // Daily Trends - Reservations per day
        Map<LocalDate, Long> dailyTrends = reservations.stream()
                .collect(Collectors.groupingBy(
                        reservation -> reservation.getStartTime().toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()
                ));

        // Weekly Trends - Reservations per week
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        Map<String, Long> weeklyTrends = reservations.stream()
                .collect(Collectors.groupingBy(
                        reservation -> {
                            LocalDateTime dt = reservation.getStartTime();
                            int year = dt.getYear();
                            int week = dt.get(weekFields.weekOfWeekBasedYear());
                            return String.format("%d-W%02d", year, week);
                        },
                        TreeMap::new,
                        Collectors.counting()
                ));

        // Monthly Trends - Reservations per month
        Map<String, Long> monthlyTrends = reservations.stream()
                .collect(Collectors.groupingBy(
                        reservation -> reservation.getStartTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        TreeMap::new,
                        Collectors.counting()
                ));

        return new AnalyticsResponse(
                peakHours,
                busiestDays,
                averagePartySize,
                totalReservations,
                completedReservations,
                cancelledReservations,
                noShows,
                noShowPercentage,
                dailyTrends,
                weeklyTrends,
                monthlyTrends
        );
    }
}
