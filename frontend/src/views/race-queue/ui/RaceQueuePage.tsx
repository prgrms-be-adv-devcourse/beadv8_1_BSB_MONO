'use client'

import type { Route } from 'next'
import Link from 'next/link'

import { AppHeader } from '@/widgets/app-header'
import { useAdmissionTimer } from '@/features/queue-admission'
import { BottomCta, buttonClassName, formatTimer, Icon, ProgressBar } from '@/shared/ui'

// 피그마 V2 · M05-Q1 대기열 · 차례 기다리기 / M05-Q2 대기열 · 내 차례
// 같은 주소에서 입장 여부에 따라 두 화면을 바꿔 그린다. 입장 여부는 대기열 API로 짧은 간격마다 다시 묻고,
// 입장하면 남은 시간을 1초씩 줄여 간다. 0이 되면 입장 시간 만료 화면(M07-1)으로 간다.
// 순번 · 예상 대기 시간은 API가 주지 않아 숫자를 그리지 않는다.
// 상단 timer bar 띠는 이 화면에 없다(신청서 M05부터). 왼쪽 위 X는 대회 상세로 이동만 한다.
// 96px 그림도 시안처럼 선을 2px로 둔다. 아이콘은 24 격자라 96px에서는 0.5가 2px이다.
const BIG_ICON_STROKE = 0.5

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
          <main className="flex flex-1 flex-col items-center justify-center gap-8 px-4 py-6">
            <h1 className="text-center text-heading-l text-text-primary">
              내 차례예요
              <br />
              지금 신청할 수 있어요
            </h1>
            <Icon
              name="CircleCheck"
              className="size-24 text-text-primary"
              strokeWidth={BIG_ICON_STROKE}
              aria-hidden
            />
            <div role="timer" className="flex flex-col items-center gap-1 text-center">
              <p className="text-label-m text-text-secondary">남은 시간</p>
              <p className="font-display text-display-m text-text-primary tabular-nums">
                {formatTimer(remaining)}
              </p>
            </div>
            <p className="text-center text-body-s text-text-secondary">
              남은 시간 안에 참가 신청과 결제를 마쳐 주세요.
              <br />
              시간이 지나면 다시 줄을 서야 해요.
            </p>
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
        <main className="flex flex-1 flex-col items-center justify-center gap-8 px-4 py-6">
          <h1 className="text-center text-heading-l text-text-primary">
            지금 신청하는 분이 많아서
            <br />
            잠시 기다리고 있어요
          </h1>
          <Icon
            name="Users"
            className="size-24 text-text-primary"
            strokeWidth={BIG_ICON_STROKE}
            aria-hidden
          />
          {/* 진행률이 아니라 기다리는 중이라는 표시라 값 없이 계속 움직인다 */}
          <ProgressBar indeterminate aria-label="대기 중" className="w-70" />
          <p className="text-center text-body-s text-text-secondary">
            차례대로 들어가고 있어요. 잠시만 기다려 주세요.
            <br />
            화면을 나가면 다시 줄을 서야 해요.
          </p>
        </main>
      )}
    </>
  )
}
