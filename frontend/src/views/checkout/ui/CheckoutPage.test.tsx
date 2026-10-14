import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import {
  BookingFlowProvider,
  SAMPLE_PARTICIPANT,
  type BookingFlowInitial,
} from '@/features/booking'

import { CheckoutPage, type CheckoutState } from './CheckoutPage'

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/checkout',
}))

function renderCheckout(initialState: BookingFlowInitial = {}, state?: CheckoutState) {
  return render(
    <BookingFlowProvider initialState={initialState}>
      <CheckoutPage state={state} />
    </BookingFlowProvider>,
  )
}

const timer = () => screen.getByRole('timer')
const pass = (ms: number) => act(() => vi.advanceTimersByTime(ms))

describe('결제 화면', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-10-14T10:00:00+09:00'))
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('앞 단계에서 고른 종목과 참가자 정보를 보여 준다', () => {
    renderCheckout({
      selection: { raceId: 'cookierun', optionId: '5k' },
      participant: { ...SAMPLE_PARTICIPANT, name: '이달림' },
    })

    expect(screen.getByText('2026 용감한 쿠키RUN in 서울 · 5K · 이달림')).toBeInTheDocument()
    // 5K는 35,000원이라 예치금(시안 예시 40,000원)으로 다 내고 충전하지 않는다.
    expect(screen.getByRole('button', { name: '35,000원 결제' })).toBeInTheDocument()
  })

  it('스토어가 비어 있으면 시안 예시 값을 보여 준다', () => {
    renderCheckout()

    expect(screen.getByText('2026 용감한 쿠키RUN in 서울 · 10K · 김러너')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '10,000원 충전하고 결제' })).toBeInTheDocument()
  })

  it('들어오면 10분부터 1초씩 줄어든다', () => {
    renderCheckout()

    expect(timer()).toHaveTextContent('10:00')
    pass(1000)
    expect(timer()).toHaveTextContent('09:59')
    pass(60_000)
    expect(timer()).toHaveTextContent('08:59')
  })

  it('10분이 지나면 시간 초과 화면으로 바뀐다', () => {
    renderCheckout()

    pass(10 * 60 * 1000 - 1000)
    expect(timer()).toHaveTextContent('00:01')
    expect(screen.queryByText('결제 시간이 지났어요')).not.toBeInTheDocument()

    pass(1000)
    expect(screen.getByText('결제 시간이 지났어요')).toBeInTheDocument()
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
  })

  it('이미 잡아 둔 자리가 있으면 그 마감 시각까지 센다', () => {
    renderCheckout({ holdExpiresAt: Date.now() + 5000 })

    expect(timer()).toHaveTextContent('00:05')
    pass(5000)
    expect(screen.getByText('결제 시간이 지났어요')).toBeInTheDocument()
  })

  it('?preview=offline 시안은 시안의 남은 시간에서 센다', () => {
    renderCheckout({}, 'offline')

    expect(timer()).toHaveTextContent('05:10')
    expect(screen.getByRole('checkbox', { name: '[필수] 환불 기한을 확인했어요' })).toBeChecked()
  })

  it('?preview=timeout 시안은 바로 시간 초과 화면을 보여 준다', () => {
    renderCheckout({}, 'timeout')

    expect(screen.getByText('결제 시간이 지났어요')).toBeInTheDocument()
  })
})
