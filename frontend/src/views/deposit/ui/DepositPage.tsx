'use client'

import Link from 'next/link'
import { useState } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { useMyBalance } from '@/entities/wallet'
import { formatWon } from '@/shared/lib'
import { Alert, buttonClassName, Chip, KeyValue, Skeleton } from '@/shared/ui'

type Kind = 'all' | 'charge' | 'pay' | 'refund'
type History = { title: string; at: string; amount: string; kind: Exclude<Kind, 'all'> }

// TODO(api): 예치금 내역 API가 생기면 바꾼다. 시안 예시 값. (잔액은 API로 불러온다)
const HISTORY: History[] = [
  { title: '결제 · 쿠키RUN 10K', at: '10/5 14:22', amount: '− ₩45,000', kind: 'pay' },
  { title: '충전 · 토스페이먼츠', at: '10/5 14:21', amount: '+ ₩10,000', kind: 'charge' },
  { title: '결제 · 춘천 10K', at: '10/2 11:30', amount: '− ₩45,000', kind: 'pay' },
  { title: '환불 · 가을 트레일 21K 취소', at: '9/30 09:10', amount: '+ ₩60,000', kind: 'refund' },
  { title: '결제 · 가을 트레일 21K', at: '9/12 20:05', amount: '− ₩60,000', kind: 'pay' },
  { title: '충전 · 토스페이먼츠', at: '9/1 18:40', amount: '+ ₩85,000', kind: 'charge' },
]

const REFUNDING: History = {
  title: '환급 신청 · 국민은행',
  at: '10/6 14:20 · 처리 중',
  amount: '− ₩5,000',
  kind: 'refund',
}

const FILTERS: { value: Kind; label: string }[] = [
  { value: 'all', label: '전체' },
  { value: 'charge', label: '충전' },
  { value: 'pay', label: '결제' },
  { value: 'refund', label: '환불 · 환급' },
]

// 피그마 V2 · M17 예치금 (+ M17-1 돌려받는 중)
export function DepositPage({ refunding = false }: { refunding?: boolean }) {
  const [kind, setKind] = useState<Kind>('all')
  const { balance, isPending, isError } = useMyBalance()
  const history = refunding ? [REFUNDING, ...HISTORY] : HISTORY
  const rows = kind === 'all' ? history : history.filter((h) => h.kind === kind)

  return (
    <>
      <AppHeader type="sub" title="예치금" />
      <main className="flex flex-1 flex-col gap-6 px-4 py-6">
        <section className="flex flex-col gap-3 rounded-lg bg-bg-surface p-5 shadow-sm inset-ring inset-ring-border-default">
          <div className="flex flex-col gap-1">
            <p className="text-label-m text-text-secondary">보유 예치금</p>
            {isPending ? (
              <Skeleton shape="rect" className="h-9 w-32" />
            ) : (
              <p className="font-display text-display-m">
                {isError || balance === undefined ? '-' : formatWon(balance)}
              </p>
            )}
            {isError && (
              <p className="text-caption text-text-tertiary">잔액을 불러오지 못했어요.</p>
            )}
          </div>
          {refunding && (
            <Alert
              title="₩5,000 돌려받는 중이에요"
              description="국민은행 계좌로 보내면 '환급 완료'로 바뀌어요."
            />
          )}
          <div className="flex gap-2">
            <Link href="/me/deposit/charge" className={buttonClassName({ className: 'flex-1' })}>
              충전하기
            </Link>
            {refunding ? (
              <span
                className={buttonClassName({
                  variant: 'secondary',
                  className: 'flex-1 bg-bg-disabled text-text-disabled inset-ring-border-disabled',
                })}
              >
                돌려받기
              </span>
            ) : (
              <Link
                href="/me/deposit/refund"
                className={buttonClassName({ variant: 'secondary', className: 'flex-1' })}
              >
                돌려받기
              </Link>
            )}
          </div>
          <p className="text-caption text-text-tertiary">
            {refunding
              ? '돌려받는 중에는 다시 신청할 수 없어요. 충전은 할 수 있어요.'
              : '남은 예치금은 언제든 전부 돌려받을 수 있어요.'}
          </p>
        </section>

        <section className="flex flex-col gap-3">
          <h2 className="text-heading-s">내역</h2>
          <div className="flex flex-wrap gap-2">
            {FILTERS.map((f) => (
              <Chip key={f.value} selected={kind === f.value} onClick={() => setKind(f.value)}>
                {f.label}
              </Chip>
            ))}
          </div>
          <ul className="flex flex-col">
            {rows.map((row) => (
              <li
                key={`${row.title}-${row.at}`}
                className="flex items-center justify-between gap-2 border-b border-border-default py-3"
              >
                <div className="flex min-w-0 flex-1 flex-col gap-1">
                  <p className="text-label-m">{row.title}</p>
                  <p className="text-caption text-text-tertiary">{row.at}</p>
                </div>
                <p className="text-label-strong tabular-nums">{row.amount}</p>
              </li>
            ))}
          </ul>
        </section>

        <section className="flex flex-col gap-3 rounded-lg bg-bg-subtle p-5">
          <h2 className="text-heading-s">돌려받는 방법</h2>
          <div className="flex flex-col gap-2">
            <KeyValue label="계좌로 돌려받기" value="남은 예치금 전부" />
            <p className="text-caption text-text-tertiary">
              관리자가 확인한 뒤 계좌로 보내 드려요.
            </p>
          </div>
        </section>
      </main>
    </>
  )
}
