import { http, HttpResponse } from 'msw'

/**
 * 가짜 응답 모드의 예치금 지갑. 진짜 서버처럼 충전하면 늘고 결제하면 준다.
 * 처음 잔액은 시안(M17 · M21)의 ₩5,000이라, 주문(50,000원)을 결제하려면 모자란 만큼 충전해야 한다.
 * 그래서 결제 → 부족 → 충전 → 다시 결제 흐름을 가짜 응답 모드에서도 끝까지 볼 수 있다.
 * 주문 1 상태(handlers.ts)처럼 서버 메모리에 둔다. 서버를 다시 켜면 처음으로 돌아간다.
 */
export const MOCK_INITIAL_BALANCE = 5_000

/** 정책 4장 예치금-1, cash-service와 같은 최소 충전 금액 */
const MIN_CHARGE = 10_000

let balance = MOCK_INITIAL_BALANCE
/** 충전 확정 결과(orderId = 멱등키). 같은 orderId로 다시 오면 처음 결과를 돌려준다 */
const charges = new Map<string, 'COMPLETED' | 'FAILED'>()

/** 테스트 사이에 잔액 · 충전 기록이 넘어가지 않게 처음으로 돌린다. */
export function resetMockWalletForTesting() {
  balance = MOCK_INITIAL_BALANCE
  charges.clear()
}

/** 주문 결제 목이 쓴다. 모자라면 차감하지 않고 모자란 금액을 돌려준다. */
export function payFromMockWallet(amount: number): { shortage: number } {
  if (balance < amount) return { shortage: amount - balance }
  balance -= amount
  return { shortage: 0 }
}

/** 환급 목이 쓴다. 남은 잔액 전부를 빼고 뺀 금액을 돌려준다. */
export function withdrawAllFromMockWallet(): number {
  const amount = balance
  balance = 0
  return amount
}

export const walletHandlers = [
  http.get('*/api/v1/cash/wallets/:holderId/balance', ({ params }) =>
    HttpResponse.json({
      status: 200,
      message: '잔액 조회 성공',
      data: { holderId: Number(params.holderId), balance },
    }),
  ),
  /**
   * 충전 확정 목. paymentKey에 "fail"이 들어 있으면 PG 승인 거절(200 · FAILED)로 답한다.
   * 진짜 서버처럼 1만 원 미만은 400(code 없음), 같은 orderId는 처음 결과를 돌려준다.
   */
  http.post('*/api/v1/cash/charges/confirm', async ({ request }) => {
    const body = (await request.json()) as {
      paymentKey?: string
      orderId?: string
      amount?: number
    }
    const orderId = body.orderId ?? ''
    const done = charges.get(orderId)
    if (done) {
      return HttpResponse.json({
        status: 200,
        message: done === 'COMPLETED' ? '충전했습니다.' : '충전에 실패했습니다.',
        data: { outcome: done, balance },
      })
    }
    const amount = body.amount ?? 0
    if (amount < MIN_CHARGE) {
      return HttpResponse.json(
        { status: 400, message: '최소 충전 금액은 10,000원입니다.', data: null },
        { status: 400 },
      )
    }
    const outcome = body.paymentKey?.includes('fail') ? 'FAILED' : 'COMPLETED'
    charges.set(orderId, outcome)
    if (outcome === 'COMPLETED') balance += amount
    return HttpResponse.json({
      status: 200,
      message: outcome === 'COMPLETED' ? '충전했습니다.' : '충전에 실패했습니다.',
      data: { outcome, balance },
    })
  }),
]
