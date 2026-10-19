'use client'

import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'

import { ApiError, queueApi, unwrapData } from '@/shared/api'

import { ADMISSION_POLL_MS } from './use-queue-admission'

/** 줄을 잃은 것(404)으로 보고 다시 등록해 볼 수 있는 횟수. 끝없이 되풀이하지 않게 막는다 */
const MAX_REREGISTER = 2

const isId = (id: number | undefined): id is number => Number.isInteger(id) && (id ?? 0) > 0

const statusOf = (error: unknown) => (error instanceof ApiError ? error.status : undefined)

/**
 * 대기열 줄서기 (POST /api/v1/queue → GET /api/v1/queue/races/{raceId}/racecourses/{raceCourseId}).
 * 화면에 들어오면 한 번 등록하고, 입장(canEnter)할 때까지 ADMISSION_POLL_MS마다 순번을 다시 묻는다.
 * 입장 시간이 끝나는 시각은 주문 서비스의 입장 확인(useQueueAdmission)이 따로 알려 주므로,
 * 이 훅은 "이제 입장 확인을 물어도 되는지(canEnter)"만 알려 준다.
 *
 * - 이미 입장한 회원의 등록(409)은 입장 가능으로 본다.
 * - 줄에서 사라지면(순번 조회 404) 다시 등록한다.
 * - 등록이 401이 아닌 오류로 실패하면(대기열 장애 · 접수 기간 아님 등) 줄서기를 건너뛰고 canEnter로 둔다.
 *   입장 여부는 어차피 주문 서비스의 입장 확인이 정하므로, 못 들어오면 그쪽이 막는다.
 */
export function useQueuePosition(raceId: number | undefined, raceCourseId: number | undefined) {
  const enabled = isId(raceId) && isId(raceCourseId)
  const id = { raceId: raceId ?? 0, raceCourseId: raceCourseId ?? 0 }

  const register = useQuery({
    queryKey: ['queue-register', id.raceId, id.raceCourseId],
    queryFn: () => queueApi.register({ raceId: id.raceId, raceCourseId: id.raceCourseId }),
    select: unwrapData,
    enabled,
    retry: false,
    // 화면에 들어올 때마다 새로 줄을 선다 (끝난 입장 · 이전 순번을 이어 쓰지 않는다)
    gcTime: 0,
    staleTime: Infinity,
    refetchOnMount: 'always',
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  })
  const registerStatus = statusOf(register.error)
  const alreadyActive = registerStatus === 409
  const registered = register.isSuccess

  const position = useQuery({
    ...queueApi.getGetStatusQueryOptions(id.raceId, id.raceCourseId),
    select: unwrapData,
    enabled: enabled && registered,
    retry: false,
    gcTime: 0,
    staleTime: 0,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    refetchInterval: (query) => {
      if (query.state.data?.data?.canEnter) return false
      const status = statusOf(query.state.error)
      // 401은 로그인으로, 404는 아래에서 다시 등록하며 멈춘다. 그 밖의 일시 오류는 다음 간격에 다시 묻는다.
      return status === 401 || status === 404 ? false : ADMISSION_POLL_MS
    },
  })

  const lost = statusOf(position.error) === 404
  const reregistered = useRef(0)
  useEffect(() => {
    if (!lost || reregistered.current >= MAX_REREGISTER) return
    reregistered.current += 1
    void register.refetch().then((result) => {
      if (result.isSuccess) void position.refetch()
    })
    // register·position은 매 렌더 새 객체지만 refetch는 같은 쿼리를 가리킨다
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lost])

  const skipped = register.isError && registerStatus !== 401 && !alreadyActive
  const status = position.data ?? register.data
  return {
    /** 이제 입장 확인(주문 서비스)을 물어도 되는지 */
    canEnter: alreadyActive || skipped || position.data?.canEnter === true,
    /** 내 순번(1부터) · 내 앞 인원. 아직 모르면 undefined */
    rank: status && !status.canEnter ? status.myRank : undefined,
    ahead: status && !status.canEnter ? status.ahead : undefined,
    /** 로그인이 필요함(401)이면 true */
    unauthorized: registerStatus === 401 || statusOf(position.error) === 401,
  }
}
