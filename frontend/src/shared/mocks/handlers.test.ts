import { describe, expect, it } from 'vitest'

import { customFetch } from '@/shared/api'

import { setMockLoggedIn } from './mock-session'

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

// 가짜 응답 모드에서 로그인 상태를 진짜처럼 흉내 내는지 확인한다
describe('인증 목', () => {
  const post = (path: string) => fetch(`http://localhost/api/v1/auth/${path}`, { method: 'POST' })

  it('로그인하지 않았으면 재발급이 401이다', async () => {
    expect((await post('reissue')).status).toBe(401)
  })

  it('로그인하면 재발급이 새 Access Token을 준다', async () => {
    await fetch('http://localhost/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'runner1@crewrun.com', password: 'password1' }),
    })

    const first = await post('reissue')
    const second = await post('reissue')

    expect(first.status).toBe(200)
    const firstToken = ((await first.json()) as { data: { accessToken: string } }).data.accessToken
    const secondToken = ((await second.json()) as { data: { accessToken: string } }).data
      .accessToken
    expect(firstToken).not.toBe(secondToken)
  })

  it('로그아웃하면 재발급이 다시 401이다', async () => {
    setMockLoggedIn(true)
    expect((await post('reissue')).status).toBe(200)

    await post('logout')

    expect((await post('reissue')).status).toBe(401)
  })
})
