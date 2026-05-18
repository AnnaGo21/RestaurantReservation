package org.example.reservations.analytics;

import java.time.LocalDate;
import java.util.Map;

public record AnalyticsResponse(
        Map<Integer, Long> peakHours,
        Map<String, Long> busiestDays,
        Double averagePartySize,
        long totalReservations,
        long completedReservations,
        long cancelledReservations,
        long noShows,
        double noShowPercentage,
        Map<LocalDate, Long> dailyTrends,
        Map<String, Long> weeklyTrends,
        Map<String, Long> monthlyTrends
) {
}
