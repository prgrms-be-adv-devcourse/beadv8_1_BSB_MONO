import { describe, expect, it } from 'vitest'

import { ageOn, isJoinableAge, latestJoinableBirth, parseBirth } from './birth'

describe('ageOn', () => {
  it('생일이 지나지 않았으면 한 살 적게 센다', () => {
    const birth = parseBirth('2012-06-15')!
    expect(ageOn(birth, new Date(2026, 5, 14))).toBe(13)
    expect(ageOn(birth, new Date(2026, 5, 15))).toBe(14)
  })
})

describe('isJoinableAge', () => {
  const today = new Date(2026, 5, 15)

  it('생일 당일부터 만 14세로 본다', () => {
    expect(isJoinableAge(parseBirth('2012-06-15')!, today)).toBe(true)
    expect(isJoinableAge(parseBirth('2012-06-16')!, today)).toBe(false)
  })

  it('미래 날짜는 막는다', () => {
    expect(isJoinableAge(parseBirth('2030-01-01')!, today)).toBe(false)
  })
})

describe('parseBirth', () => {
  it('날짜 선택기 값(YYYY-MM-DD)만 받는다', () => {
    expect(parseBirth('1995-03-02')).toEqual(new Date(1995, 2, 2))
    expect(parseBirth('1995.03.02')).toBeNull()
    expect(parseBirth('1995-02-30')).toBeNull()
    expect(parseBirth('')).toBeNull()
  })
})

describe('latestJoinableBirth', () => {
  it('오늘에서 14년 전 같은 날을 돌려준다', () => {
    expect(latestJoinableBirth(new Date(2026, 9, 20))).toBe('2012-10-20')
  })

  it('윤일에는 14년 전 2월 28일로 맞춘다', () => {
    const today = new Date(2028, 1, 29)
    expect(latestJoinableBirth(today)).toBe('2014-02-28')
    expect(isJoinableAge(parseBirth('2014-02-28')!, today)).toBe(true)
    expect(isJoinableAge(parseBirth('2014-03-01')!, today)).toBe(false)
  })
})
