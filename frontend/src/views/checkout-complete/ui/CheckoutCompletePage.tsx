'use client'

import { useQuery } from '@tanstack/react-query'
import Link from 'next/link'

import { AppHeader } from '@/widgets/app-header'
import { useBookingFlow } from '@/features/booking'
import { raceDetailQueryOptions } from '@/entities/race'
import { orderApi, unwrapData } from '@/shared/api'
import { formatNumber } from '@/shared/lib'
import { Badge, BibCard, BottomCta, buttonClassName, KeyValue, Skeleton } from '@/shared/ui'

// 피그마 V2 · M08 참가 확정
// 결제한 주문(/checkout/complete?orderId=)을 주문 API로 불러와 그린다. 주문은 로그인이 필요해 클라이언트에서 부른다.
// 배번은 주문 API에 없어 아직 보여 주지 않는다.
export function CheckoutCompletePage({ orderId }: { orderId: number }) {
  const order = orderApi.useFindById1(orderId, { query: { select: unwrapData } })
  const raceId = order.data?.raceId
  const race = useQuery({
    ...raceDetailQueryOptions(String(raceId)),
    enabled: raceId !== undefined,
  })
  const applications = useBookingFlow((s) => s.applications)

  const item = order.data?.items?.[0]
  const option = race.data?.options.find((o) => o.id === String(item?.raceCourseId))
  const participant =
    Object.values(applications).find((a) => a.participationId === item?.participationId)
      ?.participantName ?? order.data?.shippingAddress?.recipientName
  const refundDeadline = race.data?.schedule.find((row) => row.label === '전액 환불 기한')?.value

  return (
    <>
      <AppHeader type="sub" title="참가 확정" close backHref="/" />
      <main className="flex flex-1 flex-col gap-5 px-4 pt-6 pb-8">
        {order.data ? (
          <>
            <div className="flex flex-col gap-2 pb-2">
              <h2 className="text-heading-l">참가가 확정됐어요</h2>
              <p className="text-body-m text-text-secondary">
                {race.data && option
                  ? `${race.data.title} · ${option.name}`
                  : (item?.itemName ?? '')}
              </p>
            </div>
            <BibCard
              image={race.data?.images[0]}
              imagePosition="50% 12%"
              eyebrow={race.data?.title ?? ''}
              status={<Badge tone="live">참가 확정</Badge>}
              unit="No."
              number="-"
              title={[participant, option?.name].filter(Boolean).join(' · ')}
              meta={race.data ? `${race.data.startsAt} · ${race.data.place}` : undefined}
            />
            <div className="flex flex-col gap-3 rounded-lg bg-bg-surface p-5 shadow-sm inset-ring inset-ring-border-default">
              <KeyValue label="주문번호" value={order.data.orderNo ?? '-'} />
              {participant && <KeyValue label="참가자" value={participant} />}
              <KeyValue
                label="결제"
                value={`₩${formatNumber(order.data.totalAmount ?? 0)} (예치금)`}
              />
              {order.data.shippingAddress && refundDeadline && (
                <KeyValue label="키트 배송" value={`환불 기한(${refundDeadline}) 지난 뒤 배송`} />
              )}
            </div>
          </>
        ) : (
          <div aria-busy className="flex flex-col gap-5">
            <Skeleton shape="rect" className="h-16" />
            <Skeleton shape="rect" className="h-48" />
            <Skeleton shape="rect" className="h-32" />
          </div>
        )}
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        secondary={
          <Link href="/" className={buttonClassName({ variant: 'secondary', size: 'l' })}>
            홈으로
          </Link>
        }
        primary={
          <Link href="/tickets" className={buttonClassName({ size: 'l' })}>
            내 참가권 보기
          </Link>
        }
      />
    </>
  )
}
