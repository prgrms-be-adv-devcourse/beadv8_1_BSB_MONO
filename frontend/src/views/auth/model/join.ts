import { ApiError, type memberModel } from '@/shared/api'

import {
  AUTH_ERRORS,
  signupAccountSchema,
  type SignupAccountValues,
  type SignupProfileValues,
} from './schemas'

/** 화면의 "1995.03.02"를 API의 "1995-03-02"로 바꾼다. */
export function toIsoDate(birth: string): string {
  return birth.trim().replaceAll('.', '-')
}

/**
 * 가입 단계 입력을 가입 요청(POST /api/v1/auth/join) 본문으로 바꾼다.
 * 약관 동의는 가입 API에 받을 칸이 없어 보내지 않는다. 성별은 선택이라 비워 둔다.
 */
export function toJoinRequest(
  account: SignupAccountValues,
  profile: SignupProfileValues,
): memberModel.MemberJoinRequestDto {
  const { email, password } = signupAccountSchema.parse(account)
  return {
    email,
    password,
    name: profile.name.trim(),
    nickname: profile.nickname.trim(),
    phone: profile.phone.trim(),
    birthDate: toIsoDate(profile.birth),
  }
}

/** 가입 실패를 어느 칸에 보여 줄지. form은 칸이 아닌 화면 아래 문구 */
export type JoinErrorTarget = keyof SignupProfileValues | 'form'

/**
 * 가입 실패 응답을 화면 문구로 바꾼다.
 * 이메일 중복 확인 API가 없어 이메일 중복도 가입 요청의 409(MEMBER_DUPLICATE_EMAIL)로 안다.
 */
export function joinErrorMessage(error: unknown): { target: JoinErrorTarget; message: string } {
  if (!(error instanceof ApiError)) return { target: 'form', message: AUTH_ERRORS.joinUnavailable }
  const message = error.serverMessage ?? AUTH_ERRORS.joinUnavailable
  switch (error.code) {
    case 'MEMBER_DUPLICATE_EMAIL':
      return { target: 'form', message: AUTH_ERRORS.emailTaken }
    case 'MEMBER_DUPLICATE_NICKNAME':
      return { target: 'nickname', message }
    case 'MEMBER_DUPLICATE_PHONE':
      return { target: 'phone', message }
    case 'MEMBER_UNDER_AGE':
      return { target: 'birth', message: AUTH_ERRORS.birthUnder14 }
    default:
      return { target: 'form', message }
  }
}
