import { AppHeader } from '@/widgets/app-header'
import { BottomNav } from '@/widgets/bottom-nav'
import { NotFoundBody } from '@/widgets/status-screens'

// 피그마 세미 · 공통 · 찾을 수 없음
export function NotFoundPage() {
  return (
    <>
      <AppHeader />
      <NotFoundBody />
      <BottomNav />
    </>
  )
}
