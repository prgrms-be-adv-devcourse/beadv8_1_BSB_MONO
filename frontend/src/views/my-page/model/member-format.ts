/** API의 "1995-04-12"를 화면의 "1995.04.12"로 바꾼다. */
export function toDotDate(isoDate: string): string {
  return isoDate.replaceAll('-', '.')
}

/** 화면의 "1995.04.12"를 API의 "1995-04-12"로 바꾼다. */
export function toIsoDate(dotDate: string): string {
  return dotDate.trim().replaceAll('.', '-')
}

/** "010-1234-5678"이나 "01012345678"의 가운데 자리를 가린다: 010-****-5678 */
export function maskPhone(phone: string): string {
  const digits = phone.replace(/\D/g, '')
  if (digits.length < 10) return phone
  const middle = digits.length - 7
  return `${digits.slice(0, 3)}-${'*'.repeat(middle)}-${digits.slice(-4)}`
}
