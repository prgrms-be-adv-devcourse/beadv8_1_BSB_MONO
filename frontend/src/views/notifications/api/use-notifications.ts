'use client'

import { useQuery } from '@tanstack/react-query'
import type { Route } from 'next'

import { useAuthStatus } from '@/features/auth'
import { customFetch, unwrapData, type RsData } from '@/shared/api'
import type { IconName } from '@/shared/ui'

export type Notice = {
  id: string
  icon: IconName
  title: string
  body: string
  at: string
  /** today(오늘) · earlier(이전) */
  group: 'today' | 'earlier'
  href: Route
  unread: boolean
}

/**
 * 내 알림 (GET /api/v1/notification/notifications).
 * 알림 API는 백엔드에 아직 없어 가짜 응답 모드(shared/mocks/notifications.ts)에서만 응답한다.
 * 응답이 없으면(404 등) 알림이 없는 것으로 본다. 로그인 확인이 끝난 뒤에만 부른다.
 */
export function useNotifications() {
  const enabled = useAuthStatus() === 'authenticated'
  return useQuery({
    queryKey: ['notifications'],
    queryFn: async () =>
      unwrapData(await customFetch<RsData<Notice[]>>('/api/v1/notification/notifications')),
    enabled,
    retry: false,
  })
}
