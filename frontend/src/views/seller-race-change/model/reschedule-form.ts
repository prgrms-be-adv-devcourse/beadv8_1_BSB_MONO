import { addDays, format } from 'date-fns'
import { z } from 'zod'

import { formatRaceDate, parseRaceDateTimeInput } from '@/entities/race'

export const RESCHEDULE_ERRORS = {
  dateTimeFormat: '2026-12-13 08:00처럼 입력해 주세요.',
  sameDate: '지금 대회 일시와 같아요.',
} as const

/** 일정이 바뀐 날부터 전액 환불을 허용하는 기간 (백엔드 scheduleChangedAt 기준 7일) */
export const SCHEDULE_CHANGE_REFUND_DAYS = 7

export function rescheduleSchema(currentRaceDate: string) {
  return z.object({
    raceDate: z
      .string()
      .refine((value) => parseRaceDateTimeInput(value) !== null, RESCHEDULE_ERRORS.dateTimeFormat)
      .refine(
        (value) => parseRaceDateTimeInput(value) !== currentRaceDate,
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
