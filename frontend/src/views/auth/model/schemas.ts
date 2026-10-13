import { z } from 'zod'

// 인증 화면 입력 검사. 백엔드 API가 생기면 orval이 만든 zod 스키마로 바꾼다.
// 오류 문구는 피그마 시안(M03-E 로그인 실패, M04-1E 가입 입력 오류)과 helper 문구를 따른다.

export const AUTH_ERRORS = {
  emailRequired: '이메일을 입력해 주세요.',
  emailFormat: '이메일 형식이 맞지 않아요.',
  passwordRequired: '비밀번호를 입력해 주세요.',
  passwordRule: '영문 · 숫자 포함 8자 이상으로 정해 주세요.',
  passwordMismatch: '비밀번호가 서로 달라요.',
  loginFailed: '이메일 또는 비밀번호가 맞지 않아요.',
  emailTaken: '이미 가입된 이메일이에요. 로그인하거나 비밀번호를 재설정해 주세요.',
  nameRequired: '이름을 입력해 주세요.',
  birthFormat: '생년월일을 1995.03.02처럼 입력해 주세요.',
  birthUnder14: '만 14세 미만은 가입할 수 없어요',
  phoneFormat: '휴대폰 번호를 010-1234-5678처럼 입력해 주세요.',
  nicknameLength: '닉네임은 2~10자로 정해 주세요.',
} as const

const email = z
  .string()
  .trim()
  .min(1, AUTH_ERRORS.emailRequired)
  .pipe(z.email(AUTH_ERRORS.emailFormat))

// 영문 · 숫자 포함 8자 이상
const newPassword = z
  .string()
  .min(8, AUTH_ERRORS.passwordRule)
  .regex(/[A-Za-z]/, AUTH_ERRORS.passwordRule)
  .regex(/\d/, AUTH_ERRORS.passwordRule)

const BIRTH_PATTERN = /^(\d{4})\.(\d{2})\.(\d{2})$/

/** "1995.03.02"를 Date로 바꾼다. 형식이 틀리거나 없는 날짜면 null. */
export function parseBirth(value: string): Date | null {
  const match = BIRTH_PATTERN.exec(value)
  if (!match) return null
  const [year, month, day] = match.slice(1).map(Number)
  const date = new Date(year, month - 1, day)
  const valid =
    date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day
  return valid ? date : null
}

/** 오늘 기준 만 나이 */
export function ageOn(birth: Date, today: Date): number {
  const age = today.getFullYear() - birth.getFullYear()
  const beforeBirthday =
    today.getMonth() < birth.getMonth() ||
    (today.getMonth() === birth.getMonth() && today.getDate() < birth.getDate())
  return beforeBirthday ? age - 1 : age
}

const birth = z
  .string()
  .trim()
  .refine((value) => parseBirth(value) !== null, AUTH_ERRORS.birthFormat)

const name = z.string().trim().min(1, AUTH_ERRORS.nameRequired)

const phone = z
  .string()
  .trim()
  .regex(/^01[016789]-?\d{3,4}-?\d{4}$/, AUTH_ERRORS.phoneFormat)

// 비밀번호 확인 칸이 다르면 확인 칸 아래에 오류를 띄운다.
const confirmMatches = (v: { password: string; passwordConfirm: string }) =>
  v.password === v.passwordConfirm
const MISMATCH = { message: AUTH_ERRORS.passwordMismatch, path: ['passwordConfirm'] }

// 로그인은 비밀번호 규칙을 알려 주지 않는다. 틀리면 서버가 M03-E 문구로 답한다.
export const loginSchema = z.object({
  email,
  password: z.string().min(1, AUTH_ERRORS.passwordRequired),
})
export type LoginValues = z.infer<typeof loginSchema>

export const signupAccountSchema = z
  .object({ email, password: newPassword, passwordConfirm: z.string() })
  .refine(confirmMatches, MISMATCH)
export type SignupAccountValues = z.infer<typeof signupAccountSchema>

export const signupProfileSchema = z.object({
  name,
  birth: birth.refine((value) => {
    const date = parseBirth(value)
    return date === null || ageOn(date, new Date()) >= 14
  }, AUTH_ERRORS.birthUnder14),
  phone,
  nickname: z
    .string()
    .trim()
    .min(2, AUTH_ERRORS.nicknameLength)
    .max(10, AUTH_ERRORS.nicknameLength),
})
export type SignupProfileValues = z.infer<typeof signupProfileSchema>

export const findEmailSchema = z.object({ name, birth, phone })
export type FindEmailValues = z.infer<typeof findEmailSchema>

export const passwordResetSchema = z.object({ email })
export type PasswordResetValues = z.infer<typeof passwordResetSchema>

export const passwordNewSchema = z
  .object({ password: newPassword, passwordConfirm: z.string() })
  .refine(confirmMatches, MISMATCH)
export type PasswordNewValues = z.infer<typeof passwordNewSchema>
