import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { customFetch } from '@/shared/api'

import { MOCK_LOGGED_IN_COOKIE } from './mock-session'

type RaceRow = {
  raceId: number
  title: string
  minPrice: number
  raceDate: string
  applicationCloseAt: string
  saleStatus: string
  regionSido: string
}
type RacePage = {
  data: { races: RaceRow[]; page: number; size: number; totalElements: number; totalPages: number }
}
const searchRaces = (query = '') =>
  customFetch<RacePage>(`http://localhost/api/v1/product/races${query}`)
const idsOf = (body: RacePage) => body.data.races.map((race) => race.raceId)

// 생성된 목이 등록됐는지 확인한다 (API_MOCKING=on 개발 서버도 같은 handlers를 쓴다)
describe('생성된 MSW 핸들러', () => {
  it('/races/suggestions가 /races/:raceId 핸들러에 가로채이지 않는다', async () => {
    const body = await customFetch<{ message: string }>(
      'http://localhost/api/v1/product/races/suggestions?keyword=서울',
    )
    expect(body.message).toBe('연관검색어를 조회했습니다.')
  })

  it('연관검색어는 검색어가 이름에 든 대회만 돌려준다', async () => {
    const body = await customFetch<{ data: { races: { title: string }[] } }>(
      'http://localhost/api/v1/product/races/suggestions?keyword=제주',
    )
    expect(body.data.races.map((race) => race.title)).toEqual(['제주감귤국제마라톤'])
  })
})

describe('대회 목록 목', () => {
  // 대회 날짜가 "지금"에서 며칠 뒤라 시계를 세워 둔다 (10/16 낮 12시)
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'], now: new Date('2026-10-16T12:00:00+09:00') })
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('대회 8개를 마감 임박순으로 돌려준다 (마감된 대회는 맨 뒤)', async () => {
    const body = await searchRaces()

    expect(body.data.totalElements).toBe(8)
    expect(idsOf(body)).toEqual([4, 2, 5, 1, 3, 7, 6, 10])
  })

  it('접수 중 · 마감 임박 · 마감 · 오픈 예정이 섞여 있다', async () => {
    const { data } = await searchRaces()
    const now = Date.now()
    const closeMs = (race: RaceRow) => new Date(`${race.applicationCloseAt}+09:00`).getTime()
    const closingSoon = data.races.filter(
      (r) => closeMs(r) > now && closeMs(r) - now < 3 * 86_400_000,
    )
    const closed = data.races.filter((r) => closeMs(r) <= now)

    expect(closingSoon.map((r) => r.raceId)).toEqual([4, 2])
    expect(closed.map((r) => r.raceId)).toEqual([10])
    expect(data.races.filter((r) => r.saleStatus === 'SCHEDULED').map((r) => r.raceId)).toEqual([7])
  })

  it('RACE_DATE는 대회일이 빠른 순이다', async () => {
    const body = await searchRaces('?sort=RACE_DATE')

    expect(idsOf(body)).toEqual([10, 2, 1, 5, 3, 7, 4, 6])
  })

  it('LATEST는 최근에 등록한 대회부터 돌려준다', async () => {
    const body = await searchRaces('?sort=LATEST')

    expect(idsOf(body)).toEqual([7, 6, 5, 3, 1, 2, 4, 10])
  })

  it('PRICE는 최저 참가비가 낮은 순이다', async () => {
    const prices = (await searchRaces('?sort=PRICE')).data.races.map((race) => race.minPrice)

    expect(prices).toEqual([...prices].sort((a, b) => a - b))
    expect(prices[0]).toBe(30000)
  })

  it('keyword는 이름이나 지역에 든 대회만 남긴다', async () => {
    expect(idsOf(await searchRaces(`?keyword=${encodeURIComponent('쿠키')}`))).toEqual([1])
    const busan = await searchRaces(`?keyword=${encodeURIComponent('부산')}`)
    expect(idsOf(busan)).toEqual([5])
    expect(busan.data.totalElements).toBe(1)
    expect(idsOf(await searchRaces(`?keyword=${encodeURIComponent('없는대회')}`))).toEqual([])
  })

  it('page · size로 자르고 totalElements · totalPages를 계산한다', async () => {
    const first = await searchRaces('?size=3&page=0')
    const last = await searchRaces('?size=3&page=2')

    expect(idsOf(first)).toEqual([4, 2, 5])
    expect(first.data).toMatchObject({ page: 0, size: 3, totalElements: 8, totalPages: 3 })
    expect(idsOf(last)).toEqual([6, 10])
    expect((await searchRaces('?size=3&page=5')).data.races).toEqual([])
  })

  it('오픈 예정 대회는 오픈 시각을 앞당기지 않는다', async () => {
    const { data } = await searchRaces()
    const upcoming = data.races.find((race) => race.raceId === 7) as RaceRow & {
      individualOpenAt: string
    }

    expect(new Date(`${upcoming.individualOpenAt}+09:00`).getTime()).toBeGreaterThan(Date.now())
  })
})

