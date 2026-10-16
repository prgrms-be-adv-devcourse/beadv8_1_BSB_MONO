/**
 * 로그인 상태와 Access Token을 브라우저 메모리(이 모듈의 변수)에만 둔다.
 * localStorage·sessionStorage·일반 쿠키에 두지 않아 XSS로 읽히는 범위를 줄인다.
 * 새로고침하면 사라지므로 앱 시작 때 restoreSession()으로 재발급해 되살린다.
 * Refresh Token은 서버가 내려 주는 httpOnly 쿠키라 자바스크립트가 만지지 않는다.
 */

/**
 * - pending: 앱을 막 열어 재발급으로 로그인 상태를 확인하는 중 (서버 렌더링도 이 값)
 * - authenticated: Access Token이 메모리에 있다
 * - anonymous: 로그인하지 않았거나 로그아웃·재발급 실패로 토큰을 비웠다
 */
export type AuthStatus = 'pending' | 'authenticated' | 'anonymous'

let accessToken: string | null = null
let status: AuthStatus = 'pending'
/** 토큰을 비울 때마다 늘린다. 로그아웃 전에 출발한 재발급 응답이 늦게 와서 토큰을 되살리지 않게 비교한다. */
let generation = 0

const statusListeners = new Set<() => void>()
const expiredListeners = new Set<() => void>()

export function getAccessToken(): string | null {
  return accessToken
}

export function getAuthStatus(): AuthStatus {
  return status
}

export function getAuthGeneration(): number {
  return generation
}

/** 로그인·재발급으로 받은 Access Token을 메모리에 둔다. */
export function setAccessToken(token: string): void {
  accessToken = token
  setStatus('authenticated')
}

/** 토큰을 비우고 로그아웃 상태로 바꾼다 (로그아웃, 재발급 실패). */
export function clearAccessToken(): void {
  accessToken = null
  generation += 1
  setStatus('anonymous')
}

/**
 * 재발급이 실패해 로그인이 끊겼을 때 부른다. 로그인 중이던 사용자만 "세션 만료"로 알린다.
 * (앱 시작 때 재발급이 실패한 것은 원래 로그인하지 않은 사용자라 알리지 않는다)
 */
export function expireSession(): void {
  const wasAuthenticated = status === 'authenticated'
  clearAccessToken()
  if (wasAuthenticated) expiredListeners.forEach((listener) => listener())
}

/** React의 useSyncExternalStore에 넘길 구독 함수 */
export function subscribeAuthStatus(listener: () => void): () => void {
  statusListeners.add(listener)
  return () => statusListeners.delete(listener)
}

/** 로그인 중에 재발급이 실패하면 불린다 (로그인 화면으로 보낼 때 쓴다). */
export function onSessionExpired(listener: () => void): () => void {
  expiredListeners.add(listener)
  return () => expiredListeners.delete(listener)
}

function setStatus(next: AuthStatus) {
  if (status === next) return
  status = next
  statusListeners.forEach((listener) => listener())
}

/** 테스트끼리 상태가 섞이지 않게 처음 상태로 되돌린다. 앱 코드에서 쓰지 않는다. */
export function resetAuthStateForTesting(): void {
  accessToken = null
  status = 'pending'
  generation = 0
  statusListeners.clear()
  expiredListeners.clear()
}
