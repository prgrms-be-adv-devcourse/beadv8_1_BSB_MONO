import { ApiError, type memberModel } from '@/shared/api'

// 관리자 판매자 신청 API(PRO-129)와 화면을 이어 주는 곳. 오류 문구는 서버가 주는 code로 정한다.

export const ADMIN_SELLER_ERRORS = {
  notAdmin: '관리자만 이용할 수 있어요.',
  alreadyReviewed: '이미 심사한 신청이에요. 목록을 새로 불러왔어요.',
  notFound: '신청을 찾을 수 없어요. 목록을 새로 불러왔어요.',
  reasonRequired: '반려 사유를 입력해 주세요.',
  reviewFailed: '처리하지 못했어요. 잠시 후 다시 시도해 주세요.',
  loadFailed: '신청 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.',
} as const

const BY_CODE: Record<string, string> = {
  MEMBER_NOT_ADMIN: ADMIN_SELLER_ERRORS.notAdmin,
  SELLER_ALREADY_REVIEWED: ADMIN_SELLER_ERRORS.alreadyReviewed,
  SELLER_NOT_FOUND: ADMIN_SELLER_ERRORS.notFound,
}

/** 승인·반려 실패 문구. 코드를 모르는 오류는 서버 문구, 그것도 없으면 일반 문구. */
export function reviewErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return (
      (error.code && BY_CODE[error.code]) || error.serverMessage || ADMIN_SELLER_ERRORS.reviewFailed
    )
  }
  return ADMIN_SELLER_ERRORS.reviewFailed
}

/** 이미 처리됐거나 없는 신청이면 목록이 낡았다는 뜻이라 다시 불러온다. */
export function shouldRefetchAfter(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    (error.code === 'SELLER_ALREADY_REVIEWED' || error.code === 'SELLER_NOT_FOUND')
  )
}

export function loadErrorMessage(error: unknown): string {
  return error instanceof ApiError && error.code === 'MEMBER_NOT_ADMIN'
    ? ADMIN_SELLER_ERRORS.notAdmin
    : ADMIN_SELLER_ERRORS.loadFailed
}

/** 2026-10-05T21:25:35 → 10/5 */
export function toShortDate(iso: string): string {
  const [, month, day] = iso.slice(0, 10).split('-')
  return `${Number(month)}/${Number(day)}`
}

/** 목록·상세에 보일 이름. 사업자는 상호, 비사업자는 상호가 없으니 대표자(본인 성명). */
export function sellerDisplayName(seller: memberModel.SellerDto): string {
  return seller.businessName || seller.representativeName
}

export function sellerKindLabel(seller: memberModel.SellerDto): string {
  return seller.sellerType === 'BUSINESS' ? '사업자' : '비사업자'
}
