import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'

import { ApiError, type productModel } from '@/shared/api'
import { server } from '@/shared/testing'

import { RACE_FORM_DEFAULTS, type RaceFormValues } from './race-form'
import { useRegisterRace } from './use-register-race'

const values: RaceFormValues = {
  ...RACE_FORM_DEFAULTS,
  title: '2026 용감한 쿠키RUN in 서울',
  location: '서울숲 일대',
  regionSido: '서울특별시',
  regionSigungu: '성동구',
  raceDate: '2026-11-01',
  individualOpenAt: '2026-10-20',
  applicationCloseAt: '2026-10-31',
  refundDeadline: '2026-10-31',
}

const image = (name: string) => new File([new Uint8Array([1, 2, 3])], name, { type: 'image/png' })
const images = { cover: image('cover.png'), intro: [image('intro-1.png'), image('intro-2.png')] }

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useRegisterRace', () => {
  it('종목마다 사진을 따로 올리고, 받은 파일 ID로 대회를 등록한다', async () => {
    let body: productModel.RaceSaveRequestDto | undefined
    server.use(
      http.post('*/api/v1/product/races', async ({ request }) => {
        body = (await request.json()) as productModel.RaceSaveRequestDto
        return HttpResponse.json(
          {
            status: 201,
            message: '대회를 등록했습니다.',
            data: {
              raceId: 7,
              saleStatus: 'SCHEDULED',
              saleOpenDate: '2026-10-20',
              courseIds: [1, 2],
            },
          },
          { status: 201 },
        )
      }),
    )
    const { result } = renderHook(() => useRegisterRace(), { wrapper })
    act(() => result.current.mutate({ values, images }))
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(result.current.data?.raceId).toBe(7)
    expect(body).toMatchObject({
      raceDate: '2026-11-01T00:00:00',
      individualOpenAt: '2026-10-20T00:00:00',
      applicationCloseAt: '2026-10-31T23:59:59',
      refundDeadline: '2026-10-31',
      crewMinCount: 5,
    })
    expect(body?.crewSaleStartAt).toBeUndefined()
    const courseImages = body?.courses.map((c) => c.images) ?? []
    expect(courseImages).toHaveLength(2)
    for (const list of courseImages) {
      expect(list.map((i) => [i.fileType, i.sortNo])).toEqual([
        ['THUMBNAIL', 0],
        ['DETAIL', 0],
        ['DETAIL', 1],
      ])
    }
    // 한 파일은 한 종목에만 연결되므로 파일 ID가 겹치지 않는다
    const fileIds = courseImages.flat().map((i) => i.fileId)
    expect(new Set(fileIds).size).toBe(fileIds.length)
  })

  it('Idempotency-Key 헤더를 보내고, 실패 뒤 같은 입력으로 다시 보내면 같은 키 · 입력이 바뀌면 새 키를 쓴다', async () => {
    const keys: (string | null)[] = []
    server.use(
      http.post('*/api/v1/product/races', ({ request }) => {
        keys.push(request.headers.get('Idempotency-Key'))
        if (keys.length === 1) {
          return HttpResponse.json(
            { status: 500, message: '잠시 후 다시 시도해주세요.', data: null },
            { status: 500 },
          )
        }
        return HttpResponse.json(
          {
            status: 201,
            message: '대회를 등록했습니다.',
            data: {
              raceId: 7,
              saleStatus: 'SCHEDULED',
              saleOpenDate: '2026-10-20',
              courseIds: [1],
            },
          },
          { status: 201 },
        )
      }),
    )
    const { result } = renderHook(() => useRegisterRace(), { wrapper })
    act(() => result.current.mutate({ values, images }))
    await waitFor(() => expect(result.current.isError).toBe(true))
    act(() => result.current.mutate({ values, images }))
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    act(() => result.current.mutate({ values: { ...values, title: '다른 대회' }, images }))
    await waitFor(() => expect(keys).toHaveLength(3))

    expect(keys[0]).toMatch(/^[0-9a-f-]{36}$/)
    expect(keys[1]).toBe(keys[0])
    expect(keys[2]).not.toBe(keys[0])
  })

  it('승인되지 않은 판매자면(403) 서버 문구를 담은 오류가 된다', async () => {
    server.use(
      http.post('*/api/v1/product/races', () =>
        HttpResponse.json(
          { status: 403, message: '승인된 판매자만 대회를 등록·수정할 수 있습니다.', data: null },
          { status: 403 },
        ),
      ),
    )
    const { result } = renderHook(() => useRegisterRace(), { wrapper })
    act(() => result.current.mutate({ values, images }))
    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.error).toBeInstanceOf(ApiError)
    expect((result.current.error as ApiError).serverMessage).toBe(
      '승인된 판매자만 대회를 등록·수정할 수 있습니다.',
    )
  })
})
