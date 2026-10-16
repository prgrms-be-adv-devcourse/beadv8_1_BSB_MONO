import { act, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { RACE_DETAIL } from '@/entities/race'
import { clearAccessToken, setAccessToken } from '@/shared/api'
import { ToastProvider } from '@/shared/ui'

import { RaceDetailPage } from './RaceDetailPage'

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/races/1',
}))

function renderPage() {
  render(
    <ToastProvider>
      <RaceDetailPage race={RACE_DETAIL} />
    </ToastProvider>,
  )
  return screen.getByRole('link', { name: '신청하기' })
}

describe('RaceDetailPage 신청하기', () => {
  it('로그인 전이면 로그인 화면으로 보내고, 로그인 뒤 갈 대기열 주소를 next로 넘긴다', () => {
    clearAccessToken()
    const href = renderPage().getAttribute('href') ?? ''
    const url = new URL(href, 'http://localhost')
    expect(url.pathname).toBe('/login')
    expect(url.searchParams.get('next')).toMatch(
      new RegExp(`^/races/${RACE_DETAIL.id}/queue\\?option=`),
    )
  })

  it('로그인했으면 대기열(M05-Q1)로 보낸다', () => {
    const link = renderPage()
    act(() => setAccessToken('access-token'))
    expect(link.getAttribute('href')).toMatch(
      new RegExp(`^/races/${RACE_DETAIL.id}/queue\\?option=`),
    )
  })
})
