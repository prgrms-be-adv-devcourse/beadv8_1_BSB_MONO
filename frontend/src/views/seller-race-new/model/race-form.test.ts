import { describe, expect, it } from 'vitest'

import { RACE_FORM_DEFAULTS, RACE_FORM_ERRORS, raceFormSchema, raceTimeSchema } from './race-form'

const firstMessage = (time: string) => {
  const result = raceTimeSchema.safeParse(time)
  return result.success ? null : result.error.issues[0].message
}

describe('raceTimeSchema 시간 칸 규칙', () => {
  it.each(['00:00', '08:15', '12:30', '18:45', '23:45'])('%s 는 받는다', (time) => {
    expect(firstMessage(time)).toBeNull()
  })

  it.each(['08:01', '08:03', '18:07', '10:13', '23:59'])(
    '%s 는 15분 단위가 아니라 막는다',
    (time) => {
      expect(firstMessage(time)).toBe(RACE_FORM_ERRORS.timeQuarter)
    },
  )

  it.each(['24:00', '25:30', '8:30', '18:3', '1830', '18:60', 'ab:cd'])(
    '%s 는 형식이 틀려 막는다',
    (time) => {
      expect(firstMessage(time)).toBe(RACE_FORM_ERRORS.timeFormat)
    },
  )

  it('비어 있으면 시간을 입력하라고 한다', () => {
    expect(firstMessage('')).toBe(RACE_FORM_ERRORS.timeRequired)
  })
})

describe('raceFormSchema 날짜 · 시간 칸', () => {
  const valid = {
    ...RACE_FORM_DEFAULTS,
    title: '2026 용감한 쿠키RUN in 서울',
    location: '서울숲 일대',
    regionSido: '서울특별시',
    regionSigungu: '성동구',
    raceDate: '2026-11-01 08:00',
    individualOpenAt: '2026-10-20 10:00',
    applicationCloseAt: '2026-10-31 18:00',
    refundDeadline: '2026-10-31 23:45',
  }
  const messagesAt = (patch: Partial<typeof valid>, field: keyof typeof valid) => {
    const result = raceFormSchema.safeParse({ ...valid, ...patch })
    return result.success
      ? []
      : result.error.issues.filter((i) => i.path[0] === field).map((i) => i.message)
  }

  it('15분 단위 시각이면 통과한다', () => {
    expect(raceFormSchema.safeParse(valid).success).toBe(true)
  })

  it('분이 15분 단위가 아니면 그 칸에 알린다', () => {
    expect(messagesAt({ raceDate: '2026-11-01 08:07' }, 'raceDate')).toEqual([
      RACE_FORM_ERRORS.timeQuarter,
    ])
    expect(messagesAt({ applicationCloseAt: '2026-10-31 18:01' }, 'applicationCloseAt')).toEqual([
      RACE_FORM_ERRORS.timeQuarter,
    ])
  })

  it('날짜를 안 골랐으면 날짜를, 시간을 안 쳤으면 시간을 알린다', () => {
    expect(messagesAt({ raceDate: ' 08:00' }, 'raceDate')).toEqual([RACE_FORM_ERRORS.dateRequired])
    expect(messagesAt({ raceDate: '' }, 'raceDate')).toEqual([RACE_FORM_ERRORS.dateRequired])
    expect(messagesAt({ raceDate: '2026-11-01 ' }, 'raceDate')).toEqual([
      RACE_FORM_ERRORS.timeRequired,
    ])
  })

  it('접수 시작 < 마감 같은 기존 순서 검사는 그대로다', () => {
    expect(messagesAt({ applicationCloseAt: '2026-10-20 09:45' }, 'applicationCloseAt')).toEqual([
      RACE_FORM_ERRORS.closeAfterOpen,
    ])
  })
})
