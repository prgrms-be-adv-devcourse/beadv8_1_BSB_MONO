import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ADMISSION_POLL_MS } from '@/features/queue-admission'
import { orderMock, server } from '@/shared/testing'

import { RaceQueuePage } from './RaceQueuePage'

const replace = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn(), replace }),
  usePathname: () => '/races/1/queue',
}))

const NOT_ADMITTED = { admitted: false, admissionEndsAt: null, remainingSeconds: 0 }
const admitted = (remainingSeconds: number) => ({
  admitted: true,
  admissionEndsAt: '2026-10-16T10:10:00',
  remainingSeconds,
})

function renderQueue(props: { previewRemainingSeconds?: number } = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <RaceQueuePage raceId="1" optionId="2" {...props} />
    </QueryClientProvider>,
  )
}

describe('RaceQueuePage', () => {
  beforeEach(() => {
    // MSW 응답(Promise)이 흐르도록 실제 시간도 같이 흘려보낸다.
    vi.useFakeTimers({ shouldAdvanceTime: true })
    replace.mockClear()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('입장 전이면 차례 기다리기(M05-Q1)를 값 없는 막대와 함께 보여 준다', async () => {
    server.use(orderMock.getFindAdmissionMockHandler({ status: 200, data: NOT_ADMITTED }))
    renderQueue()

    expect(screen.getByRole('heading', { name: '대기열' })).toBeInTheDocument()
    expect(screen.getByText('차례를 기다리고 있어요')).toBeInTheDocument()
    expect(screen.getByText(/차례가 되면 이 화면에서 바로 신청할 수 있어요\./)).toBeInTheDocument()
    expect(screen.getByText(/이 화면을 나가면 다시 줄을 서야 해요\./)).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).not.toHaveAttribute('aria-valuenow')
    expect(screen.getByRole('link', { name: '대회로 돌아가기' })).toHaveAttribute(
      'href',
      '/races/1',
    )
    await act(() => vi.advanceTimersByTimeAsync(ADMISSION_POLL_MS))
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
  })

  it('입장할 때까지 다시 묻다가 입장하면 내 차례(M05-Q2)로 바뀐다', async () => {
    // 두 번은 기다리는 중, 세 번째에 입장한다
    let asked = 0
    server.use(
      orderMock.getFindAdmissionMockHandler(() => {
        asked += 1
        return { status: 200, data: asked < 3 ? NOT_ADMITTED : admitted(600) }
      }),
    )
    renderQueue()

    await vi.waitFor(() => expect(asked).toBe(1))
    expect(screen.getByText('차례를 기다리고 있어요')).toBeInTheDocument()

    await act(() => vi.advanceTimersByTimeAsync(ADMISSION_POLL_MS))
    await vi.waitFor(() => expect(asked).toBe(2))
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()

    await act(() => vi.advanceTimersByTimeAsync(ADMISSION_POLL_MS))
    expect(await screen.findByRole('timer')).toHaveTextContent(
      '내 차례예요. 시간 안에 신청과 결제를 마쳐 주세요10:00',
    )
    expect(screen.getByText('내 차례예요')).toBeInTheDocument()
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: '신청하러 가기' })).toHaveAttribute(
      'href',
      '/races/1/apply?option=2',
    )

    // 입장하면 더 묻지 않는다
    await act(() => vi.advanceTimersByTimeAsync(ADMISSION_POLL_MS * 2))
    expect(asked).toBe(3)
  })

  it('남은 시간이 1초씩 줄고 0이 되면 입장 시간 만료 화면(M07-1)으로 간다', async () => {
    server.use(orderMock.getFindAdmissionMockHandler({ status: 200, data: admitted(2) }))
    renderQueue()

    expect(await screen.findByRole('timer')).toHaveTextContent('00:02')
    await act(() => vi.advanceTimersByTimeAsync(1000))
    expect(screen.getByRole('timer')).toHaveTextContent('00:01')
    expect(replace).not.toHaveBeenCalled()

    await act(() => vi.advanceTimersByTimeAsync(1000))
    await vi.waitFor(() =>
      expect(replace).toHaveBeenCalledWith('/checkout?result=timeout&raceId=1'),
    )
  })

  it('시안 미리보기는 API를 부르지 않고 시안의 남은 시간을 보여 준다', () => {
    renderQueue({ previewRemainingSeconds: 583 })

    expect(screen.getByRole('timer')).toHaveTextContent(
      '내 차례예요. 시간 안에 신청과 결제를 마쳐 주세요09:43',
    )
  })
})
