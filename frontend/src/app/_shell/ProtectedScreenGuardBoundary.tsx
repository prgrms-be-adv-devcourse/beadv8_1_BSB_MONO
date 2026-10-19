'use client'

import type { ReactNode } from 'react'

import { ProtectedScreenGuard } from '@/features/auth'

// 서버 컴포넌트인 레이아웃이 features/auth 묶음(훅 파일 포함)을 바로 가져오면 서버 쪽 빌드가 막힌다.
// 클라이언트 파일 하나를 사이에 둔다.
export function ProtectedScreenGuardBoundary({ children }: { children: ReactNode }) {
  return <ProtectedScreenGuard>{children}</ProtectedScreenGuard>
}
