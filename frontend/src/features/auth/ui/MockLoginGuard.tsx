'use client'

import { usePathname, useRouter } from 'next/navigation'
import { useEffect, type ReactNode } from 'react'

import { clientEnv } from '@/shared/config'

import { loginHref } from '../lib/login-redirect'
import { isProtectedPath } from '../lib/protected-path'
import { useAuthStatus } from '../model/use-auth-status'

/**
 * 가짜 응답 모드(API_MOCKING=on)에서만, 로그인해야 하는 화면을 비로그인으로 열면 로그인 화면으로 보낸다.
 * 가짜 응답은 토큰이 없어도 회원 정보를 돌려줘서, 막지 않으면 로그인하지 않은 채 마이 화면이 열리기 때문이다.
 * 실제 API 모드에서는 아무것도 하지 않는다(그대로 통과). 로그인 확인(재발급)이 끝나기 전에는 화면을 그리지 않는다.
 */
export function MockLoginGuard({ children }: { children: ReactNode }) {
  const router = useRouter()
  const pathname = usePathname()
  const status = useAuthStatus()
  const guarded = clientEnv.NEXT_PUBLIC_API_MOCKING === 'on' && isProtectedPath(pathname)

  useEffect(() => {
    if (guarded && status === 'anonymous') {
      router.replace(loginHref(`${pathname}${window.location.search}`))
    }
  }, [guarded, status, pathname, router])

  if (guarded && status !== 'authenticated') {
    return null
  }
  return children
}
