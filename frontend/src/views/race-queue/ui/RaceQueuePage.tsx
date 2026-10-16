'use client'

import type { Route } from 'next'
import Link from 'next/link'

import { AppHeader } from '@/widgets/app-header'
import { BottomCta, buttonClassName, EmptyState, ProgressBar, TimerBar } from '@/shared/ui'

import type { QueueAdmission } from '../model/admission'

const WAITING_TITLE = '차례를 기다리고 있어요'

// 피그마 V2 · M05-Q1 대기열 · 차례 기다리기 / M05-Q2 대기열 · 내 차례
// 같은 주소에서 입장 여부(admission.admitted)에 따라 두 화면을 바꿔 그린다.
// 순번 · 예상 대기 시간은 API가 주지 않아 숫자를 그리지 않는다.
// TODO(api): 입장 여부를 GET /api/v1/order/queue/admission 으로 다시 물어 admission을 채우고, 남은 초를 줄여 간다.
export function RaceQueuePage({
  raceId,
  optionId,
  admission,
}: {
  raceId: string
  /** 줄을 선 종목. 신청 화면(M05)으로 넘길 때 쓴다 */
  optionId?: string
  /** 입장 확인 결과. 아직 모르면 기다리는 화면을 보여 준다 */
  admission?: QueueAdmission
}) {
  const raceHref = `/races/${raceId}` as Route
  const applyHref = optionId
    ? (`/races/${raceId}/apply?option=${encodeURIComponent(optionId)}` as const)
    : (`/races/${raceId}/apply` as const)

  return (
    <>
      <AppHeader type="sub" title="대기열" close backHref={raceHref} />
      {admission?.admitted ? (
        <>
          <TimerBar
            message="내 차례예요. 시간 안에 신청과 결제를 마쳐 주세요"
            remainingSeconds={admission.remainingSeconds}
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
