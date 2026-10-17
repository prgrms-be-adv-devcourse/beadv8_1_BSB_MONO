import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { ApiError, orderApi, orderModel, unwrapData } from '@/shared/api'

/** 한 번에 받는 키트 주문 수 (API 최대 100) */
const KIT_PAGE_SIZE = 100

export type KitFilters = {
  raceId?: number
  status?: orderModel.FindKitsStatus
}

/** 판매자 키트 주문 목록. 로그인한 승인 판매자 것만 돌려준다. */
export function useSellerKits(filters: KitFilters) {
  const params = { ...filters, size: KIT_PAGE_SIZE }
  return useQuery({
    queryKey: orderApi.getFindKitsQueryKey(params),
    queryFn: async ({ signal }) => unwrapData(await orderApi.findKits(params, { signal })),
  })
}

/** 키트 배송 건수 (배송 준비 · 배송 중 · 배송 완료 · 환불 기한 전) */
export function useSellerKitSummary(raceId?: number) {
  const params = { raceId }
  return useQuery({
    queryKey: orderApi.getSummarizeQueryKey(params),
    queryFn: async ({ signal }) => unwrapData(await orderApi.summarize(params, { signal })),
  })
}

export type KitShipment = { orderItemId: number; carrier: string; trackingNumber: string }

export type KitFailure = { orderItemId: number; error: unknown }

/** 서버가 준 문구. 없으면 (연결 끊김 등) 다시 시도하라는 말 */
export function kitErrorMessage(error: unknown) {
  return (error instanceof ApiError && error.serverMessage) || '잠시 뒤 다시 시도해 주세요.'
}

/** 목록 · 건수 쿼리 이름표 (params 없이 앞부분만 맞춰 모든 필터의 캐시를 새로 받는다) */
function useRefreshKits() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: orderApi.getFindKitsQueryKey() }),
      queryClient.invalidateQueries({ queryKey: orderApi.getSummarizeQueryKey() }),
    ])
}

/**
 * 고른 건들을 하나씩 처리한다. 한 건이 실패해도 나머지는 계속하고, 실패한 건만 돌려준다.
 * 성공 · 실패와 상관없이 목록과 건수를 새로 받는다 (실패 원인이 상태 어긋남일 수 있다).
 */
function useBulkKitAction<Item extends { orderItemId: number }>(
  run: (item: Item) => Promise<unknown>,
) {
  const refresh = useRefreshKits()
  return useMutation({
    mutationFn: async (items: Item[]) => {
      const results = await Promise.allSettled(items.map(run))
      const failures: KitFailure[] = []
      results.forEach((result, i) => {
        if (result.status === 'rejected') {
          failures.push({ orderItemId: items[i].orderItemId, error: result.reason })
        }
      })
      return { total: items.length, failures }
    },
    onSettled: refresh,
  })
}

/** 키트 발송 (배송 준비 → 배송 중). 택배사 · 송장 번호는 건마다 다르다. */
export function useShipKits() {
  return useBulkKitAction<KitShipment>(async ({ orderItemId, carrier, trackingNumber }) =>
    unwrapData(await orderApi.ship(orderItemId, { carrier, trackingNumber })),
  )
}

/** 키트 배송 완료 (배송 중 → 배송 완료) */
export function useCompleteKitDeliveries() {
  return useBulkKitAction<{ orderItemId: number }>(async ({ orderItemId }) =>
    unwrapData(await orderApi.completeDelivery(orderItemId)),
  )
}
