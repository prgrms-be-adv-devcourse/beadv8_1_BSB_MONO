'use client'

import { Fragment, useState } from 'react'

import { ConsoleCard, ConsolePage } from '@/widgets/console-page'
import { cn } from '@/shared/lib'
import {
  Badge,
  Button,
  Checkbox,
  Chip,
  DataTable,
  FOCUS_RING,
  Select,
  StatTile,
  TextField,
  type BadgeTone,
} from '@/shared/ui'

type Kit = {
  id: string
  buyer: string
  option: string
  status: string
  tone: BadgeTone
  address: string
}

// TODO(api): 키트 배송 API로 바꾼다. 지금은 시안 예시 값.
const KITS: Kit[] = [
  {
    id: 'G-20261004-0101',
    buyer: '달리는곰',
    option: '10K · 티셔츠 M',
    status: '배송 준비',
    tone: 'neutral',
    address: '서울 마포구',
  },
  {
    id: 'G-20261004-0117',
    buyer: '새벽6시',
    option: '10K · 티셔츠 L',
    status: '배송 준비',
    tone: 'neutral',
    address: '경기 성남시',
  },
  {
    id: 'G-20261003-0093',
    buyer: '한강토끼',
    option: '하프 · 티셔츠 M',
    status: '배송 중',
    tone: 'brand',
    address: '서울 송파구',
  },
  {
    id: 'G-20261002-0071',
    buyer: '오르막왕',
    option: '10K · 티셔츠 S',
    status: '배송 완료',
    tone: 'success',
    address: '부산 해운대구',
  },
  {
    id: 'G-20261001-0042',
    buyer: '주말러너',
    option: '하프 · 티셔츠 L',
    status: '배송 완료',
    tone: 'success',
    address: '인천 연수구',
  },
]

const FLOW = ['배송 준비', '배송 중', '배송 완료'] as const

// 버튼 문구의 조사 (배송 중으로 · 배송 완료로)
const TO: Record<(typeof FLOW)[number], string> = {
  '배송 준비': '배송 준비로',
  '배송 중': '배송 중으로',
  '배송 완료': '배송 완료로',
}

const RACES = [{ value: 'cookierun', label: '2026 용감한 쿠키RUN in 서울' }]
const STATUSES = FLOW.map((s) => ({ value: s, label: s }))
const PERIODS = [{ value: '10', label: '10/1 ~ 10/31' }]
const CARRIERS = ['CJ대한통운', '롯데택배', '한진택배', '우체국택배'].map((c) => ({
  value: c,
  label: c,
}))

// 피그마 V2 · S08 키트 배송 관리
export function SellerKitsPage() {
  const [checked, setChecked] = useState<string[]>(['G-20261004-0101', 'G-20261004-0117'])
  const [next, setNext] = useState<(typeof FLOW)[number]>('배송 중')

  const toggle = (id: string, on: boolean) =>
    setChecked((prev) => (on ? [...prev, id] : prev.filter((key) => key !== id)))

  return (
    <ConsolePage
      title="키트 배송 관리"
      description="키트는 환불 기한이 지난 뒤 배송해요. 배송 상태는 판매자가 바꾸고, 송장 번호를 넣으면 배송 중이 돼요."
      // TODO(api): 배송 목록 엑셀 내려받기
      actions={<Button variant="secondary">엑셀로 내려받기</Button>}
    >
      <div className="flex gap-4">
        <StatTile label="배송 준비" value="12" unit="건" />
        <StatTile label="배송 중" value="30" unit="건" />
        <StatTile label="배송 완료" value="158" unit="건" />
        <StatTile label="환불 기한 전" value="41" unit="건" />
      </div>

      <ConsoleCard>
        <div className="flex items-end gap-4">
          <Select label="대회" items={RACES} defaultValue="cookierun" className="flex-1" />
          <Select label="상태" items={STATUSES} defaultValue="배송 준비" className="flex-1" />
          <Select label="기간" items={PERIODS} defaultValue="10" className="flex-1" />
          <Button variant="secondary">조회</Button>
        </div>
      </ConsoleCard>

      <div className="flex flex-wrap items-start gap-6">
        <ConsoleCard
          title="주문 12건"
          aside={
            <button
              type="button"
              className={cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)}
            >
              주문번호로 찾기
            </button>
          }
          className="min-w-130 flex-1"
        >
          <DataTable
            label="키트 주문"
            rows={KITS}
            rowKey={(k) => k.id}
            selected={checked}
            columns={[
              {
                key: 'check',
                header: <span className="sr-only">고르기</span>,
                width: 'w-12',
                render: (k) => (
                  <Checkbox
                    aria-label={`${k.id} 고르기`}
                    checked={checked.includes(k.id)}
                    onCheckedChange={(on) => toggle(k.id, on)}
                  />
                ),
              },
              { key: 'id', header: '주문번호', width: 'w-40', render: (k) => k.id },
              { key: 'buyer', header: '구매자', width: 'w-22.5', render: (k) => k.buyer },
              { key: 'option', header: '키트 옵션', render: (k) => k.option },
              {
                key: 'status',
                header: '상태',
                width: 'w-25',
                render: (k) => <Badge tone={k.tone}>{k.status}</Badge>,
              },
              { key: 'address', header: '배송지', width: 'w-27.5', render: (k) => k.address },
            ]}
          />
          <p className="text-body-s text-text-tertiary">
            구매자 실명·연락처는 배송 처리에 필요한 범위에서만 보여요.
          </p>
        </ConsoleCard>

        <ConsoleCard title="배송 상태 바꾸기" className="w-90 shrink-0">
          <p className="text-body-s text-text-secondary">{checked.length}건 선택 · 배송 준비</p>
          <div className="flex items-center gap-2">
            {FLOW.map((step, i) => (
              <Fragment key={step}>
                {i > 0 && <span className="text-label-m text-text-tertiary">→</span>}
                <Chip selected={step === next} onClick={() => setNext(step)}>
                  {step}
                </Chip>
              </Fragment>
            ))}
          </div>
          <Select label="택배사" items={CARRIERS} defaultValue="CJ대한통운" />
          <TextField
            label="송장 번호"
            defaultValue="6849 1234 5678"
            helper="여러 건을 고르면 줄마다 따로 넣어요"
          />
          <p className="text-body-s text-text-tertiary">
            키트 배송은 정산과 관계없어요. 정산은 환불 기한과 대회 일정으로 정해져요.
          </p>
          {/* TODO(api): 배송 상태 바꾸기 */}
          <Button className="w-full" disabled={checked.length === 0}>
            {TO[next]} 바꾸기
          </Button>
        </ConsoleCard>
      </div>
    </ConsolePage>
  )
}
