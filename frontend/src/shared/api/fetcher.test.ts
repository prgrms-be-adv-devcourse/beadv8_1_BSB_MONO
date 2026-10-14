import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { server } from '@/shared/mocks/node'

import { ApiError } from './api-error'
import { customFetch } from './fetcher'

// jsdom 환경이라 브라우저 경로를 탄다. 상대 경로는 jsdom 기본 주소(localhost)로 풀린다.
describe('customFetch', () => {
  it('MSW 핸들러의 JSON 응답을 돌려준다', async () => {
    await expect(customFetch('http://localhost/api/health')).resolves.toEqual({
      status: 'ok',
      mocked: true,
    })
  })

  it('2xx가 아니면 ApiError를 던진다', async () => {
    server.use(
      http.get('*/api/races', () => HttpResponse.json({ message: 'nope' }, { status: 409 })),
    )

    const error = await customFetch('http://localhost/api/races').catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 409, body: { message: 'nope' } })
  })
})

describe('customFetch 옵션과 오류', () => {
  it('idempotencyKey를 Idempotency-Key 헤더로 보낸다', async () => {
    let received: string | null = null
    server.use(
      http.post('*/api/v1/order/orders', ({ request }) => {
        received = request.headers.get('Idempotency-Key')
        return HttpResponse.json({ status: 201, message: 'ok', data: null }, { status: 201 })
      }),
    )

    await customFetch('http://localhost/api/v1/order/orders', {
      method: 'POST',
      idempotencyKey: 'key-1',
    })
    expect(received).toBe('key-1')
  })

  it('RsData 오류 응답이면 서버 문구와 오류 코드를 ApiError에 담는다', async () => {
    server.use(
      http.get('*/api/v1/product/races/:raceId', () =>
        HttpResponse.json(
          { status: 404, message: '존재하지 않는 대회입니다.', data: null, code: 'RACE_NOT_FOUND' },
          { status: 404 },
        ),
      ),
    )

    const error = await customFetch('http://localhost/api/v1/product/races/999').catch(
      (e: unknown) => e,
    )
    expect(error).toMatchObject({
      status: 404,
      serverMessage: '존재하지 않는 대회입니다.',
      code: 'RACE_NOT_FOUND',
    })
  })
})
