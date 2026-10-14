import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { BookingFlowProvider, useBookingFlow } from '@/features/booking'
import { RACE_DETAIL } from '@/entities/race'

import { RaceApplyPage } from './RaceApplyPage'

const push = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push, back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/races/cookierun/apply',
}))

beforeEach(() => push.mockClear())

// 결제 화면이 읽을 스토어 값을 그대로 꺼내 본다.
function StoreProbe() {
  const selection = useBookingFlow((s) => s.selection)
  const participant = useBookingFlow((s) => s.participant)
  return <output data-testid="store">{JSON.stringify({ selection, participant })}</output>
}

describe('참가 신청 · 참가자 정보', () => {
  it('바로 결제를 누르면 고른 종목과 입력한 참가자 정보를 예매 흐름 스토어에 남긴다', async () => {
    const user = userEvent.setup()
    render(
      <BookingFlowProvider>
        <RaceApplyPage race={RACE_DETAIL} optionId="5k" />
        <StoreProbe />
      </BookingFlowProvider>,
    )

    const emergencyName = screen.getByLabelText('비상 연락처 이름')
    await user.clear(emergencyName)
    await user.type(emergencyName, '박가족')
    await user.click(screen.getByRole('radio', { name: '여성' }))
    await user.click(screen.getByRole('checkbox', { name: '[필수] 개인정보 제3자 제공 동의' }))
    await user.click(screen.getByRole('button', { name: '바로 결제' }))

    expect(push).toHaveBeenCalledWith('/checkout')
    const stored = JSON.parse(screen.getByTestId('store').textContent ?? '{}')
    expect(stored.selection).toEqual({ raceId: 'cookierun', optionId: '5k' })
    expect(stored.participant).toMatchObject({
      name: '김러너',
      gender: 'female',
      emergencyName: '박가족',
    })
  })
})
