'use client'

import type { Query } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'

import { orderApi, unwrapData, type orderModel } from '@/shared/api'

import { admissionExpiredHref } from '../lib/queue-error'

/** 차례를 기다리는 동안(M05-Q1) 입장 여부를 다시 묻는 간격 */
export const ADMISSION_POLL_MS = 3000

/**
 * - wait: 대기열 화면(M05-Q1·Q2). 들어올 때마다 새로 묻고, 입장할 때까지 ADMISSION_POLL_MS마다 다시 묻는다
 * - follow: 신청서(M05) · 결제(M07). 대기열 화면에서 받은 답을 그대로 이어 쓴다(다시 묻지 않는다)
 */
export type AdmissionMode = 'wait' | 'follow'

type AdmissionQuery = Query<orderModel.RsDataQueueAdmissionDto>

/** API에 보낼 수 있는 ID (양의 정수). 시안 대회처럼 숫자가 아닌 ID는 묻지 않는다 */
const isId = (id: number | undefined): id is number => Number.isInteger(id) && (id ?? 0) > 0

/** 받은 답의 입장 끝 시각 (이 기기 시계 기준 ms). 답을 받은 순간 + 서버가 센 남은 초. 입장 전이면 null */
function deadlineOf(data: orderModel.QueueAdmissionDto | undefined, receivedAt: number) {
  return data?.admitted ? receivedAt + data.remainingSeconds * 1000 : null
}

/** 캐시의 답이 입장 중이고 아직 끝나지 않았는지 */
function isAdmittedNow(query: AdmissionQuery) {
  const deadline = deadlineOf(query.state.data?.data, query.state.dataUpdatedAt)
  return deadline !== null && deadline > Date.now()
}

/**
 * 이 종목의 대기열 입장 상태 (GET /api/v1/order/queue/admission).
 * 남은 시간은 서버의 remainingSeconds를 받은 순간의 이 기기 시각에 더해 끝 시각을 잡는다.
 * 기기 시계가 서버와 달라도 남은 시간이 어긋나지 않고, 화면을 옮겨도 같은 캐시(같은 끝 시각)를 쓴다.
 */
export function useQueueAdmission(
  raceId: number | undefined,
  raceCourseId: number | undefined,
  mode: AdmissionMode,
) {
  const enabled = isId(raceId) && isId(raceCourseId)
  const query = orderApi.useFindAdmission(
    { raceId: raceId ?? 0, raceCourseId: raceCourseId ?? 0 },
    {
      query: {
        enabled,
        select: unwrapData,
        refetchOnWindowFocus: false,
        refetchOnReconnect: false,
        ...(mode === 'wait'
          ? {
              staleTime: 0,
              refetchOnMount: 'always' as const,
              // 입장하면 멈춘다. 화면을 벗어나거나 탭이 숨겨지면 TanStack Query가 멈춘다.
              refetchInterval: (q: AdmissionQuery) =>
                isAdmittedNow(q) ? false : ADMISSION_POLL_MS,
            }
          : {
              // 끝난 답만 다시 묻는다. 끝나지 않은 답은 대기열 화면에서 받은 끝 시각을 그대로 쓴다.
              staleTime: (q: AdmissionQuery) => (isAdmittedNow(q) ? Infinity : 0),
            }),
      },
    },
  )
  // 대기열 화면은 이전에 받아 둔 답(끝났을 수 있다)이 아니라 들어와서 새로 받은 답으로 판단한다.
  const ready = mode === 'wait' ? query.isFetchedAfterMount && query.isSuccess : query.isSuccess
  return {
    /** 입장 끝 시각 (이 기기 시계 기준 ms). 입장 전 · 모름이면 null */
    deadline: ready ? deadlineOf(query.data, query.dataUpdatedAt) : null,
    /** 다시 묻는 중. 끝난 답을 새로 받는 동안 만료로 보지 않게 쓴다 */
    isFetching: query.isFetching,
  }
}

/** 끝 시각까지 남은 초. 1초 안쪽이 남아도 1초로 보여 주고, 지나면 0. */
export function secondsUntil(deadline: number, now = Date.now()) {
  return Math.max(0, Math.ceil((deadline - now) / 1000))
}

/**
 * 끝 시각(deadline)까지 남은 초를 1초마다 다시 센다. 끝 시각이 없으면 null.
 * 남은 초를 1씩 빼지 않고 매번 끝 시각과 비교하므로, 탭이 백그라운드에서 느려져도 어긋나지 않는다.
 */
export function useAdmissionCountdown(deadline: number | null) {
  const [remaining, setRemaining] = useState(() =>
    deadline === null ? null : secondsUntil(deadline),
  )

  // 끝 시각이 바뀌면 다음 1초를 기다리지 않고 이번 렌더에서 바로 맞춘다.
  const [prevDeadline, setPrevDeadline] = useState(deadline)
  if (prevDeadline !== deadline) {
    setPrevDeadline(deadline)
    setRemaining(deadline === null ? null : secondsUntil(deadline))
  }

  useEffect(() => {
    if (deadline === null) return
    const id = setInterval(() => {
      const next = secondsUntil(deadline)
      setRemaining(next)
      if (next === 0) clearInterval(id)
    }, 1000)
    return () => clearInterval(id)
  }, [deadline])

  return remaining
}

/**
 * 대기열 내 차례(M05-Q2) → 신청서(M05)의 남은 시간 띠.
 * 남은 시간이 0이 되면 입장 시간 만료 화면(M07-1)으로 보낸다.
 * @returns admitted: 입장 중인지 / remaining: 띠에 보여 줄 남은 초 (입장 중이 아니면 null)
 */
export function useAdmissionTimer(
  raceId: number | undefined,
  raceCourseId: number | undefined,
  mode: AdmissionMode,
) {
  const router = useRouter()
  const { deadline, isFetching } = useQueueAdmission(raceId, raceCourseId, mode)
  const remaining = useAdmissionCountdown(deadline)
  // 끝난 답을 새로 받는 중이면 아직 만료로 보지 않는다 (다시 입장했을 수 있다).
  const expired = remaining === 0 && !isFetching

  useEffect(() => {
    if (expired) router.replace(admissionExpiredHref(raceId))
  }, [expired, raceId, router])

  const live = remaining !== null && !(remaining === 0 && isFetching)
  return { admitted: live, remaining: live ? remaining : null }
}
