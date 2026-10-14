import { format } from 'date-fns'
import { ko } from 'date-fns/locale'

import type { SelectItem } from '../field/Select'
import type { DateRange } from './selection'

// 시간 드롭다운: 30분 단위 + 하루 끝 마감 시각 23:59 (환불 기한·접수 마감처럼 "그날까지"를 뜻할 때)
export const TIME_ITEMS: SelectItem<string>[] = [
  ...Array.from({ length: 48 }, (_, i) => {
    const time = `${String(Math.floor(i / 2)).padStart(2, '0')}:${i % 2 ? '30' : '00'}`
    return { value: time, label: time }
  }),
  { value: '23:59', label: '23:59' },
]

/** 목록에 없는 시각(예: 10:15)이 기본값으로 들어오면 순서에 맞춰 끼워 넣어 그대로 보여 준다. */
export function timeItemsWith(...times: (string | null)[]): SelectItem<string>[] {
  const extra = times.filter(
    (t): t is string => t !== null && !TIME_ITEMS.some((item) => item.value === t),
  )
  if (extra.length === 0) {
    return TIME_ITEMS
  }
  return [...TIME_ITEMS, ...extra.map((t) => ({ value: t, label: t }))].sort((a, b) =>
    a.value.localeCompare(b.value),
  )
}

export function timeOf(date: Date | undefined): string | null {
  return date ? format(date, 'HH:mm') : null
}

/** 날짜(일)에 'HH:mm' 시각을 붙인다. 둘 중 하나라도 없으면 아직 다 고르지 않은 것이다. */
export function withTime(date: Date | undefined, time: string | null): Date | undefined {
  if (!date || !time) {
    return undefined
  }
  const [hours, minutes] = time.split(':').map(Number)
  const next = new Date(date)
  next.setHours(hours, minutes, 0, 0)
  return next
}

// 2026-11-01 (일)
export function singleDateText(date: Date): string {
  return format(date, 'yyyy-MM-dd (EEE)', { locale: ko })
}

// 10/14 (수) ~ 10/31 (토). 종료일을 아직 안 골랐으면 시작일만.
export function rangeDateText({ from, to }: DateRange): string {
  const text = (date: Date) => format(date, 'M/d (EEE)', { locale: ko })
  if (!from) {
    return ''
  }
  return to ? `${text(from)} ~ ${text(to)}` : `${text(from)} ~`
}
