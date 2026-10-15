'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useEffect, useState, type ReactNode } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { SAMPLE_PARTICIPANT, useBookingFlow } from '@/features/booking'
import { parseKstDateTime, RACE_DETAIL, raceDetailQueryOptions } from '@/entities/race'
import { orderApi, unwrapData, type orderModel } from '@/shared/api'
import { formatNumber } from '@/shared/lib'
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
  Skeleton,
  Stepper,
  type IconName,
} from '@/shared/ui'

import { useDepositBalance } from '../api/deposit-balance'
import { splitPayment } from '../lib/deposit'
import { toPayFailure, type PayFailure } from '../lib/pay-error'
import { useHoldCountdown } from '../model/use-hold-countdown'

export type CheckoutState = 'default' | 'offline' | 'timeout' | 'soldout' | 'failed'

type ResultView = 'timeout' | 'soldout' | 'failed'

// 결제가 끝나지 못한 결과 화면 (M07-1 시간 초과 · M07-2 자리 마감 · M07-3 실패)
const RESULT: Record<
  ResultView,
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

// ?preview=offline 시안에서 보여 주는 예치금 잔액과 남은 시간 (5분 10초)
const OFFLINE_PREVIEW_BALANCE = 40_000
const OFFLINE_PREVIEW_SECONDS = 310

/** 충전창(예치금 충전 화면). 충전하러 갈 때 결제 기한을 2분 늘린다. */
const CHARGE_HREF = '/me/deposit/charge'

const won = (amount: number) => `₩${formatNumber(amount)}`

// 피그마 V2 · M07 결제 · 부족분 충전 (+ 연결 끊김, M07-1~3 결과)
// 결제 화면은 주문(/checkout?orderId=)으로 그린다. 남은 시간은 주문의 결제 기한(paymentDueAt)까지 센다.
// ?preview=offline|timeout|soldout|failed 시안은 API를 부르지 않고 시안 예시 값을 보여 준다.
// TODO(api): 토스페이먼츠 충전을 붙인다. 지금은 충전창으로 보내기만 한다.
export function CheckoutPage({
  state = 'default',
  orderId,
}: {
  state?: CheckoutState
  /** state가 default일 때 결제할 주문 */
  orderId?: number
}) {
  if (state === 'timeout' || state === 'soldout' || state === 'failed') {
    return <CheckoutResult view={state} />
  }
  if (state === 'offline') return <OfflinePreview />
  // 주문 없이 들어오는 경우는 page.tsx에서 notFound()로 막는다
  if (orderId === undefined) return <CheckoutResult view="failed" />
  return <OrderCheckout orderId={orderId} />
}

function CheckoutResult({ view, onRetry }: { view: ResultView; onRetry?: () => void }) {
  const result = RESULT[view]
  return (
    <>
      <AppHeader type="sub" title="결제" close backHref="/" />
      <main className="flex flex-1 items-center justify-center px-4 py-6">
        <EmptyState icon={result.icon} title={result.title} description={result.description} />
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          onRetry ? (
            <Button size="l" onClick={onRetry}>
              {result.action}
            </Button>
          ) : (
            <Link href={result.href} className={buttonClassName({ size: 'l' })}>
              {result.action}
            </Link>
          )
        }
      />
    </>
  )
}

/** 연결 끊김 시안 (M07 오프라인). 결제를 누른 뒤라 동의는 체크된 채로 둔다. */
function OfflinePreview() {
  const [expiresAt] = useState(() => Date.now() + OFFLINE_PREVIEW_SECONDS * 1000)
  const remaining = useHoldCountdown(expiresAt)
  const [agreed, setAgreed] = useState(true)
  const race = RACE_DETAIL
  const option = race.options[0]
  if (remaining === 0) return <CheckoutResult view="timeout" />
  return (
    <CheckoutForm
      offline
      remaining={remaining}
      summary={`${race.title} · ${option.name} · ${SAMPLE_PARTICIPANT.name}`}
      amount={option.price}
      balance={OFFLINE_PREVIEW_BALANCE}
      refund={race.refund}
      agreed={agreed}
      onAgreedChange={setAgreed}
      action={
        <Button size="l" disabled={!agreed}>
          다시 시도하기
        </Button>
      }
    />
  )
}

