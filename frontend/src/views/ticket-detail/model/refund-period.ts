import { addDays, format, parseISO } from 'date-fns'

/** 일정이 바뀐 뒤 전액 환불해 주는 날 수 (정책 5장 4-4 "알린 날부터 7일 동안", 알린 날이 1일째) */
export const SCHEDULE_CHANGE_REFUND_DAYS = 7

/** 일정 변경 뒤 전액 환불 기간의 마지막 날 (yyyy-MM-dd, 그날 23:59까지). 일정이 바뀐 적 없으면 undefined */
export function scheduleChangeRefundLastDay(scheduleChangedAt?: string | null): string | undefined {
  if (!scheduleChangedAt) return undefined
  const changedOn = parseISO(scheduleChangedAt.slice(0, 10))
  return format(addDays(changedOn, SCHEDULE_CHANGE_REFUND_DAYS - 1), 'yyyy-MM-dd')
}

/**
 * 전액 환불로 취소할 수 있는 마지막 날 (yyyy-MM-dd). 주문 서비스의 환불 판정과 같다:
 * 환불 기한까지이거나, 일정이 바뀐 뒤 7일 안이면 기한과 상관없이 전액 환불.
 */
export function fullRefundLastDay(
  refundDeadline?: string,
  scheduleChangedAt?: string | null,
): string | undefined {
  const days = [refundDeadline?.slice(0, 10), scheduleChangeRefundLastDay(scheduleChangedAt)]
  return days
    .filter((d): d is string => !!d)
    .sort()
    .at(-1)
}
