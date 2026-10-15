import { differenceInCalendarDays, format, parseISO } from 'date-fns'
import { ko } from 'date-fns/locale'

// 백엔드 일시는 시간대 없는 한국 시간(LocalDateTime, 예: 2026-11-01T08:00:00)이다.
// parseISO는 시간대 없는 값을 실행 환경의 현지 시각으로 읽으므로, 벽시계 글자 그대로 보여 주는 데만 쓴다.

/** 2026-11-01T08:00:00 → 11/1 (일) 08:00 */
export function formatRaceDateTime(value: string): string {
  return format(parseISO(value), 'M/d (EEE) HH:mm', { locale: ko })
}

/** 2026-11-01(T…) → 11/1 (일) */
export function formatRaceDate(value: string): string {
  return format(parseISO(value.slice(0, 10)), 'M/d (EEE)', { locale: ko })
}

/** 오늘(한국 날짜)부터 그 날까지 남은 날. 서버(UTC)에서 그려도 한국 날짜로 센다. */
export function daysUntil(value: string, now: Date = new Date()): number {
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(now)
  return differenceInCalendarDays(parseISO(value.slice(0, 10)), parseISO(today))
}

/** 남은 날 → D-12, D-DAY. 지났으면 마감 */
export function formatDDay(value: string, now?: Date): string {
  const days = daysUntil(value, now)
  if (days < 0) return '마감'
  return days === 0 ? 'D-DAY' : `D-${days}`
}

// 판매자 입력 칸은 "2026-11-01 08:00"처럼 쓰고, 백엔드에는 시간대 없는 한국 시간으로 보낸다.
const DATE_TIME_INPUT = /^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})$/
const DATE_INPUT = /^(\d{4})-(\d{2})-(\d{2})$/

function isRealDate(year: string, month: string, day: string): boolean {
  const date = new Date(Number(year), Number(month) - 1, Number(day))
  return (
    date.getFullYear() === Number(year) &&
    date.getMonth() === Number(month) - 1 &&
    date.getDate() === Number(day)
  )
}

/** 입력 "2026-11-01 08:00" → "2026-11-01T08:00:00". 형식이 틀리거나 없는 날이면 null */
export function parseRaceDateTimeInput(text: string): string | null {
  const match = DATE_TIME_INPUT.exec(text.trim())
  if (!match) return null
  const [, year, month, day, hour, minute] = match
  if (!isRealDate(year, month, day) || Number(hour) > 23 || Number(minute) > 59) return null
  return `${year}-${month}-${day}T${hour}:${minute}:00`
}

/** 입력 "2026-10-31" → "2026-10-31". 형식이 틀리거나 없는 날이면 null */
export function parseRaceDateInput(text: string): string | null {
  const match = DATE_INPUT.exec(text.trim())
  if (!match) return null
  const [, year, month, day] = match
  return isRealDate(year, month, day) ? `${year}-${month}-${day}` : null
}

/** "2026-11-01T08:00:00" → 입력 칸 값 "2026-11-01 08:00" */
export function toRaceDateTimeInput(value: string): string {
  return `${value.slice(0, 10)} ${value.slice(11, 16)}`
}
