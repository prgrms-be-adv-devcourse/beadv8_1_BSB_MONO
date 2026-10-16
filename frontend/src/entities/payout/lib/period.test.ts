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

  // 백엔드: 끝 날 < 시작 날 + 1개월 (자바 plusMonths는 달 끝으로 잘림)
  const plusOneMonth = (iso: string) => {
    const [y, m, d] = iso.split('-').map(Number)
    const last = new Date(y, m + 1, 0).getDate()
    return new Date(y, m, Math.min(d, last))
  }
  const withinBackendRule = (p: { from: string; to: string }) => {
    const [y, m, d] = p.to.split('-').map(Number)
    return new Date(y, m - 1, d) < plusOneMonth(p.from)
  }

  it.each([
    ['4/30', new Date(2026, 3, 30), '2026-04-01'],
    ['10/31', new Date(2026, 9, 31), '2026-10-01'],
    ['2월 말', new Date(2026, 1, 28), '2026-02-01'],
    ['윤년 2월 말', new Date(2028, 1, 29), '2028-02-01'],
  ])('오늘이 %s이어도 모든 기간이 백엔드 규칙을 지킨다', (_name, today, firstFrom) => {
    const periods = detailPeriods(today, 4)
    expect(periods[0].from).toBe(firstFrom)
    for (const period of periods) {
      expect(withinBackendRule(period)).toBe(true)
    }
    // 기간끼리 빈틈 없이 이어진다
    for (let i = 0; i < periods.length - 1; i++) {
      const next = new Date(periods[i].from)
      next.setDate(next.getDate() - 1)
      expect(next.toISOString().slice(0, 10)).toBe(periods[i + 1].to)
    }
  })

  it('10/31이면 두 번째 기간도 9/1 ~ 9/30', () => {
    const periods = detailPeriods(new Date(2026, 9, 31), 2)
    expect(periods[1]).toMatchObject({ from: '2026-09-01', to: '2026-09-30' })
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
