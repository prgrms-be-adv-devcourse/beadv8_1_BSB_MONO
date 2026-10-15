import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { BookingFlowProvider, useBookingFlow, type BookingFlowInitial } from '@/features/booking'
import { RACE_DETAIL, type RaceOption } from '@/entities/race'
import { orderMock, server } from '@/shared/testing'

import { RaceApplyPage } from './RaceApplyPage'

const push = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push, back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/races/1/apply',
}))

// 키트 · 티셔츠가 있는 종목 (상품 API의 종목 ID는 숫자다)
const COURSE: RaceOption = {
  ...RACE_DETAIL.options[0],
  id: '3',
  kitIncluded: true,
  shirtSizes: ['S', 'M', 'L'],
}
const RACE = { ...RACE_DETAIL, options: [COURSE] }

beforeEach(() => push.mockClear())

// 결제 · 장바구니 화면이 읽을 스토어 값을 그대로 꺼내 본다.
function StoreProbe() {
  const selection = useBookingFlow((s) => s.selection)
  const applications = useBookingFlow((s) => s.applications)
  return <output data-testid="store">{JSON.stringify({ selection, applications })}</output>
}

function renderPage(initialState: BookingFlowInitial = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <BookingFlowProvider initialState={initialState}>
        <RaceApplyPage race={RACE} option={COURSE} />
        <StoreProbe />
      </BookingFlowProvider>
    </QueryClientProvider>,
  )
}

/** 참가 신청 요청 본문을 받아 두고, 응답은 명세 예시(참가 신청 ID 1)로 돌려준다 */
function captureApply() {
  const bodies: unknown[] = []
  server.use(
    http.post('*/api/v1/order/participations', async ({ request }) => {
      bodies.push(await request.json())
      return HttpResponse.json(orderMock.getApplyResponseMock(), { status: 201 })
    }),
  )
  return bodies
}

function captureAddItem() {
  const added: unknown[] = []
  server.use(
    http.post('*/api/v1/order/cart/items', async ({ request }) => {
      added.push(await request.json())
      return HttpResponse.json(orderMock.getAddItemResponseMock())
    }),
  )
  return added
}

const cartWith = (items: { id: number; createDate: string; raceCourseId: number }[]) =>
  orderMock.getFindCartMockHandler({
    status: 200,
    message: '장바구니를 조회했습니다.',
    data: { memberId: 1, items },
  })

const agree = (user: ReturnType<typeof userEvent.setup>) =>
  user.click(screen.getByRole('checkbox', { name: '[필수] 개인정보 제3자 제공 동의' }))

