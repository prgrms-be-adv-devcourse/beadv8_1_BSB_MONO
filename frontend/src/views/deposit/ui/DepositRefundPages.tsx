'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'

import { AppHeader } from '@/widgets/app-header'
import {
  BottomCta,
  Button,
  buttonClassName,
  KeyValue,
  Select,
  Stepper,
  TextField,
} from '@/shared/ui'

const BANKS = ['국민은행', '신한은행', '우리은행', '하나은행', '카카오뱅크', '토스뱅크'].map(
  (b) => ({ value: b, label: b }),
)

// 피그마 V2 · M22 예치금 돌려받기
// TODO(api): 환급 신청을 보낸다
export function DepositRefundPage() {
  const router = useRouter()
  return (
    <>
      <AppHeader type="sub" title="예치금 돌려받기" />
      <main className="flex flex-1 flex-col gap-4 px-4 py-6">
        <section className="flex flex-col gap-1 rounded-lg bg-bg-surface p-5 shadow-sm">
          <p className="text-label-m text-text-secondary">돌려받을 금액</p>
          <p className="font-display text-display-l">₩5,000</p>
          <p className="text-body-s text-text-secondary">
            남은 예치금 전부를 아래 계좌로 돌려받아요.
            <br />
            관리자가 확인한 뒤 보내요.
          </p>
        </section>
        <section className="flex flex-col gap-4 rounded-lg bg-bg-surface p-5 shadow-sm">
          <h2 className="text-heading-s">환급 계좌</h2>
          <Select label="은행" items={BANKS} defaultValue="국민은행" />
          <TextField label="계좌번호" inputMode="numeric" defaultValue="123456-01-234567" />
          <TextField label="예금주" defaultValue="김러너" helper="본인 명의 계좌만 쓸 수 있어요" />
        </section>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" onClick={() => router.push('/me/deposit/refund/done')}>
            ₩5,000 돌려받기 신청
          </Button>
        }
      />
    </>
  )
}

// 피그마 V2 · M22-1 예치금 돌려받기 · 신청 완료
export function DepositRefundDonePage() {
  return (
    <>
      <AppHeader type="sub" title="신청 완료" close backHref="/me/deposit?preview=refunding" />
      <main className="flex flex-1 flex-col gap-5 px-4 pt-6 pb-8">
        <div className="flex flex-col gap-2">
          <h2 className="text-heading-l">돌려받기를 신청했어요</h2>
          <p className="text-body-m text-text-secondary">관리자가 확인한 뒤 아래 계좌로 보내요.</p>
        </div>
        <section className="flex flex-col gap-4 rounded-lg bg-bg-surface p-5 shadow-sm">
          <h3 className="text-heading-s">진행 단계</h3>
          <Stepper steps={['신청', '처리 중', '환급 완료']} current={1} />
          <p className="text-caption text-text-tertiary">
            관리자가 보내면 &apos;환급 완료&apos;로 바뀌고 알림이 가요.
          </p>
        </section>
        <section className="flex flex-col gap-3 rounded-lg bg-bg-surface p-5 shadow-sm">
          <KeyValue label="신청 금액" value="₩5,000" />
          <KeyValue label="받을 계좌" value="국민은행 123456-**-***567" />
          <KeyValue label="예금주" value="김러너" />
          <KeyValue label="지금 예치금" value="₩0" />
          <KeyValue label="신청 일시" value="10/6 (화) 14:20" />
        </section>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        secondary={
          <Link href="/" className={buttonClassName({ variant: 'secondary', size: 'l' })}>
            홈으로
          </Link>
        }
        primary={
          <Link href="/me/deposit?preview=refunding" className={buttonClassName({ size: 'l' })}>
            예치금 보기
          </Link>
        }
      />
    </>
  )
}
