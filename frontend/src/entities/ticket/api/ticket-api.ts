import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query'

import { ApiError, orderApi, orderModel, unwrapData } from '@/shared/api'

import { toTicketId } from '../lib/ticket-id'
import type { MyTicket } from '../model/types'

// 주문 · 참가 신청 API는 로그인한 회원 것만 돌려준다. 클라이언트(TanStack Query)에서 부른다.

/** 한 번에 받는 주문 수 (API 최대 100) */
const ORDER_PAGE_SIZE = 100

/** 참가권으로 보여 줄 주문 상태: 결제했던 주문 (결제 대기 · 기한 만료는 빼고) */
const TICKET_ORDER_STATUSES: orderModel.OrderSummaryDtoStatus[] = [
  orderModel.OrderSummaryDtoStatus.PAID,
  orderModel.OrderSummaryDtoStatus.PARTIAL_CANCELED,
  orderModel.OrderSummaryDtoStatus.CANCELED,
]

/** 참가권으로 보여 줄 주문 상품 상태 */
const TICKET_ITEM_STATUSES: orderModel.OrderItemDtoStatus[] = [
  orderModel.OrderItemDtoStatus.PAID,
  orderModel.OrderItemDtoStatus.CANCELED,
]

async function fetchOrder(orderId: number, signal?: AbortSignal) {
  return unwrapData(await orderApi.findById1(orderId, { signal }))
}

/** 주문 상세의 주문 상품 → 참가권 */
export function toMyTickets(order: orderModel.OrderDto): MyTicket[] {
  return (order.items ?? [])
    .filter((item) => item.status && TICKET_ITEM_STATUSES.includes(item.status))
    .map((item) => ({
      id: toTicketId(order.id ?? 0, item.id ?? 0),
      orderId: order.id ?? 0,
      orderItemId: item.id ?? 0,
      orderNo: order.orderNo ?? '',
      raceId: order.raceId ?? 0,
      raceCourseId: item.raceCourseId ?? 0,
      participationId: item.participationId ?? 0,
      itemName: item.itemName ?? '',
      amount: item.amount ?? 0,
      itemStatus: item.status ?? orderModel.OrderItemDtoStatus.PAID,
    }))
}

/**
 * 내 참가권 전체. 주문 목록(GET /api/v1/order/orders)에는 대회 ID와 주문 상품이 없어서
 * 결제했던 주문마다 주문 상세(GET /api/v1/order/orders/{orderId})를 한 번씩 더 부른다.
 */
export function myTicketsQueryOptions() {
  const params = { page: 0, size: ORDER_PAGE_SIZE }
  return queryOptions({
    queryKey: [...orderApi.getFindMineQueryKey(params), 'tickets'],
    queryFn: async ({ signal }) => {
      const page = unwrapData(await orderApi.findMine(params, { signal }))
      const orders = (page.orders ?? []).filter(
        (o) => o.id !== undefined && o.status && TICKET_ORDER_STATUSES.includes(o.status),
      )
      const details = await Promise.all(orders.map((o) => fetchOrder(o.id ?? 0, signal)))
      return details.flatMap(toMyTickets)
    },
  })
}

/**
 * 내 참가 대회 수(마이 화면). 내 참가권 화면이 보여 주는 카드(참가 예정 + 지난 대회)와 같은 목록을 센다.
 * 같은 쿼리 키라 두 화면이 같은 데이터를 쓴다. enabled가 false면 부르지 않는다.
 */
export function myTicketCountQueryOptions(enabled: boolean) {
  return queryOptions({
    ...myTicketsQueryOptions(),
    select: (tickets: MyTicket[]) => tickets.length,
    enabled,
  })
}

/**
 * 주문 상세 (GET /api/v1/order/orders/{orderId}).
 * 결제 · 완료 화면(orderApi.useFindById1)과 같은 캐시 키를 쓰므로 캐시에는 응답 그대로(RsData) 넣고,
 * 꺼낼 때만 select로 주문을 만든다. 모양이 다르면 결제 직후 참가권을 열 때 주문을 못 읽는다.
 */
export function orderQueryOptions(orderId: number) {
  return queryOptions({
    ...orderApi.getFindById1QueryOptions<orderModel.OrderDto, Error>(orderId, {
      query: { select: unwrapData },
    }),
  })
}

/** 참가 신청 (GET /api/v1/order/participations/{participationId}). 참가자 정보가 들어 있다. */
export function participationQueryOptions(participationId: number) {
  return queryOptions({
    queryKey: orderApi.getFindByIdQueryKey(participationId),
    queryFn: async ({ signal }) => unwrapData(await orderApi.findById(participationId, { signal })),
  })
}

/** 주문 · 참가 신청 · 예치금 캐시를 지운다 (취소 · 수정 뒤 목록 · 상세 · 잔액을 새로 받게) */
function invalidateTicketQueries(queryClient: ReturnType<typeof useQueryClient>) {
  return queryClient.invalidateQueries({
    predicate: ({ queryKey }) =>
      typeof queryKey[0] === 'string' &&
      (queryKey[0].startsWith('/api/v1/order/') || queryKey[0].startsWith('/api/v1/cash/')),
  })
}

/**
 * 결제한 참가권 취소 (POST /api/v1/order/orders/{orderId}/items/{orderItemId}/cancel).
 * 환불 기한이 지났으면 409, 정산 서비스에 못 붙으면 503 → ticketErrorMessage로 문구를 보여 준다.
 */
export function useCancelTicket() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ orderId, orderItemId }: { orderId: number; orderItemId: number }) =>
      unwrapData(await orderApi.cancelItem(orderId, orderItemId)),
    onSuccess: (order) => {
      if (order.id !== undefined) {
        queryClient.setQueryData(orderApi.getFindById1QueryKey(order.id), order)
      }
      return invalidateTicketQueries(queryClient)
    },
  })
}

/** 참가자 정보 수정 (PUT /api/v1/order/participations/{participationId}/participant). 접수 마감 뒤에는 409 */
export function useChangeParticipant() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({
      participationId,
      body,
    }: {
      participationId: number
      body: orderModel.ParticipantInfoRequestDto
    }) => unwrapData(await orderApi.changeParticipant(participationId, body)),
    onSuccess: (participation) => {
      if (participation.id !== undefined) {
        queryClient.setQueryData(orderApi.getFindByIdQueryKey(participation.id), participation)
      }
      return invalidateTicketQueries(queryClient)
    },
  })
}

/**
 * 요청 실패 문구. 409(환불 기한 지남 · 접수 마감 등) · 503(잠시 연결 안 됨)은 서버가 준 문구를 그대로 보여 준다.
 * 서버 문구가 없으면(게이트웨이 오류 등) 기본 문구.
 */
export function ticketErrorMessage(error: Error | null): string | undefined {
  if (!error) return undefined
  if (error instanceof ApiError && error.serverMessage) return error.serverMessage
  if (error instanceof ApiError && error.status === 503) {
    return '지금은 처리할 수 없어요. 잠시 뒤 다시 시도해 주세요.'
  }
  return '요청을 처리하지 못했어요. 잠시 뒤 다시 시도해 주세요.'
}
