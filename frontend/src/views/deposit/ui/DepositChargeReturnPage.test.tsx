import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { requestCharge } from '@/features/deposit-charge'
import { ApiError, orderApi } from '@/shared/api'
import { server } from '@/shared/testing'

import { DepositChargeReturnPage } from './DepositChargeReturnPage'

const replace = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn(), replace }),
  usePathname: () => '/me/deposit/charge/return',
}))

const PENDING_KEY = 'crewrun:pending-charge'
const ORDER_ID = 'charge-11111111-2222-3333-4444-555555555555'

function savePending(pending: { orderId: string; amount: number; returnOrderId?: number }) {
  sessionStorage.setItem(PENDING_KEY, JSON.stringify(pending))
}

function renderPage(props: Parameters<typeof DepositChargeReturnPage>[0]) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <DepositChargeReturnPage {...props} />
    </QueryClientProvider>,
  )
}

/** 충전 확정 요청 본문을 모으고 outcome으로 답한다 */
function confirmHandler(outcome: 'COMPLETED' | 'FAILED', bodies: unknown[] = []) {
  return http.post('*/api/v1/cash/charges/confirm', async ({ request }) => {
    bodies.push(await request.json())
    return HttpResponse.json({ status: 200, message: '아무 문구', data: { outcome, balance: 0 } })
  })
}

const success = (patch: Partial<Parameters<typeof DepositChargeReturnPage>[0]> = {}) => ({
  result: 'success',
  paymentKey: 'pk_test',
  orderId: ORDER_ID,
  amount: 12_000,
  ...patch,
})

describe('충전 결과 화면 (토스 결제창에서 돌아옴)', () => {
  beforeEach(() => {
    replace.mockClear()
    sessionStorage.clear()
  })

  it('충전 성공: 돌아온 값으로 충전을 확정하고 같은 주문의 결제 화면으로 돌아가 다시 결제하게 한다', async () => {
    savePending({ orderId: ORDER_ID, amount: 12_000, returnOrderId: 1 })
    const bodies: unknown[] = []
    server.use(confirmHandler('COMPLETED', bodies))

    renderPage(success({ returnOrderId: 1 }))

    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/checkout?orderId=1&charged=1'))
    expect(bodies).toEqual([{ paymentKey: 'pk_test', orderId: ORDER_ID, amount: 12_000 }])
    // 다 쓴 충전 기록은 지운다
    expect(sessionStorage.getItem(PENDING_KEY)).toBeNull()
  })

  it('충전 실패(PG 승인 거절, 200 · FAILED): 결제 화면으로 돌아가 결제 실패(M07-3)를 보여 주게 한다', async () => {
    savePending({ orderId: ORDER_ID, amount: 12_000, returnOrderId: 1 })
    server.use(confirmHandler('FAILED'))

    renderPage(success({ returnOrderId: 1 }))

    await vi.waitFor(() =>
      expect(replace).toHaveBeenCalledWith('/checkout?orderId=1&charge=failed'),
    )
  })

  it('충전 확정이 네트워크 오류로 끝나도 실패로 보낸다', async () => {
    savePending({ orderId: ORDER_ID, amount: 12_000, returnOrderId: 1 })
    server.use(http.post('*/api/v1/cash/charges/confirm', () => HttpResponse.error()))

    renderPage(success({ returnOrderId: 1 }))

    await vi.waitFor(() =>
      expect(replace).toHaveBeenCalledWith('/checkout?orderId=1&charge=failed'),
    )
  })

  it('돌아온 금액이 떠나기 전에 적어 둔 금액과 다르면 확정하지 않는다', async () => {
    savePending({ orderId: ORDER_ID, amount: 12_000, returnOrderId: 1 })
    const bodies: unknown[] = []
    server.use(confirmHandler('COMPLETED', bodies))

    renderPage(success({ amount: 100, returnOrderId: 1 }))

    await vi.waitFor(() =>
      expect(replace).toHaveBeenCalledWith('/checkout?orderId=1&charge=failed'),
    )
    expect(bodies).toEqual([])
  })

  it('결제창을 닫고 돌아오면(failUrl · PAY_PROCESS_CANCELED) 결제 화면으로 조용히 돌아간다', async () => {
    savePending({ orderId: ORDER_ID, amount: 12_000, returnOrderId: 1 })

    renderPage({
      result: 'fail',
      code: 'PAY_PROCESS_CANCELED',
      orderId: ORDER_ID,
      returnOrderId: 1,
    })

    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/checkout?orderId=1'))
  })

  it('예치금 화면에서 충전했으면 성공은 예치금(M17), 실패는 충전 화면(M21)으로 간다', async () => {
    savePending({ orderId: ORDER_ID, amount: 12_000 })
    server.use(confirmHandler('COMPLETED'))
    const { unmount } = renderPage(success())
    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/me/deposit'))
    unmount()

    replace.mockClear()
    renderPage({ result: 'fail', code: 'REJECT_CARD_COMPANY', orderId: ORDER_ID })
    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/me/deposit/charge'))
  })

  it('가짜 응답 모드에서 잔액 부족 → 충전 → 같은 주문으로 다시 결제까지 이어진다', async () => {
    // 목 잔액 5,000원, 주문 50,000원
    const firstPay = await orderApi.pay(1).catch((error: unknown) => error)
    expect(firstPay).toBeInstanceOf(ApiError)
    expect((firstPay as ApiError).code).toBe('ORDER_INSUFFICIENT_BALANCE')
    const shortage = ((firstPay as ApiError).body as { data: { shortage: number } }).data.shortage
    expect(shortage).toBe(45_000)

    const navigate = vi.fn()
    await requestCharge(
      { amount: shortage, returnOrderId: 1 },
      { mocking: true, clientKey: undefined, origin: 'http://localhost', navigate },
    )
    const back = new URL(navigate.mock.calls[0][0] as string).searchParams
    renderPage({
      result: back.get('result') ?? undefined,
      paymentKey: back.get('paymentKey') ?? undefined,
      orderId: back.get('orderId') ?? undefined,
      amount: Number(back.get('amount')),
      returnOrderId: Number(back.get('returnOrderId')),
    })
    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/checkout?orderId=1&charged=1'))

    const secondPay = await orderApi.pay(1)
    expect(secondPay.data?.status).toBe('PAID')
  })
})
