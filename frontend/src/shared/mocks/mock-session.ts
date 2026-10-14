/**
 * 가짜 응답 모드(MSW)에서만 쓰는 "가짜 로그인됨" 표시. 실제 앱 코드는 이 파일을 쓰지 않는다.
 * 가짜 로그인이 성공하면 켜고, 가짜 로그아웃이면 끈다. 가짜 재발급은 이 표시가 있을 때만 성공한다.
 * (진짜 서버의 Refresh Token 쿠키 역할을 흉내 낸다)
 *
 * sessionStorage가 있으면 거기에 두고, 없으면(Node에서 도는 /api 라우트 등) 이 모듈의 변수에 둔다.
 */
const KEY = 'crewrun:mock-logged-in'

let memoryFlag = false

function storage(): Storage | null {
  try {
    return typeof sessionStorage === 'undefined' ? null : sessionStorage
  } catch {
    return null
  }
}

export function isMockLoggedIn(): boolean {
  const store = storage()
  return store ? store.getItem(KEY) === '1' : memoryFlag
}

export function setMockLoggedIn(loggedIn: boolean): void {
  const store = storage()
  if (!store) {
    memoryFlag = loggedIn
    return
  }
  if (loggedIn) store.setItem(KEY, '1')
  else store.removeItem(KEY)
}

/** 테스트끼리 표시가 섞이지 않게 지운다. 앱 코드에서 쓰지 않는다. */
export function resetMockSessionForTesting(): void {
  memoryFlag = false
  storage()?.removeItem(KEY)
}
