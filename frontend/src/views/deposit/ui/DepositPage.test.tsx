import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { setAccessToken } from '@/shared/api'
import { server } from '@/shared/testing'

import { DepositPage } from './DepositPage'

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/me/deposit',
}))

beforeEach(() => setAccessToken('access-token'))

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <DepositPage />
    </QueryClientProvider>,
  )
}

describe('DepositPage 잔액', () => {
  it('내 회원 ID로 지갑 잔액을 불러와 보여 준다', async () => {
    const holderIds: string[] = []
    server.use(
      http.get('*/api/v1/member/members/me', () =>
        HttpResponse.json({ status: 200, message: '조회 성공', data: { id: 7, nickname: '러너' } }),
      ),
      http.get('*/api/v1/cash/wallets/:holderId/balance', ({ params }) => {
        holderIds.push(String(params.holderId))
        return HttpResponse.json({
          status: 200,
          message: '잔액 조회 성공',
          data: { holderId: 7, balance: 12000 },
        })
      }),
    )
    renderPage()

    expect(await screen.findByText('₩12,000')).toBeInTheDocument()
    expect(holderIds).toEqual(['7'])
  })

  it('지갑이 없으면(404) 잔액 대신 - 와 안내를 보여 준다', async () => {
    server.use(
      http.get('*/api/v1/cash/wallets/:holderId/balance', () =>
        HttpResponse.json(
          { status: 404, message: '존재하지 않는 지갑입니다.', data: null },
          { status: 404 },
        ),
      ),
    )
    renderPage()

    expect(await screen.findByText('잔액을 불러오지 못했어요.')).toBeInTheDocument()
    expect(screen.getByText('-')).toBeInTheDocument()
  })
})
