import { orderModel } from '@/shared/api'
import type { BadgeTone } from '@/shared/ui'

export type KitStatus = orderModel.SellerKitDtoKitDeliveryStatus

/** 배송 흐름. 배송 준비 → 배송 중 → 배송 완료 순서로만 바뀐다. */
export const KIT_FLOW: KitStatus[] = ['PREPARING', 'SHIPPING', 'DELIVERED']

export const KIT_STATUS: Record<KitStatus, { label: string; tone: BadgeTone }> = {
  PREPARING: { label: '배송 준비', tone: 'neutral' },
  SHIPPING: { label: '배송 중', tone: 'brand' },
  DELIVERED: { label: '배송 완료', tone: 'success' },
}

/** 목록 · 건수 상단에서 "발송할 수 있는" 건수 = 배송 준비 중 환불 기한이 지난 건 */
export function shippableCount(summary: orderModel.SellerKitSummaryDto) {
  return Math.max((summary.preparing ?? 0) - (summary.beforeRefundDeadline ?? 0), 0)
}

/** 배송지는 시 · 구까지만 보여 준다 (표 칸이 좁다). 전체 주소는 판매자가 배송 처리할 때 필요한 범위라 API가 준다. */
export function shortAddress(address?: string) {
  return (address ?? '').split(' ').slice(0, 2).join(' ')
}
