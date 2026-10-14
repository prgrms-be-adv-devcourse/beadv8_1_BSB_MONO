'use client'

import { useEffect, useState } from 'react'

import { HOLD_SECONDS } from '@/features/booking'

/** 마감 시각까지 남은 초. 1초 안쪽이 남아도 1초로 보여 주고, 지나면 0. */
export function secondsUntil(expiresAt: number, now = Date.now()) {
  return Math.max(0, Math.ceil((expiresAt - now) / 1000))
}

/**
 * 자리 잡기 마감 시각(expiresAt)까지 남은 초를 1초마다 다시 센다.
 * 남은 시간을 1씩 빼지 않고 매번 마감 시각과 비교하므로, 탭이 백그라운드에서 느려져도 어긋나지 않는다.
 * 마감 시각이 아직 없으면 정책의 10분을 보여 준다.
 */
export function useHoldCountdown(expiresAt: number | null) {
  const [remaining, setRemaining] = useState(() =>
    expiresAt === null ? HOLD_SECONDS : secondsUntil(expiresAt),
  )

  useEffect(() => {
    if (expiresAt === null) return
    const id = setInterval(() => {
      const next = secondsUntil(expiresAt)
      setRemaining(next)
      if (next === 0) clearInterval(id)
    }, 1000)
    return () => clearInterval(id)
  }, [expiresAt])

  return remaining
}
