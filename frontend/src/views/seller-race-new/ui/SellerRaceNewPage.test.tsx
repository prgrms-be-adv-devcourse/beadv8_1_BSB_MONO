import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'

import { server } from '@/shared/testing'
import { ToastProvider } from '@/shared/ui'

import { RACE_FORM_ERRORS } from '../model/race-form'
import { SellerRaceNewPage } from './SellerRaceNewPage'

// 화면이 '오늘'을 불러올 때 읽으므로 가져오기 전에 날짜를 고정한다 (지난 날은 고를 수 없다)
vi.hoisted(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 1))
})

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/seller/races/new',
}))

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <SellerRaceNewPage />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

/** 달력 칸에서 날짜(들)를 고르고 선택 완료를 누른다 (shared/ui date-time-field.test와 같은 방식) */
async function pickDates(label: string, days: number[]) {
  const group = screen.getByRole('group', { name: label })
  await userEvent.click(within(group).getAllByRole('button')[0])
  for (const day of days) {
    await userEvent.click(screen.getByRole('button', { name: new RegExp(`10월 ${day}일`) }))
  }
  await userEvent.click(screen.getByRole('button', { name: '선택 완료' }))
}

/** 시간 칸에 숫자만 친다 (콜론은 칸이 넣는다) */
async function typeTime(name: string, digits: string) {
  await userEvent.type(screen.getByRole('textbox', { name }), digits)
}

describe('SellerRaceNewPage', () => {
  it('빈 칸과 대표 이미지가 없으면 등록 요청을 보내지 않고 오류를 보여 준다', async () => {
    let requested = false
    server.use(
      http.post('*/api/v1/product/races', () => {
        requested = true
        return HttpResponse.json({ status: 201, message: '', data: {} }, { status: 201 })
      }),
    )
    renderPage()
    await userEvent.click(screen.getByRole('button', { name: '등록하기' }))

    expect(await screen.findByText(RACE_FORM_ERRORS.titleRequired)).toBeInTheDocument()
    expect(screen.getByText(RACE_FORM_ERRORS.sidoRequired)).toBeInTheDocument()
    expect(screen.getByText(RACE_FORM_ERRORS.coverRequired)).toBeInTheDocument()
    expect(screen.getAllByText(RACE_FORM_ERRORS.dateRequired)).toHaveLength(3)
    expect(requested).toBe(false)
  })

  it('대회와 같은 날 접수 마감 시간이 대회 시각보다 늦으면 알려 준다', async () => {
    renderPage()
    await pickDates('대회 일시', [25])
    await typeTime('대회 일시 시간', '0800')
    // 대회일 뒤 날짜는 달력에서 고를 수 없으므로, 같은 날(10/25) 18:00으로 마감한다
    await pickDates('접수 기간', [20, 25])
    await typeTime('접수 기간 시작 시간', '1000')
    await typeTime('접수 기간 끝 시간', '1800')
    await userEvent.click(screen.getByRole('button', { name: '등록하기' }))
    expect(await screen.findByText(RACE_FORM_ERRORS.closeBeforeRace)).toBeInTheDocument()
  })

  it('등록하기를 누르기 전에도 순서 오류를 칸 아래에 보여 주고, 대회 일시를 바꾸면 다시 계산한다', async () => {
    renderPage()
    await pickDates('대회 일시', [25])
    await typeTime('대회 일시 시간', '0800')
    await pickDates('접수 기간', [20, 24])
    await typeTime('접수 기간 시작 시간', '1000')
    await typeTime('접수 기간 끝 시간', '1800')
    expect(screen.queryByText(RACE_FORM_ERRORS.closeBeforeRace)).not.toBeInTheDocument()

    // 대회를 접수 마감(10/24) 전날로 당기면 접수 칸에 바로 오류가 생긴다
    await pickDates('대회 일시', [23])
    expect(await screen.findByText(RACE_FORM_ERRORS.closeBeforeRace)).toBeInTheDocument()

    // 다시 뒤로 미루면 오류가 사라진다
    await pickDates('대회 일시', [26])
    await waitFor(() =>
      expect(screen.queryByText(RACE_FORM_ERRORS.closeBeforeRace)).not.toBeInTheDocument(),
    )
  })

  it('접수 기간 달력에서 이틀 뒤 전 날과 대회일 뒤 날은 고를 수 없다', async () => {
    renderPage()
    await pickDates('대회 일시', [25])
    const group = screen.getByRole('group', { name: '접수 기간' })
    await userEvent.click(within(group).getAllByRole('button')[0])
    // 오늘은 10/1(테스트 고정), 이틀 뒤는 10/3
    expect(screen.getByRole('button', { name: /10월 2일/ })).toBeDisabled()
    expect(screen.getByRole('button', { name: /10월 3일/ })).toBeEnabled()
    expect(screen.getByRole('button', { name: /10월 25일/ })).toBeEnabled()
    expect(screen.getByRole('button', { name: /10월 26일/ })).toBeDisabled()
  })

  it('시간 칸은 숫자만 쳐도 콜론을 넣고, 15분 단위가 아니면 알려 준다', async () => {
    renderPage()
    await pickDates('대회 일시', [25])
    const time = screen.getByRole('textbox', { name: '대회 일시 시간' })
    await userEvent.type(time, '0807')
    expect(time).toHaveValue('08:07')
    await userEvent.click(screen.getByRole('button', { name: '등록하기' }))
    expect(await screen.findByText(RACE_FORM_ERRORS.timeQuarter)).toBeInTheDocument()
  })

  it('종목 표는 등록할 종목 값으로 그린다', () => {
    renderPage()
    expect(screen.getByRole('cell', { name: '₩45,000' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: '2,000명' })).toBeInTheDocument()
  })
})
