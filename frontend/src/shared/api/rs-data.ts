/**
 * 백엔드 공통 응답 감싸개 (common의 RsData). 성공·실패 모두 이 모양이다.
 * - 성공: data에 결과
 * - 실패: data는 null, message에 사용자에게 보여 줄 문구
 */
export interface RsData<T = unknown> {
  status: number
  message: string
  data: T
  /** 오류 코드 (PRO-124에서 생길 예정. 그 전까지는 오지 않는다) */
  code?: string
}

/**
 * 생성된 요청 함수는 RsData 전체를 돌려준다. 화면에서는 data만 쓰므로 여기서 꺼낸다.
 * TanStack Query에서는 `select: unwrapData`로 쓴다.
 */
export function unwrapData<T>(response: { status?: number; message?: string; data?: T | null }): T {
  if (response.data === undefined || response.data === null) {
    throw new Error('응답에 data가 없습니다.')
  }
  return response.data
}

/** 실패 응답 본문이 RsData 모양이면 돌려준다. 게이트웨이·프록시 오류처럼 다른 모양이면 undefined. */
export function asRsData(body: unknown): RsData<null> | undefined {
  if (typeof body !== 'object' || body === null) return undefined
  const { status, message, code } = body as Record<string, unknown>
  if (typeof status !== 'number' || typeof message !== 'string') return undefined
  return { status, message, data: null, code: typeof code === 'string' ? code : undefined }
}
