import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { BookingFlowProvider } from '@/features/booking'
import { orderMock, server } from '@/shared/testing'

import { CheckoutPage, type CheckoutState } from './CheckoutPage'

const push = vi.fn()
const replace = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push, back: vi.fn(), replace }),
  usePathname: () => '/checkout',
}))

// 지금은 2026-10-14 10:00 (한국 시간). 주문의 결제 기한은 10분 뒤.
const NOW = new Date('2026-10-14T10:00:00+09:00')
const DUE_AT = '2026-10-14T10:10:00'

/** 상품 API 명세 예시의 대회 1 · 종목 1(10K, 45,000원)을 결제 대기로 잡아 둔 주문 */
function orderResponse(
  patch: { status?: 'CREATED' | 'EXPIRED' | 'PAID'; paymentDueAt?: string } = {},
) {
  const example = orderMock.getFindById1ResponseMock()
  return {
    ...example,
    data: {
      ...example.data,
      status: patch.status ?? 'CREATED',
      totalAmount: 45000,
      paymentDueAt: patch.paymentDueAt ?? DUE_AT,
      items: example.data!.items!.map((item) => ({ ...item, raceCourseId: 1, amount: 45000 })),
    },
  } as const
}

const balanceHandler = (balance: number) =>
  http.get('*/api/v1/cash/wallets/:holderId/balance', () =>
    HttpResponse.json({ status: 200, message: '잔액 조회 성공', data: { holderId: 1, balance } }),
  )

const conflict = (message: string) =>
  HttpResponse.json({ status: 409, message, data: null }, { status: 409 })

function renderCheckout(state?: CheckoutState) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <BookingFlowProvider
        initialState={{
          applications: {
            '1': { raceId: '1', optionId: '1', participationId: 1, participantName: '이달림' },
          },
        }}
      >
        <CheckoutPage state={state} orderId={state ? undefined : 1} />
      </BookingFlowProvider>
    </QueryClientProvider>,
  )
}

const timer = () => screen.getByRole('timer')
const agree = (user: ReturnType<typeof userEvent.setup>) =>
  user.click(screen.getByRole('checkbox', { name: '[필수] 환불 기한을 확인했어요' }))