describe('참가 신청 · 참가자 정보', () => {
  it('지난 신청 값으로 채우고, 바로 결제를 누르면 참가 신청을 저장한 뒤 결제로 간다', async () => {
    const bodies = captureApply()
    const user = userEvent.setup()
    renderPage()

    // 명세 예시의 지난 신청 값
    expect(await screen.findByLabelText('이름')).toHaveValue('김러너')
    expect(screen.getByLabelText('키트 배송 우편번호')).toHaveValue('04700')

    const emergencyName = screen.getByLabelText('비상 연락처 이름')
    await user.clear(emergencyName)
    await user.type(emergencyName, '박가족')
    await user.click(screen.getByRole('radio', { name: '여성' }))
    await agree(user)
    await user.click(screen.getByRole('button', { name: '바로 결제' }))

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/checkout'))
    expect(bodies[0]).toEqual({
      raceCourseId: 3,
      thirdPartyConsent: true,
      participant: {
        name: '김러너',
        birth: '1994-06-25',
        gender: 'FEMALE',
        phone: '010-1234-5678',
        emergencyContactName: '박가족',
        emergencyContactPhone: '010-9876-5432',
        tshirtSize: 'L',
        shippingAddress: {
          recipientName: '김러너',
          recipientPhone: '010-1234-5678',
          zipcode: '04700',
          address: '서울 성동구 왕십리로 00',
          addressDetail: '101동 1001호',
        },
      },
    })
    const stored = JSON.parse(screen.getByTestId('store').textContent ?? '{}')
    expect(stored.selection).toEqual({ raceId: RACE_DETAIL.id, optionId: '3' })
    expect(stored.applications['3']).toMatchObject({
      participationId: 1,
      participantName: '김러너',
    })
  })

  it('지난 신청이 없으면 회원 정보의 휴대폰 번호만 채운다', async () => {
    server.use(
      orderMock.getFindLatestParticipantMockHandler({
        status: 200,
        message: '이전에 신청한 참가자 정보가 없습니다.',
        data: null,
      }),
    )
    renderPage()

    expect(await screen.findByLabelText('휴대폰 번호')).toHaveValue('010-1234-5678')
    expect(screen.getByLabelText('이름')).toHaveValue('')
  })

  it('장바구니가 비어 있으면 참가 신청을 저장하고 그 종목을 바로 담는다', async () => {
    captureApply()
    const added = captureAddItem()
    server.use(cartWith([]))
    const user = userEvent.setup()
    renderPage()

    await screen.findByLabelText('이름')
    await agree(user)
    await user.click(screen.getByRole('button', { name: '장바구니 담기' }))

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/cart'))
    expect(added).toEqual([{ raceCourseId: 3 }])
    expect(screen.queryByText('장바구니의 대회를 바꿀까요?')).not.toBeInTheDocument()
  })

  it('장바구니에 다른 대회가 담겨 있으면 바꿀지 묻고, 바꾸면 그 항목을 빼고 담는다', async () => {
    captureApply()
    const added = captureAddItem()
    const removed: string[] = []
    server.use(
      cartWith([{ id: 9, createDate: '2026-10-12T10:15:30', raceCourseId: 1 }]),
      http.delete('*/api/v1/order/cart/items/:cartItemId', ({ params }) => {
        removed.push(String(params.cartItemId))
        return HttpResponse.json(orderMock.getRemoveItemResponseMock())
      }),
    )
    const user = userEvent.setup()
    renderPage()

    await screen.findByLabelText('이름')
    await agree(user)
    await user.click(screen.getByRole('button', { name: '장바구니 담기' }))

    expect(await screen.findByText('장바구니의 대회를 바꿀까요?')).toBeInTheDocument()
    expect(added).toHaveLength(0)

    await user.click(screen.getByRole('button', { name: '바꾸기' }))

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/cart'))
    expect(removed).toEqual(['9'])
    expect(added).toEqual([{ raceCourseId: 3 }])
  })

  it('같은 대회의 종목이 담겨 있으면 묻지 않고 담는다', async () => {
    captureApply()
    captureAddItem()
    server.use(cartWith([{ id: 2, createDate: '2026-10-12T10:15:30', raceCourseId: 3 }]))
    const user = userEvent.setup()
    renderPage()

    await screen.findByLabelText('이름')
    await agree(user)
    await user.click(screen.getByRole('button', { name: '장바구니 담기' }))

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/cart'))
  })

  it('같은 대회에 이미 신청했으면(409) 서버 문구를 보여 주고 넘어가지 않는다', async () => {
    server.use(
      http.post('*/api/v1/order/participations', () =>
        HttpResponse.json(
          { status: 409, message: '이미 이 대회에 신청했습니다.', data: null },
          { status: 409 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderPage()

    await screen.findByLabelText('이름')
    await agree(user)
    await user.click(screen.getByRole('button', { name: '바로 결제' }))

    expect(await screen.findByText('이미 이 대회에 신청했습니다.')).toBeInTheDocument()
    expect(push).not.toHaveBeenCalled()
  })

  it('나이 제한 밖이면(400) 서버 문구를 보여 준다', async () => {
    const message = '참가할 수 있는 나이가 아닙니다(대회 당일 만 14세 이상, 신청자 만 12세).'
    server.use(
      http.post('*/api/v1/order/participations', () =>
        HttpResponse.json({ status: 400, message, data: null }, { status: 400 }),
      ),
    )
    const user = userEvent.setup()
    renderPage()

    await screen.findByLabelText('이름')
    await agree(user)
    await user.click(screen.getByRole('button', { name: '바로 결제' }))

    expect(await screen.findByText(message)).toBeInTheDocument()
  })

  it('빈 칸이 있으면 보내지 않고 칸 아래에 문구를 보여 준다', async () => {
    const bodies = captureApply()
    server.use(
      orderMock.getFindLatestParticipantMockHandler({ status: 200, message: '', data: null }),
    )
    const user = userEvent.setup()
    renderPage()

    await screen.findByLabelText('이름')
    await agree(user)
    await user.click(screen.getByRole('button', { name: '바로 결제' }))

    // 참가자 이름 · 비상 연락처 이름
    expect(await screen.findAllByText('이름을 입력해 주세요.')).toHaveLength(2)
    expect(screen.getByText('키트를 받을 주소를 입력해 주세요.')).toBeInTheDocument()
    expect(bodies).toHaveLength(0)
  })

  it('이번에 이미 저장한 종목이면 새로 신청하지 않고 참가자 정보를 고친다', async () => {
    const bodies = captureApply()
    let changedId: string | undefined
    server.use(
      http.put('*/api/v1/order/participations/:participationId/participant', ({ params }) => {
        changedId = String(params.participationId)
        return HttpResponse.json(orderMock.getChangeParticipantResponseMock())
      }),
    )
    const user = userEvent.setup()
    renderPage({
      applications: {
        '3': {
          raceId: RACE_DETAIL.id,
          optionId: '3',
          participationId: 7,
          participantName: '김러너',
        },
      },
    })

    await screen.findByLabelText('이름')
    await agree(user)
    await user.click(screen.getByRole('button', { name: '바로 결제' }))

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/checkout'))
    expect(changedId).toBe('7')
    expect(bodies).toHaveLength(0)
  })
})