describe('대회 상세 목', () => {
  it('목록의 모든 대회가 상세로 열리고 종목이 있다', async () => {
    for (const raceId of [1, 2, 3, 4, 5, 6, 7, 10]) {
      const body = await customFetch<{
        data: { raceId: number; courses: { raceCourseId: number }[] }
      }>(`http://localhost/api/v1/product/races/${raceId}`)

      expect(body.data.raceId).toBe(raceId)
      expect(body.data.courses.length).toBeGreaterThan(0)
    }
  })

  it('없는 대회는 404, 숫자가 아니면 400이다', async () => {
    const missing = await fetch('http://localhost/api/v1/product/races/999')
    const invalid = await fetch('http://localhost/api/v1/product/races/abc')

    expect(missing.status).toBe(404)
    expect(invalid.status).toBe(400)
  })

  it('종목 이미지는 대회마다 다른 파일이다', async () => {
    const urlOf = async (courseId: number) =>
      (
        await customFetch<{ data: { url: string }[] }>(
          `http://localhost/api/v1/file/files?refType=RaceCourse&refId=${courseId}`,
        )
      ).data[0]?.url

    expect(await urlOf(1)).toBe('/mock/races/cookierun.webp')
    expect(await urlOf(11)).toBe('/mock/races/youth10k.webp')
  })
})

// 가짜 응답 모드에서 로그인 상태를 진짜처럼(Refresh Token 쿠키로) 흉내 내는지 확인한다.
// Node의 MSW는 응답 쿠키를 저장하지 않으므로, 브라우저처럼 받은 쿠키를 다음 요청에 직접 붙인다.
describe('인증 목', () => {
  const post = (path: string, cookie?: string) =>
    fetch(`http://localhost/api/v1/auth/${path}`, {
      method: 'POST',
      headers: cookie ? { Cookie: cookie } : {},
    })
  /** Set-Cookie에서 "이름=값"만 꺼낸다 (브라우저가 다음 요청에 붙이는 모양) */
  const cookieOf = (response: Response) => response.headers.get('set-cookie')?.split(';')[0] ?? ''
  const accessTokenOf = async (response: Response) =>
    ((await response.json()) as { data: { accessToken: string } }).data.accessToken

  it('쿠키 없이 재발급하면 401이다', async () => {
    expect((await post('reissue')).status).toBe(401)
  })

  it('로그인하면 진짜처럼 httpOnly Refresh Token 쿠키를 심는다', async () => {
    const login = await fetch('http://localhost/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'runner1@crewrun.com', password: 'password1' }),
    })

    const setCookie = login.headers.get('set-cookie') ?? ''
    expect(setCookie).toMatch(/^mockRefreshToken=mock-refresh-/)
    expect(setCookie).toContain('Path=/api/v1/auth')
    expect(setCookie).toContain('HttpOnly')
    expect(setCookie).toContain('SameSite=Lax')
  })

  it('로그인 쿠키로 재발급하면 매번 새 Access Token과 새 쿠키를 준다', async () => {
    const login = await fetch('http://localhost/api/v1/auth/login', { method: 'POST' })

    const first = await post('reissue', cookieOf(login))
    const second = await post('reissue', cookieOf(first))

    expect(first.status).toBe(200)
    expect(second.status).toBe(200)
    expect(cookieOf(first)).not.toBe(cookieOf(login))
    expect(await accessTokenOf(first)).not.toBe(await accessTokenOf(second))
  })

  it('MOCK_LOGGED_IN_COOKIE를 붙이면 로그인된 채로 재발급할 수 있다', async () => {
    expect((await post('reissue', MOCK_LOGGED_IN_COOKIE)).status).toBe(200)
  })

  it('로그아웃하면 Max-Age=0으로 쿠키를 지운다', async () => {
    const logout = await post('logout', MOCK_LOGGED_IN_COOKIE)

    expect(logout.headers.get('set-cookie')).toMatch(
      /^mockRefreshToken=; Max-Age=0;.*Path=\/api\/v1\/auth/,
    )
  })
})

