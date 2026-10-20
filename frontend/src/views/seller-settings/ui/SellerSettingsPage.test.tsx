import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { setAccessToken, type memberModel } from '@/shared/api'
import { server } from '@/shared/testing'

import { SellerSettingsPage } from './SellerSettingsPage'

const SELLER: memberModel.SellerDto = {
  id: 1,
  memberId: 1,
  sellerType: 'BUSINESS',
  businessName: '크루런 스포츠',
  businessNumber: '123-45-67890',
  representativeName: '김러너',
  status: 'APPROVED',
  requestedAt: '2026-10-05T21:25:35',
  reviewedAt: '2026-10-06T10:12:08',
  rejectReason: null,
}

function renderPage(seller: Partial<memberModel.SellerDto> = {}) {
  server.use(
    http.get('*/api/v1/member/sellers/me', () =>
      HttpResponse.json({ status: 200, message: '성공', data: { ...SELLER, ...seller } }),
    ),
  )
  setAccessToken('access-token')
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <SellerSettingsPage />
    </QueryClientProvider>,
  )
}

describe('SellerSettingsPage', () => {
  it('내 판매자 정보를 보기 전용으로 보여 준다', async () => {
    renderPage()

    expect(await screen.findByText('123-45-67890')).toBeInTheDocument()
    expect(screen.getByText('크루런 스포츠')).toBeInTheDocument()
    expect(screen.getByText('김러너')).toBeInTheDocument()
    expect(screen.getByText('승인')).toBeInTheDocument()
    expect(screen.getByText('2026. 10. 6.')).toBeInTheDocument()
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
  })

  it('비사업자는 판매자 유형과 성명만 보여 준다', async () => {
    renderPage({ sellerType: 'INDIVIDUAL', businessName: null, businessNumber: null })

    expect(await screen.findByText('비사업자')).toBeInTheDocument()
    expect(screen.getByText('성명')).toBeInTheDocument()
    expect(screen.queryByText('사업자등록번호')).not.toBeInTheDocument()
  })

  it('정산 계좌는 입력 칸 없이 준비 중 안내만 보여 준다', async () => {
    renderPage()

    expect(await screen.findByText('정산 계좌 등록은 준비 중이에요')).toBeInTheDocument()
    expect(screen.queryByLabelText('계좌번호')).not.toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })
})
