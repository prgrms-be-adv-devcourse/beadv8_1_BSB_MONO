/** 참가권 화면 주소의 ID. 취소 API가 주문 ID와 주문 상품 ID를 함께 받으므로 둘을 묶는다. */
export function toTicketId(orderId: number, orderItemId: number): string {
  return `${orderId}-${orderItemId}`
}

/** `주문ID-주문상품ID` → 두 ID. 모양이 다르면 null */
export function parseTicketId(ticketId: string): { orderId: number; orderItemId: number } | null {
  const match = /^(\d+)-(\d+)$/.exec(ticketId)
  if (!match) return null
  const orderId = Number(match[1])
  const orderItemId = Number(match[2])
  if (orderId <= 0 || orderItemId <= 0) return null
  return { orderId, orderItemId }
}
