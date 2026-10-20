import type { Route } from 'next'

import { ApiError } from '@/shared/api'

/** 대기열에 입장 중이 아니라 [결제하기]를 할 수 없음 (줄 서는 중 · 입장 시간이 끝남 · 대기열을 거치지 않음) */
export const QUEUE_NOT_ADMITTED_CODE = 'ORDER_QUEUE_NOT_ADMITTED'

/** 주문 생성 · 결제가 대기열 입장 전이라 막혔는지 (403). 서버 문구가 아니라 code로 판단한다. */
export function isQueueNotAdmitted(error: unknown) {
  return error instanceof ApiError && error.code === QUEUE_NOT_ADMITTED_CODE
}

/** 입장 시간 만료 결과 화면(M07-1). 대회를 알면 그 대회 상세로 돌아가게 넘긴다. */
export function admissionExpiredHref(raceId: number | undefined): Route {
  return (
    raceId === undefined || !Number.isInteger(raceId)
      ? '/checkout?result=timeout'
      : `/checkout?result=timeout&raceId=${raceId}`
  ) as Route
}

/** 대기열에 등록하지 못한 이유(등록 오류의 HTTP 상태)에 맞는 안내 문구 */
export function skippedNotice(status: number) {
  switch (status) {
    case 403:
      return {
        title: '지금은 접수 기간이 아니에요',
        description: '접수 기간에 다시 들어와 주세요.',
      }
    case 404:
      return {
        title: '대회 정보를 찾지 못했어요',
        description: '대회 상세로 돌아가 다시 시도해 주세요.',
      }
    case 503:
      return {
        title: '대기열이 잠시 불안정해요',
        description: '잠시 뒤에 다시 시도해 주세요.',
      }
    default:
      return {
        title: '지금은 입장할 수 없어요',
        description: '대회 상세로 돌아가 다시 시도해 주세요.',
      }
  }
}
