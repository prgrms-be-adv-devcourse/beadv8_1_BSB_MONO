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
  returnOrderId?: number
}

/** completed: 잔액에 들어감 · failed: 승인 거절·확정 실패 · canceled: 사용자가 결제창을 닫음 */
export type ChargeOutcome = 'completed' | 'failed' | 'canceled'

async function confirm({ result, paymentKey, orderId, amount, code }: ChargeReturn) {
  if (result !== 'success') return isChargeCanceled(code) ? 'canceled' : 'failed'
  const pending = readPendingCharge()
  // 떠나기 전에 적어 둔 충전과 다르면(주소를 고친 경우 등) 확정하지 않는다
  if (!paymentKey || !pending || pending.orderId !== orderId || pending.amount !== amount) {
    return 'failed'
  }
  try {
    const charge = unwrapData(
      await cashApi.confirmWalletCharge({
        paymentKey,
        orderId: pending.orderId,
        amount: pending.amount,
      }),
    )
    return charge.outcome === 'COMPLETED' ? 'completed' : 'failed'
  } catch {
    // 네트워크 오류로 결과를 모르면 실패로 보낸다. 실제로 들어갔다면 다시 결제할 때 서버 잔액이 맞춰 준다.
    return 'failed'
  }
}

/**
 * 결제창에서 돌아오면 충전 확정(POST /api/v1/cash/charges/confirm)을 한 번 부르고 결과를 알린다.
 * 같은 orderId로 다시 불러도 서버가 처음 결과를 돌려주므로(멱등) 새로고침해도 두 번 충전되지 않는다.
 */
export function useConfirmChargeReturn(
  params: ChargeReturn,
  onDone: (outcome: ChargeOutcome) => void,
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
    void confirm(current).then((outcome) => {
      clearPendingCharge()
      latest.current.onDone(outcome)
    })
  }, [])
}
