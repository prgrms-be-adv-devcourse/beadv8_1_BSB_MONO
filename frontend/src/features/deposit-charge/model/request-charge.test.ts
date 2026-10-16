import { beforeEach, describe, expect, it, vi } from 'vitest'

import { readPendingCharge } from '../lib/pending-charge'
import { chargeAmountFor, requestCharge } from './request-charge'

const env = (patch: { mocking?: boolean } = {}) => ({
  mocking: false,
  clientKey: undefined,
  origin: 'https://crewrun.test',
  navigate: vi.fn<(url: string) => void>(),
  ...patch,
})

beforeEach(() => sessionStorage.clear())

describe('chargeAmountFor', () => {
  it('1만 원보다 적게 모자라면 1만 원, 그 이상이면 모자란 만큼 충전한다', () => {
    expect(chargeAmountFor(0)).toBe(10_000)
    expect(chargeAmountFor(5_000)).toBe(10_000)
    expect(chargeAmountFor(12_000)).toBe(12_000)
  })
})

describe('requestCharge', () => {
  it('가짜 응답 모드에서는 결제창 대신 성공 주소로 바로 보내고, 충전 orderId · 금액 · 돌아갈 주문을 적어 둔다', async () => {
    const mock = env({ mocking: true })

    await expect(requestCharge({ amount: 12_000, returnOrderId: 1 }, mock)).resolves.toBe(
      'redirecting',
    )

    const pending = readPendingCharge()
    expect(pending).toMatchObject({ amount: 12_000, returnOrderId: 1 })
    expect(pending?.orderId).toMatch(/^charge-[0-9a-f-]{36}$/)

    const url = new URL(mock.navigate.mock.calls[0][0])
    expect(url.origin + url.pathname).toBe('https://crewrun.test/me/deposit/charge/return')
    expect(url.searchParams.get('result')).toBe('success')
    expect(url.searchParams.get('returnOrderId')).toBe('1')
    expect(url.searchParams.get('orderId')).toBe(pending?.orderId)
    expect(url.searchParams.get('amount')).toBe('12000')
    expect(url.searchParams.get('paymentKey')).toMatch(/^mock-payment-/)
  })

  it('누를 때마다 새 충전 orderId를 만든다 (확정 전 재시도는 돌아온 화면이 같은 값을 쓴다)', async () => {
    const mock = env({ mocking: true })
    await requestCharge({ amount: 10_000 }, mock)
    const first = readPendingCharge()?.orderId
    await requestCharge({ amount: 10_000 }, mock)

    expect(readPendingCharge()?.orderId).not.toBe(first)
  })

  it('클라이언트 키가 없으면 결제창을 띄우지 않고 실패한다', async () => {
    const real = env()

    await expect(requestCharge({ amount: 10_000 }, real)).rejects.toThrow(
      'NEXT_PUBLIC_TOSS_CLIENT_KEY',
    )
    expect(real.navigate).not.toHaveBeenCalled()
  })
})
