'use client'

import { useQuery } from '@tanstack/react-query'
import type { Route } from 'next'
import Link from 'next/link'
import type { ReactNode } from 'react'

import { ConsoleCard, ConsoleLoading, ConsoleOffline, ConsolePage } from '@/widgets/console-page'
import { formatMonthDay, formatWeekday } from '@/entities/payout'
import { myRacesQueryOptions, toSellerRaceRow } from '@/entities/race'
import { shippableCount, useSellerKitSummary } from '@/entities/seller-kit'
import { ApiError, payoutApi, unwrapData } from '@/shared/api'
import { cn, formatNumber } from '@/shared/lib'
import {
  Alert,
  Badge,
  buttonClassName,
  DataTable,
  EmptyState,
  FOCUS_RING,
  ProgressBar,
  Skeleton,
  StatTile,
  type BadgeTone,
} from '@/shared/ui'

function errorText(error: unknown) {
  return (error instanceof ApiError && error.serverMessage) || '잠시 뒤 다시 시도해 주세요.'
}

// TODO(api): 처리할 일 API가 없다. 키트 배송 대기만 키트 건수 API로 채우고, 나머지는 시안 예시 값이다.
// (위 "처리할 일" 숫자 타일도 예시 값)
const TODOS = [
  { title: '접수 마감 임박', description: '쿠키RUN · 10/31 18:00 접수 마감', count: 'D-7' },
  { title: '지급보류 알림', description: 'HD-20261027-01A2 · 정산 계좌 미등록', count: '1건' },
]

type Order = {
  id: string
  nickname: string
  race: string
  course: string
  status: string
  tone: BadgeTone
  paidAt: string
}

// TODO(api): 최근 주문 API가 없다. 지금은 시안 예시 값.
const ORDERS: Order[] = [
  {
    id: 'R-20261019-0907',
    nickname: '주말장거리',
    race: '2026 용감한 쿠키RUN in 서울',
    course: '5K',
    status: '참가 확정',
    tone: 'brand',
    paidAt: '10/19',
  },
  {
    id: 'R-20261019-0911',
    nickname: '새벽런',
    race: '2026 춘천 나이트런',
    course: '10K',
    status: '참가 확정',
    tone: 'brand',
    paidAt: '10/19',
  },
  {
    id: 'R-20261018-0874',
    nickname: '한강조깅',
    race: '2026 춘천 나이트런',
    course: '5K',
    status: '취소 · 환불',
    tone: 'neutral',
    paidAt: '10/18',
  },
  {
    id: 'R-20261018-0850',
    nickname: '달리는쿠키',
    race: '2026 용감한 쿠키RUN in 서울',
    course: '10K',
    status: '참가 확정',
    tone: 'brand',
    paidAt: '10/18',
  },
]

const LINK = cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)

// 피그마 V2 · S05 판매자 홈 (+ 판매자 · 관리자 공통 로딩 · 연결 끊김)
export function SellerDashboardPage({ state }: { state?: 'loading' | 'offline' }) {
  return (
    <ConsolePage
      title="판매자 홈"
      description="오늘 처리할 일과 다음 정산을 한눈에 봐요."
      actions={
        <Link href="/seller/races/new" className={buttonClassName()}>
          대회 등록
        </Link>
      }
    >
      {state === 'loading' && <ConsoleLoading />}
      {state === 'offline' && <ConsoleOffline />}
      {!state && <DashboardBody />}
    </ConsolePage>
  )
}

const TODO_ROW = 'flex items-center gap-3 border-b border-border-default py-3 last:border-b-0'

// 처리할 일 한 줄. href가 있으면 그 화면으로 가는 링크
function TodoRow({
  href,
  title,
  description,
  count,
}: {
  href?: Route
  title: string
  description: string
  count: ReactNode
}) {
  const body = (
    <>
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <p className="text-label-l">{title}</p>
        <p className="text-body-s text-text-secondary">{description}</p>
      </div>
      {count}
    </>
  )
  return href ? (
    <Link href={href} className={cn(TODO_ROW, 'rounded-sm', FOCUS_RING)}>
      {body}
    </Link>
  ) : (
    <div className={TODO_ROW}>{body}</div>
  )
}

