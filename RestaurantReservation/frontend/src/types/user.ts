export type Role = 'OWNER' | 'MANAGER' | 'STAFF'

export interface AppUser {
  id: number
  fullName: string
  email: string
  role: Role
  restaurantId: number
}
