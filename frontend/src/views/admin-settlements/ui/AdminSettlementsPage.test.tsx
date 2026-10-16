import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { payoutMock, server } from '@/shared/testing'
import { ToastProvider } from '@/shared/ui'

import { AdminSettlementsPage } from './AdminSettlementsPage'

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <AdminSettlementsPage />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('AdminSettlementsPage', () => {
  it('표에서 고른 정산에 지급보류를 건다', async () => {
    let request: { path: string; body: unknown } | null = null
    server.use(
      http.post('*/api/v1/payout/payouts/:payoutId/holds', async ({ request: req }) => {
        request = { path: new URL(req.url).pathname, body: await req.json() }
        return HttpResponse.json(payoutMock.getHoldResponseMock(), { status: 201 })
      }),
    )
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByText('ST-20261120-2D91'))
    expect(screen.getByText('ST-20261120-2D91 · 나이트런컴퍼니')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '지급보류 걸기' }))

    expect(await screen.findByText('지급보류를 걸었어요.')).toBeInTheDocument()
    expect(request).toEqual({
      path: '/api/v1/payout/payouts/3/holds',
      body: { reasonType: 'RACE_CANCELED' },
    })
  })

  it('이미 지급 완료된 정산이면(409) 서버 문구를 보여 준다', async () => {
    server.use(
      http.post('*/api/v1/payout/payouts/:payoutId/holds', () =>
        HttpResponse.json(
          { status: 409, message: '이미 지급 완료된 정산 내역입니다.', data: null },
          { status: 409 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: '지급보류 걸기' }))

    // 오류 토스트는 화면 낭독기용 알림에도 같은 글자가 들어간다
    expect(
      (await screen.findAllByText('이미 지급 완료된 정산 내역입니다.')).length,
    ).toBeGreaterThan(0)
  })

  it('보류 중인 행의 보류 해제를 누르면 해제 API를 부른다', async () => {
    let path = ''
    server.use(
      http.post('*/api/v1/payout/holds/:holdId/release', ({ request }) => {
        path = new URL(request.url).pathname
        return HttpResponse.json(payoutMock.getReleaseResponseMock())
      }),
    )
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: '보류 해제' }))

    expect(await screen.findByText('지급보류를 풀었어요.')).toBeInTheDocument()
    expect(path).toBe('/api/v1/payout/holds/1/release')
  })
})
