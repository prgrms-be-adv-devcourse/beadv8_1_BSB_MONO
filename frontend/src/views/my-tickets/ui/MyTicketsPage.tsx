'use client'

import Link from 'next/link'
import { useState } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { BottomNav } from '@/widgets/bottom-nav'
import { PAST_TICKETS, UPCOMING_TICKETS, type Ticket } from '@/entities/ticket'
import { cn } from '@/shared/lib'
import {
  Badge,
  BibCard,
  buttonClassName,
  EmptyState,
  FOCUS_RING,
  Segmented,
  Skeleton,
} from '@/shared/ui'

const STATUS: Record<Ticket['status'], { label: string; tone: 'live' | 'onHero' }> = {
  confirmed: { label: '참가 확정', tone: 'live' },
  finished: { label: '종료', tone: 'onHero' },
  canceled: { label: '취소', tone: 'onHero' },
}

export type MyTicketsState = 'loading' | 'empty'

// 피그마 V2 · M15 내 참가 대회 (+ 로딩, 없음)
// TODO(api): 회원 · 예치금 · 참가권 API로 바꾼다
export function MyTicketsPage({ state }: { state?: MyTicketsState }) {
  const [tab, setTab] = useState<'upcoming' | 'past'>('upcoming')
  const loading = state === 'loading'
  const upcoming = state === 'empty' ? [] : UPCOMING_TICKETS
  const tickets = tab === 'upcoming' ? upcoming : PAST_TICKETS

  return (
    <>
      <AppHeader />
      <main className="flex flex-1 flex-col gap-6 px-4 py-6">
        <div className="flex items-center gap-3">
          <div className="size-12 shrink-0 rounded-full bg-bg-subtle" />
          <div className="flex min-w-0 flex-1 flex-col gap-1">
            {loading ? (
              <>
                <Skeleton className="h-4 w-48" />
                <Skeleton className="w-32" />
              </>
            ) : (
              <>
                <p className="text-heading-m">달리는곰</p>
                <p className="text-body-s text-text-secondary">김러너</p>
              </>
            )}
          </div>
          <Link href="/me/edit" className={buttonClassName({ variant: 'secondary', size: 's' })}>
            정보 수정
          </Link>
        </div>

        <div className="flex flex-col gap-3 rounded-lg bg-bg-brand-subtle p-5">
          <div className="flex items-center justify-between gap-2">
            <Link
              href="/me/deposit"
              className={cn('rounded-sm text-label-m text-text-secondary', FOCUS_RING)}
            >
              예치금
            </Link>
            {loading ? (
              <Skeleton className="h-5 w-16" />
            ) : (
              <p className="font-display text-display-s">₩5,000</p>
            )}
          </div>
          <div className="flex gap-2">
            <Link
              href="/me/deposit/charge"
              className={buttonClassName({ variant: 'secondary', size: 's', className: 'flex-1' })}
            >
              충전
            </Link>
            <Link
              href="/me/deposit/refund"
              className={buttonClassName({ variant: 'secondary', size: 's', className: 'flex-1' })}
            >
              돌려받기
            </Link>
          </div>
        </div>

        <div className="flex flex-1 flex-col gap-3">
          <Segmented
            label="참가 대회"
            value={tab}
            onChange={setTab}
            items={[
              { value: 'upcoming', label: '참가 예정', count: upcoming.length },
              { value: 'past', label: '지난 대회', count: PAST_TICKETS.length },
            ]}
          />
          {loading && <TicketSkeletons />}
          {!loading && tickets.length === 0 && (
            <div className="flex flex-1 items-center justify-center">
              <EmptyState
                icon="Calendar"
                title="아직 신청한 대회가 없어요"
                description="대회를 찾아 첫 참가권을 받아 보세요."
                action={
                  <Link href="/races" className={buttonClassName({ variant: 'secondary' })}>
                    대회 찾기
                  </Link>
                }
              />
            </div>
          )}
          {!loading &&
            tickets.map((ticket) => (
              <Link
                key={ticket.id}
                href={`/tickets/${ticket.id}`}
                className={cn('rounded-lg', FOCUS_RING)}
              >
                <BibCard
                  image={ticket.image}
                  imagePosition="50% 12%"
                  eyebrow={ticket.eyebrow}
                  status={
                    <Badge tone={STATUS[ticket.status].tone}>{STATUS[ticket.status].label}</Badge>
                  }
                  unit="No."
                  number={ticket.bib}
                  title={`${ticket.runner} · ${ticket.course}`}
                  meta={ticket.summary}
                />
              </Link>
            ))}
        </div>
      </main>
      <BottomNav />
    </>
  )
}

// 피그마 M15 · 로딩: 카드 두 장 자리
function TicketSkeletons() {
  return (
    <div role="status" aria-label="불러오는 중" className="flex flex-col gap-3">
      {[0, 1].map((i) => (
        <div
          key={i}
          className="flex flex-col gap-4 rounded-lg bg-bg-surface p-5 shadow-md inset-ring inset-ring-border-default"
        >
          <div className="flex justify-between">
            <Skeleton className="w-24" />
            <Skeleton shape="rect" className="h-4 w-16 rounded-full" />
          </div>
          <div className="flex items-end gap-2">
            <Skeleton className="w-4" />
            <Skeleton shape="rect" className="h-7 w-24" />
          </div>
          <Skeleton className="w-11/12" />
          <Skeleton className="w-2/3" />
        </div>
      ))}
    </div>
  )
}
