import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'

import { orderApi, unwrapData } from '@/shared/api'

import { orderQueryOptions, participationQueryOptions } from './ticket-api'

// 결제 · 완료 화면(orderApi.useFindById1)과 참가권 상세(orderQueryOptions)는 같은 주문 캐시를 쓴다.
// 결제 직후 참가권을 열 때 모양이 다르면 주문 상품을 못 읽어 "참가권을 찾을 수 없어요"가 나온다.
describe('orderQueryOptions', () => {
  it('결제 · 완료 화면이 받아 둔 주문 캐시에서도 주문 상품을 읽는다', async () => {
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false, staleTime: 60_000 } },
    })
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    )
    const complete = renderHook(() => orderApi.useFindById1(1, { query: { select: unwrapData } }), {
      wrapper,
    })
    await waitFor(() => expect(complete.result.current.data?.items?.length).toBeGreaterThan(0))
    complete.unmount()

    const ticket = renderHook(() => useQuery(orderQueryOptions(1)), { wrapper })
    await waitFor(() => expect(ticket.result.current.data?.items?.length).toBeGreaterThan(0))
    expect(ticket.result.current.isError).toBe(false)
  })
})

// 참가 신청도 결제 · 완료 화면(orderApi.useFindById)과 같은 캐시 키를 쓴다. 모양이 다르면 결제 직후 참가자 정보가 빈다.
describe('participationQueryOptions', () => {
  it('결제 · 완료 화면이 받아 둔 참가 신청 캐시에서도 참가자를 읽는다', async () => {
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false, staleTime: 60_000 } },
    })
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    )
    const complete = renderHook(() => orderApi.useFindById(1, { query: { select: unwrapData } }), {
      wrapper,
    })
    await waitFor(() => expect(complete.result.current.data?.participant?.name).toBeTruthy())
    complete.unmount()

    const ticket = renderHook(() => useQuery(participationQueryOptions(1)), { wrapper })
    await waitFor(() => expect(ticket.result.current.data?.participant?.name).toBeTruthy())
  })
})
