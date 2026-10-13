import type { ReactNode } from 'react'

import { ConsoleSidebar } from '@/widgets/console-sidebar'

export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-dvh flex-1">
      <ConsoleSidebar role="admin" />
      {children}
    </div>
  )
}
