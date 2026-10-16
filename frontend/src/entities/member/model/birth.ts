// 생년월일 규칙. 백엔드 Member.isJoinableAge(만 14세 이상)와 @Past(오늘 이전)를 옮겼다.
// 가입과 정보 수정이 같은 기준으로 거르도록 두 화면이 함께 쓴다.

/** 가입·수정에 필요한 최소 만 나이 (백엔드 Member.MIN_JOIN_AGE) */
export const MIN_JOIN_AGE = 14

/** 만 14세 미만이거나 미래 날짜일 때 보여 주는 문구 (가입 화면 시안 문구) */
export const BIRTH_UNDER_14_MESSAGE = '만 14세 미만은 가입할 수 없어요'

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

/** 오늘 기준 만 나이. 미래 날짜면 0보다 작거나 같은 값이 나온다. */
export function ageOn(birth: Date, today: Date): number {
  const age = today.getFullYear() - birth.getFullYear()
  const beforeBirthday =
    today.getMonth() < birth.getMonth() ||
    (today.getMonth() === birth.getMonth() && today.getDate() < birth.getDate())
  return beforeBirthday ? age - 1 : age
}

/** 만 14세 이상인 날짜인가. 미래 날짜는 나이가 14보다 작아 함께 걸러진다. */
export function isJoinableAge(birth: Date, today: Date = new Date()): boolean {
  return ageOn(birth, today) >= MIN_JOIN_AGE
}
