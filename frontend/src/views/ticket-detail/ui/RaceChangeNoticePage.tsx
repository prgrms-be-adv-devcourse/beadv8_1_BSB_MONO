'use client'

import { useRouter } from 'next/navigation'

import { AppHeader } from '@/widgets/app-header'
import { Badge, BottomCta, Button, Icon, KeyValue, ProgressBar } from '@/shared/ui'

// 피그마 V2 · M24 대회 변경 안내 (일정 변경 알림에서 들어온다)
// TODO(api): 변경 내용과 환불 가능 기간을 서버에서 받는다
export function RaceChangeNoticePage({ ticketId }: { ticketId: string }) {
  const router = useRouter()
  return (
    <>
      <AppHeader type="sub" title="대회 일정 변경" />
      <main className="flex flex-1 flex-col gap-4 px-4 py-6">
        <div className="flex flex-col items-start gap-2">
          <Badge tone="warning">일정 변경</Badge>
          <h2 className="text-heading-l">춘천 나이트런 날짜가 바뀌었어요</h2>
          <p className="text-body-s text-text-secondary">
            주최 측 사정으로 대회 날짜가 일주일 미뤄졌어요.
          </p>
        </div>

        <section className="flex flex-col gap-4 rounded-lg bg-bg-surface p-5 shadow-sm inset-ring inset-ring-border-default">
          <div className="flex items-center gap-4">
            <div className="flex flex-1 flex-col gap-1">
              <span className="text-caption text-text-secondary">이전</span>
              <span className="font-display text-display-l text-text-tertiary line-through">
                12.6
              </span>
              <span className="text-caption text-text-secondary">(일) 19:00</span>
            </div>
            <Icon name="ArrowRight" className="size-5 text-text-secondary" />
            <div className="flex flex-1 flex-col gap-1">
              <span className="text-caption text-text-secondary">새 날짜</span>
              <span className="font-display text-display-l">12.13</span>
              <span className="text-caption text-text-secondary">(일) 19:00</span>
            </div>
          </div>
          <KeyValue label="장소" value="의암호 일대 (그대로)" />
        </section>

        <section className="flex flex-col gap-3 rounded-lg bg-bg-surface p-5 shadow-sm inset-ring inset-ring-border-default">
          <p className="text-label-m">전액 환불받을 수 있는 기간</p>
          <p className="flex items-baseline gap-2">
            <span className="font-display text-display-l">6</span>
            <span className="text-label-l">일 남았어요</span>
          </p>
          <ProgressBar value={90} />
          <p className="text-body-s text-text-secondary">
            10/5 알림 · 10/12 (월) 23:59까지 전액 환불돼요.
          </p>
        </section>

        <section className="flex flex-col gap-3 rounded-lg bg-bg-surface p-5 shadow-sm inset-ring inset-ring-border-default">
          <h3 className="text-heading-s">내 참가권</h3>
          <KeyValue label="종목" value="10K" />
          <KeyValue label="참가자" value="김러너" />
          <KeyValue label="결제 금액" value="₩45,000" />
          <KeyValue label="환불받을 금액" value="₩45,000 (예치금)" />
        </section>

        <p className="text-caption text-text-tertiary">
          그대로 참가하면 따로 할 일은 없어요. 참가권은 새 날짜로 유지돼요.
        </p>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        secondary={
          <Button variant="secondary" size="l" onClick={() => router.push('/tickets')}>
            그대로 참가
          </Button>
        }
        primary={
          <Button size="l" onClick={() => router.push(`/tickets/${ticketId}/canceled`)}>
            전액 환불받기
          </Button>
        }
      />
    </>
  )
}
