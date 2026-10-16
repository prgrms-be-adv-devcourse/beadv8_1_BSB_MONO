'use client'

import type { Route } from 'next'
import Link from 'next/link'

import { AppHeader } from '@/widgets/app-header'
import { useAdmissionTimer } from '@/features/queue-admission'
import { BottomCta, buttonClassName, EmptyState, ProgressBar, TimerBar } from '@/shared/ui'

const WAITING_TITLE = '차례를 기다리고 있어요'

// 피그마 V2 · M05-Q1 대기열 · 차례 기다리기 / M05-Q2 대기열 · 내 차례
// 같은 주소에서 입장 여부에 따라 두 화면을 바꿔 그린다. 입장 여부는 대기열 API로 짧은 간격마다 다시 묻고,
// 입장하면 남은 시간을 1초씩 줄여 간다. 0이 되면 입장 시간 만료 화면(M07-1)으로 간다.
// 순번 · 예상 대기 시간은 API가 주지 않아 숫자를 그리지 않는다.
export function RaceQueuePage({
  raceId,
  optionId,
  previewRemainingSeconds,
}: {
  raceId: string
  /** 줄을 선 종목. 입장 여부를 묻고, 신청 화면(M05)으로 넘길 때 쓴다 */
  optionId: string
  /** 시안 미리보기(?preview=admitted). 있으면 API를 부르지 않고 이 남은 초로 내 차례를 보여 준다 */
  previewRemainingSeconds?: number
}) {
  const preview = previewRemainingSeconds !== undefined
  const timer = useAdmissionTimer(
    preview ? undefined : Number(raceId),
    preview ? undefined : Number(optionId),
    'wait',
  )
  const remaining = preview ? previewRemainingSeconds : timer.remaining

  const raceHref = `/races/${raceId}` as Route
  const applyHref = `/races/${raceId}/apply?option=${encodeURIComponent(optionId)}` as const

  return (
    <>
      <AppHeader type="sub" title="대기열" close backHref={raceHref} />
      {remaining !== null ? (
        <>
          <TimerBar
            message="내 차례예요. 시간 안에 신청과 결제를 마쳐 주세요"
            remainingSeconds={remaining}
          />
          <main className="flex flex-1 flex-col items-center justify-center gap-6 px-4 py-6">
            <EmptyState
              icon="CircleAlert"
              title="내 차례예요"
              description={
                <>
                  남은 시간 안에 참가 신청과 결제를 마쳐 주세요.
                  <br />
                  시간이 지나면 다시 줄을 서야 해요.
                </>
              }
            />
          </main>
          <BottomCta
            className="sticky bottom-0 z-10"
            primary={
              <Link href={applyHref} className={buttonClassName({ size: 'l' })}>
                신청하러 가기
              </Link>
            }
          />
        </>
      ) : (
        <>
          <main className="flex flex-1 flex-col items-center justify-center gap-6 px-4 py-6">
            <EmptyState
              icon="CircleAlert"
              title={WAITING_TITLE}
              description={
                <>
                  차례가 되면 이 화면에서 바로 신청할 수 있어요.
                  <br />이 화면을 나가면 다시 줄을 서야 해요.
                </>
              }
            />
            {/* 진행률이 아니라 기다리는 중이라는 표시라 값 없이 계속 움직인다 */}
            <ProgressBar indeterminate aria-label={WAITING_TITLE} />
          </main>
          <BottomCta
            className="sticky bottom-0 z-10"
            primary={
              <Link
                href={raceHref}
                className={buttonClassName({ size: 'l', variant: 'secondary' })}
              >
                대회로 돌아가기
              </Link>
            }
          />
        </>
      )}
    </>
  )
}
