import { describe, expect, it } from 'vitest'

import { customFetch } from '@/shared/api'

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
