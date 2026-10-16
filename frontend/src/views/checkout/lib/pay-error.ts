import { isQueueNotAdmitted } from '@/features/queue-admission'
import { ApiError } from '@/shared/api'

/**
 * 결제(POST /orders/{orderId}/payment)가 안 된 이유.
 * - insufficient: 예치금이 모자람 → 결제 화면에서 충전 안내
 * - timeout: 결제 기한이 지남 · 대기열 입장 시간이 끝남(403 ORDER_QUEUE_NOT_ADMITTED) → 시간 초과 화면(M07-1)
 * - soldout: 남은 자리가 없음 → 자리 마감 화면(M07-2)
 * - failed: 그 밖의 실패 → 결제 실패 화면(M07-3)
 */
export type PayFailure = {
  kind: 'insufficient' | 'timeout' | 'soldout' | 'failed'
  /** 서버가 보낸 문구 */
  message?: string
}

/**
 * 409는 모두 같은 상태 코드라 서버 문구로 나눈다.
 * TODO(api): 오류 코드(PRO-124)가 생기면 문구 대신 code로 나눈다.
 */
export function toPayFailure(error: unknown): PayFailure {
  if (isQueueNotAdmitted(error)) return { kind: 'timeout' }
  if (!(error instanceof ApiError) || error.status !== 409) return { kind: 'failed' }
  const message = error.serverMessage
  const text = message ?? ''
  if (text.includes('부족')) return { kind: 'insufficient', message }
  if (text.includes('기한')) return { kind: 'timeout', message }
  if (text.includes('자리') || text.includes('매진')) return { kind: 'soldout', message }
  return { kind: 'failed', message }
}
