import { describe, expect, it } from 'vitest'

import { fullRefundLastDay, scheduleChangeRefundLastDay } from './refund-period'

describe('refund-period', () => {
  it('일정을 알린 날을 1일째로 7일째 되는 날까지 전액 환불', () => {
    expect(scheduleChangeRefundLastDay('2026-10-13T14:00:00')).toBe('2026-10-19')
    expect(scheduleChangeRefundLastDay(null)).toBeUndefined()
  })

  it('환불 기한과 일정 변경 환불 기간 중 늦은 날', () => {
    expect(fullRefundLastDay('2026-10-31', null)).toBe('2026-10-31')
    expect(fullRefundLastDay('2026-07-15', '2026-07-19T10:00:00')).toBe('2026-07-25')
    expect(fullRefundLastDay('2026-10-31', '2026-10-01T10:00:00')).toBe('2026-10-31')
  })
})
