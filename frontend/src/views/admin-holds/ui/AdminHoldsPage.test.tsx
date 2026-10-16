import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'

import { payoutMock, server } from '@/shared/testing'
import { ToastProvider } from '@/shared/ui'

import { AdminHoldsPage } from './AdminHoldsPage'

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <AdminHoldsPage />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('AdminHoldsPage', () => {
  it('보류 해제를 누르면 선택한 지급보류의 해제 API를 부른다', async () => {
    let url = ''
    server.use(
      http.post('*/api/v1/payout/holds/:holdId/release', ({ request }) => {
        url = new URL(request.url).pathname
        return HttpResponse.json(payoutMock.getReleaseResponseMock())
      }),
    )
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: '보류 해제' }))

    expect(await screen.findByText('지급보류를 풀었어요.')).toBeInTheDocument()
    expect(url).toBe('/api/v1/payout/holds/2/release')
  })

  it('이미 해제된 보류면(409) 서버 문구를 보여 준다', async () => {
    server.use(
      http.post('*/api/v1/payout/holds/:holdId/release', () =>
        HttpResponse.json(
          { status: 409, message: '이미 해제된 지급보류입니다.', data: null },
          { status: 409 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: '보류 해제' }))

    // 오류 토스트는 화면 낭독기용 알림에도 같은 글자가 들어간다
    expect((await screen.findAllByText('이미 해제된 지급보류입니다.')).length).toBeGreaterThan(0)
  })

  it('정산 내역 번호와 사유로 보류를 건다', async () => {
    let request: { path: string; body: unknown } | null = null
    server.use(
      http.post('*/api/v1/payout/payouts/:payoutId/holds', async ({ request: req }) => {
        request = { path: new URL(req.url).pathname, body: await req.json() }
        return HttpResponse.json(payoutMock.getHoldResponseMock(), { status: 201 })
      }),
    )
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByRole('textbox', { name: '정산 내역 번호' }), '12')
    await user.click(screen.getByRole('combobox', { name: '보류 사유' }))
    await user.click(await screen.findByRole('option', { name: '정산 계좌 미등록' }))
    await user.click(screen.getByRole('button', { name: '보류 걸기' }))

    expect(await screen.findByText('지급보류를 걸었어요.')).toBeInTheDocument()
    expect(request).toEqual({
      path: '/api/v1/payout/payouts/12/holds',
      body: { reasonType: 'ACCOUNT_NOT_REGISTERED' },
    })
  })

  it('번호가 숫자가 아니면 API를 부르지 않는다', async () => {
    const called = vi.fn()
    server.use(
      http.post('*/api/v1/payout/payouts/:payoutId/holds', () => {
        called()
        return HttpResponse.json(payoutMock.getHoldResponseMock(), { status: 201 })
      }),
    )
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByRole('textbox', { name: '정산 내역 번호' }), 'ST-20261120-3F02')
    await user.click(screen.getByRole('button', { name: '보류 걸기' }))

    expect(await screen.findByText('정산 내역 번호를 숫자로 넣어 주세요.')).toBeInTheDocument()
    expect(called).not.toHaveBeenCalled()
  })
})
