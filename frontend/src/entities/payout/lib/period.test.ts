import { describe, expect, it } from 'vitest'

import { detailPeriods, formatMonthDay, formatWeekday, recentPayoutMonths } from './period'

describe('detailPeriods', () => {
  it('오늘로 끝나는 1개월 기간부터 한 달씩 앞으로 만든다', () => {
    expect(detailPeriods(new Date(2026, 9, 15), 2)).toEqual([
      {
        value: '2026-09-16~2026-10-15',
        label: '9/16 ~ 10/15',
        from: '2026-09-16',
        to: '2026-10-15',
      },
      {
        value: '2026-08-16~2026-09-15',
        label: '8/16 ~ 9/15',
        from: '2026-08-16',
        to: '2026-09-15',
      },
    ])
  })

  it('한 달 전에 같은 날이 없으면 달 끝 다음 날부터 (백엔드 1개월 한도 안)', () => {
    const [period] = detailPeriods(new Date(2026, 2, 31), 1)
    expect(period).toMatchObject({ from: '2026-03-01', to: '2026-03-31' })
  })

  it('연도를 넘어간다', () => {
    const [period] = detailPeriods(new Date(2026, 0, 10), 1)
    expect(period).toMatchObject({ from: '2025-12-11', to: '2026-01-10' })
  })
})

describe('recentPayoutMonths', () => {
  it('이번 달까지 12개월', () => {
    expect(recentPayoutMonths(new Date(2026, 9, 15))).toEqual({
      fromMonth: '2025-11',
      toMonth: '2026-10',
    })
  })
})

describe('날짜 표시', () => {
  it('월/일과 요일', () => {
    expect(formatMonthDay('2026-11-20')).toBe('11/20')
    expect(formatMonthDay('2026-10-12T10:00:00')).toBe('10/12')
    expect(formatWeekday('2026-11-20')).toBe('금')
  })
})
