import type { payoutModel } from '@/shared/api'
import type { BadgeTone } from '@/shared/ui'

// 정산 API의 enum 값을 화면 문구로 바꾼다. 문구는 백엔드 enum의 한글 이름과 같다.

type Label = { label: string; tone: BadgeTone }

export const PAYOUT_STATUS: Record<payoutModel.PayoutStatus, Label> = {
  CALCULATED: { label: '지급 예정', tone: 'neutral' },
  HELD: { label: '지급보류', tone: 'warning' },
  PAID: { label: '지급 완료', tone: 'success' },
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
  SALE: '판매',
  REFUND: '환불',
}

/** 관리자 보류 걸기 사유 고르기 */
export const HOLD_REASON_ITEMS: { value: payoutModel.PayoutHoldReason; label: string }[] = [
  { value: 'RACE_CANCELED', label: HOLD_REASON.RACE_CANCELED },
  { value: 'ACCOUNT_NOT_REGISTERED', label: HOLD_REASON.ACCOUNT_NOT_REGISTERED },
]
