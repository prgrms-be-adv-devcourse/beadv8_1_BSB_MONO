import {
  clearAccessToken,
  expireSession,
  getAccessToken,
  getAuthGeneration,
  getAuthStatus,
  resetAuthStateForTesting,
  setAccessToken,
} from './access-token'
import type { RsDataTokenReissueResponseDto } from './generated/member/member.schemas'

/**
 * Refresh Token 쿠키(httpOnly, Path=/api/v1/auth)로 새 Access Token을 받는 주소.
 * 생성된 memberApi.reissue는 customFetch를 지나므로 여기서 쓰면 customFetch ↔ 재발급이 서로를 부른다.
 * 그래서 재발급만 fetch로 직접 보낸다.
 */
const REISSUE_URL = '/api/v1/auth/reissue'

/**
 * 재발급을 탭 사이에서 한 번에 하나씩 보내기 위한 Web Locks 이름.
 * 아래 single-flight(reissuing)는 모듈 변수라 탭마다 따로 있다. 탭 두 개가 동시에 401을 받으면
 * 둘 다 같은 Refresh Token으로 재발급을 보내고, 늦게 간 쪽이 재사용(탈취)으로 보여 로그인이 끊긴다.
 * 쿠키는 탭끼리 공유되므로 차례대로만 보내면 뒤 탭은 앞 탭이 받아 온 새 쿠키로 보낸다.
 */
const REISSUE_LOCK_NAME = 'auth:reissue'

let reissuing: Promise<string | null> | null = null
let restoring: Promise<void> | null = null

/**
 * Access Token을 재발급한다. 성공하면 새 토큰, 서버가 거절하면(401 등) 토큰을 비우고 null.
 *
 * single-flight: 이미 재발급 중이면 새로 보내지 않고 그 결과를 같이 기다린다.
 * Refresh Token은 재발급마다 바뀌므로(회전) 여러 요청이 각자 재발급하면 이미 쓴 토큰을 다시 보내게 되고,
 * 백엔드는 이를 탈취로 보고 로그인을 끊는다.
 *
 * 네트워크 오류(서버에 닿지 못함)는 로그인이 끊긴 것이 아니므로 토큰을 비우지 않고 오류를 그대로 던진다.
 */
export function reissueAccessToken(): Promise<string | null> {
  reissuing ??= requestReissue().finally(() => {
    reissuing = null
  })
  return reissuing
}

async function requestReissue(): Promise<string | null> {
  const startedAt = getAuthGeneration()
  const response = await withReissueLock(() =>
    fetch(REISSUE_URL, { method: 'POST', credentials: 'same-origin' }),
  )
  // 잠금이나 재발급을 기다리는 사이에 로그아웃했다면 결과를 버린다
  if (getAuthGeneration() !== startedAt) return null

  const token = response.ok ? await readAccessToken(response) : null
  if (!token) {
    expireSession()
    return null
  }
  setAccessToken(token)
  return token
}

/**
 * 다른 탭의 재발급이 끝날 때까지 기다렸다가 보낸다. 새 쿠키는 응답 헤더를 받을 때 저장되므로
 * fetch가 끝나면 잠금을 푼다. navigator.locks가 없는 환경(구형 브라우저, 서버, jsdom)에서는 잠금 없이 보낸다.
 */
async function withReissueLock(send: () => Promise<Response>): Promise<Response> {
  const locks =
    typeof navigator === 'undefined' ? undefined : (navigator.locks as LockManager | undefined)
  if (!locks) return send()
  return await locks.request(REISSUE_LOCK_NAME, send)
}

async function readAccessToken(response: Response): Promise<string | null> {
  try {
    const body = (await response.json()) as RsDataTokenReissueResponseDto
    const token = body.data?.accessToken
    return typeof token === 'string' && token !== '' ? token : null
  } catch {
    return null
  }
}

/**
 * 요청이 401을 받았을 때 다시 보낼 토큰을 구한다.
 * 그 요청을 보낸 뒤 다른 요청이 이미 재발급을 끝냈다면(토큰이 바뀌었다면) 재발급하지 않고 새 토큰을 쓴다.
 */
export function tokenForRetry(sentToken: string | null): Promise<string | null> {
  const current = getAccessToken()
  if (current && current !== sentToken) return Promise.resolve(current)
  return reissueAccessToken()
}

/**
 * 새로고침 뒤 로그인 상태를 되살린다. 앱 시작 때 한 번 부른다(여러 번 불러도 한 번만 재발급).
 * 재발급이 실패하거나 서버에 닿지 못하면 로그아웃 상태로 둔다.
 */
export function restoreSession(): Promise<void> {
  if (getAuthStatus() !== 'pending') return Promise.resolve()
  restoring ??= reissueAccessToken().then(
    (token) => {
      if (!token && getAuthStatus() === 'pending') clearAccessToken()
    },
    () => {
      if (getAuthStatus() === 'pending') clearAccessToken()
    },
  )
  return restoring
}

/** 테스트끼리 상태가 섞이지 않게 처음 상태로 되돌린다. 앱 코드에서 쓰지 않는다. */
export function resetAuthSessionForTesting(): void {
  reissuing = null
  restoring = null
  resetAuthStateForTesting()
}
