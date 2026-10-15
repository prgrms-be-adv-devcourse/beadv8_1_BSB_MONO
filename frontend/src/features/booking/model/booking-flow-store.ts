'use client'

import { createStore } from 'zustand/vanilla'

import { createStoreContext } from '@/shared/lib'

/** 자리를 잡아 두는 시간 (docs/erd/cash.md 결제 정책 4: 임시 확보부터 10분) */
export const HOLD_SECONDS = 10 * 60

/** 참가자 정보 단계(M05)에서 입력한 값 */
export type BookingParticipant = {
  name: string
  birth: string
  gender: 'male' | 'female'
  shirtSize: string
  emergencyName: string
  emergencyPhone: string
  address: string
}

/** 종목 선택 단계에서 고른 대회·종목. 대회 정보 자체는 서버 데이터라 id만 둔다. */
export type BookingSelection = {
  raceId: string
  optionId: string
}

// TODO(api): 회원 정보 · 지난 신청 값으로 채운다. 지금은 시안 예시 값.
export const SAMPLE_PARTICIPANT: BookingParticipant = {
  name: '김러너',
  birth: '1995.03.02',
  gender: 'male',
  shirtSize: 'M (95)',
  emergencyName: '김보호',
  emergencyPhone: '010-9876-5432',
  address: '서울 성동구 왕십리로 00, 101동 1001호',
}

// 예매 흐름(종목 선택 → 참가자 정보 → 결제) 단계 사이에 넘기는 값.
// 단계를 오가도 값이 남도록 (shop) 레이아웃에서 Provider를 둔다.
// UI 상태만 둔다. 주문 생성과 응답(자리 잡기 마감 시각 등)은 PRO-137에서 TanStack Query로 다룬다.
type BookingFlowState = {
  selection: BookingSelection | null
  participant: BookingParticipant | null
  /** 자리 잡기가 끝나는 시각 (epoch ms). 결제 단계에 들어가기 전에는 null */
  holdExpiresAt: number | null
  /** 참가자 정보 단계를 마치면 고른 종목과 입력값을 남긴다. 새 신청이라 잡아 둔 자리도 비운다. */
  submitApplication: (selection: BookingSelection, participant: BookingParticipant) => void
  /**
   * 결제 단계에 들어갈 때 자리를 잡는다. 아직 마감 전인 자리가 있으면 그대로 두고,
   * 없거나 마감이 지났으면 새로 잡는다.
   * TODO(api): 주문 생성 응답의 마감 시각을 expiresAt으로 넘긴다.
   */
  startHold: (expiresAt?: number) => void
}

/** 테스트·시안 미리보기에서 단계 중간부터 시작할 때 넘기는 값 */
export type BookingFlowInitial = Partial<
  Pick<BookingFlowState, 'selection' | 'participant' | 'holdExpiresAt'>
>

export const { Provider: BookingFlowProvider, useStore: useBookingFlow } = createStoreContext(
  'BookingFlow',
  (initial: BookingFlowInitial = {}) =>
    createStore<BookingFlowState>()((set, get) => ({
      selection: null,
      participant: null,
      holdExpiresAt: null,
      ...initial,
      submitApplication: (selection, participant) =>
        set({ selection, participant, holdExpiresAt: null }),
      startHold: (expiresAt) => {
        // StrictMode에서 두 번 불려도 처음 잡은 마감 시각을 지킨다
        const current = get().holdExpiresAt
        if (current !== null && current > Date.now()) return
        set({ holdExpiresAt: expiresAt ?? Date.now() + HOLD_SECONDS * 1000 })
      },
    })),
)
