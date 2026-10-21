import { addDays, format } from 'date-fns'
import { z } from 'zod'

import { formatRaceDate, parseRaceDateInput } from '@/entities/race'

export const RESCHEDULE_ERRORS = {
  dateFormat: '2026-12-13처럼 입력해 주세요.',
  sameDate: '지금 대회일과 같아요.',
} as const

/** 일정이 바뀐 날부터 전액 환불을 허용하는 기간 (백엔드 scheduleChangedAt 기준 7일) */
export const SCHEDULE_CHANGE_REFUND_DAYS = 7

/**
 * 새 대회일 칸('yyyy-MM-dd') → 서버에 보낼 일시. 날짜만 바꾸고 시각은 지금 대회 일시의 것을 그대로 둔다.
 * 형식이 틀리면 null
 */
export function toRescheduledRaceDate(text: string, currentRaceDate: string): string | null {
  const day = parseRaceDateInput(text)
  return day ? `${day}${currentRaceDate.slice(10)}` : null
}

export function rescheduleSchema(currentRaceDate: string) {
  return z.object({
    raceDate: z
      .string()
      .refine((value) => parseRaceDateInput(value) !== null, RESCHEDULE_ERRORS.dateFormat)
      .refine(
        (value) => parseRaceDateInput(value) !== currentRaceDate.slice(0, 10),
        RESCHEDULE_ERRORS.sameDate,
      ),
  })
}

export type RescheduleValues = { raceDate: string }

/** 오늘 알리면 전액 환불을 받을 수 있는 마지막 날 (M/d) */
export function refundUntil(now: Date = new Date()): string {
  return format(addDays(now, SCHEDULE_CHANGE_REFUND_DAYS), 'M/d')
}

/** 참가자에게 보낼 알림 문구 미리보기 */
export function rescheduleNotice(
  title: string,
  currentRaceDate: string,
  nextRaceDate: string | null,
  now?: Date,
): string {
  const next = nextRaceDate ? formatRaceDate(nextRaceDate) : '새 대회일'
  return (
    `[${title}] 대회일이 ${formatRaceDate(currentRaceDate)}에서 ${next}로 바뀌었어요.\n` +
    `오늘부터 ${SCHEDULE_CHANGE_REFUND_DAYS}일 동안(${refundUntil(now)}까지)은 전액 환불받을 수 있어요.`
  )
}
