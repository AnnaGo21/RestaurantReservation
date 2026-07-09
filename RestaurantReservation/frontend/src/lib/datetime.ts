// Backend sends LocalDateTime as "YYYY-MM-DDTHH:mm:ss" in the restaurant's
// local time — slicing avoids browser timezone re-interpretation.
export function formatTime(isoDateTime: string): string {
  return isoDateTime.slice(11, 16)
}

export function todayIsoDate(): string {
  const now = new Date()
  return toIsoDate(now.getFullYear(), now.getMonth(), now.getDate())
}

export function shiftIsoDate(isoDate: string, days: number): string {
  const [year, month, day] = isoDate.split('-').map(Number)
  const shifted = new Date(year, month - 1, day + days)
  return toIsoDate(shifted.getFullYear(), shifted.getMonth(), shifted.getDate())
}

export function formatDateLabel(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString('en-GB', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

export function addMinutes(isoDateTime: string, minutes: number): string {
  const [datePart, timePart] = isoDateTime.split('T')
  const [year, month, day] = datePart.split('-').map(Number)
  const [hours, mins] = timePart.split(':').map(Number)
  const shifted = new Date(year, month - 1, day, hours, mins + minutes)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${shifted.getFullYear()}-${pad(shifted.getMonth() + 1)}-${pad(shifted.getDate())}T${pad(shifted.getHours())}:${pad(shifted.getMinutes())}:00`
}

function toIsoDate(year: number, monthIndex: number, day: number): string {
  const mm = String(monthIndex + 1).padStart(2, '0')
  const dd = String(day).padStart(2, '0')
  return `${year}-${mm}-${dd}`
}