// 가짜 응답 모드에서 차례 기다리기(M05-Q1) → 내 차례(M05-Q2)를 볼 수 있게 대기열 순서를 흉내 내는지 확인한다.
describe('대기열 입장 목', () => {
  const ask = async () =>
    (
      await customFetch<{ data: { admitted: boolean; remainingSeconds: number } }>(
        'http://localhost/api/v1/order/queue/admission?raceId=1&raceCourseId=1',
      )
    ).data

  it('두 번은 기다리는 중으로 답하고, 세 번째에 입장해 10분 남았다고 답한다', async () => {
    expect((await ask()).admitted).toBe(false)
    expect((await ask()).admitted).toBe(false)
    const admitted = await ask()
    expect(admitted.admitted).toBe(true)
    expect(admitted.remainingSeconds).toBeGreaterThan(590)
    expect(admitted.remainingSeconds).toBeLessThanOrEqual(600)
  })
})

// 대기열 줄서기 목: 등록 → 순번이 줄어듦 → 차례 → 입장 확인 흐름이 한 번에 돌아가는지 확인한다.
describe('대기열 줄서기 목', () => {
  type Status = { data: { myRank: number; ahead: number; canEnter: boolean } }
  const base = 'http://localhost/api/v1/queue'
  const register = () =>
    customFetch<Status>(base, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ raceId: 1, raceCourseId: 1 }),
    })
  const poll = () => customFetch<Status>(`${base}/races/1/racecourses/1`)

  it('등록하면 순번을 주고, 물을 때마다 줄다가 차례가 오면 바로 입장 확인이 통과한다', async () => {
    expect((await register()).data).toMatchObject({ myRank: 3, ahead: 2, canEnter: false })
    expect((await poll()).data).toMatchObject({ ahead: 1, canEnter: false })
    expect((await poll()).data).toMatchObject({ ahead: 0, canEnter: false })
    expect((await poll()).data.canEnter).toBe(true)

    const admission = await customFetch<{ data: { admitted: boolean } }>(
      'http://localhost/api/v1/order/queue/admission?raceId=1&raceCourseId=1',
    )
    expect(admission.data.admitted).toBe(true)
  })

  it('줄을 서지 않고 순번을 물으면 404, 입장 중에 다시 등록하면 409다', async () => {
    await expect(poll()).rejects.toMatchObject({ status: 404 })

    await register()
    await poll()
    await poll()
    await poll()
    await customFetch('http://localhost/api/v1/order/queue/admission?raceId=1&raceCourseId=1')
    await expect(register()).rejects.toMatchObject({ status: 409 })
  })
})

// 서버(proxy.ts)가 화면 요청에서 로그인 여부를 알 수 있게, 가짜 로그인이 Path=/ 표시 쿠키를 같이 심고 지우는지 확인한다.
describe('가짜 로그인 표시 쿠키', () => {
  const call = (path: string, cookie?: string) =>
    fetch(`http://localhost/api/v1/auth/${path}`, {
      method: 'POST',
      headers: cookie ? { cookie } : undefined,
    })
  const cookiesOf = (response: Response) => response.headers.getSetCookie()

  it('로그인하면 Refresh Token 쿠키와 Path=/ 표시 쿠키를 함께 심는다', async () => {
    const cookies = cookiesOf(await call('login'))
    expect(cookies.some((c) => /^mockRefreshToken=.*Path=\/api\/v1\/auth/.test(c))).toBe(true)
    expect(cookies.some((c) => /^mockLoggedIn=1;.*Path=\/;/.test(c))).toBe(true)
  })

  it('로그아웃하면 두 쿠키를 모두 지운다', async () => {
    const cookies = cookiesOf(await call('logout', MOCK_LOGGED_IN_COOKIE))
    expect(cookies.some((c) => /^mockRefreshToken=; Max-Age=0/.test(c))).toBe(true)
    expect(cookies.some((c) => /^mockLoggedIn=; Max-Age=0/.test(c))).toBe(true)
  })
})
