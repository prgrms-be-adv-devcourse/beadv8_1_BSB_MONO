import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { setAccessToken } from '@/shared/api'
import { server } from '@/shared/testing'

import { maskAccount } from '../lib/refund'
import { DepositRefundDonePage, DepositRefundPage } from './DepositRefundPages'

const push = vi.fn()
const replace = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push, back: vi.fn(), replace }),
  usePathname: () => '/me/deposit/refund',
}))

function renderWithClient(ui: React.ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>)
}

const balanceHandler = (balance: number) =>
  http.get('*/api/v1/cash/wallets/:holderId/balance', () =>
    HttpResponse.json({ status: 200, message: '잔액 조회 성공', data: { holderId: 1, balance } }),
  )

type WithdrawalBody = {
  bankCode: string
  accountNumber: string
  accountHolder: string
  idempotencyKey: string
}

async function fillAccount(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('계좌번호'), '123456-01-234567')
  await user.type(screen.getByLabelText('예금주'), '김러너')
}

const submit = (amount = '₩5,000') =>
  screen.findByRole('button', { name: `${amount} 돌려받기 신청` })

describe('예치금 돌려받기 (M22)', () => {
  beforeEach(() => {
    push.mockClear()
    replace.mockClear()
    sessionStorage.clear()
    setAccessToken('access-token')
  })

  it('남은 잔액 전부를 은행 코드 · 숫자만 남긴 계좌번호와 함께 보내고 완료 화면으로 간다', async () => {
    const bodies: WithdrawalBody[] = []
    server.use(
      http.post('*/api/v1/cash/withdrawals', async ({ request }) => {
        bodies.push((await request.json()) as WithdrawalBody)
        return HttpResponse.json({
          status: 200,
          message: '아무 문구',
          data: { withdrawnAmount: 5000, balance: 0 },
        })
      }),
    )
    const user = userEvent.setup()
    renderWithClient(<DepositRefundPage />)

    const button = await submit()
    expect(button).toBeDisabled()
    await fillAccount(user)
    await user.click(button)

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/me/deposit/refund/done'))
    expect(bodies).toHaveLength(1)
    expect(bodies[0]).toMatchObject({
      bankCode: '004',
      accountNumber: '12345601234567',
      accountHolder: '김러너',
    })
    expect(bodies[0].idempotencyKey).toMatch(/^[0-9a-f-]{36}$/)
  })

  it('잔액이 0원이면 신청 버튼을 막는다', async () => {
    server.use(balanceHandler(0))
    const user = userEvent.setup()
    renderWithClient(<DepositRefundPage />)

    const button = await submit('₩0')
    await fillAccount(user)

    expect(button).toBeDisabled()
  })

  it('두 번 눌러도 요청은 한 번만 간다', async () => {
    const keys: string[] = []
    let release: () => void = () => {}
    server.use(
      http.post('*/api/v1/cash/withdrawals', async ({ request }) => {
        keys.push(((await request.json()) as WithdrawalBody).idempotencyKey)
        await new Promise<void>((resolve) => (release = resolve))
        return HttpResponse.json({
          status: 200,
          message: '아무 문구',
          data: { withdrawnAmount: 5000, balance: 0 },
        })
      }),
    )
    const user = userEvent.setup()
    renderWithClient(<DepositRefundPage />)

    const button = await submit()
    await fillAccount(user)
    await user.dblClick(button)
    await vi.waitFor(() => expect(keys).toHaveLength(1))
    expect(button).toBeDisabled()
    release()

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/me/deposit/refund/done'))
    expect(keys).toHaveLength(1)
  })

  it('네트워크 오류 뒤 다시 누르면 처음과 같은 키로 보낸다', async () => {
    const keys: string[] = []
    server.use(
      http.post('*/api/v1/cash/withdrawals', async ({ request }) => {
        keys.push(((await request.json()) as WithdrawalBody).idempotencyKey)
        if (keys.length === 1) return HttpResponse.error()
        return HttpResponse.json({
          status: 200,
          message: '아무 문구',
          data: { withdrawnAmount: 5000, balance: 0 },
        })
      }),
    )
    const user = userEvent.setup()
    renderWithClient(<DepositRefundPage />)

    const button = await submit()
    await fillAccount(user)
    await user.click(button)
    expect(await screen.findByText('잠시 뒤 다시 시도해 주세요.')).toBeInTheDocument()
    await user.click(button)

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/me/deposit/refund/done'))
    expect(keys).toHaveLength(2)
    expect(keys[1]).toBe(keys[0])
  })

  it('서버가 5xx로 답해도 다음 시도에 같은 키를 쓰고, 4xx로 거절하면 새 키를 쓴다', async () => {
    const keys: string[] = []
    const answers = [500, 400, 200]
    server.use(
      http.post('*/api/v1/cash/withdrawals', async ({ request }) => {
        keys.push(((await request.json()) as WithdrawalBody).idempotencyKey)
        const status = answers[keys.length - 1]
        if (status !== 200) {
          return HttpResponse.json({ status, message: '아무 문구', data: null }, { status })
        }
        return HttpResponse.json({
          status: 200,
          message: '아무 문구',
          data: { withdrawnAmount: 5000, balance: 0 },
        })
      }),
    )
    const user = userEvent.setup()
    renderWithClient(<DepositRefundPage />)

    const button = await submit()
    await fillAccount(user)
    await user.click(button)
    await screen.findByText('잠시 뒤 다시 시도해 주세요.')
    await user.click(button)
    await vi.waitFor(() => expect(keys).toHaveLength(2))
    await vi.waitFor(() => expect(button).toBeEnabled())
    await user.click(button)

    await vi.waitFor(() => expect(push).toHaveBeenCalledWith('/me/deposit/refund/done'))
    expect(keys[1]).toBe(keys[0])
    expect(keys[2]).not.toBe(keys[1])
  })

  it('가짜 응답 모드에서는 같은 키로 두 번 보내도 한 번만 빠지고 같은 결과를 받는다', async () => {
    const send = (idempotencyKey: string) =>
      fetch('http://localhost/api/v1/cash/withdrawals', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          bankCode: '004',
          accountNumber: '1',
          accountHolder: '김러너',
          idempotencyKey,
        }),
      }).then((r) => r.json() as Promise<{ data: { withdrawnAmount: number; balance: number } }>)

    const first = await send('same-key')
    const again = await send('same-key')

    expect(first.data).toEqual({ withdrawnAmount: 5000, balance: 0 })
    expect(again.data).toEqual({ withdrawnAmount: 5000, balance: 0 })
    // 다른 키로 보내면 잔액이 없어 거절된다
    const other = await fetch('http://localhost/api/v1/cash/withdrawals', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idempotencyKey: 'other-key' }),
    })
    expect(other.status).toBe(400)
  })
})

