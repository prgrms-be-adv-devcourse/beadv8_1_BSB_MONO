import { ApiError, type memberModel } from '@/shared/api'
import type { BadgeTone } from '@/shared/ui'

// 판매자 신청(GET /api/v1/member/sellers/me) 값을 화면에 보여 주는 규칙. 신청 결과 · 신청 내용 · 판매자 설정이 같이 쓴다.

/** 상태 배지. 배경색으로만 구분한다(심사 중 Neutral · 반려 Danger · 승인 Success) */
export const SELLER_STATUS: Record<
  memberModel.SellerDtoStatus,
  { label: string; tone: BadgeTone }
> = {
  PENDING: { label: '심사 중', tone: 'neutral' },
  REJECTED: { label: '반려', tone: 'danger' },
  APPROVED: { label: '승인', tone: 'success' },
}

export const SELLER_TYPE_LABEL: Record<memberModel.SellerDtoSellerType, string> = {
  BUSINESS: '사업자',
  INDIVIDUAL: '비사업자',
}

/** '2026-10-02T10:12:08' → '2026. 10. 2.' (글자 그대로, 시간대 변환 없음) */
export function formatSellerDate(value: string): string {
  const [year, month, day] = value.slice(0, 10).split('-').map(Number)
  return `${year}. ${month}. ${day}.`
}

/** 내 신청이 없을 때(404 SELLER_NOT_FOUND)는 오류가 아니라 "아직 신청 전"이다. */
export function isNoApplication(error: unknown): boolean {
  return error instanceof ApiError && error.code === 'SELLER_NOT_FOUND'
}
