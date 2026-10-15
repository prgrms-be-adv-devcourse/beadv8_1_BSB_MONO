import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import {
  BookingFlowProvider,
  HOLD_SECONDS,
  useBookingFlow,
  type BookingFlowInitial,
} from './booking-flow-store'

function renderBookingFlow(initialState: BookingFlowInitial = {}) {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <BookingFlowProvider initialState={initialState}>{children}</BookingFlowProvider>
  )
  return renderHook(
    () => ({
      holdExpiresAt: useBookingFlow((s) => s.holdExpiresAt),
      startHold: useBookingFlow((s) => s.startHold),
    }),
    { wrapper },
  )
}

describe('startHold', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-10-14T10:00:00+09:00'))
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('마감 전인 자리가 있으면 그대로 둔다', () => {
    const expiresAt = Date.now() + 5000
    const { result } = renderBookingFlow({ holdExpiresAt: expiresAt })

    act(() => result.current.startHold())

    expect(result.current.holdExpiresAt).toBe(expiresAt)
  })

  it('마감이 지난 자리는 지금부터 10분으로 새로 잡는다', () => {
    const { result } = renderBookingFlow({ holdExpiresAt: Date.now() - 1000 })

    act(() => result.current.startHold())

    expect(result.current.holdExpiresAt).toBe(Date.now() + HOLD_SECONDS * 1000)
  })
})
