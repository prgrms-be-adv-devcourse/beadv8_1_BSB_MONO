import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'

import {
  BookingFlowProvider,
  SAMPLE_PARTICIPANT,
  useBookingFlow,
  type BookingFlowInitial,
} from './booking-flow-store'

function renderBookingFlow(initialState: BookingFlowInitial = {}) {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <BookingFlowProvider initialState={initialState}>{children}</BookingFlowProvider>
  )
  return renderHook(
    () => ({
      selection: useBookingFlow((s) => s.selection),
      applications: useBookingFlow((s) => s.applications),
      submitApplication: useBookingFlow((s) => s.submitApplication),
    }),
    { wrapper },
  )
}

describe('submitApplication', () => {
  it('고른 종목을 남기고, 다른 종목에 저장해 둔 참가 신청은 그대로 둔다', () => {
    const earlier = { raceId: '2', optionId: '7', participationId: 3, participantName: '김러너' }
    const { result } = renderBookingFlow({ applications: { '7': earlier } })

    act(() =>
      result.current.submitApplication(
        { raceId: '1', optionId: '1', participationId: 11, participantName: '김러너' },
        SAMPLE_PARTICIPANT,
      ),
    )

    expect(result.current.selection).toEqual({ raceId: '1', optionId: '1' })
    expect(result.current.applications).toEqual({
      '7': earlier,
      '1': { raceId: '1', optionId: '1', participationId: 11, participantName: '김러너' },
    })
  })
})
