import { format } from 'date-fns'
import { ko } from 'date-fns/locale'

import type { DateRange } from './selection'

/**
 * 시간 칸에 치는 글자를 HH:mm 모양으로 다듬는다. 숫자만 쳐도 두 자리 뒤에 콜론을 넣는다(1830 → 18:30).
 * 맞는 시각인지는 여기서 보지 않는다(폼의 zod 규칙이 본다).
 */
export function formatTimeTyping(raw: string): string {
  if (raw.includes(':')) {
    const [hours, minutes = ''] = raw.split(':')
    return `${hours.replace(/\D/g, '').slice(0, 2)}:${minutes.replace(/\D/g, '').slice(0, 2)}`
  }
  const digits = raw.replace(/\D/g, '').slice(0, 4)
  return digits.length > 2 ? `${digits.slice(0, 2)}:${digits.slice(2)}` : digits
}

/** 칸을 떠날 때 한 자리 시를 두 자리로 채운다(9:30 → 09:30). 그 밖의 글자는 그대로 둔다. */
export function padTime(text: string): string {
  const match = /^(\d):(\d{2})$/.exec(text)
  return match ? `0${match[1]}:${match[2]}` : text
}

/** 폼에 넘기는 글자 'yyyy-MM-dd HH:mm'. 날짜나 시간이 비면 그 자리가 빈 채로 간다(예: ' 18:30'). */
export function dateTimeText(date: Date | undefined, time: string): string {
  const day = date ? format(date, 'yyyy-MM-dd') : ''
  return day || time ? `${day} ${time}` : ''
}

export function timeOf(date: Date | undefined): string {
  return date ? format(date, 'HH:mm') : ''
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
