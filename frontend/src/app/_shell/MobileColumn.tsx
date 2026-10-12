import type { ReactNode } from 'react'

// 피그마 "viewport · 480": 바깥은 bg-subtle, 기둥은 bg-default. app-mobile이 글자·여백을 모바일 값으로 고정한다.
export function MobileColumn({ children }: { children: ReactNode }) {
  return (
    <div className="flex flex-1 justify-center bg-bg-subtle">
      <div className="app-mobile flex min-h-dvh w-full max-w-(--layout-app-max-width) flex-col bg-bg-default">
        {children}
      </div>
    </div>
  )
}
