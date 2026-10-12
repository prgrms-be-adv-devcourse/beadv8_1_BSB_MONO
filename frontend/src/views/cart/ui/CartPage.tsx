'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { RACE_DETAIL } from '@/entities/race'
import { formatWon } from '@/shared/lib'
import { Alert, Badge, BottomCta, Button, buttonClassName, EmptyState, KeyValue } from '@/shared/ui'

// 피그마 V2 · M06 장바구니 (+ 비어 있음). 장바구니에는 대회 1개, 참가권 1장만 담긴다.
// TODO(api): 장바구니 API로 바꾼다. 지금은 시안 예시 한 건.
export function CartPage({ empty = false }: { empty?: boolean }) {
  const router = useRouter()
  const [removed, setRemoved] = useState(empty)
  const race = RACE_DETAIL
  const option = race.options[0]

  if (removed) {
    return (
      <>
        <AppHeader type="sub" title="장바구니" />
        <main className="flex flex-1 items-center justify-center px-4 py-6">
          <EmptyState
            icon="Cart"
            title="장바구니가 비어 있어요"
            description="마음에 드는 대회를 찾아 참가권을 담아 보세요."
            action={
              <Link href="/races" className={buttonClassName({ variant: 'secondary' })}>
                대회 찾기
              </Link>
            }
          />
        </main>
      </>
    )
  }

  return (
    <>
      <AppHeader type="sub" title="장바구니" />
      <main className="flex flex-1 flex-col gap-6 px-4 py-6">
        <Alert
          tone="warning"
          title="담기만 해서는 자리가 잡히지 않아요"
          description="결제할 때 가격 · 정원 · 기한을 다시 확인해요."
        />
        <div className="flex gap-4 rounded-lg bg-bg-surface px-5 py-4 shadow-sm">
          <Image
            src={race.images[0]}
            alt=""
            width={64}
            height={64}
            className="size-16 shrink-0 rounded-md object-cover"
          />
          <div className="flex min-w-0 flex-1 flex-col gap-2">
            <div className="flex items-center justify-between gap-2">
              <p className="min-w-0 flex-1 text-label-strong">
                {race.title} · {option.name}
              </p>
              <Badge tone="brand">참가권</Badge>
            </div>
            <p className="text-body-s text-text-secondary">
              참가자 김러너 · 수량 1 (고정) · 접수 마감 10/31
            </p>
            <div className="flex items-center justify-between gap-2">
              <Button variant="ghost" size="s" onClick={() => setRemoved(true)}>
                삭제
              </Button>
              <p className="text-heading-s">{formatWon(option.price)}</p>
            </div>
          </div>
        </div>
        <div className="flex flex-col gap-3 rounded-lg bg-bg-subtle p-5">
          <KeyValue label="참가권 1개" value={formatWon(option.price)} />
          <div className="flex items-center justify-between gap-2">
            <span className="text-label-strong text-text-secondary">합계</span>
            <span className="text-heading-s">{formatWon(option.price)}</span>
          </div>
        </div>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        price={{ label: '총 결제 금액', amount: formatWon(option.price) }}
        primary={
          <Button size="l" onClick={() => router.push('/checkout')}>
            결제하기
          </Button>
        }
      />
    </>
  )
}
