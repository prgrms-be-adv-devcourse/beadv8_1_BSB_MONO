// 휴대폰 번호를 입력하는 대로 하이픈을 넣는다. 백엔드는 하이픈이 있어도 없어도 받지만(^01[016789]-?\d{3,4}-?\d{4}$)
// 화면 안내가 010-1234-5678이라 하이픈을 넣은 값을 그대로 보낸다. 중복 검사 해시는 백엔드가 숫자만 남겨 만든다.

/** 휴대폰 번호의 최대 자릿수 */
export const PHONE_MAX_DIGITS = 11

const onlyDigits = (value: string) => value.replace(/\D/g, '').slice(0, PHONE_MAX_DIGITS)

/**
 * 숫자만 남겨 하이픈을 넣는다. 11자리는 3-4-4, 10자리(011 등)는 3-3-4.
 * 치는 중(8·9자리)에는 010 번호처럼 3-4-나머지로 보여 주다가, 10자리에서 멈추면 3-3-4로 바뀐다.
 */
export function formatPhoneNumber(value: string): string {
  const digits = onlyDigits(value)
  if (digits.length <= 3) return digits
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  if (digits.length === 10) return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
}

/**
 * 입력 칸의 새 값과 커서 위치를 받아, 하이픈을 넣은 값과 옮길 커서 위치를 돌려준다.
 * 커서 앞의 숫자 개수를 그대로 두는 방식이라 붙여넣기 · 중간 수정에서도 커서가 튀지 않는다.
 * 하이픈만 지운 경우(숫자는 그대로)는 다시 하이픈이 생겨 지워지지 않으므로, 지운 방향의 숫자 하나를 함께 지운다.
 */
export function formatPhoneInput(
  prev: string,
  next: string,
  caret: number,
  inputType?: string,
): { value: string; caret: number } {
  let digits = next.replace(/\D/g, '')
  let digitsBefore = next.slice(0, caret).replace(/\D/g, '').length
  const onlyHyphenRemoved = next.length < prev.length && digits === prev.replace(/\D/g, '')
  if (onlyHyphenRemoved && inputType === 'deleteContentBackward' && digitsBefore > 0) {
    digits = digits.slice(0, digitsBefore - 1) + digits.slice(digitsBefore)
    digitsBefore -= 1
  } else if (onlyHyphenRemoved && inputType === 'deleteContentForward') {
    digits = digits.slice(0, digitsBefore) + digits.slice(digitsBefore + 1)
  }

  const value = formatPhoneNumber(digits)
  const target = Math.min(digitsBefore, PHONE_MAX_DIGITS)
  let position = 0
  for (let seen = 0; position < value.length && seen < target; position += 1) {
    if (/\d/.test(value[position])) seen += 1
  }
  return { value, caret: position }
}
