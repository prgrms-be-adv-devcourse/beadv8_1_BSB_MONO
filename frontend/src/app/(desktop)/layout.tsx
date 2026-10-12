import type { ReactNode } from 'react'

import { DesktopHeader } from '@/widgets/desktop-header'

// 판매자 등록 신청처럼 아직 판매자가 아닌 회원이 PC에서 보는 화면 (사이드바 없음)
export default function DesktopLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-dvh flex-1 flex-col">
      <DesktopHeader />
      {children}
    </div>
  )
}
