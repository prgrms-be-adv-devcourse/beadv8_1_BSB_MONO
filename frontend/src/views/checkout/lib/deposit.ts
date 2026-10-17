import { MIN_CHARGE } from '@/features/deposit-charge'

/**
 * 참가비를 예치금으로 내고, 모자라면 충전한다. 1만 원보다 적게 모자라면 1만 원을 충전한다.
 * 화면에 보여 주는 계산이다. 실제로 충전할 금액은 결제 API가 돌려준 부족 금액(shortage)으로 정한다.
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
