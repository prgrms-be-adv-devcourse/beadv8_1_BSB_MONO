'use client'

import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { Alert, BottomCta, Button, KeyValue } from '@/shared/ui'

const STEPS = [10_000, 50_000, 100_000]
const BALANCE = 5_000
const won = (n: number) => n.toLocaleString('ko-KR')

// 피그마 V2 · M21 예치금 충전
// TODO(api): 토스페이먼츠 결제창을 띄운다
export function DepositChargePage() {
  const router = useRouter()
  const [amount, setAmount] = useState(40_000)

  return (
    <>
      <AppHeader type="sub" title="예치금 충전" />
      <main className="flex flex-1 flex-col gap-4 px-4 py-6">
        <Alert
          title="결제에 ₩40,000이 부족해요"
          description="부족한 만큼 충전하면 바로 결제로 돌아가요. 1만 원보다 적게 모자라면 1만 원을 충전해요."
        />
        <section className="flex flex-col gap-4 rounded-lg bg-bg-surface p-5 shadow-sm">
          <label htmlFor="charge-amount" className="text-label-m">
            충전할 금액
          </label>
          <div className="flex items-baseline justify-end gap-1 rounded-md px-4 py-3 inset-ring inset-ring-border-strong">
            <input
              id="charge-amount"
              inputMode="numeric"
              value={won(amount)}
              onChange={(e) => setAmount(Number(e.target.value.replace(/\D/g, '')) || 0)}
              className="w-full bg-transparent text-right font-display text-display-m outline-none"
            />
            <span className="text-label-m">원</span>
          </div>
          <div className="flex gap-2">
            {STEPS.map((step) => (
              <Button
                key={step}
                variant="secondary"
                className="flex-1 justify-start"
                onClick={() => setAmount((a) => a + step)}
              >
                +{step / 10_000}만
              </Button>
            ))}
          </div>
          <p className="text-caption text-text-tertiary">1만 원부터 충전할 수 있어요.</p>
          <div className="flex flex-col gap-2 rounded-md bg-bg-subtle p-4">
            <KeyValue label="지금 예치금" value={`₩${won(BALANCE)}`} />
            <div className="flex items-center justify-between">
              <span className="text-body-s text-text-tertiary">충전 후 예치금</span>
              <span className="text-label-strong">₩{won(BALANCE + amount)}</span>
            </div>
          </div>
        </section>
        <section className="flex flex-col gap-2 rounded-lg bg-bg-surface p-5 shadow-sm">
          <KeyValue label="충전 방법" value="토스페이먼츠 (카드 · 계좌이체)" />
          <p className="text-caption text-text-tertiary">
            쓰지 않은 예치금은 언제든 전부 돌려받을 수 있어요.
          </p>
        </section>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" disabled={amount < 10_000} onClick={() => router.push('/checkout')}>
            ₩{won(amount)} 충전하기
          </Button>
        }
      />
    </>
  )
}
