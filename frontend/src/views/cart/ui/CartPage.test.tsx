import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { BookingFlowProvider, type BookingApplication } from '@/features/booking'
import { orderMock, server } from '@/shared/testing'

import { CartPage } from './CartPage'

const push = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push, back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/cart',
}))

beforeEach(() => push.mockClear())

// 상품 API 명세 예시: 대회 1 「2026 용감한 쿠키RUN in 서울」의 종목 1 = 10K, 45,000원
const APPLICATION: BookingApplication = {
  raceId: '1',
  optionId: '1',
  participationId: 11,
  participantName: '이달림',
}

const cartWith = (items: { id: number; raceCourseId: number }[]) =>
  orderMock.getFindCartMockHandler({
    status: 200,
    message: '장바구니를 조회했습니다.',
    data: {
      memberId: 1,
      items: items.map((i) => ({ ...i, createDate: '2026-10-12T10:15:30' })),
    },
  })

function renderCart(
  applications: Record<string, BookingApplication> = { '1': APPLICATION },
  queueDialogOpen = false,
) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <BookingFlowProvider initialState={{ applications }}>
        <CartPage queueDialogOpen={queueDialogOpen} />
      </BookingFlowProvider>
    </QueryClientProvider>,
  )
}

/** 장바구니 결제 요청의 Idempotency-Key · 본문을 받아 두고 응답을 돌려준다 */
function captureCheckout(respond: () => Response) {
  const calls: { key: string | null; body: unknown }[] = []
  server.use(
    http.post('*/api/v1/order/cart/checkout', async ({ request }) => {
      calls.push({ key: request.headers.get('Idempotency-Key'), body: await request.json() })
      return respond()
    }),
  )
  return calls
}

const checkoutResult = (outcome: 'CREATED' | 'SOLD_OUT', withOrder = outcome === 'CREATED') => {
  const created = orderMock.getCheckoutResponseMock().data!.results![0]
  return HttpResponse.json({
    status: 200,
    message: '',
    data: {
      orderedCount: withOrder ? 1 : 0,
      results: [
        withOrder
          ? { ...created, participationId: 11 }
          : {
              participationId: 11,
              outcome,
              message: '남은 자리가 없습니다. 참가 신청서를 다시 작성해 신청할 수 있습니다.',
              order: null,
            },
      ],
    },
  })
}

describe('장바구니', () => {
  it('담은 참가권을 대회 정보와 함께 보여 주고, 빼면 목록에서 사라진다', async () => {
    server.use(
      cartWith([{ id: 1, raceCourseId: 1 }]),
      orderMock.getRemoveItemMockHandler({
        status: 200,
        message: '장바구니에서 뺐습니다.',
        data: { memberId: 1, items: [] },
      }),
    )
    const user = userEvent.setup()
    renderCart()

    expect(await screen.findByText('2026 용감한 쿠키RUN in 서울 · 10K')).toBeInTheDocument()
    expect(screen.getByText(/참가자 이달림/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '삭제' }))

    expect(await screen.findByText('장바구니가 비어 있어요')).toBeInTheDocument()
  })

  it('장바구니가 비어 있으면 빈 화면을 보여 준다', async () => {
    server.use(cartWith([]))
    renderCart()

    expect(await screen.findByText('장바구니가 비어 있어요')).toBeInTheDocument()
  })

  it('결제하기를 누르면 담은 참가권의 참가 신청 1건으로 주문을 만들고 결제 화면으로 간다', async () => {
    server.use(cartWith([{ id: 1, raceCourseId: 1 }]))
    const calls = captureCheckout(() => checkoutResult('CREATED'))
    const user = userEvent.setup()
    renderCart()

    await screen.findByText('2026 용감한 쿠키RUN in 서울 · 10K')
    await user.click(screen.getByRole('button', { name: '결제하기' }))

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/checkout?orderId=1'))
    expect(calls).toHaveLength(1)
    expect(calls[0].body).toEqual({ participationIds: [11] })
    expect(calls[0].key).toBeTruthy()
  })

  it('실패한 뒤 다시 눌러도 같은 Idempotency-Key를 보내 주문이 한 번만 만들어진다', async () => {
    server.use(cartWith([{ id: 1, raceCourseId: 1 }]))
    let first = true
    const calls = captureCheckout(() => {
      if (first) {
        first = false
        return HttpResponse.json(
          { status: 503, message: '잠시 뒤 다시 시도해주세요.', data: null },
          { status: 503 },
        )
      }
      return checkoutResult('CREATED')
    })
    const user = userEvent.setup()
    renderCart()

    await screen.findByText('2026 용감한 쿠키RUN in 서울 · 10K')
    await user.click(screen.getByRole('button', { name: '결제하기' }))
    expect(await screen.findByText('잠시 뒤 다시 시도해주세요.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '결제하기' }))

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/checkout?orderId=1'))
    expect(calls).toHaveLength(2)
    expect(calls[1].key).toBe(calls[0].key)
  })

  it('매진이면 담은 참가권 줄에 실패 이유를 보여 준다', async () => {
    server.use(cartWith([{ id: 1, raceCourseId: 1 }]))
    captureCheckout(() => checkoutResult('SOLD_OUT'))
    const user = userEvent.setup()
    renderCart()

    await screen.findByText('2026 용감한 쿠키RUN in 서울 · 10K')
    await user.click(screen.getByRole('button', { name: '결제하기' }))

    expect(await screen.findByText('매진')).toBeInTheDocument()
    expect(
      screen.getByText('남은 자리가 없습니다. 참가 신청서를 다시 작성해 신청할 수 있습니다.'),
    ).toBeInTheDocument()
    expect(push).not.toHaveBeenCalled()
  })

  it('이 기기에서 저장한 신청서가 없는 항목은 결제할 수 없다고 알린다', async () => {
    server.use(cartWith([{ id: 1, raceCourseId: 1 }]))
    renderCart({})

    expect(
      await screen.findByText('신청서를 찾을 수 없어 결제할 수 없어요. 빼고 다시 담아 주세요.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '결제하기' })).toBeDisabled()
  })
})

describe('CartPage 대기열 입장 전 결제 창 (M06-Q)', () => {
  it('창 문구를 보여 주고 닫기를 누르면 창만 닫는다', async () => {
    const user = userEvent.setup()
    server.use(cartWith([{ id: 1, raceCourseId: 1 }]))
    renderCart({ '1': APPLICATION }, true)

    const dialog = await screen.findByRole('alertdialog', { name: '아직 결제할 차례가 아니에요' })
    expect(dialog).toHaveTextContent('대기열에서 차례가 된 뒤 결제할 수 있어요.')
    expect(dialog).toHaveTextContent('줄을 서서 차례를 기다려 주세요.')

    await user.click(screen.getByRole('button', { name: '닫기' }))

    await vi.waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
    expect(push).not.toHaveBeenCalled()
  })

  it('줄 서기를 누르면 담긴 대회 · 종목의 대기열로 간다', async () => {
    const user = userEvent.setup()
    server.use(cartWith([{ id: 1, raceCourseId: 1 }]))
    renderCart({ '1': APPLICATION }, true)

    // 대회 상세를 받아 줄에 대회 이름이 그려진 뒤 누른다
    await screen.findByText(/2026 용감한 쿠키RUN in 서울/)
    await user.click(screen.getByRole('button', { name: '줄 서기' }))

    expect(push).toHaveBeenCalledWith('/races/1/queue?option=1')
  })
})