function OrderCheckout({ orderId }: { orderId: number }) {
  const router = useRouter()
  const queryClient = useQueryClient()
  const completeHref = `/checkout/complete?orderId=${orderId}` as const
  const order = orderApi.useFindById1(orderId, { query: { select: unwrapData } })
  const raceId = order.data?.raceId
  const race = useQuery({
    ...raceDetailQueryOptions(String(raceId)),
    enabled: raceId !== undefined,
  })
  const deposit = useDepositBalance()
  const applications = useBookingFlow((s) => s.applications)
  // 필수 동의는 결제 화면에 들어올 때마다 사용자가 직접 체크한다.
  const [agreed, setAgreed] = useState(false)
  const [failure, setFailure] = useState<PayFailure | null>(null)

  const dueAt = order.data?.paymentDueAt
  const remaining = useHoldCountdown(dueAt ? parseKstDateTime(dueAt).getTime() : null)

  const pay = useMutation({
    mutationFn: async () => unwrapData(await orderApi.pay(orderId)),
    onSuccess: () => router.push(completeHref),
    onError: (error) => {
      const next = toPayFailure(error)
      setFailure(next)
      if (next.kind === 'insufficient') void deposit.refetch()
    },
  })

  // 충전하러 가기 전에 결제 기한을 2분 늘린다. 이미 늘렸거나 실패해도 충전창으로는 간다.
  const extend = useMutation({
    mutationFn: async () => unwrapData(await orderApi.extendDeadline(orderId)),
    onSuccess: (extended) =>
      queryClient.setQueryData(orderApi.getFindById1QueryKey(orderId), {
        status: 200,
        message: '',
        data: extended,
      }),
    onSettled: () => router.push(CHARGE_HREF),
  })

  const status = order.data?.status
  // 이미 결제한 주문이면 완료 화면으로 보낸다 (뒤로 가기 · 새로고침).
  useEffect(() => {
    if (status === 'PAID') router.replace(completeHref)
  }, [status, completeHref, router])

  if (order.isError) return <CheckoutResult view="failed" onRetry={() => void order.refetch()} />
  if (!order.data || status === 'PAID') return <CheckoutSkeleton />
  if (status === 'EXPIRED' || remaining === 0 || failure?.kind === 'timeout') {
    return <CheckoutResult view="timeout" />
  }
  if (failure?.kind === 'soldout') return <CheckoutResult view="soldout" />
  if (failure?.kind === 'failed' || status !== 'CREATED') {
    return (
      <CheckoutResult
        view="failed"
        onRetry={() => {
          setFailure(null)
          void order.refetch()
        }}
      />
    )
  }

  const amount = order.data.totalAmount ?? 0
  const balance = deposit.balance
  const payment = balance === undefined ? undefined : splitPayment(amount, balance)
  const busy = pay.isPending || extend.isPending

  return (
    <CheckoutForm
      remaining={remaining}
      summary={orderSummary(order.data, race.data ?? undefined, applications)}
      amount={amount}
      balance={balance}
      refund={race.data?.refund}
      agreed={agreed}
      onAgreedChange={setAgreed}
      alert={
        failure?.kind === 'insufficient' && failure.message ? (
          <Alert tone="danger" title={failure.message} />
        ) : undefined
      }
      action={
        payment && payment.charge > 0 ? (
          <Button size="l" disabled={!agreed || busy} onClick={() => extend.mutate()}>
            {`${won(payment.charge)} 충전하고 결제`}
          </Button>
        ) : (
          <Button size="l" disabled={!agreed || busy} onClick={() => pay.mutate()}>
            {`${won(amount)} 결제`}
          </Button>
        )
      }
    />
  )
}

/** 대회 · 종목 · 참가자. 대회 상세를 아직 못 받았으면 주문 상품 이름을 쓴다. */
function orderSummary(
  order: orderModel.OrderDto,
  race: { title: string; options: { id: string; name: string }[] } | undefined,
  applications: Record<string, { participationId: number; participantName: string }>,
) {
  const item = order.items?.[0]
  const option = race?.options.find((o) => o.id === String(item?.raceCourseId))
  const name = race && option ? `${race.title} · ${option.name}` : (item?.itemName ?? '')
  const participant =
    Object.values(applications).find((a) => a.participationId === item?.participationId)
      ?.participantName ?? order.shippingAddress?.recipientName
  return participant ? `${name} · ${participant}` : name
}

function CheckoutSkeleton() {
  return (
    <>
      <AppHeader type="sub" title="결제" />
      <main aria-busy className="flex flex-1 flex-col gap-4 px-4 py-6">
        <Skeleton shape="rect" className="h-96" />
      </main>
    </>
  )
}

function CheckoutForm({
  offline = false,
  remaining,
  summary,
  amount,
  balance,
  refund,
  agreed,
  onAgreedChange,
  alert,
  action,
}: {
  offline?: boolean
  remaining: number
  summary: string
  amount: number
  /** 예치금 잔액. 불러오지 못하면 undefined */
  balance?: number
  refund?: { label: string; value: string }[]
  agreed: boolean
  onAgreedChange: (agreed: boolean) => void
  alert?: ReactNode
  action: ReactNode
}) {
  const payment = balance === undefined ? undefined : splitPayment(amount, balance)
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
            <p className="text-body-s text-text-secondary">{summary}</p>
            <p className="text-[32px] leading-11 font-bold tracking-[-0.015em]">{won(amount)}</p>
          </div>

          <div className="flex flex-col gap-3 border-t border-border-default pt-4">
            <KeyValue label="예치금에서" value={payment ? won(payment.fromDeposit) : '-'} />
            <KeyValue
              label="충전 (최소 1만 원)"
              value={payment ? `+${won(payment.charge)}` : '-'}
            />
            <KeyValue
              label="결제 뒤 남는 예치금"
              value={payment ? won(payment.balanceAfter) : '-'}
            />
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
            {refund && (
              <div className="flex flex-col gap-2 rounded-[12px] px-4 pt-3 pb-4 inset-ring inset-ring-border-default">
                <p className="text-body-s text-text-secondary">환불 규정</p>
                {refund.map((row) => (
                  <KeyValue key={row.label} label={row.label} value={row.value} />
                ))}
              </div>
            )}
            <Checkbox
              checked={agreed}
              onCheckedChange={onAgreedChange}
              label="[필수] 환불 기한을 확인했어요"
            />
          </div>

          {alert}
        </div>
      </main>
      <BottomCta className="sticky bottom-0 z-10" primary={action} />
    </>
  )
}

function formatTimer(seconds: number) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}
