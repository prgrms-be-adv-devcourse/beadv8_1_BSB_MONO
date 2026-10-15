import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getAuthStatus, setAccessToken } from '@/shared/api'
import { server } from '@/shared/testing'

import { MyPage } from './MyPage'

const replace = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn(), replace }),
  usePathname: () => '/me',
}))

beforeEach(() => replace.mockClear())

describe('MyPage 로그아웃', () => {
  it('로그아웃을 누르면 로그아웃 API를 부르고, 토큰·캐시를 비운 뒤 로그인 화면으로 보낸다', async () => {
    let authorization: string | null = null
    server.use(
      http.post('*/api/v1/auth/logout', ({ request }) => {
        authorization = request.headers.get('Authorization')
        return HttpResponse.json({ status: 200, message: '로그아웃 성공', data: null })
      }),
    )
    setAccessToken('access-token')
    const client = new QueryClient()
    client.setQueryData(['/api/v1/member/members/me'], { data: { nickname: '러너1' } })
    const user = userEvent.setup()
    render(
      <QueryClientProvider client={client}>
        <MyPage />
      </QueryClientProvider>,
    )

    await user.click(screen.getByRole('button', { name: '로그아웃' }))

    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/login'))
    expect(authorization).toBe('Bearer access-token')
    expect(getAuthStatus()).toBe('anonymous')
    expect(client.getQueryData(['/api/v1/member/members/me'])).toBeUndefined()
  })

  it('로그아웃 요청이 실패해도 이 브라우저에서는 로그아웃 상태로 바꾼다', async () => {
    server.use(http.post('*/api/v1/auth/logout', () => HttpResponse.error()))
    setAccessToken('access-token')
    const user = userEvent.setup()
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MyPage />
      </QueryClientProvider>,
    )

    await user.click(screen.getByRole('button', { name: '로그아웃' }))

    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/login'))
    expect(getAuthStatus()).toBe('anonymous')
  })
})
