// Mirrors AnalyticsResponse (org.example.reservations.analytics.AnalyticsResponse).
// Backend Map<Integer,Long> / Map<LocalDate,Long> serialise as JSON objects
// whose keys are always strings — reflect that here.
export interface AnalyticsResponse {
  peakHours: Record<string, number>
  busiestDays: Record<string, number>
  averagePartySize: number
  totalReservations: number
  completedReservations: number
  cancelledReservations: number
  noShows: number
  noShowPercentage: number
  dailyTrends: Record<string, number>
  weeklyTrends: Record<string, number>
  monthlyTrends: Record<string, number>
}
