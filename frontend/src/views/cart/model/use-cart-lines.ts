'use client'

import { useQueries } from '@tanstack/react-query'

import { useBookingFlow, type BookingApplication } from '@/features/booking'
import { raceDetailQueryOptions, type RaceDetail, type RaceOption } from '@/entities/race'
import { orderApi, unwrapData, type orderModel } from '@/shared/api'

/** 장바구니 한 줄. 장바구니 항목에 붙은 결제 전 참가 신청과 대회 상세를 붙인다. */
export type CartLine = {
  cartItemId: number
  raceCourseId: number
  /** 결제에 보낼 참가 신청. 없으면(신청서 없이 담음) 결제할 수 없다 */
  application?: BookingApplication
  race?: RaceDetail
  option?: RaceOption
}

/** 장바구니 목록 + 줄마다 그릴 대회 정보. 장바구니를 불러오는 동안 lines는 undefined */
export function useCartLines() {
  const cart = orderApi.useFindCart({ query: { select: unwrapData } })
  const applications = useBookingFlow((s) => s.applications)
  const items: orderModel.CartItemDto[] = cart.data?.items ?? []

  const applicationOf = (item: orderModel.CartItemDto) =>
    fromCart(item) ?? applications[String(item.raceCourseId)]
  const raceIds = [...new Set(items.flatMap((item) => applicationOf(item)?.raceId ?? []))]
  const races = useQueries({
    queries: raceIds.map((raceId) => raceDetailQueryOptions(raceId)),
  })
  const raceById = new Map(raceIds.map((id, i) => [id, races[i]?.data ?? undefined]))

  const lines: CartLine[] | undefined = cart.data
    ? items.map((item) => {
        const raceCourseId = item.raceCourseId ?? 0
        const application = applicationOf(item)
        const race = application ? raceById.get(application.raceId) : undefined
        return {
          cartItemId: item.id ?? 0,
          raceCourseId,
          application,
          race,
          option: race?.options.find((o) => o.id === String(raceCourseId)),
        }
      })
    : undefined

  return {
    lines,
    isError: cart.isError,
    racesLoading: races.some((r) => r.isPending),
  }
}

/**
 * 장바구니 API가 붙여 주는 결제 전 참가 신청. 서버 값이라 새로고침 · 다른 기기에서도 있다.
 * 백엔드가 이 값을 주기 전(배포 전)이나 신청서 없이 담았으면 undefined이고, 그때는 이 기기의 메모리 값을 쓴다.
 */
function fromCart(item: orderModel.CartItemDto): BookingApplication | undefined {
  const { participationId, raceId, raceCourseId } = item
  if (participationId == null || raceId == null || raceCourseId === undefined) return undefined
  return {
    raceId: String(raceId),
    optionId: String(raceCourseId),
    participationId,
    participantName: item.participantName ?? '',
  }
}
