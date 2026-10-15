'use client'

import Link from 'next/link'
import { useState } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { BottomNav } from '@/widgets/bottom-nav'
import { cn } from '@/shared/lib'
import { EmptyState, FOCUS_RING, Icon, type IconName } from '@/shared/ui'

type Notice = {
  id: string
  icon: IconName
  title: string
  body: string
  at: string
  href: '/me/deposit' | '/tickets/R-20261002-0388/notice' | '/tickets'
  unread: boolean
}

// TODO(api): 알림 API로 바꾼다. 시안 예시 값.
const TODAY: Notice[] = [
  {
    id: 'n1',
    icon: 'Calendar',
    title: '예치금을 돌려받았어요',
    body: '₩5,000을 국민은행 계좌로 보냈어요.\n예치금 내역에서 확인할 수 있어요.',
    at: '방금',
    href: '/me/deposit',
    unread: true,
  },
  {
    id: 'n2',
    icon: 'Calendar',
    title: '대회 일정이 바뀌었어요',
    body: '춘천 나이트런 · 12/6 → 12/13.\n10/12까지 전액 환불할 수 있어요.',
    at: '5시간 전',
    href: '/tickets/R-20261002-0388/notice',
    unread: true,
  },
]
const EARLIER: Notice[] = [
  {
    id: 'n3',
    icon: 'CircleCheck',
    title: '대회가 취소됐어요',
    body: '한강 나이트런 10K · 참가비 전액\n₩45,000을 예치금으로 돌려드렸어요.',
    at: '10/5',
    href: '/tickets',
    unread: false,
  },
  {
    id: 'n4',
    icon: 'CircleCheck',
    title: '환불이 끝났어요',
    body: '가을 트레일 21K ·\n₩60,000이 예치금으로 들어왔어요.',
    at: '9/30',
    href: '/me/deposit',
    unread: false,
  },
]

// 피그마 V2 · M25 알림 (+ 없음)
export function NotificationsPage({ empty = false }: { empty?: boolean }) {
  const [read, setRead] = useState(false)
  const today = empty ? [] : TODAY.map((n) => ({ ...n, unread: n.unread && !read }))
  const earlier = empty ? [] : EARLIER
  const unreadCount = today.filter((n) => n.unread).length

  return (
    <>
      <AppHeader />
      <main className="flex flex-1 flex-col gap-4 px-4 pt-4 pb-8">
        <h1 className="text-heading-l">알림</h1>
        {empty ? (
          <div className="flex flex-1 items-center justify-center">
            <EmptyState
              icon="Bell"
              title="아직 알림이 없어요"
              description="대회 일정이 바뀌거나 환불이 끝나면 여기로 알려 드려요."
              className="max-w-80"
            />
          </div>
        ) : (
          <>
            <div className="flex items-center justify-between py-1">
              <p className="text-body-s text-text-secondary">읽지 않은 알림 {unreadCount}개</p>
              <button
                type="button"
                onClick={() => setRead(true)}
                className={cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)}
              >
                모두 읽음
              </button>
            </div>
            <NoticeGroup title="오늘" items={today} />
            <NoticeGroup title="이전" items={earlier} />
          </>
        )}
      </main>
      <BottomNav />
    </>
  )
}

function NoticeGroup({ title, items }: { title: string; items: Notice[] }) {
  return (
    <section className="flex flex-col gap-2">
      <h2 className="pt-2 text-label-strong">{title}</h2>
      <ul className="overflow-hidden rounded-lg bg-bg-surface inset-ring inset-ring-border-default">
        {items.map((n) => (
          <li key={n.id} className="border-b border-border-default last:border-b-0">
            <Link
              href={n.href}
              className={cn('flex gap-3 px-4 py-4', n.unread && 'bg-bg-brand-subtle', FOCUS_RING)}
            >
              <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-bg-surface shadow-sm">
                <Icon name={n.icon} className="size-4" />
              </span>
              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <div className="flex items-start justify-between gap-2">
                  <p className="text-label-m">{n.title}</p>
                  <span className="flex items-center gap-2 text-caption text-text-secondary">
                    {n.at}
                    {n.unread && (
                      <span aria-label="읽지 않음" className="size-1.5 rounded-full bg-bg-brand" />
                    )}
                  </span>
                </div>
                <p className="text-body-s whitespace-pre-line text-text-secondary">{n.body}</p>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