describe('결제 화면', () => {
  beforeEach(() => {
    // MSW 응답(Promise)이 흐르도록 실제 시간도 같이 흘려보낸다.
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.setSystemTime(NOW)
    push.mockClear()
    replace.mockClear()
    server.use(orderMock.getFindById1MockHandler(orderResponse()), balanceHandler(100_000))
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('주문 금액 · 대회 · 참가자와 결제 기한까지 남은 시간을 보여 준다', async () => {
    renderCheckout()

    expect(
      await screen.findByText('2026 용감한 쿠키RUN in 서울 · 10K · 이달림'),
    ).toBeInTheDocument()
    // 예치금 10만 원에서 45,000원을 내고 55,000원이 남는다
    expect(screen.getByText('₩55,000')).toBeInTheDocument()
    expect(timer()).toHaveTextContent(/10:00|09:5\d/)
  })

  it('대기열에 입장 중이면 결제 기한 대신 대기열에서 받은 남은 시간을 센다', async () => {
    server.use(
      orderMock.getFindAdmissionMockHandler({
        status: 200,
        data: { admitted: true, admissionEndsAt: DUE_AT, remainingSeconds: 300 },
      }),
    )
    renderCheckout()

    await screen.findByText('2026 용감한 쿠키RUN in 서울 · 10K · 이달림')
    await vi.waitFor(() => expect(timer()).toHaveTextContent(/05:00|04:5\d/))
  })

  it('잔액이 충분하면 예치금으로 결제하고 결제 완료 화면으로 간다', async () => {
    let paid = false
    server.use(
      http.post('*/api/v1/order/orders/:orderId/payment', () => {
        paid = true
        return HttpResponse.json(orderMock.getPayResponseMock())
      }),
    )
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    renderCheckout()

    const pay = await screen.findByRole('button', { name: '₩45,000 결제' })
    await agree(user)
    await user.click(pay)

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/checkout/complete?orderId=1'))
    expect(paid).toBe(true)
  })

  it('잔액이 모자라면 충전 안내를 보이고, 충전하러 가기는 결제 기한을 늘리지 않고 충전창으로 간다', async () => {
    server.use(balanceHandler(40_000))
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    renderCheckout()

    // 45,000원 중 40,000원은 예치금에서, 모자란 5,000원은 최소 충전 금액 1만 원으로 충전한다.
    const button = await screen.findByRole('button', { name: '₩10,000 충전하고 결제' })
    expect(screen.getByText('+₩10,000')).toBeInTheDocument()
    await agree(user)
    await user.click(button)

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/me/deposit/charge'))
  })

  it('결제 때 예치금이 모자라다고 하면(409) 서버 문구로 충전 안내를 보여 준다', async () => {
    const message = '예치금이 12,000원 부족합니다. 충전한 뒤 결제 기한 안에 다시 결제해주세요.'
    server.use(http.post('*/api/v1/order/orders/:orderId/payment', () => conflict(message)))
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    renderCheckout()

    const pay = await screen.findByRole('button', { name: '₩45,000 결제' })
    await agree(user)
    await user.click(pay)

    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(push).not.toHaveBeenCalled()
  })

  it('결제 때 기한이 지났다고 하면(409) 시간 초과 화면을 보여 준다', async () => {
    server.use(
      http.post('*/api/v1/order/orders/:orderId/payment', () =>
        conflict('결제 기한이 지났습니다.'),
      ),
    )
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    renderCheckout()

    const pay = await screen.findByRole('button', { name: '₩45,000 결제' })
    await agree(user)
    await user.click(pay)

    expect(await screen.findByText('결제 시간이 지났어요')).toBeInTheDocument()
    expect(screen.getByText(/입장 시간이 끝나 잡아 둔 자리가 풀렸어요\./)).toBeInTheDocument()
    expect(screen.getByText(/충전한 예치금은 그대로 남아 있어요\./)).toBeInTheDocument()
    // 주문의 대회 상세(M02)로 돌아간다
    expect(screen.getByRole('link', { name: '대회 다시 보기' })).toHaveAttribute('href', '/races/1')
  })

  it('결제 때 남은 자리가 없다고 하면(409) 자리 마감 화면을 보여 준다', async () => {
    server.use(
      http.post('*/api/v1/order/orders/:orderId/payment', () =>
        conflict('남은 자리가 없습니다. 참가 신청서를 다시 작성해 신청할 수 있습니다.'),
      ),
    )
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    renderCheckout()

    const pay = await screen.findByRole('button', { name: '₩45,000 결제' })
    await agree(user)
    await user.click(pay)

    expect(await screen.findByText('남은 자리가 없어요')).toBeInTheDocument()
  })

  it('결제 기한이 지나면 시간 초과 화면으로 바뀐다', async () => {
    renderCheckout()
    await screen.findByRole('timer')

    act(() => vi.advanceTimersByTime(10 * 60 * 1000))

    expect(await screen.findByText('결제 시간이 지났어요')).toBeInTheDocument()
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
  })

  it('기한이 만료된 주문이면 바로 시간 초과 화면을 보여 준다', async () => {
    server.use(orderMock.getFindById1MockHandler(orderResponse({ status: 'EXPIRED' })))
    renderCheckout()

    expect(await screen.findByText('결제 시간이 지났어요')).toBeInTheDocument()
  })

  it('이미 결제한 주문이면 결제 완료 화면으로 보낸다', async () => {
    server.use(orderMock.getFindById1MockHandler(orderResponse({ status: 'PAID' })))
    renderCheckout()

    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/checkout/complete?orderId=1'))
  })

  it('?preview=offline 시안은 시안의 남은 시간에서 센다', () => {
    renderCheckout('offline')

    expect(timer()).toHaveTextContent('05:10')
    expect(screen.getByRole('checkbox', { name: '[필수] 환불 기한을 확인했어요' })).toBeChecked()
  })

  it('?preview=timeout 시안은 바로 시간 초과 화면을 보여 준다', () => {
    renderCheckout('timeout')

    expect(screen.getByText('결제 시간이 지났어요')).toBeInTheDocument()
    // 대회를 모르는 미리보기는 대회 목록으로 간다
    expect(screen.getByRole('link', { name: '대회 다시 보기' })).toHaveAttribute('href', '/races')
  })

  it('대기열 입장 시간이 끝나 넘어오면(?result=timeout&raceId=) 그 대회 상세로 돌아간다', () => {
    const client = new QueryClient()
    render(
      <QueryClientProvider client={client}>
        <CheckoutPage state="timeout" raceId={1} />
      </QueryClientProvider>,
    )

    expect(screen.getByText('결제 시간이 지났어요')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '대회 다시 보기' })).toHaveAttribute('href', '/races/1')
  })
})
