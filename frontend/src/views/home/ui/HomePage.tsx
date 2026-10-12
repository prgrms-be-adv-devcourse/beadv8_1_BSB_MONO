import { AppHeader } from '@/widgets/app-header'
import { BottomNav } from '@/widgets/bottom-nav'

export function HomePage() {
  return (
    <>
      <AppHeader />
      <main className="flex-1 px-(--layout-page-margin) py-8">
        <h1 className="text-heading-l">러닝 대회 접수</h1>
      </main>
      <BottomNav />
    </>
  )
}
