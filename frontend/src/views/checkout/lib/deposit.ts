/** 모자랄 때 충전하는 최소 금액 */
export const MIN_CHARGE = 10_000

/**
 * 참가비를 예치금으로 내고, 모자라면 충전한다. 1만 원보다 적게 모자라면 1만 원을 충전한다.
 * TODO(api): 주문 생성 응답의 금액으로 바꾼다.
 */
export function splitPayment(price: number, balance: number) {
  const shortfall = Math.max(0, price - balance)
  const charge = shortfall === 0 ? 0 : Math.max(MIN_CHARGE, shortfall)
  return {
    fromDeposit: Math.min(price, balance),
    charge,
    balanceAfter: balance + charge - price,
  }
}
