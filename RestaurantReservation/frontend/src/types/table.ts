export type TableStatus = 'AVAILABLE' | 'RESERVED' | 'OUT_OF_SERVICE'

export interface RestaurantTable {
  id: number
  label: string
  capacity: number
  status: TableStatus
  positionX: number | null
  positionY: number | null
  restaurantId: number
}

export interface CreateTableRequest {
  label: string
  capacity: number
  status: TableStatus
  positionX: number | null
  positionY: number | null
}
