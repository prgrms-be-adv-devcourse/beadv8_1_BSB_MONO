'use client'

import { Dialog } from '@/shared/ui'

// 피그마 V2 · M06-Q 장바구니 · 대기열 입장 전 결제 (Dialog)
// 결제가 403 ORDER_QUEUE_NOT_ADMITTED로 막히면 띄운다. 서버 message 대신 이 창 문구를 쓴다.
export function QueueRequiredDialog({
  open,
  onOpenChange,
  onQueue,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** "줄 서기" — 장바구니에 담긴 대회 · 종목의 대기열(M05-Q1)로 간다 */
  onQueue: () => void
}) {
  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="아직 결제할 차례가 아니에요"
      body={
        <>
          대기열에서 차례가 된 뒤 결제할 수 있어요.
          <br />
          줄을 서서 차례를 기다려 주세요.
        </>
      }
      cancelLabel="닫기"
      confirmLabel="줄 서기"
      onConfirm={onQueue}
    />
  )
}
