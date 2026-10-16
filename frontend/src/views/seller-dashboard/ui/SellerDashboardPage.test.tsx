import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { server } from '@/shared/testing'

import { SellerDashboardPage } from './SellerDashboardPage'

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <SellerDashboardPage />
    </QueryClientProvider>,
  )
}

describe('SellerDashboardPage', () => {
  it('정산 요약과 판매 중 대회를 API로 보여 준다', async () => {
    renderPage()

    expect(await screen.findByText('₩94,500')).toBeInTheDocument()
    expect(screen.getByText('(화)')).toBeInTheDocument()
    expect(await screen.findByText('접수율')).toBeInTheDocument()
    expect(screen.getByText('15/600명')).toBeInTheDocument()
    expect(screen.getByText('판매 중 대회')).toBeInTheDocument()
  })

  it('정산 요약과 대회 목록이 실패하면 각각 서버 문구를 보여 준다', async () => {
    server.use(
      http.get('*/api/v1/payout/seller/summary', () =>
        HttpResponse.json(
          { status: 403, message: '정산 요약을 볼 수 없습니다.', data: null },
          { status: 403 },
        ),
      ),
      http.get('*/api/v1/product/races/mine', () =>
        HttpResponse.json(
          { status: 403, message: '승인된 판매자만 볼 수 있습니다.', data: null },
          { status: 403 },
        ),
      ),
    )
    renderPage()

    expect(await screen.findByText('정산 요약을 불러오지 못했어요')).toBeInTheDocument()
    expect(screen.getByText('정산 요약을 볼 수 없습니다.')).toBeInTheDocument()
    expect(await screen.findByText('대회 목록을 불러오지 못했어요')).toBeInTheDocument()
    expect(screen.getByText('승인된 판매자만 볼 수 있습니다.')).toBeInTheDocument()
  })

  it('처리할 일의 배송 대기 키트를 키트 건수 API로 채우고 키트 화면으로 연결한다', async () => {
    renderPage()

    // 배송 준비 3건 중 환불 기한 전 1건은 아직 보낼 수 없어 2건
    const link = await screen.findByRole('link', { name: /배송 대기 키트/ })
    expect(link).toHaveAttribute('href', '/seller/kits')
    expect(await within(link).findByText('2건')).toBeInTheDocument()
  })

  it('키트 건수를 불러오지 못하면 건수 대신 - 를 보여 준다', async () => {
    server.use(
      http.get('*/api/v1/order/seller/kits/summary', () =>
        HttpResponse.json(
          {
            status: 503,
            code: 'ORDER_EXTERNAL_SERVICE_UNAVAILABLE',
            message: '잠시 뒤',
            data: null,
          },
          { status: 503 },
        ),
      ),
    )
    renderPage()

    const link = await screen.findByRole('link', { name: /배송 대기 키트/ })
    expect(await within(link).findByText('-')).toBeInTheDocument()
  })
})
