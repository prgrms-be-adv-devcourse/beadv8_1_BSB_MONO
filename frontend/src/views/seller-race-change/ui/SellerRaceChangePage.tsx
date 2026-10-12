'use client'

import { useState } from 'react'

import { ConsoleCard, ConsolePage } from '@/widgets/console-page'
import { cn } from '@/shared/lib'
import { Button, Dialog, FOCUS_RING, KeyValue, StatTile, Textarea, TextField } from '@/shared/ui'

type Mode = 'reschedule' | 'cancel'

const NOTICE =
  '[2026 춘천 나이트런] 대회일이 12월 6일에서 12월 13일(일)로 바뀌었어요.\n오늘부터 7일 동안(10/12까지)은 전액 환불받을 수 있어요.'

const CANCEL_SUMMARY = [
  {
    label: '참가권 (키트 포함)',
    title: '1,500건 전액 자동 환불',
    body: '₩82,500,000 · 1차 정산 전이라 바로 예치금으로 돌려줘요. 이미 받은 키트는 돌려받지 않아요.',
  },
  {
    label: '1차 정산 뒤 취소라면',
    title: '1차 정산금 회수 뒤 환불',
    body: '회수가 끝나야 구매자에게 환불돼요. 그동안 기다린다는 걸 구매자에게 알려요.',
  },
  {
    label: '정산',
    title: '지급보류 · 대회 취소 처리',
    body: '환불이 끝나면 관리자가 보류를 풀어요.',
  },
]

// 피그마 V2 · S09 대회 취소 · 일정 변경 (+ S09-1 대회 취소 확인)
// TODO(api): 대회 정보를 불러오고 일정 변경 알림 · 대회 취소를 보낸다. 지금은 시안 예시 값.
export function SellerRaceChangePage({ confirmOpen = false }: { confirmOpen?: boolean }) {
  const [mode, setMode] = useState<Mode>('reschedule')
  const [cancelOpen, setCancelOpen] = useState(confirmOpen)

  return (
    <ConsolePage
      title="대회 취소 · 일정 변경"
      description="2026 춘천 나이트런 · 대회 12/6(일) · 참가자 1,500명"
      actions={<Button disabled={mode !== 'reschedule'}>일정 변경 알리기</Button>}
    >
      <div role="radiogroup" aria-label="할 일" className="flex flex-wrap gap-4">
        <ModeCard
          selected={mode === 'reschedule'}
          onSelect={() => setMode('reschedule')}
          title="일정 변경"
          description="대회 날짜를 바꾸고 참가자에게 알려요"
        />
        <ModeCard
          selected={mode === 'cancel'}
          onSelect={() => setMode('cancel')}
          title="대회 취소"
          description="참가권(키트 포함)을 모두 자동 환불해요"
        />
      </div>

      <div className="flex flex-wrap items-start gap-6">
        <ConsoleCard title="일정 변경" className="min-w-130 flex-1">
          <KeyValue label="지금 대회일" value="2026. 12. 6 (일)" />
          <TextField label="새 대회일" defaultValue="2026. 12. 13 (일)" />
          <Textarea
            label="알림 문구 미리보기"
            helper="참가자 전원에게 앱 알림과 문자로 보내요"
            showCounter
            maxLength={200}
            defaultValue={NOTICE}
          />
        </ConsoleCard>

        <ConsoleCard title="영향 요약" className="w-100 shrink-0">
          <StatTile label="알림 받을 참가자" value="1,500" unit="명" className="flex-none" />
          <div className="flex flex-col gap-3 rounded-md bg-bg-subtle p-4">
            <KeyValue label="전액 환불 허용" value="알린 날부터 7일 (~10/12)" />
            <KeyValue label="최종 정산 대상일" value="바뀐 대회일로 다시 잡혀요" />
          </div>
          <p className="text-body-s text-text-tertiary">
            지금은 1차 정산 전이라 환불이 바로 돼요. 1차 정산 뒤였다면 1차 정산금을 회수한 뒤
            환불해요.
          </p>
        </ConsoleCard>
      </div>

      <ConsoleCard title="대회 취소하면">
        <div className="flex gap-4">
          {CANCEL_SUMMARY.map((item) => (
            <div
              key={item.label}
              className="flex flex-1 flex-col gap-1 rounded-md bg-bg-subtle p-4"
            >
              <p className="text-caption text-text-secondary">{item.label}</p>
              <p className="text-label-l">{item.title}</p>
              <p className="text-body-s text-text-secondary">{item.body}</p>
            </div>
          ))}
        </div>
        <div className="flex items-center justify-between gap-4">
          <p className="text-body-s text-text-danger">
            취소하면 되돌릴 수 없어요. 참가자에게 바로 알림이 가요.
          </p>
          <Button variant="danger" onClick={() => setCancelOpen(true)}>
            대회 취소하기
          </Button>
        </div>
      </ConsoleCard>

      <Dialog
        open={cancelOpen}
        onOpenChange={setCancelOpen}
        type="danger"
        title="대회를 취소할까요?"
        body="참가자 1,500명에게 ₩82,500,000을 자동 환불하고 바로 알림을 보내요. 되돌릴 수 없어요."
        confirmLabel="대회 취소하기"
        cancelLabel="그대로 두기"
        onConfirm={() => setCancelOpen(false)}
      />
    </ConsolePage>
  )
}

function ModeCard({
  selected,
  onSelect,
  title,
  description,
}: {
  selected: boolean
  onSelect: () => void
  title: string
  description: string
}) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      onClick={onSelect}
      className={cn(
        'flex min-w-100 flex-1 items-center gap-3 rounded-lg p-5 text-left',
        selected
          ? 'bg-bg-brand-subtle inset-ring-2 inset-ring-border-brand'
          : 'bg-bg-surface inset-ring inset-ring-border-default',
        FOCUS_RING,
      )}
    >
      {/* 피그마 Radio 원 20: 고르면 두꺼운 브랜드 테두리 */}
      <span
        aria-hidden
        className={cn(
          'size-5 shrink-0 rounded-full bg-bg-surface',
          selected
            ? 'inset-ring-6 inset-ring-border-brand'
            : 'inset-ring-[1.5px] inset-ring-border-strong',
        )}
      />
      <span className="flex min-w-0 flex-1 flex-col gap-1">
        <span className="text-label-l">{title}</span>
        <span className="text-body-s text-text-secondary">{description}</span>
      </span>
    </button>
  )
}
