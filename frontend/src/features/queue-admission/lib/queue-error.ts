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
