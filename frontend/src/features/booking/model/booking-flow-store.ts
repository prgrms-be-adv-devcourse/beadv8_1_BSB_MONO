'use client'

import { createStore } from 'zustand/vanilla'

import type { orderModel } from '@/shared/api'
import { createStoreContext } from '@/shared/lib'

/** 자리를 잡아 두는 시간 (docs/erd/cash.md 결제 정책 4: 임시 확보부터 10분) */
export const HOLD_SECONDS = 10 * 60

/** 참가자 정보 단계(M05)에서 입력한 값. 주문 API의 참가자 정보 요청과 같은 모양이다. */
export type BookingParticipant = orderModel.ParticipantInfoRequestDto

/** 종목 선택 단계에서 고른 대회·종목. 대회 정보 자체는 서버 데이터라 id만 둔다. */
export type BookingSelection = {
  raceId: string
  /** 종목 ID (raceCourseId) */
  optionId: string
}

/**
 * 저장한 참가 신청. 장바구니 API는 종목 ID만 돌려주므로, 장바구니 결제에 보낼 참가 신청 ID와
 * 장바구니에 그릴 대회 ID·참가자 이름을 종목별로 남겨 둔다.
 */
export type BookingApplication = BookingSelection & {
  participationId: number
  participantName: string
}

// 시안 미리보기(?preview=)에서 보여 주는 참가자 예시 값
export const SAMPLE_PARTICIPANT: BookingParticipant = {
  name: '김러너',
  birth: '1995-03-02',
  gender: 'MALE',
  phone: '010-1234-5678',
  tshirtSize: 'M',
  emergencyContactName: '김보호',
  emergencyContactPhone: '010-9876-5432',
  shippingAddress: {
    recipientName: '김러너',
    recipientPhone: '010-1234-5678',
    zipcode: '04700',
    address: '서울 성동구 왕십리로 00',
    addressDetail: '101동 1001호',
  },
}

// 예매 흐름(종목 선택 → 참가자 정보 → 장바구니 · 결제) 단계 사이에 넘기는 값.
// 단계를 오가도 값이 남도록 (shop) 레이아웃에서 Provider를 둔다.
// UI 상태만 둔다. 주문 생성과 응답(자리 잡기 마감 시각 등)은 TanStack Query로 다룬다.
type BookingFlowState = {
  selection: BookingSelection | null
  participant: BookingParticipant | null
  /** 종목 ID → 저장한 참가 신청 */
  applications: Record<string, BookingApplication>
  /** 자리 잡기가 끝나는 시각 (epoch ms). 결제 단계에 들어가기 전에는 null */
  holdExpiresAt: number | null
  /** 참가 신청을 저장하면 고른 종목과 입력값, 참가 신청 ID를 남긴다. 새 신청이라 잡아 둔 자리도 비운다. */
  submitApplication: (application: BookingApplication, participant: BookingParticipant) => void
  /**
   * 결제 단계에 들어갈 때 자리를 잡는다. 아직 마감 전인 자리가 있으면 그대로 두고,
   * 없거나 마감이 지났으면 새로 잡는다.
   * TODO(api): 주문 생성 응답의 마감 시각을 expiresAt으로 넘긴다.
   */
  startHold: (expiresAt?: number) => void
}

/** 테스트·시안 미리보기에서 단계 중간부터 시작할 때 넘기는 값 */
export type BookingFlowInitial = Partial<
  Pick<BookingFlowState, 'selection' | 'participant' | 'applications' | 'holdExpiresAt'>
>

export const { Provider: BookingFlowProvider, useStore: useBookingFlow } = createStoreContext(
  'BookingFlow',
  (initial: BookingFlowInitial = {}) =>
    createStore<BookingFlowState>()((set, get) => ({
      selection: null,
      participant: null,
      applications: {},
      holdExpiresAt: null,
      ...initial,
      submitApplication: (application, participant) =>
        set((s) => ({
          selection: { raceId: application.raceId, optionId: application.optionId },
          participant,
          applications: { ...s.applications, [application.optionId]: application },
          holdExpiresAt: null,
        })),
      startHold: (expiresAt) => {
        // StrictMode에서 두 번 불려도 처음 잡은 마감 시각을 지킨다
        const current = get().holdExpiresAt
        if (current !== null && current > Date.now()) return
        set({ holdExpiresAt: expiresAt ?? Date.now() + HOLD_SECONDS * 1000 })
      },
    })),
)
