'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { UPCOMING_TICKETS } from '@/entities/ticket'
import { cn } from '@/shared/lib'
import { Alert, Badge, BibCard, Button, Dialog, FOCUS_RING, KeyValue } from '@/shared/ui'

const LINK = cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)

// 피그마 V2 · M16 참가권 상세 · 취소 (+ M16-1 참가 취소 확인)
// TODO(api): 참가권을 불러오고 취소 요청을 보낸다. 지금은 시안 예시 값.
export function TicketDetailPage({
  ticketId,
  confirmOpen = false,
}: {
  ticketId: string
  confirmOpen?: boolean
}) {
  const router = useRouter()
  const ticket = UPCOMING_TICKETS.find((t) => t.id === ticketId) ?? UPCOMING_TICKETS[0]
  const [cancelOpen, setCancelOpen] = useState(confirmOpen)

  return (
    <>
      <AppHeader type="sub" title="참가권" backHref="/tickets" />
      <main className="flex flex-1 flex-col gap-4 px-4 py-6">
        <BibCard
          image={ticket.image}
          imagePosition="50% 12%"
          eyebrow={ticket.eyebrow}
          status={<Badge tone="live">참가 확정</Badge>}
          unit="No."
          number={ticket.bib}
          title={`${ticket.runner} · ${ticket.course}`}
          meta={`주문번호 ${ticket.id} · 11/1 (일) 08:00`}
        />

        <section className="flex flex-col gap-3 rounded-lg bg-bg-surface p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <h2 className="text-[18px] leading-6.5 font-bold tracking-[-0.015em]">참가자 정보</h2>
            <Link href={`/tickets/${ticket.id}/edit`} className={LINK}>
              고치기
            </Link>
          </div>
          <div className="flex flex-col gap-2">
            <p className="text-body-m text-text-secondary">
              사이즈 M (95) · 비상 연락처 010-98**-5432
            </p>
            <p className="text-caption text-text-tertiary">
              접수 마감(10/31) 전까지 고칠 수 있어요.
            </p>
          </div>
        </section>

        <section className="flex flex-col gap-3 rounded-lg bg-bg-surface p-5 shadow-sm">
          <h2 className="text-[18px] leading-6.5 font-bold tracking-[-0.015em]">환불 기준</h2>
          <div className="flex flex-col gap-2">
            <KeyValue label="~ 10/31 (토) 23:59 (지금)" value="전액 환불" />
            <KeyValue label="11/1 (일) 00:00 ~" value="개인 사정 환불 불가" />
          </div>
        </section>

        <Alert
          title="오늘 취소하면 ₩45,000을 예치금으로 돌려받아요"
          description="키트도 함께 취소돼요."
        />

        <div className="flex flex-col gap-2">
          <Button variant="danger" size="l" className="w-full" onClick={() => setCancelOpen(true)}>
            참가 취소하기
          </Button>
          <p className="text-caption text-text-tertiary">
            취소하면 자리는 바로 다른 사람에게 열려요.
          </p>
        </div>
      </main>
      <Dialog
        open={cancelOpen}
        onOpenChange={setCancelOpen}
        type="danger"
        title="참가를 취소할까요?"
        body="₩45,000을 예치금으로 돌려받고 키트도 함께 취소돼요. 되돌릴 수 없어요."
        confirmLabel="취소하기"
        cancelLabel="그대로 두기"
        onConfirm={() => router.push(`/tickets/${ticket.id}/canceled`)}
      />
    </>
  )
}
