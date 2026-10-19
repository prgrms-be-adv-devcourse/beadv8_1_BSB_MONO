import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { clearAccessToken, setAccessToken } from '@/shared/api'
import { server } from '@/shared/testing'

import { NotificationsPage } from './NotificationsPage'

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/notifications',
}))

afterEach(() => clearAccessToken())

function renderPage() {
  setAccessToken('test-token')
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <NotificationsPage />
    </QueryClientProvider>,
  )
}

describe('NotificationsPage', () => {
  it('알림 API의 알림을 오늘 · 이전으로 나눠 보여 준다', async () => {
    renderPage()

    expect(await screen.findByText('예치금을 돌려받았어요')).toBeInTheDocument()
    expect(screen.getByText('환불이 끝났어요')).toBeInTheDocument()
    expect(screen.getByText('읽지 않은 알림 2개')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '오늘' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '이전' })).toBeInTheDocument()
  })

  it('알림이 없으면(방금 가입한 회원) 빈 화면을 보여 주고 읽지 않음 표시가 없다', async () => {
    server.use(
      http.get('*/api/v1/notification/notifications', () =>
        HttpResponse.json({ status: 200, message: '알림을 조회했습니다.', data: [] }),
      ),
    )
    renderPage()

    expect(await screen.findByText('아직 알림이 없어요')).toBeInTheDocument()
    expect(screen.queryByLabelText('읽지 않음')).not.toBeInTheDocument()
    expect(screen.queryByText(/읽지 않은 알림/)).not.toBeInTheDocument()
  })

  it('알림 API가 없으면(404) 알림이 없는 것으로 보여 준다', async () => {
    server.use(
      http.get('*/api/v1/notification/notifications', () =>
        HttpResponse.json({ status: 404, message: '없음', data: null }, { status: 404 }),
      ),
    )
    renderPage()

    expect(await screen.findByText('아직 알림이 없어요')).toBeInTheDocument()
  })
})
