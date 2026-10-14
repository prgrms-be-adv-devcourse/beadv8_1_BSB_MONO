import type { ApiError } from './api-error'

// 서버 오류 응답(RsData)의 code를 화면 문구로 바꾼다. 문구는 피그마 시안 말투(~요)를 따른다.
// 가입·로그인 문구는 views/auth/model/schemas.ts의 AUTH_ERRORS와 같은 문장이다.
export const ERROR_CODE_MESSAGES: Record<string, string> = {
  MEMBER_DUPLICATE_EMAIL: '이미 가입된 이메일이에요. 로그인하거나 비밀번호를 재설정해 주세요.',
  MEMBER_DUPLICATE_NICKNAME: '이미 사용 중인 닉네임이에요.',
  MEMBER_DUPLICATE_PHONE: '이미 가입된 휴대폰 번호예요.',
  MEMBER_NOT_FOUND: '회원 정보를 찾을 수 없어요.',
  MEMBER_UNDER_AGE: '만 14세 미만은 가입할 수 없어요',
  MEMBER_LOGIN_FAILED: '이메일 또는 비밀번호가 맞지 않아요.',
}

// 표에 없는 코드이거나 코드가 없을 때 상태 코드로 고르는 일반 문구
const STATUS_MESSAGES: Record<number, string> = {
  400: '입력한 내용을 다시 확인해 주세요.',
  401: '다시 로그인해 주세요.',
  403: '이 기능을 쓸 권한이 없어요.',
  404: '찾을 수 없는 정보예요.',
  409: '이미 처리된 요청이에요. 새로고침한 뒤 다시 확인해 주세요.',
}

const SERVER_ERROR_MESSAGE = '일시적인 오류가 생겼어요. 잠시 후 다시 시도해 주세요.'
const DEFAULT_MESSAGE = '문제가 생겼어요. 다시 시도해 주세요.'

/** 오류 응답 본문에서 code를 꺼낸다. 객체가 아니거나 code가 문자열이 아니면 undefined. */
function readErrorCode(body: unknown): string | undefined {
  if (typeof body !== 'object' || body === null || !('code' in body)) return undefined
  return typeof body.code === 'string' ? body.code : undefined
}

/** ApiError를 화면에 보여 줄 문구로 바꾼다. 코드 표 → 상태 코드별 일반 문구 → 기본 문구 순서로 고른다. */
export function getErrorMessage(error: ApiError): string {
  const code = readErrorCode(error.body)
  if (code !== undefined && Object.hasOwn(ERROR_CODE_MESSAGES, code)) {
    return ERROR_CODE_MESSAGES[code]
  }
  if (error.status >= 500) return SERVER_ERROR_MESSAGE
  return STATUS_MESSAGES[error.status] ?? DEFAULT_MESSAGE
}