function DashboardBody() {
  const summary = payoutApi.useSummary({ query: { select: unwrapData } })
  const races = useQuery({
    ...myRacesQueryOptions(),
    select: (list) => list.map(toSellerRaceRow),
  })
  const onSale = races.data?.filter((r) => r.saleStatus === 'ON_SALE')
  const kits = useSellerKitSummary()

  return (
    <>
      <div className="flex gap-4">
        {summary.data ? (
          <>
            <StatTile
              label="다음 정산일"
              value={formatMonthDay(summary.data.nextPayDate)}
              unit={`(${formatWeekday(summary.data.nextPayDate)})`}
            />
            <StatTile
              tone="accent"
              className="flex-2"
              label="정산 예정액"
              value={`₩${formatNumber(summary.data.scheduledPayoutAmount)}`}
            />
          </>
        ) : summary.isError ? (
          <Alert
            tone="danger"
            className="flex-3"
            title="정산 요약을 불러오지 못했어요"
            description={errorText(summary.error)}
          />
        ) : (
          <Skeleton className="h-24 flex-3" />
        )}
        {onSale ? (
          <StatTile label="판매 중 대회" value={String(onSale.length)} unit="개" />
        ) : (
          <Skeleton className="h-24 flex-1" />
        )}
        {/* TODO(api): 처리할 일 건수 API가 없다. 예시 값 */}
        <StatTile label="처리할 일" value="5" unit="건" />
      </div>

      <div className="flex flex-wrap items-start gap-6">
        <ConsoleCard
          title="진행 중 대회"
          aside={
            <Link href="/seller/races" className={LINK}>
              대회 관리
            </Link>
          }
          className="min-w-130 flex-1"
        >
          {races.isError ? (
            <Alert
              tone="danger"
              title="대회 목록을 불러오지 못했어요"
              description={errorText(races.error)}
            />
          ) : !onSale ? (
            <Skeleton className="h-40 w-full" />
          ) : onSale.length === 0 ? (
            <EmptyState
              title="판매 중인 대회가 없어요"
              description="대회를 등록하면 여기에서 접수 현황을 볼 수 있어요"
            />
          ) : (
            onSale.map((race) => (
              <div key={race.id} className="flex flex-col gap-3 rounded-md bg-bg-subtle p-5">
                <div className="flex items-center gap-2">
                  <p className="text-label-l">{race.title}</p>
                  <Badge tone={race.badge.tone}>{race.badge.label}</Badge>
                </div>
                <p className="text-body-s text-text-secondary">{race.period}</p>
                <ProgressBar
                  label="접수율"
                  value={race.capacity > 0 ? Math.round((race.sold / race.capacity) * 100) : 0}
                  valueText={`${formatNumber(race.sold)}/${formatNumber(race.capacity)}명`}
                />
              </div>
            ))
          )}
        </ConsoleCard>

        <ConsoleCard title="처리할 일" className="w-100 shrink-0 gap-0">
          <TodoRow
            href="/seller/kits"
            title="배송 대기 키트"
            description="환불 기한이 지난 주문 · 송장 번호를 넣으면 배송 중으로 바뀌어요"
            count={
              kits.data ? (
                <Badge tone="warning">{shippableCount(kits.data)}건</Badge>
              ) : kits.isError ? (
                <Badge tone="neutral">-</Badge>
              ) : (
                <Skeleton className="h-6 w-10" />
              )
            }
          />
          {TODOS.map((todo) => (
            <TodoRow
              key={todo.title}
              title={todo.title}
              description={todo.description}
              count={<Badge tone="warning">{todo.count}</Badge>}
            />
          ))}
        </ConsoleCard>
      </div>

      <ConsoleCard
        title="최근 주문"
        aside={
          <Link href="/seller/sales" className={LINK}>
            주문 · 참가자에서 보기
          </Link>
        }
      >
        <DataTable
          label="최근 주문"
          rows={ORDERS}
          rowKey={(order) => order.id}
          columns={[
            { key: 'id', header: '주문번호', width: 'w-42.5', render: (o) => o.id },
            { key: 'nickname', header: '닉네임', width: 'w-58.75', render: (o) => o.nickname },
            { key: 'race', header: '대회', render: (o) => o.race },
            { key: 'course', header: '종목', width: 'w-30', render: (o) => o.course },
            {
              key: 'status',
              header: '상태',
              width: 'w-40',
              render: (o) => <Badge tone={o.tone}>{o.status}</Badge>,
            },
            {
              key: 'paidAt',
              header: '결제일',
              align: 'right',
              width: 'w-30',
              render: (o) => o.paidAt,
            },
          ]}
        />
      </ConsoleCard>
    </>
  )
}
