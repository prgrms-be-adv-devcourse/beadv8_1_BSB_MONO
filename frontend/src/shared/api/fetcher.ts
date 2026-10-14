import { ApiError } from './api-error'

const isServer = typeof window === 'undefined'

export interface CustomFetchInit extends RequestInit {
  /** 주문 생성·장바구니 결제에 붙이는 Idempotency-Key 헤더 값 (`createIdempotencyKey()`로 만든다) */
  idempotencyKey?: string
}

/**
 * 모든 API 호출이 지나가는 fetch 래퍼. orval이 생성하는 코드도 이 함수를 쓴다.
 *
 * - 브라우저: 상대 경로 /api/... 그대로 → rewrites(로컬·Vercel) 또는 Nginx(AWS)
 * - 서버(RSC): 상대 경로를 쓸 수 없으므로 BACKEND_URL을 앞에 붙인다.
 *   API_MOCKING=on이면 네트워크를 타지 않고 MSW 핸들러에서 바로 응답을 받는다.
 *
 * 응답 본문(RsData)은 그대로 돌려준다. 생성된 타입·MSW 목이 RsData 모양이라 여기서 벗기면 서로 어긋난다.
 * data는 화면 쪽에서 `unwrapData`로 꺼낸다. 2xx가 아니면 ApiError(serverMessage, code)를 던진다.
 */
export async function customFetch<T>(url: string, init: CustomFetchInit = {}): Promise<T> {
  const { idempotencyKey, ...requestInit } = init
  if (idempotencyKey) {
    const headers = new Headers(requestInit.headers)
    headers.set('Idempotency-Key', idempotencyKey)
    requestInit.headers = headers
  }

  const response = isServer ? await serverFetch(url, requestInit) : await fetch(url, requestInit)

  const body = await parseBody(response)
  if (!response.ok) throw new ApiError(response.status, body, url)
  return body as T
}

async function serverFetch(url: string, init: RequestInit): Promise<Response> {
  const { getServerEnv } = await import('@/shared/config')
  const env = getServerEnv()
  const request = new Request(new URL(url, env.BACKEND_URL), init)

  if (env.API_MOCKING === 'on') {
    const { resolveMockResponse } = await import('@/shared/mocks')
    return resolveMockResponse(request)
  }

  // TODO(auth): 인증 방식이 정해지면 next/headers의 cookies()로 쿠키를 전달한다.
  return fetch(request)
}

async function parseBody(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined
  const type = response.headers.get('content-type') ?? ''
  return type.includes('application/json') ? response.json() : response.text()
}
