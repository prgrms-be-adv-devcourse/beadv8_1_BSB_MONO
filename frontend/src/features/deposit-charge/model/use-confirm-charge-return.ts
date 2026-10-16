'use client'

import { useEffect, useRef } from 'react'

import { cashApi, unwrapData } from '@/shared/api'

import { clearPendingCharge, readPendingCharge } from '../lib/pending-charge'
import { isChargeCanceled } from './request-charge'

/** 토스 결제창이 돌아오며 붙여 준 값 (successUrl·failUrl의 쿼리) */
export type ChargeReturn = {
  result?: string
  paymentKey?: string
  orderId?: string
  amount?: number
  /** failUrl에만 붙는 토스 오류 코드 */
  code?: string
}

/** completed: 잔액에 들어감 · failed: 승인 거절·확정 실패 · canceled: 사용자가 결제창을 닫음 */
export type ChargeOutcome = 'completed' | 'failed' | 'canceled'

type ChargeResult = {
  outcome: ChargeOutcome
  /**
   * 돌아가 다시 결제할 주문. 주소가 아니라 떠나기 전에 적어 둔 값(같은 충전 orderId일 때만)을 쓴다.
   * 주소를 고치거나 다른 충전의 주소를 다시 열어 상관없는 주문이 저절로 결제되지 않게 한다.
   */
  returnOrderId?: number
}

async function confirm({
  result,
  paymentKey,
  orderId,
  amount,
  code,
}: ChargeReturn): Promise<ChargeResult> {
  const pending = readPendingCharge()
  const sameCharge = pending !== null && pending.orderId === orderId
  const returnOrderId = sameCharge ? pending.returnOrderId : undefined
  if (result !== 'success') {
    return { outcome: isChargeCanceled(code) ? 'canceled' : 'failed', returnOrderId }
  }
  // 떠나기 전에 적어 둔 충전과 다르면(주소를 고친 경우 등) 확정하지 않는다
  if (!paymentKey || !sameCharge || pending.amount !== amount) {
    return { outcome: 'failed', returnOrderId }
  }
  try {
    const charge = unwrapData(
      await cashApi.confirmWalletCharge({
        paymentKey,
        orderId: pending.orderId,
        amount: pending.amount,
      }),
    )
    return { outcome: charge.outcome === 'COMPLETED' ? 'completed' : 'failed', returnOrderId }
  } catch {
    // 네트워크 오류로 결과를 모르면 실패로 보낸다. 실제로 들어갔다면 다시 결제할 때 서버 잔액이 맞춰 준다.
    return { outcome: 'failed', returnOrderId }
  }
}

/**
 * 결제창에서 돌아오면 충전 확정(POST /api/v1/cash/charges/confirm)을 한 번 부르고 결과를 알린다.
 * 같은 orderId로 다시 불러도 서버가 처음 결과를 돌려주므로(멱등) 새로고침해도 두 번 충전되지 않는다.
 */
export function useConfirmChargeReturn(
  params: ChargeReturn,
  onDone: (result: ChargeResult) => void,
) {
  const started = useRef(false)
  const latest = useRef({ params, onDone })
  useEffect(() => {
    latest.current = { params, onDone }
  })
  useEffect(() => {
    // 개발 모드의 effect 두 번 실행에도 한 번만 부른다
    if (started.current) return
    started.current = true
    const { params: current } = latest.current
    void confirm(current).then((result) => {
      clearPendingCharge()
      latest.current.onDone(result)
    })
  }, [])
}
