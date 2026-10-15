'use client'

import Link from 'next/link'
import { useEffect, useRef, useState } from 'react'

import { ConsoleCard, ConsolePage } from '@/widgets/console-page'
import { cn } from '@/shared/lib'
import {
  Alert,
  Badge,
  Button,
  buttonClassName,
  DataTable,
  Divider,
  FOCUS_RING,
  KeyValue,
  TextField,
  useToast,
  type BadgeTone,
} from '@/shared/ui'

type Race = {
  id: string
  title: string
  date: string
  sold: number
  capacity: number
  status: string
  tone: BadgeTone
}

// TODO(api): 판매자 대회 목록 API로 바꾼다. 지금은 시안 예시 값.
const RACES: Race[] = [
  {
    id: 'cookierun',
    title: '2026 용감한 쿠키RUN in 서울',
    date: '11/1',
    sold: 2410,
    capacity: 3000,
    status: '판매 중',
    tone: 'brand',
  },
  {
    id: 'chuncheon',
    title: '2026 춘천 나이트런',
    date: '12/6',
    sold: 1500,
    capacity: 5000,
    status: '판매 중',
    tone: 'brand',
  },
  {
    id: 'busan',
    title: '2027 부산 바다 10K',
    date: '3/14',
    sold: 0,
    capacity: 3000,
    status: '판매 예정',
    tone: 'neutral',
  },
  {
    id: 'trail',
    title: '2026 가을 트레일런',
    date: '11/22',
    sold: 0,
    capacity: 800,
    status: '작성 중',
    tone: 'neutral',
  },
  {
    id: 'hangang',
    title: '2026 봄 한강 10K',
    date: '4/12',
    sold: 2000,
    capacity: 2000,
    status: '종료',
    tone: 'neutral',
  },
]

const n = (value: number) => value.toLocaleString('ko-KR')

// 피그마 V2 · S06 대회 관리 (+ S02-4 등록 완료 토스트)
export function SellerRacesPage({ registeredRaceId }: { registeredRaceId?: string }) {
  const [selectedId, setSelectedId] = useState(RACES[0].id)
  const toast = useToast()
  const shown = useRef(false)

  useEffect(() => {
    if (!registeredRaceId || shown.current) {
      return
    }
    shown.current = true
    toast({
      message: '대회를 등록했어요',
      tone: 'success',
      action: {
        label: '링크 복사',
        onClick: () =>
          void navigator.clipboard?.writeText(
            `${window.location.origin}/races/${registeredRaceId}`,
          ),
      },
    })
  }, [registeredRaceId, toast])

  const race = RACES.find((r) => r.id === selectedId) ?? RACES[0]

  return (
    <ConsolePage
      title="대회 관리"
      description="판매가 시작된 대회는 구매자에게 유리한 변경만 할 수 있어요."
      actions={
        <Link href="/seller/races/new" className={buttonClassName()}>
          대회 등록
        </Link>
      }
    >
      <div className="flex flex-wrap items-start gap-6">
        <ConsoleCard
          title={`대회 ${RACES.length}개`}
          aside={
            <button
              type="button"
              className={cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)}
            >
              대회명으로 찾기
            </button>
          }
          className="min-w-130 flex-1"
        >
          <DataTable
            label="내 대회"
            rows={RACES}
            rowKey={(r) => r.id}
            selected={selectedId}
            onRowClick={(r) => setSelectedId(r.id)}
            columns={[
              { key: 'title', header: '대회', render: (r) => r.title },
              {
                key: 'date',
                header: '대회일',
                align: 'right',
                width: 'w-24',
                render: (r) => r.date,
              },
              {
                key: 'sold',
                header: '판매 / 정원',
                align: 'right',
                width: 'w-37.5',
                render: (r) => `${n(r.sold)} / ${n(r.capacity)}`,
              },
              {
                key: 'status',
                header: '상태',
                width: 'w-28',
                render: (r) => <Badge tone={r.tone}>{r.status}</Badge>,
              },
            ]}
          />
          <p className="text-body-s text-text-tertiary">
            줄을 고르면 오른쪽에서 판매 중 변경을 할 수 있어요.
          </p>
        </ConsoleCard>

        <ConsoleCard title="판매 중 변경" className="w-110 shrink-0">
          <p className="text-body-s text-text-secondary">
            {race.title} · {race.status}
          </p>
          <div className="flex flex-col gap-3">
            <h3 className="text-label-l">정원 늘리기</h3>
            <KeyValue label="이미 판매된 수량" value={`${n(race.sold)}명`} />
            <TextField
              key={race.id}
              label="모집 정원"
              inputMode="numeric"
              defaultValue={n(race.id === 'cookierun' ? 3500 : race.capacity)}
              helper={`판매된 ${n(race.sold)}명보다 적게 줄일 수 없어요`}
            />
          </div>
          <Divider />
          <div className="flex flex-col gap-3">
            <h3 className="text-label-l">가격 · 환불 기한</h3>
            <KeyValue label="참가비 (10K)" value="₩45,000" />
            <KeyValue label="환불 기한" value="10/31(토) 23:59 (늘리기만 할 수 있어요)" />
            <Alert
              title="구매자에게 유리할 때만 바꿀 수 있어요"
              description="가격 내리기, 환불 기한 늘리기만 돼요."
            />
          </div>
          {/* TODO(api): 판매 중 변경 저장 */}
          <Button variant="secondary" className="w-full">
            변경 저장
          </Button>
        </ConsoleCard>
      </div>
    </ConsolePage>
  )
}
