import Link from 'next/link'

import { AppHeader } from '@/widgets/app-header'
import { RACE_DETAIL } from '@/entities/race'
import { Badge, BibCard, BottomCta, buttonClassName, KeyValue } from '@/shared/ui'

// 피그마 V2 · M08 참가 확정
// TODO(api): 주문 결과를 받아 그린다. 지금은 시안 예시 값.
export function CheckoutCompletePage() {
  const race = RACE_DETAIL
  return (
    <>
      <AppHeader type="sub" title="참가 확정" close backHref="/" />
      <main className="flex flex-1 flex-col gap-5 px-4 pt-6 pb-8">
        <div className="flex flex-col gap-2 pb-2">
          <h2 className="text-heading-l">참가가 확정됐어요</h2>
          <p className="text-body-m text-text-secondary">{race.title} · 10K</p>
        </div>
        <BibCard
          image={race.images[0]}
          imagePosition="50% 12%"
          eyebrow="COOKIE RUN 2026"
          status={<Badge tone="live">참가 확정</Badge>}
          unit="No."
          number="1024"
          title="김러너 · 10K"
          meta="11/1 (일) 08:00 · 서울숲 일대"
        />
        <div className="flex flex-col gap-3 rounded-lg bg-bg-surface p-5 shadow-sm">
          <KeyValue label="주문번호" value="R-20261005-0012" />
          <KeyValue label="참가자" value="김러너" />
          <KeyValue label="결제" value="₩45,000 (예치금)" />
          <KeyValue label="남은 예치금" value="₩5,000" />
          <KeyValue label="키트 배송" value="환불 기한(10/31) 지난 뒤 배송" />
        </div>
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
