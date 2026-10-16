'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { cn } from '@/shared/lib'
import {
  Badge,
  BottomCta,
  Button,
  buttonClassName,
  Checkbox,
  Dialog,
  FOCUS_RING,
  KeyValue,
} from '@/shared/ui'

// 피그마 V2 · M31 회원 탈퇴 (남은 일이 있으면 탈퇴 버튼을 막는다)
// 피그마 V2 · M31-C 회원 탈퇴 최종 확인 (탈퇴하기를 누르면 뜨는 창)
// TODO(api): 남은 예치금 · 끝나지 않은 참가권을 확인한다 (blocked)
// TODO(api): PRO-130 탈퇴 요청을 보낸다
export function WithdrawPage({ blocked = true }: { blocked?: boolean }) {
  const router = useRouter()
  const [checked, setChecked] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)

  return (
    <>
      <AppHeader type="sub" title="회원 탈퇴" />
      <main className="flex flex-1 flex-col gap-4 px-4 py-6">
        <div className="flex flex-col gap-1">
          <h2 className="text-heading-l">탈퇴하기 전에 남은 일이 있어요</h2>
          <p className="text-body-s text-text-secondary">아래 2가지를 마치면 탈퇴할 수 있어요.</p>
        </div>
        <section className="flex flex-col rounded-lg bg-bg-surface px-5 py-2 shadow-sm inset-ring inset-ring-border-default">
          <div className="flex gap-3 border-b border-border-default py-4">
            <Badge tone="info">안내</Badge>
            <div className="flex flex-col items-start gap-1">
              <p className="text-label-m">남은 예치금 ₩5,000</p>
              <p className="text-body-s text-text-secondary">
                돌려받기를 신청하면 환급이 끝난 뒤 탈퇴돼요.
              </p>
              <Link
                href="/me/deposit/refund"
                className={buttonClassName({ variant: 'secondary', size: 's' })}
              >
                돌려받기 먼저
              </Link>
            </div>
          </div>
          <div className="flex gap-3 py-4">
            <Badge tone="warning">필수</Badge>
            <div className="flex flex-col items-start gap-1">
              <p className="text-label-m">끝나지 않은 참가권 1장</p>
              <p className="text-body-s text-text-secondary">
                용감한 쿠키RUN(11/1)을 취소하거나 대회가 끝난 뒤 탈퇴할 수 있어요.
              </p>
              <Link href="/tickets" className={cn('rounded-sm text-label-s', FOCUS_RING)}>
                참가권 보기
              </Link>
            </div>
          </div>
        </section>
        <section className="flex flex-col gap-2 rounded-lg bg-bg-subtle p-4">
          <p className="text-label-strong">탈퇴하면 개인정보는 바로 지워요</p>
          <p className="text-caption text-text-secondary">
            다만 법에 따라 아래 기록은 정해진 기간만 보관해요.
          </p>
          <KeyValue label="계약 · 결제 · 환불 기록" value="5년" />
          <KeyValue label="소비자 불만 · 분쟁 기록" value="3년" />
          <KeyValue label="접속 기록" value="3개월" />
        </section>
        <Checkbox
          checked={checked}
          onCheckedChange={setChecked}
          disabled={blocked}
          label="안내를 모두 확인했어요"
        />
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" disabled={blocked || !checked} onClick={() => setConfirmOpen(true)}>
            탈퇴하기
          </Button>
        }
      />
      <Dialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        type="danger"
        title="정말 탈퇴할까요?"
        body={
          <>
            계정과 개인정보가 바로 지워져요.
            <br />
            탈퇴하면 되돌릴 수 없어요.
          </>
        }
        confirmLabel="탈퇴하기"
        cancelLabel="그대로 두기"
        onConfirm={() => router.push('/me/withdraw/done')}
      />
    </>
  )
}

// 피그마 V2 · M31-1 회원 탈퇴 완료
export function WithdrawDonePage() {
  return (
    <>
      <AppHeader type="sub" title="탈퇴 완료" close backHref="/" />
      <main className="flex flex-1 flex-col gap-2 px-4 py-6">
        <h2 className="text-heading-l">탈퇴했어요</h2>
        <p className="text-body-s text-text-secondary">
          그동안 크루런과 함께 달려 주셔서 고마워요.
        </p>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Link href="/" className={buttonClassName({ size: 'l' })}>
            홈으로
          </Link>
        }
      />
    </>
  )
}
