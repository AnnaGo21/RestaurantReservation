import type { Reservation } from './reservation'
import type { RestaurantTable } from './table'

export interface DashboardData {
  todayReservations: Reservation[]
  upcomingReservations: Reservation[]
  occupiedTables: RestaurantTable[]
  freeTables: RestaurantTable[]
  noShowPercentage: number
  totalReservationsToday: number
}
