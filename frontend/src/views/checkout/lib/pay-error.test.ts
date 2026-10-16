import { describe, expect, it } from 'vitest'

import { ApiError } from '@/shared/api'

import { toPayFailure } from './pay-error'

const conflict = (message: string) =>
  new ApiError(409, { status: 409, message, data: null }, '/api/v1/order/orders/1/payment')

describe('toPayFailure', () => {
  it('409 문구로 잔액 부족 · 기한 지남 · 매진을 나눈다', () => {
    expect(
      toPayFailure(
        conflict('예치금이 12,000원 부족합니다. 충전한 뒤 결제 기한 안에 다시 결제해주세요.'),
      ).kind,
    ).toBe('insufficient')
    expect(toPayFailure(conflict('결제 기한이 지났습니다.')).kind).toBe('timeout')
    expect(toPayFailure(conflict('결제할 수 없는 주문입니다(현재 상태: 기한 만료).')).kind).toBe(
      'timeout',
    )
    expect(toPayFailure(conflict('남은 자리가 없습니다.')).kind).toBe('soldout')
  })

  it('대기열 입장 시간이 끝나 막히면(403 ORDER_QUEUE_NOT_ADMITTED) 문구와 상관없이 시간 초과로 본다', () => {
    const error = new ApiError(
      403,
      { status: 403, code: 'ORDER_QUEUE_NOT_ADMITTED', message: '아무 문구', data: null },
      '/api/v1/order/orders/1/payment',
    )
    expect(toPayFailure(error)).toEqual({ kind: 'timeout' })
  })

  it('409가 아니거나 서버에 닿지 못하면 실패로 본다', () => {
    expect(toPayFailure(new ApiError(500, null, '/x')).kind).toBe('failed')
    expect(toPayFailure(new TypeError('Failed to fetch')).kind).toBe('failed')
  })
})
