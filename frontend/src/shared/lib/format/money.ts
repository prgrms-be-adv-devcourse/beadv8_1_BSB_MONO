/** 35000 → ₩35,000 */
export function formatWon(amount: number) {
  return `₩${amount.toLocaleString('ko-KR')}`
}

/** 3000 → 3,000 */
export function formatNumber(value: number) {
  return value.toLocaleString('ko-KR')
}
