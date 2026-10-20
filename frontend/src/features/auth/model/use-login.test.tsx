import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'

import { server } from '@/shared/testing'

import { useLogin } from './use-login'

describe('useLogin', () => {
  it('로그인에 성공하면 이전 회원의 캐시(잔액 등)를 비운다', async () => {
    server.use(
      http.post('*/api/v1/auth/login', () =>
        HttpResponse.json({
          status: 200,
          message: '로그인 성공',
          data: { accessToken: 'token-b', member: { id: 2, nickname: 'B' } },
        }),
      ),
    )
    const client = new QueryClient()
    client.setQueryData(['/api/v1/cash/wallets/balance'], { data: { balance: 12000 } })
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    )
    const { result } = renderHook(() => useLogin(), { wrapper })

    await act(async () => {
      result.current.mutate({ email: 'b@example.com', password: 'password1!' })
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(client.getQueryData(['/api/v1/cash/wallets/balance'])).toBeUndefined()
  })
})
