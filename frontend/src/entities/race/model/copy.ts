/** 대회 상세·신청에 공통으로 쓰는 환불 규정 줄 (정책: 환불 기한까지 전액, 이후 개인 사정 환불 불가) */
export function refundRows(deadline: string) {
  return [
    { label: `환불 기한 ${deadline}까지`, value: '전액 환불' },
    { label: '환불 기한이 지난 뒤', value: '개인 사정 환불 불가' },
    { label: '대회 취소 · 일정 변경', value: '전액 환불' },
  ]
}

export const MAIL_ORDER_NOTICE = '크루런은 통신판매중개자이며 거래 당사자가 아니에요.'
