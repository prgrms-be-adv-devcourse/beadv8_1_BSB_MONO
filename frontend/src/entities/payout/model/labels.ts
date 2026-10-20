import type { payoutModel } from '@/shared/api'
import type { BadgeTone } from '@/shared/ui'

// 정산 API의 enum 값을 화면 문구로 바꾼다. 문구는 백엔드 enum의 한글 이름과 같다.

type Label = { label: string; tone: BadgeTone }

export const PAYOUT_STATUS: Record<payoutModel.PayoutStatus, Label> = {
  CALCULATED: { label: '지급 예정', tone: 'neutral' },
  HELD: { label: '지급보류', tone: 'warning' },
  PAID: { label: '지급 완료', tone: 'success' },
  CANCELED: { label: '취소', tone: 'neutral' },
}

export const PAYOUT_STAGE: Record<payoutModel.PayoutStage, string> = {
  FIRST: '1차',
  FINAL: '최종',
}

export const HOLD_STATUS: Record<payoutModel.PayoutHoldStatus, Label> = {
  HELD: { label: '지급보류', tone: 'warning' },
  RELEASED: { label: '해제', tone: 'neutral' },
}

export const HOLD_REASON: Record<payoutModel.PayoutHoldReason, string> = {
  ACCOUNT_NOT_REGISTERED: '정산 계좌 미등록',
  RACE_CANCELED: '대회 취소 처리',
}

export const RECORD_TYPE: Record<payoutModel.PayoutRecordType, string> = {
  ADVANCE_PAID: '선금',
  SALE: '판매',
  FEE: '수수료',
  ADVANCE_OFFSET: '선지급 차감',
  RECOVERY_REQUESTED: '회수 요청',
  RECEIVABLE_ADDED: '갚을 돈 추가',
  RECOVERY_OFFSET: '회수 상계',
  RECOVERY_RECEIVED: '회수 입금',
  ADVANCE_PAID_CANCELED: '선금 취소',
  SALE_CANCELED: '판매 취소',
  FEE_CANCELED: '수수료 취소',
  ADVANCE_OFFSET_CANCELED: '선지급 차감 취소',
  RECOVERY_OFFSET_CANCELED: '회수 상계 취소',
}

/** 관리자 보류 걸기 사유 고르기 */
export const HOLD_REASON_ITEMS: { value: payoutModel.PayoutHoldReason; label: string }[] = [
  { value: 'RACE_CANCELED', label: HOLD_REASON.RACE_CANCELED },
  { value: 'ACCOUNT_NOT_REGISTERED', label: HOLD_REASON.ACCOUNT_NOT_REGISTERED },
]
