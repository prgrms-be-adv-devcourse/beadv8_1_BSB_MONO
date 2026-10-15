import { describe, expect, it } from 'vitest'

import { splitPayment } from './deposit'

describe('splitPayment', () => {
  it('1만 원보다 적게 모자라면 1만 원을 충전한다', () => {
    expect(splitPayment(45_000, 40_000)).toEqual({
      fromDeposit: 40_000,
      charge: 10_000,
      balanceAfter: 5_000,
    })
  })

  it('1만 원 넘게 모자라면 모자란 만큼 충전한다', () => {
    expect(splitPayment(80_000, 40_000)).toEqual({
      fromDeposit: 40_000,
      charge: 40_000,
      balanceAfter: 0,
    })
  })

  it('예치금이 넉넉하면 충전하지 않는다', () => {
    expect(splitPayment(15_000, 40_000)).toEqual({
      fromDeposit: 15_000,
      charge: 0,
      balanceAfter: 25_000,
    })
  })
})
