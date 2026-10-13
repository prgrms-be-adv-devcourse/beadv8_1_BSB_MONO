import { describe, expect, it } from 'vitest'

import {
  ageOn,
  AUTH_ERRORS,
  findEmailSchema,
  loginSchema,
  parseBirth,
  passwordNewSchema,
  signupAccountSchema,
  signupProfileSchema,
} from './schemas'

// 실패한 첫 오류 문구를 필드 이름과 함께 돌려준다.
function firstError(result: {
  success: boolean
  error?: { issues: { path: PropertyKey[]; message: string }[] }
}) {
  const issue = result.error?.issues[0]
  return issue && { path: issue.path.join('.'), message: issue.message }
}

describe('loginSchema', () => {
  it('이메일 형식이 틀리면 형식 오류를 낸다', () => {
    const result = loginSchema.safeParse({ email: 'runner', password: 'x' })
    expect(firstError(result)).toEqual({ path: 'email', message: AUTH_ERRORS.emailFormat })
  })

  it('비밀번호가 비면 입력해 달라고 한다', () => {
    const result = loginSchema.safeParse({ email: 'runner@crewrun.kr', password: '' })
    expect(firstError(result)).toEqual({ path: 'password', message: AUTH_ERRORS.passwordRequired })
  })

  it('맞는 값은 통과한다', () => {
    expect(loginSchema.safeParse({ email: 'runner@crewrun.kr', password: 'a' }).success).toBe(true)
  })
})

describe('signupAccountSchema', () => {
  const valid = { email: 'runner@crewrun.kr', password: 'password1', passwordConfirm: 'password1' }

  it('맞는 값은 통과한다', () => {
    expect(signupAccountSchema.safeParse(valid).success).toBe(true)
  })

  it.each(['pass1', 'password', '12345678'])(
    '비밀번호 규칙(영문 · 숫자 8자 이상)에 안 맞으면 막는다: %s',
    (password) => {
      const result = signupAccountSchema.safeParse({
        ...valid,
        password,
        passwordConfirm: password,
      })
      expect(firstError(result)).toEqual({ path: 'password', message: AUTH_ERRORS.passwordRule })
    },
  )

  it('비밀번호 확인이 다르면 확인 칸에 오류를 낸다', () => {
    const result = signupAccountSchema.safeParse({ ...valid, passwordConfirm: 'password2' })
    expect(firstError(result)).toEqual({
      path: 'passwordConfirm',
      message: AUTH_ERRORS.passwordMismatch,
    })
  })
})

describe('signupProfileSchema', () => {
  const valid = {
    name: '김러너',
    birth: '1995.03.02',
    phone: '010-1234-5678',
    nickname: '달리는곰',
  }

  it('맞는 값은 통과한다', () => {
    expect(signupProfileSchema.safeParse(valid).success).toBe(true)
  })

  it('없는 날짜나 형식이 틀린 생년월일은 막는다', () => {
    for (const birth of ['1995-03-02', '1995.02.30', '19950302']) {
      const result = signupProfileSchema.safeParse({ ...valid, birth })
      expect(firstError(result)).toEqual({ path: 'birth', message: AUTH_ERRORS.birthFormat })
    }
  })

  it('만 14세 미만은 막는다', () => {
    const year = new Date().getFullYear() - 10
    const result = signupProfileSchema.safeParse({ ...valid, birth: `${year}.01.01` })
    expect(firstError(result)).toEqual({ path: 'birth', message: AUTH_ERRORS.birthUnder14 })
  })

  it('휴대폰 번호 형식과 닉네임 길이를 검사한다', () => {
    expect(firstError(signupProfileSchema.safeParse({ ...valid, phone: '02-123-4567' }))).toEqual({
      path: 'phone',
      message: AUTH_ERRORS.phoneFormat,
    })
    expect(firstError(signupProfileSchema.safeParse({ ...valid, nickname: '곰' }))).toEqual({
      path: 'nickname',
      message: AUTH_ERRORS.nicknameLength,
    })
  })
})

describe('찾기 · 재설정 스키마', () => {
  it('이메일 찾기는 나이 제한 없이 생년월일 형식만 본다', () => {
    const year = new Date().getFullYear() - 5
    const result = findEmailSchema.safeParse({
      name: '김러너',
      birth: `${year}.01.01`,
      phone: '01012345678',
    })
    expect(result.success).toBe(true)
  })

  it('새 비밀번호 확인이 다르면 막는다', () => {
    const result = passwordNewSchema.safeParse({ password: 'password1', passwordConfirm: 'x' })
    expect(firstError(result)?.path).toBe('passwordConfirm')
  })
})

describe('ageOn', () => {
  it('생일이 지나지 않았으면 한 살 적게 센다', () => {
    const birth = parseBirth('2012.06.15')!
    expect(ageOn(birth, new Date(2026, 5, 14))).toBe(13)
    expect(ageOn(birth, new Date(2026, 5, 15))).toBe(14)
  })
})
