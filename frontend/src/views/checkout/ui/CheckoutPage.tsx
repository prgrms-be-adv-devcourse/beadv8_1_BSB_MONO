'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useEffect, useState, type ReactNode } from 'react'

import { AppHeader } from '@/widgets/app-header'
import {
  Alert,
  BottomCta,
  Button,
  buttonClassName,
  Checkbox,
  EmptyState,
  KeyValue,
  Radio,
  RadioGroup,
  Stepper,
  type IconName,
} from '@/shared/ui'

export type CheckoutState = 'default' | 'offline' | 'timeout' | 'soldout' | 'failed'

// 결제가 끝나지 못한 결과 화면 (M07-1 시간 초과 · M07-2 자리 마감 · M07-3 실패)
const RESULT: Record<
  'timeout' | 'soldout' | 'failed',
  {
    icon: IconName
    title: string
    description: ReactNode
    action: string
    href: '/races' | '/checkout'
  }
> = {
  timeout: {
    icon: 'CircleAlert',
    title: '결제 시간이 지났어요',
    description: (
      <>
        10분이 지나 잡아 둔 자리가 풀렸어요.
        <br />
        충전한 예치금은 그대로 남아 있어요.
      </>
    ),
    action: '대회 다시 보기',
    href: '/races',
  },
  soldout: {
    icon: 'TriangleAlert',
    title: '남은 자리가 없어요',
    description: (
      <>
        결제하는 동안 정원이 다 찼거나 접수가 끝났어요.
        <br />
        결제된 금액은 없어요.
      </>
    ),
    action: '다른 대회 보기',
    href: '/races',
  },
  failed: {
    icon: 'CircleAlert',
    title: '결제하지 못했어요',
    description: (
      <>
        잠시 뒤 다시 시도해 주세요.
        <br />
        결제 시간 안이면 잡아 둔 자리는 그대로예요.
      </>
    ),
    action: '다시 결제하기',
    href: '/checkout',
  },
}

// 자리를 잡아 두는 시간 (정책 3장: 10분)
const HOLD_SECONDS = 10 * 60

// 피그마 V2 · M07 결제 · 부족분 충전 (+ 연결 끊김, M07-1~3 결과)
// TODO(api): 주문 생성·예치금 계산·토스페이먼츠 충전을 붙인다. 금액은 시안 예시 값.
export function CheckoutPage({ state = 'default' }: { state?: CheckoutState }) {
  const router = useRouter()
  // 필수 동의는 사용자가 직접 체크한다. 연결 끊김은 이미 동의하고 결제를 누른 뒤라 체크된 채로 둔다.
  const [agreed, setAgreed] = useState(state === 'offline')
  const remaining = useCountdown(state === 'offline' ? 310 : 522)

  if (state === 'timeout' || state === 'soldout' || state === 'failed') {
    const result = RESULT[state]
    return (
      <>
        <AppHeader type="sub" title="결제" close backHref="/" />
        <main className="flex flex-1 items-center justify-center px-4 py-6">
          <EmptyState icon={result.icon} title={result.title} description={result.description} />
        </main>
        <BottomCta
          className="sticky bottom-0 z-10"
          primary={
            <Link href={result.href} className={buttonClassName({ size: 'l' })}>
              {result.action}
            </Link>
          }
        />
      </>
    )
  }

  const offline = state === 'offline'

  return (
    <>
      <AppHeader type="sub" title="결제" />
      <div
        role="timer"
        className="sticky top-(--layout-header-height) z-[5] flex items-center justify-between bg-bg-brand-subtle px-4 py-3 text-text-brand"
      >
        <p className="text-body-s">
          {offline
            ? '연결이 끊긴 동안에도 시간은 흘러요'
            : '자리를 잡아 뒀어요. 시간 안에 결제해 주세요'}
        </p>
        <p className="text-heading-s tabular-nums">{formatTimer(remaining)}</p>
      </div>
      <main className="flex flex-1 flex-col px-4 py-6">
        <div className="flex flex-col gap-7 rounded-lg bg-bg-surface p-6 shadow-sm inset-ring inset-ring-border-default">
          <Stepper steps={['종목 선택', '참가자 정보', '결제']} current={2} />

          {offline && (
            <Alert
              tone="danger"
              title="연결이 끊겼어요"
              description="결제는 한 번만 처리돼요. 다시 연결되면 결제가 됐는지 확인해 드려요."
            />
          )}

          <div className="flex flex-col gap-1">
            <p className="text-body-s text-text-secondary">
              2026 용감한 쿠키RUN in 서울 · 10K · 김러너
            </p>
            <p className="text-[32px] leading-11 font-bold tracking-[-0.015em]">₩45,000</p>
          </div>

          <div className="flex flex-col gap-3 border-t border-border-default pt-4">
            <KeyValue label="예치금에서" value="₩40,000" />
            <KeyValue label="충전 (최소 1만 원)" value="+₩10,000" />
            <KeyValue label="결제 뒤 남는 예치금" value="₩5,000" />
            <p className="text-caption text-text-tertiary">
              1만 원보다 적게 모자라면 1만 원을 충전해요.
              <br />
              결제가 안 돼도 충전한 돈은 예치금으로 남아요.
            </p>
          </div>

          <fieldset className="flex flex-col gap-3">
            <legend className="mb-3 text-label-m">충전 수단</legend>
            <RadioGroup defaultValue="toss">
              <Radio value="toss" label="카드 · 간편결제 (토스페이먼츠)" />
            </RadioGroup>
          </fieldset>

          <div className="flex flex-col gap-3">
            <div className="flex flex-col gap-2 rounded-[12px] px-4 pt-3 pb-4 inset-ring inset-ring-border-default">
              <p className="text-body-s text-text-secondary">환불 규정</p>
              <KeyValue label="환불 기한 10/31 (토) 23:59까지" value="전액 환불" />
              <KeyValue label="환불 기한이 지난 뒤" value="개인 사정 환불 불가" />
              <KeyValue label="대회 취소 · 일정 변경" value="전액 환불" />
            </div>
            <Checkbox
              checked={agreed}
              onCheckedChange={setAgreed}
              label="[필수] 환불 기한을 확인했어요"
            />
          </div>
        </div>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" disabled={!agreed} onClick={() => router.push('/checkout/complete')}>
            {offline ? '다시 시도하기' : '₩10,000 충전하고 결제'}
          </Button>
        }
      />
    </>
  )
}

function useCountdown(initial: number) {
  const [seconds, setSeconds] = useState(Math.min(initial, HOLD_SECONDS))
  useEffect(() => {
    const id = setInterval(() => setSeconds((s) => Math.max(0, s - 1)), 1000)
    return () => clearInterval(id)
  }, [])
  return seconds
}

function formatTimer(seconds: number) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}
