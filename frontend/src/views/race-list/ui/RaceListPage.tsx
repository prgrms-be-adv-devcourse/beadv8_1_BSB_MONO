'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useState } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { BottomNav } from '@/widgets/bottom-nav'
import { RACE_LIST, type RaceListItem } from '@/entities/race'
import { cn } from '@/shared/lib'
import {
  Badge,
  BottomSheet,
  Button,
  Chip,
  EmptyState,
  FOCUS_RING,
  Icon,
  IconButton,
  Skeleton,
  useToast,
} from '@/shared/ui'

export type RaceListState = 'loading' | 'empty' | 'menu'

// 피그마 V2 · M18 대회 찾기 (+ 로딩, 결과 없음, 더보기 메뉴)
// TODO(api): 검색 · 필터 · 정렬을 대회 목록 API에 붙인다
export function RaceListPage({ state }: { state?: RaceListState }) {
  const toast = useToast()
  const empty = state === 'empty'
  const loading = state === 'loading'
  const [query, setQuery] = useState(empty ? '제주 트레일' : '')
  const [menuRace, setMenuRace] = useState<RaceListItem | null>(
    state === 'menu' ? RACE_LIST[0] : null,
  )
  const races = empty ? [] : RACE_LIST

  function copyLink(race: RaceListItem) {
    void navigator.clipboard?.writeText(`${window.location.origin}/races/${race.id}`)
    setMenuRace(null)
    toast({ message: '링크를 복사했어요', tone: 'success' })
  }

  return (
    <>
      <AppHeader />
      <main className="flex flex-1 flex-col gap-4 px-4 pt-8 pb-8">
        <h1 className="text-heading-xl">대회 찾기</h1>
        <label className="flex items-center gap-2 rounded-md bg-bg-surface p-4 inset-ring inset-ring-border-default focus-within:inset-ring-2 focus-within:inset-ring-border-focus">
          <Icon name="Search" className="size-5" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="대회 이름이나 지역으로 찾기"
            aria-label="대회 찾기"
            className="min-w-0 flex-1 bg-transparent text-body-m outline-none placeholder:text-text-tertiary"
          />
        </label>
        <div className="flex flex-wrap gap-2">
          <Chip>종목</Chip>
          <Chip>지역</Chip>
          <Chip>11월</Chip>
        </div>
        <div className="flex items-center justify-between">
          {loading ? (
            <Skeleton className="w-12" />
          ) : (
            <p className="text-label-m text-text-secondary">대회 {empty ? 0 : 24}개</p>
          )}
          <button
            type="button"
            className={cn('flex items-center gap-1 rounded-sm text-label-m', FOCUS_RING)}
          >
            마감 임박순
            <Icon name="ChevronDown" className="size-4" />
          </button>
        </div>

        {loading && <ListSkeleton />}
        {!loading && races.length === 0 && (
          <div className="flex flex-1 items-center justify-center">
            <EmptyState
              icon="Search"
              title={`'${query}'에 맞는 대회가 없어요`}
              description="다른 지역이나 종목으로 찾아보세요."
              action={
                <Button variant="secondary" onClick={() => setQuery('')}>
                  필터 초기화
                </Button>
              }
            />
          </div>
        )}
        {!loading && races.length > 0 && (
          <ul className="flex flex-col gap-3">
            {races.map((race) => (
              <li key={race.id} className="relative">
                <Link
                  href={`/races/${race.id}`}
                  className={cn(
                    'flex gap-3 rounded-lg bg-bg-surface p-4 pr-12 shadow-sm',
                    FOCUS_RING,
                  )}
                >
                  <Image
                    src={race.image}
                    alt=""
                    width={80}
                    height={80}
                    className="size-20 shrink-0 rounded-md object-cover"
                  />
                  <div className="flex min-w-0 flex-1 flex-col items-start gap-1">
                    <Badge tone={race.badge.tone}>{race.badge.label}</Badge>
                    <p className="text-heading-s">{race.title}</p>
                    <p className="text-body-s text-text-secondary">{race.meta}</p>
                    <div className="flex w-full items-center justify-between pt-1">
                      <span className="text-label-m">{race.price}</span>
                      <span className="text-caption text-text-secondary">{race.remaining}</span>
                    </div>
                  </div>
                </Link>
                <IconButton
                  size="s"
                  aria-label={`${race.title} 더보기`}
                  className="absolute top-2 right-2"
                  onClick={() => setMenuRace(race)}
                >
                  <Icon name="MoreVertical" />
                </IconButton>
              </li>
            ))}
          </ul>
        )}
      </main>
      <BottomNav />
      <BottomSheet
        open={menuRace !== null}
        onOpenChange={(open) => !open && setMenuRace(null)}
        title={menuRace?.title ?? ''}
        footer={
          <Button variant="secondary" size="l" onClick={() => setMenuRace(null)}>
            닫기
          </Button>
        }
      >
        <div className="flex gap-8 px-2 py-2">
          <SheetAction
            icon="Copy"
            label="링크 복사"
            onClick={() => menuRace && copyLink(menuRace)}
          />
          {/* TODO: 기기 공유 창(navigator.share) */}
          <SheetAction icon="Share" label="공유하기" onClick={() => setMenuRace(null)} />
        </div>
      </BottomSheet>
    </>
  )
}

function SheetAction({
  icon,
  label,
  onClick,
}: {
  icon: 'Copy' | 'Share'
  label: string
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn('flex items-center gap-3 rounded-md', FOCUS_RING)}
    >
      <span className="flex size-12 items-center justify-center rounded-full bg-bg-subtle">
        <Icon name={icon} className="size-5" />
      </span>
      <span className="text-label-s">{label}</span>
    </button>
  )
}

// 피그마 M18 · 로딩: 목록 카드 4장 자리
function ListSkeleton() {
  return (
    <div role="status" aria-label="불러오는 중" className="flex flex-col gap-3">
      {[0, 1, 2, 3].map((i) => (
        <div key={i} className="flex gap-3 rounded-lg bg-bg-surface p-4 shadow-sm">
          <Skeleton shape="rect" className="size-20 shrink-0" />
          <div className="flex flex-1 flex-col gap-3 pt-1">
            <Skeleton shape="rect" className="h-4 w-12" />
            <Skeleton className="w-11/12" />
            <Skeleton className="w-4/5" />
            <div className="flex justify-between">
              <Skeleton className="w-10" />
              <Skeleton className="w-14" />
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
