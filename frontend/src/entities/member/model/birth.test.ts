import { describe, expect, it } from 'vitest'

import { ageOn, isJoinableAge, parseBirth } from './birth'

describe('ageOn', () => {
  it('생일이 지나지 않았으면 한 살 적게 센다', () => {
    const birth = parseBirth('2012.06.15')!
    expect(ageOn(birth, new Date(2026, 5, 14))).toBe(13)
    expect(ageOn(birth, new Date(2026, 5, 15))).toBe(14)
  })
})

describe('isJoinableAge', () => {
  const today = new Date(2026, 5, 15)

  it('생일 당일부터 만 14세로 본다', () => {
    expect(isJoinableAge(parseBirth('2012.06.15')!, today)).toBe(true)
    expect(isJoinableAge(parseBirth('2012.06.16')!, today)).toBe(false)
  })

  it('미래 날짜는 막는다', () => {
    expect(isJoinableAge(parseBirth('2030.01.01')!, today)).toBe(false)
  })
})
