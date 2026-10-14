/**
 * 주문 생성·장바구니 결제처럼 두 번 처리되면 안 되는 요청에 붙일 Idempotency-Key를 만든다 (백엔드 허용 길이 1~64자).
 * 결제하기를 누를 때 한 번 만들어 두고, 같은 시도를 다시 보낼 때(재시도·두 번 클릭)는 같은 키를 쓴다.
 */
export function createIdempotencyKey(): string {
  return crypto.randomUUID()
}
