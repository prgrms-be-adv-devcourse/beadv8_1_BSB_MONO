import { describe, expect, it } from 'vitest'

import {
  buildRaceRequest,
  earliestOpenAt,
  IDEMPOTENCY_KEY_MAX_LENGTH,
  idempotencyKeyOf,
  kstDate,
  opensBy,
  validateRaceRequest,
} from './build-request.mjs'
import { DEMO_RACES } from './races.mjs'

// 2026-10-20(화) 15:00 KST
const NOW = Date.parse('2026-10-20T15:00:00+09:00')
const fileIdsFor = (raceIndex: number, count: number) =>
  Array.from({ length: count }, (_, i) => raceIndex * 10 + i + 1)

const requests = DEMO_RACES.map((race, n) =>
  buildRaceRequest(race, { now: NOW, fileIds: fileIdsFor(n, race.courses.length) }),
)
const valid = () => buildRaceRequest(DEMO_RACES[0], { now: NOW, fileIds: [1, 2] })

describe('날짜 계산', () => {
  it('한국 날짜로 오늘을 본다 (UTC로는 전날인 새벽 시각)', () => {
    expect(kstDate(Date.parse('2026-10-20T00:30:00+09:00'))).toBe('2026-10-20')
    expect(kstDate(Date.parse('2026-10-20T23:59:00+09:00'))).toBe('2026-10-20')
  })

  it('접수 시작은 오늘로부터 이틀 뒤 00:00이다', () => {
    expect(earliestOpenAt(NOW)).toBe('2026-10-22T00:00:00')
    expect(earliestOpenAt(Date.parse('2026-10-30T23:00:00+09:00'))).toBe('2026-11-01T00:00:00')
  })

  it('10/20에 돌리면 발표 날(10/22)에 접수가 열려 있고, 10/21에 돌리면 아니다', () => {
    expect(opensBy(NOW, '2026-10-22')).toBe(true)
    expect(opensBy(Date.parse('2026-10-21T09:00:00+09:00'), '2026-10-22')).toBe(false)
  })
})

describe('데모 대회 요청 본문', () => {
  it('8개 대회가 모두 서버 규칙을 통과한다', () => {
    expect(requests).toHaveLength(8)
    for (const request of requests) {
      expect(validateRaceRequest(request, { now: NOW }), request.title).toEqual([])
    }
  })

  it('10/22에 접수 중이고, 접수 마감은 10/25~11/10, 대회는 11월이다', () => {
    for (const request of requests) {
      expect(request.individualOpenAt).toBe('2026-10-22T00:00:00')
      expect(request.applicationCloseAt >= '2026-10-25T00:00:00').toBe(true)
      expect(request.applicationCloseAt <= '2026-11-10T23:59:59').toBe(true)
      expect(request.raceDate.slice(0, 7)).toBe('2026-11')
      expect(request.raceDate > request.applicationCloseAt).toBe(true)
    }
  })

  it('접수 마감은 대회마다 다르다', () => {
    const closes = new Set(requests.map((request) => request.applicationCloseAt))
    expect(closes.size).toBe(requests.length)
  })

  it('하프 · 풀 거리는 소수 둘째 자리까지로 반올림한다', () => {
    const courses = requests.flatMap((request) => request.courses)
    expect(courses.find((course) => course.courseType === 'HALF')?.distanceKm).toBe(21.1)
    expect(courses.find((course) => course.courseType === 'FULL')?.distanceKm).toBe(42.2)
  })

  it('종목마다 대표 이미지 1장을 서로 다른 파일로 붙인다', () => {
    const jtbc = requests[3]
    expect(jtbc.courses.map((course) => course.images)).toEqual([
      [{ fileId: 31, fileType: 'THUMBNAIL', sortNo: 0 }],
      [{ fileId: 32, fileType: 'THUMBNAIL', sortNo: 0 }],
      [{ fileId: 33, fileType: 'THUMBNAIL', sortNo: 0 }],
    ])
  })

  it('크루 예매 없이 개인 예매만 연다', () => {
    for (const request of requests) {
      expect(request.crewSaleStartAt).toBeNull()
      expect(request.crewSaleEndAt).toBeNull()
      expect(request.crewMinCount).toBe(5)
      expect(request.courses.every((course) => course.crewQuota === 0)).toBe(true)
    }
  })

  it('Idempotency-Key는 대회마다 고정이고 서로 다르며 64자 이하다', () => {
    const keys = DEMO_RACES.map(idempotencyKeyOf)
    expect(keys[0]).toBe('seed-demo-cookierun-seoul')
    expect(new Set(keys).size).toBe(keys.length)
    expect(keys.every((key) => key.length <= IDEMPOTENCY_KEY_MAX_LENGTH)).toBe(true)
  })

  it('드라이런(파일 ID 없음)은 파일 검사만 건너뛴다', () => {
    const request = buildRaceRequest(DEMO_RACES[0], { now: NOW })
    expect(validateRaceRequest(request, { now: NOW, requireFiles: false })).toEqual([])
    expect(validateRaceRequest(request, { now: NOW }).length).toBeGreaterThan(0)
  })
})

