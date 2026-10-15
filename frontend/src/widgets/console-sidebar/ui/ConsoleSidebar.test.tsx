import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getAuthStatus, setAccessToken } from '@/shared/api'
import { server } from '@/shared/testing'

import { ConsoleSidebar } from './ConsoleSidebar'

const replace = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn(), replace }),
  usePathname: () => '/seller',
}))

beforeEach(() => replace.mockClear())

describe('ConsoleSidebar 로그아웃', () => {
  it('로그아웃을 누르면 로그아웃 API를 부르고 토큰을 비운 뒤 로그인 화면으로 보낸다', async () => {
    let called = false
    server.use(
      http.post('*/api/v1/auth/logout', () => {
        called = true
        return HttpResponse.json({ status: 200, message: '로그아웃 성공', data: null })
      }),
    )
    setAccessToken('access-token')
    const user = userEvent.setup()
    render(
      <QueryClientProvider client={new QueryClient()}>
        <ConsoleSidebar role="seller" />
      </QueryClientProvider>,
    )

    await user.click(screen.getByRole('button', { name: '로그아웃' }))

    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/login'))
    expect(called).toBe(true)
    expect(getAuthStatus()).toBe('anonymous')
  })
})
