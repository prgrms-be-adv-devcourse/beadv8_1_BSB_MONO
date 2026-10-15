'use client'

import { useQueries } from '@tanstack/react-query'

import { useBookingFlow, type BookingApplication } from '@/features/booking'
import { raceDetailQueryOptions, type RaceDetail, type RaceOption } from '@/entities/race'
import { orderApi, unwrapData, type orderModel } from '@/shared/api'

/** 장바구니 한 줄. 장바구니 API는 종목 ID만 주므로 이번에 저장한 참가 신청과 대회 상세를 붙인다. */
export type CartLine = {
  cartItemId: number
  raceCourseId: number
  /** 이 기기에서 저장한 참가 신청. 없으면(다른 기기에서 담음 · 새로고침) 결제에 보낼 참가 신청 ID를 모른다 */
  application?: BookingApplication
  race?: RaceDetail
  option?: RaceOption
}

/** 장바구니 목록 + 줄마다 그릴 대회 정보. 장바구니를 불러오는 동안 lines는 undefined */
export function useCartLines() {
  const cart = orderApi.useFindCart({ query: { select: unwrapData } })
  const applications = useBookingFlow((s) => s.applications)
  const items: orderModel.CartItemDto[] = cart.data?.items ?? []

  const raceIds = [
    ...new Set(items.flatMap((item) => applications[String(item.raceCourseId)]?.raceId ?? [])),
  ]
  const races = useQueries({
    queries: raceIds.map((raceId) => raceDetailQueryOptions(raceId)),
  })
  const raceById = new Map(raceIds.map((id, i) => [id, races[i]?.data ?? undefined]))

  const lines: CartLine[] | undefined = cart.data
    ? items.map((item) => {
        const raceCourseId = item.raceCourseId ?? 0
        const application = applications[String(raceCourseId)]
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
