import Link from 'next/link'

import { AppHeader } from '@/widgets/app-header'
import { BottomCta, buttonClassName, KeyValue } from '@/shared/ui'

// 피그마 V2 · M16-2 참가 취소 완료
export function TicketCanceledPage({ ticketId }: { ticketId: string }) {
  return (
    <>
      <AppHeader type="sub" title="취소 완료" close backHref="/tickets" />
      <main className="flex flex-1 flex-col gap-5 px-4 pt-6 pb-8">
        <div className="flex flex-col gap-2">
          <h2 className="text-heading-l">참가를 취소했어요</h2>
          <p className="text-body-m text-text-secondary">2026 용감한 쿠키RUN in 서울 · 10K</p>
        </div>
        <div className="flex flex-col gap-3 rounded-lg bg-bg-surface p-5 shadow-sm">
          <KeyValue label="주문번호" value={ticketId} />
          <KeyValue label="돌려받은 금액" value="₩45,000 (예치금)" />
          <KeyValue label="지금 예치금" value="₩50,000" />
          <KeyValue label="키트" value="함께 취소됨" />
          <KeyValue label="취소 일시" value="10/6 (화) 14:10" />
        </div>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        secondary={
          <Link href="/me/deposit" className={buttonClassName({ variant: 'secondary', size: 'l' })}>
            예치금 보기
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
