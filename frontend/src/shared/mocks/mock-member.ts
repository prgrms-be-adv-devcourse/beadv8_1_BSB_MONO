/**
 * 가짜 응답 모드에서 "방금 가입한 회원"과 "시연용 기존 회원(runner1)"을 가르는 쿠키.
 *
 * 가짜 API는 Vercel 서버 함수에서 돌아 요청마다 다른 인스턴스일 수 있어, 가입 정보를 서버 메모리에 두면 사라진다.
 * 그래서 로그인 상태(mock-session.ts)처럼 요청에 따라오는 쿠키에 둔다. 값은 가입 때 넣은 프로필(JSON)이고 비밀번호는 담지 않는다.
 *
 * - mockJoined (Path=/api/v1/auth): 가입이 심고, 로그인이 읽는다. 가입 → 로그인 사이에만 쓴다.
 * - mockMember (Path=/): 방금 가입한 회원으로 로그인하면 심고, 로그아웃 · 시연 계정 로그인이 지운다.
 *   있으면 내 정보는 그 프로필, 참가권 · 예치금 · 알림은 비어 있다. 없으면 시연용 기존 회원(runner1)이다.
 *
 * 쿠키가 없는 요청(테스트 · 직접 호출)은 시연용 기존 회원으로 본다. 가짜 응답 모드 전용이며 권한 판단에 쓰지 않는다.
 */
export const DEMO_MEMBER_EMAIL = 'runner1@crewrun.com'

export const MOCK_JOINED_COOKIE_NAME = 'mockJoined'
export const MOCK_MEMBER_COOKIE_NAME = 'mockMember'

const MAX_AGE_SECONDS = 60 * 60 * 24 * 14

export type MockMemberProfile = {
  email: string
  name: string
  nickname: string
  phone: string
  birthDate: string
}

function encode(profile: MockMemberProfile): string {
  return encodeURIComponent(JSON.stringify(profile))
}

function decode(value: string | undefined): MockMemberProfile | null {
  if (!value) return null
  try {
    const raw = JSON.parse(decodeURIComponent(value)) as Partial<MockMemberProfile>
    const { email, name, nickname, phone, birthDate } = raw
    if (!email || !name || !nickname || !phone || !birthDate) return null
    return { email, name, nickname, phone, birthDate }
  } catch {
    return null
  }
}

/** 가입 요청 본문에서 프로필을 만든다. 칸이 모자라면 null (가입 목은 그래도 성공으로 답한다) */
export function profileFromJoinBody(body: Record<string, unknown>): MockMemberProfile | null {
  const text = (key: string) => (typeof body[key] === 'string' ? (body[key] as string) : '')
  return decode(
    encode({
      email: text('email'),
      name: text('name'),
      nickname: text('nickname'),
      phone: text('phone'),
      birthDate: text('birthDate'),
    }),
  )
}

const attributes = (path: string) => `Path=${path}; HttpOnly; SameSite=Lax`

export const issueMockJoinedCookie = (profile: MockMemberProfile) =>
  `${MOCK_JOINED_COOKIE_NAME}=${encode(profile)}; Max-Age=${MAX_AGE_SECONDS}; ${attributes('/api/v1/auth')}`

export const issueMockMemberCookie = (profile: MockMemberProfile) =>
  `${MOCK_MEMBER_COOKIE_NAME}=${encode(profile)}; Max-Age=${MAX_AGE_SECONDS}; ${attributes('/')}`

export const clearMockMemberCookie = () =>
  `${MOCK_MEMBER_COOKIE_NAME}=; Max-Age=0; ${attributes('/')}`

/** 가입 때 심은 프로필 (로그인 요청에서 읽는다) */
export const joinedProfileOf = (cookies: Record<string, string>) =>
  decode(cookies[MOCK_JOINED_COOKIE_NAME])

/** 방금 가입한 회원의 프로필. 시연용 기존 회원이면 null */
export const freshMemberOf = (cookies: Record<string, string>) =>
  decode(cookies[MOCK_MEMBER_COOKIE_NAME])

/** 테스트에서 방금 가입한 회원으로 요청할 때 붙이는 Cookie 헤더 값 */
export function freshMemberCookieHeader(profile: MockMemberProfile): string {
  return `${MOCK_MEMBER_COOKIE_NAME}=${encode(profile)}`
}
