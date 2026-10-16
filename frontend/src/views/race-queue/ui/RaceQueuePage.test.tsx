import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { RaceQueuePage } from './RaceQueuePage'

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/races/1/queue',
}))

describe('RaceQueuePage', () => {
  it('입장 전이면 차례 기다리기(M05-Q1)를 값 없는 막대와 함께 보여 준다', () => {
    render(<RaceQueuePage raceId="1" optionId="2" />)

    expect(screen.getByRole('heading', { name: '대기열' })).toBeInTheDocument()
    expect(screen.getByText('차례를 기다리고 있어요')).toBeInTheDocument()
    expect(screen.getByText(/차례가 되면 이 화면에서 바로 신청할 수 있어요\./)).toBeInTheDocument()
    expect(screen.getByText(/이 화면을 나가면 다시 줄을 서야 해요\./)).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).not.toHaveAttribute('aria-valuenow')
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: '대회로 돌아가기' })).toHaveAttribute(
      'href',
      '/races/1',
    )
  })

  it('내 차례(M05-Q2)면 남은 시간 띠와 신청 화면 이동을 보여 준다', () => {
    render(
      <RaceQueuePage
        raceId="1"
        optionId="2"
        admission={{ admitted: true, admissionEndsAt: null, remainingSeconds: 583 }}
      />,
    )

    expect(screen.getByRole('timer')).toHaveTextContent(
      '내 차례예요. 시간 안에 신청과 결제를 마쳐 주세요09:43',
    )
    expect(screen.getByText('내 차례예요')).toBeInTheDocument()
    expect(screen.getByText(/남은 시간 안에 참가 신청과 결제를 마쳐 주세요\./)).toBeInTheDocument()
    expect(screen.getByText(/시간이 지나면 다시 줄을 서야 해요\./)).toBeInTheDocument()
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: '신청하러 가기' })).toHaveAttribute(
      'href',
      '/races/1/apply?option=2',
    )
  })
})
