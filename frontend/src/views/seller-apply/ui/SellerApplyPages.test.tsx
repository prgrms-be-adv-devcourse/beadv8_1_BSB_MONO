import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { SellerApplyPage, SellerApplyResultPage } from './SellerApplyPages'

const push = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push, back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/seller-apply',
}))

beforeEach(() => push.mockClear())

describe('SellerApplyPage 신청 폼 검사', () => {
  it('약관에 동의하기 전에는 신청하기가 꺼져 있다', () => {
    render(<SellerApplyPage />)
    expect(screen.getByRole('button', { name: '신청하기' })).toBeDisabled()
  })

  it('사업자 필수 칸을 비우면 오류를 보여 주고 이동하지 않는다', async () => {
    const user = userEvent.setup()
    render(<SellerApplyPage />)
    await user.click(screen.getByRole('checkbox', { name: /판매자 이용약관/ }))
    await user.click(screen.getByRole('button', { name: '신청하기' }))
    expect(await screen.findByText('사업자등록번호를 입력해 주세요.')).toBeInTheDocument()
    expect(screen.getByText('상호를 입력해 주세요.')).toBeInTheDocument()
    expect(screen.getByText('대표자를 입력해 주세요.')).toBeInTheDocument()
    expect(push).not.toHaveBeenCalled()
  })

  it('사업자등록번호 형식이 틀리면 막는다', async () => {
    const user = userEvent.setup()
    render(<SellerApplyPage />)
    await user.type(screen.getByLabelText('사업자등록번호'), '1234567890')
    await user.type(screen.getByLabelText('상호'), '(주)러닝')
    await user.type(screen.getByLabelText('대표자'), '김주최')
    await user.click(screen.getByRole('checkbox', { name: /판매자 이용약관/ }))
    await user.click(screen.getByRole('button', { name: '신청하기' }))
    expect(
      await screen.findByText('사업자등록번호를 123-45-67890처럼 입력해 주세요.'),
    ).toBeInTheDocument()
    expect(push).not.toHaveBeenCalled()
  })

  it('올바르게 채우면 결과 화면으로 간다 (정산 계좌는 비워도 된다)', async () => {
    const user = userEvent.setup()
    render(<SellerApplyPage />)
    await user.type(screen.getByLabelText('사업자등록번호'), '123-45-67890')
    await user.type(screen.getByLabelText('상호'), '(주)러닝')
    await user.type(screen.getByLabelText('대표자'), '김주최')
    await user.click(screen.getByRole('checkbox', { name: /판매자 이용약관/ }))
    await user.click(screen.getByRole('button', { name: '신청하기' }))
    await waitFor(() => expect(push).toHaveBeenCalledWith('/seller-apply/result'))
  })

  it('사업자가 아니면 성명 · 연락처만 검사한다', async () => {
    const user = userEvent.setup()
    render(<SellerApplyPage />)
    await user.click(screen.getByRole('radio', { name: '사업자가 아니에요' }))
    await user.click(screen.getByRole('checkbox', { name: /판매자 이용약관/ }))
    await user.click(screen.getByRole('button', { name: '신청하기' }))
    expect(await screen.findByText('성명을 입력해 주세요.')).toBeInTheDocument()
    expect(screen.getByText('연락처를 입력해 주세요.')).toBeInTheDocument()
    expect(push).not.toHaveBeenCalled()
  })
})

describe('SellerApplyResultPage 상태별 카드', () => {
  it.each([
    ['pending', '심사 중', '반려'],
    ['rejected', '반려', '심사 중'],
    ['approved', '승인', '심사 중'],
  ] as const)('%s 상태는 카드 하나만 보여 준다', (status, shown, hidden) => {
    render(<SellerApplyResultPage status={status} />)
    expect(screen.getAllByRole('heading', { level: 2 })).toHaveLength(1)
    expect(screen.getByText(shown)).toBeInTheDocument()
    expect(screen.queryByText(hidden)).not.toBeInTheDocument()
  })

  it('상태를 주지 않으면 심사 중 카드를 보여 준다', () => {
    render(<SellerApplyResultPage />)
    expect(screen.getByText('관리자가 신청 내용을 확인하고 있어요')).toBeInTheDocument()
  })
})