describe('예치금 돌려받기 · 환급 완료 (M22-1)', () => {
  beforeEach(() => {
    replace.mockClear()
    sessionStorage.clear()
  })

  it('신청 화면이 넘긴 값으로 환급 금액 · 가린 계좌 · 지금 예치금을 보여 준다', () => {
    sessionStorage.setItem(
      'crewrun:last-withdrawal',
      JSON.stringify({
        amount: 5000,
        balance: 0,
        bankName: '국민은행',
        maskedAccount: '123456-**-***567',
        accountHolder: '김러너',
        at: new Date('2026-10-06T14:20:00+09:00').getTime(),
      }),
    )
    renderWithClient(<DepositRefundDonePage />)

    expect(screen.getByRole('heading', { name: '예치금을 돌려받았어요' })).toBeInTheDocument()
    expect(screen.getByText('₩5,000')).toBeInTheDocument()
    expect(screen.getByText('국민은행 123456-**-***567')).toBeInTheDocument()
    expect(screen.getByText('₩0')).toBeInTheDocument()
    expect(screen.getByText('10/6 (화) 14:20')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '예치금 보기' })).toHaveAttribute('href', '/me/deposit')
  })

  it('넘겨받은 값이 없으면(주소로 바로 들어옴) 예치금 화면으로 보낸다', async () => {
    renderWithClient(<DepositRefundDonePage />)

    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/me/deposit'))
  })
})

describe('maskAccount', () => {
  it('앞 6자리와 끝 3자리만 남기고 하이픈은 그대로 둔다', () => {
    expect(maskAccount('123456-01-234567')).toBe('123456-**-***567')
    expect(maskAccount('110123456789')).toBe('110123***789')
  })
})
