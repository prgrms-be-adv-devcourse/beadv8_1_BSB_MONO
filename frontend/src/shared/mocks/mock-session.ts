/**
 * 가짜 응답 모드(MSW)에서만 쓰는 가짜 Refresh Token 쿠키. 실제 앱 코드는 이 파일을 쓰지 않는다.
 * 가짜 로그인이 쿠키를 심고, 가짜 로그아웃이 지운다. 가짜 재발급은 요청에 이 쿠키가 있을 때만 성공한다.
 *
 * 로그인 상태를 서버 메모리가 아니라 브라우저 쿠키에 두므로 브라우저마다 따로 유지되고,
 * 개발 서버 재시작·HMR·여러 서버 인스턴스(Vercel)에서도 사라지지 않는다.
 *
 * 이름은 진짜 쿠키(refreshToken)와 다르게 둔다. 같은 주소에서 API_MOCKING을 껐다 켜도
 * 가짜 쿠키가 진짜 백엔드로 가거나, 진짜 쿠키를 가짜 로그인으로 착각하지 않게 하기 위해서다.
 * 속성은 진짜와 같되 Secure만 뺀다. 로컬 http(휴대폰에서 http://<내부 IP> 접속 등)에서도 쿠키가 저장돼야 한다.
 */
export const MOCK_REFRESH_COOKIE_NAME = 'mockRefreshToken'

const COOKIE_ATTRIBUTES = 'Path=/api/v1/auth; HttpOnly; SameSite=Lax'

/** 진짜 서버처럼 14일 */
const MAX_AGE_SECONDS = 60 * 60 * 24 * 14

/** 새 가짜 Refresh Token 쿠키를 심는 Set-Cookie 값 */
export function issueMockRefreshCookie(): string {
  const value = `mock-refresh-${crypto.randomUUID()}`
  return `${MOCK_REFRESH_COOKIE_NAME}=${value}; Max-Age=${MAX_AGE_SECONDS}; ${COOKIE_ATTRIBUTES}`
}

/** 가짜 Refresh Token 쿠키를 지우는 Set-Cookie 값 */
export function clearMockRefreshCookie(): string {
  return `${MOCK_REFRESH_COOKIE_NAME}=; Max-Age=0; ${COOKIE_ATTRIBUTES}`
}

/**
 * 테스트에서 로그인된 채로 요청할 때 붙이는 Cookie 헤더 값.
 * Node(테스트·/api 라우트)의 MSW는 쿠키를 저장해 두지 않으므로 요청마다 직접 붙인다.
 */
export const MOCK_LOGGED_IN_COOKIE = `${MOCK_REFRESH_COOKIE_NAME}=mock-refresh-for-test`

/**
 * 가짜 로그인 표시 쿠키. 로그인했는지만 서버(proxy.ts)가 알 수 있게 Path=/ 로 둔다.
 * 진짜 Refresh Token 쿠키는 Path=/api/v1/auth라 화면 요청에 실리지 않아 서버가 로그인 여부를 모른다.
 * 가짜 응답 모드에서는 이 표시로 비로그인 요청을 화면을 그리기 전에 로그인 화면으로 보낸다.
 * 값에는 아무 정보도 없고(1), 권한 판단에 쓰지 않는다. 가짜 응답 모드 전용이다.
 */
export const MOCK_LOGIN_MARK_COOKIE_NAME = 'mockLoggedIn'

const MARK_ATTRIBUTES = 'Path=/; HttpOnly; SameSite=Lax'

export function issueMockLoginMarkCookie(): string {
  return `${MOCK_LOGIN_MARK_COOKIE_NAME}=1; Max-Age=${MAX_AGE_SECONDS}; ${MARK_ATTRIBUTES}`
}

export function clearMockLoginMarkCookie(): string {
  return `${MOCK_LOGIN_MARK_COOKIE_NAME}=; Max-Age=0; ${MARK_ATTRIBUTES}`
}
