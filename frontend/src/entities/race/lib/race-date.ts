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
