import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { ToastProvider } from '@/shared/ui'

import { AdminSettlementsPage } from './AdminSettlementsPage'

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <AdminSettlementsPage />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('AdminSettlementsPage', () => {
  it('예시 행으로는 지급보류를 걸거나 풀 수 없다', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByText('ST-20261120-2D91'))

    expect(screen.getByRole('button', { name: '지급보류 걸기' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '보류 해제' })).toBeDisabled()
  })
})