describe('서버 규칙 검사', () => {
  it('필수 칸이 비면 잡는다', () => {
    expect(validateRaceRequest({ ...valid(), title: ' ' }, { now: NOW })).toContain(
      '대회 이름: 1~200자',
    )
    expect(validateRaceRequest({ ...valid(), courses: [] }, { now: NOW })).toContain(
      '종목: 1개 이상',
    )
  })

  it('길이 제한을 넘으면 잡는다', () => {
    expect(
      validateRaceRequest({ ...valid(), regionSido: '가'.repeat(21) }, { now: NOW }),
    ).toContain('시·도: 1~20자')
  })

  it('접수 시작이 오늘로부터 이틀 뒤보다 이르면 잡는다', () => {
    const request = { ...valid(), individualOpenAt: '2026-10-21T00:00:00' }
    expect(validateRaceRequest(request, { now: NOW })).toContain(
      '개인 예매 오픈: 오늘로부터 이틀 뒤(2026-10-22)부터',
    )
  })

  it('날짜 순서가 틀리면 잡는다', () => {
    const closeBeforeOpen = { ...valid(), applicationCloseAt: '2026-10-22T00:00:00' }
    expect(validateRaceRequest(closeBeforeOpen, { now: NOW })).toContain(
      '접수 마감: 개인 예매 오픈보다 늦게',
    )
    const closeAfterRace = { ...valid(), applicationCloseAt: '2026-11-01T09:00:00' }
    expect(validateRaceRequest(closeAfterRace, { now: NOW })).toContain(
      '접수 마감: 대회 일시보다 늦을 수 없음',
    )
    const lateRefund = { ...valid(), refundDeadline: '2026-11-02' }
    expect(validateRaceRequest(lateRefund, { now: NOW })).toContain(
      '환불 기준일: 대회 날짜보다 늦을 수 없음',
    )
  })

  it('15분 단위가 아닌 시각을 잡는다', () => {
    const request = { ...valid(), raceDate: '2026-11-01T08:10:00' }
    expect(validateRaceRequest(request, { now: NOW })).toContain(
      'raceDate: 15분 단위(00·15·30·45분)',
    )
  })

  it('종목 값의 범위를 잡는다', () => {
    const base = valid()
    const course = (patch: Record<string, unknown>) => ({
      ...base,
      courses: [{ ...base.courses[0], ...patch }],
    })
    expect(validateRaceRequest(course({ distanceKm: 21.0975 }), { now: NOW })).toContain(
      '종목 10K: 거리 0 초과, 소수 둘째 자리까지',
    )
    expect(validateRaceRequest(course({ minAge: 13 }), { now: NOW })).toContain(
      '종목 10K: 최소 나이 14세 이상',
    )
    expect(validateRaceRequest(course({ price: 0 }), { now: NOW })).toContain(
      '종목 10K: 참가비 0원 초과',
    )
    expect(validateRaceRequest(course({ kitPrice: 50000 }), { now: NOW })).toContain(
      '종목 10K: 키트 금액은 참가비 이하',
    )
    expect(validateRaceRequest(course({ courseType: 'KM_21' }), { now: NOW })).toContain(
      '종목 10K: 종목 구분은 KM_5·KM_10·HALF·FULL·ULTRA',
    )
    expect(validateRaceRequest(course({ crewQuota: 10 }), { now: NOW })).toContain(
      '종목 10K: 크루 예매가 없으면 크루 정원 0',
    )
  })

  it('두 종목에 같은 파일을 붙이면 잡는다', () => {
    const request = buildRaceRequest(DEMO_RACES[0], { now: NOW, fileIds: [7, 7] })
    expect(validateRaceRequest(request, { now: NOW })).toContain(
      '종목 5K: 같은 파일(7)을 두 번 연결할 수 없음',
    )
  })
})
