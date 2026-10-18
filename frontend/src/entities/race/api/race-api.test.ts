import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { server } from '@/shared/testing'

import { fetchRaceDetail } from './race-api'

describe('fetchRaceDetail', () => {
  it('상품 API의 대회와 파일 API의 종목 이미지를 합친다', async () => {
    const race = await fetchRaceDetail('1')
    expect(race?.title).toBe('2026 용감한 쿠키RUN in 서울')
    expect(race?.options[0]).toMatchObject({
      id: '1',
      image: '/mock/races/seoul-forest-autumn-run.webp',
    })
  })

  it('없는 대회(404)면 null', async () => {
    server.use(
      http.get('*/api/v1/product/races/:raceId', () =>
        HttpResponse.json(
          { status: 404, message: '존재하지 않는 대회입니다.', data: null },
          { status: 404 },
        ),
      ),
    )
    await expect(fetchRaceDetail('999')).resolves.toBeNull()
  })

  it('숫자가 아닌 ID는 API를 부르지 않고 null', async () => {
    await expect(fetchRaceDetail('cookierun')).resolves.toBeNull()
  })

  it('이미지를 못 받아도 대회는 보여 준다', async () => {
    server.use(http.get('*/api/v1/file/files', () => HttpResponse.error()))
    const race = await fetchRaceDetail('1')
    expect(race?.images).toEqual([])
  })
})
