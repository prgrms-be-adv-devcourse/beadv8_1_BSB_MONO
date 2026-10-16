import { describe, expect, it } from 'vitest'

import { customFetch } from '@/shared/api'

import { MOCK_LOGGED_IN_COOKIE } from './mock-session'

// 생성된 목이 등록됐는지 확인한다 (API_MOCKING=on 개발 서버도 같은 handlers를 쓴다)
describe('생성된 MSW 핸들러', () => {
  it('GET /api/v1/product/races가 명세 예시로 응답한다', async () => {
    const body = await customFetch<{ data: { races: { title: string }[] } }>(
      'http://localhost/api/v1/product/races?sort=DEADLINE',
    )
    expect(body.data.races[0]?.title).toBe('2026 용감한 쿠키RUN in 서울')
  })

  it('/races/suggestions가 /races/:raceId 핸들러에 가로채이지 않는다', async () => {
    const body = await customFetch<{ message: string }>(
      'http://localhost/api/v1/product/races/suggestions?keyword=서울',
    )
    expect(body.message).toBe('연관검색어를 조회했습니다.')
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
