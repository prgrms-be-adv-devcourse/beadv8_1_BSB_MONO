'use client'

import { useState } from 'react'

import { ConsoleCard, ConsolePage } from '@/widgets/console-page'
import { cn } from '@/shared/lib'
import {
  Badge,
  Button,
  DataTable,
  Divider,
  FOCUS_RING,
  KeyValue,
  Select,
  StatTile,
  TextField,
  type BadgeTone,
} from '@/shared/ui'

type Hold = {
  id: string
  seller: string
  settlementId: string
  amount: string
  reason: string
  status: string
  tone: BadgeTone
}

// TODO(api): 지급보류 · 회수 API로 바꾼다. 시안 예시 값.
const HOLDS: Hold[] = [
  {
    id: 'HD-20261027-01A2',
    seller: '춘천러닝클럽',
    settlementId: 'ST-20261120-1C07',
    amount: '₩860,000',
    reason: '정산 계좌 미등록',
    status: '지급보류',
    tone: 'warning',
  },
  {
    id: 'HD-20261025-0C13',
    seller: '트레일앤코',
    settlementId: 'ST-20261120-1B07',
    amount: '₩2,400,000',
    reason: '대회 취소 처리',
    status: '지급보류',
    tone: 'warning',
  },
  {
    id: 'HD-20260921-0B71',
    seller: '나이트런컴퍼니',
    settlementId: 'ST-20260921-09B2',
    amount: '₩300,000',
    reason: '정산 계좌 미등록',
    status: '해제',
    tone: 'neutral',
  },
]

const CLAWBACKS = [
  { seller: '나이트런컴퍼니', settlementId: 'ST-20261020-2D40', amount: '₩1,040,000' },
]

const REASONS = [
  { value: 'race-canceled', label: '대회 취소 처리' },
  { value: 'no-account', label: '정산 계좌 미등록' },
]

// 피그마 V2 · A06 지급보류 관리
export function AdminHoldsPage() {
  const [selectedId, setSelectedId] = useState('HD-20261025-0C13')
  const hold = HOLDS.find((h) => h.id === selectedId) ?? HOLDS[1]

  return (
    <ConsolePage
      title="지급보류 관리"
      description="보류한 금액은 지급 완료로 바꿀 수 없어요. 풀면 다음 정산일(매월 20일) 묶음에 넣어 지급해요."
      // TODO(api): 지급보류 목록 엑셀 내려받기
      actions={<Button variant="secondary">엑셀로 내려받기</Button>}
    >
      <div className="flex gap-4">
        <StatTile label="지급보류 중" value="2" unit="건" />
        <StatTile label="보류 금액" value="3,260,000" unit="원" />
        <StatTile label="이번 달 해제" value="1" unit="건" />
      </div>

      <ConsoleCard
        title={`지급보류 ${HOLDS.length}건`}
        aside={
          <button
            type="button"
            className={cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)}
          >
            ID로 찾기
          </button>
        }
      >
        <DataTable
          label="지급보류"
          rows={HOLDS}
          rowKey={(h) => h.id}
          selected={selectedId}
          onRowClick={(h) => setSelectedId(h.id)}
          columns={[
            { key: 'id', header: '지급보류 ID', width: 'w-42.5', render: (h) => h.id },
            { key: 'seller', header: '판매자', width: 'w-35', render: (h) => h.seller },
            {
              key: 'settlementId',
              header: '정산 ID',
              width: 'w-42.5',
              render: (h) => h.settlementId,
            },
            {
              key: 'amount',
              header: '금액',
              align: 'right',
              width: 'w-27.5',
              render: (h) => h.amount,
            },
            { key: 'reason', header: '사유', render: (h) => h.reason },
            {
              key: 'status',
              header: '상태',
              width: 'w-21',
              render: (h) => <Badge tone={h.tone}>{h.status}</Badge>,
            },
          ]}
        />
      </ConsoleCard>

      <div className="flex flex-wrap items-start gap-6">
        <ConsoleCard title="보류 처리" className="w-110 shrink-0">
          <div className="flex flex-col gap-3 rounded-md bg-bg-subtle p-4">
            <p className="text-label-m">
              {hold.id} · {hold.seller}
            </p>
            <KeyValue label="보류 금액" value={hold.amount} />
            <KeyValue label="사유" value={hold.reason} />
            {/* TODO(api): 보류 해제 */}
            <Button variant="secondary" className="w-full" disabled={hold.status === '해제'}>
              보류 해제
            </Button>
            <p className="text-caption text-text-tertiary">
              해제하면 다음 정산일(11/20) 묶음에 넣어 지급해요.
            </p>
          </div>
          <Divider />
          <h3 className="text-label-l">새 보류 걸기</h3>
          <TextField label="정산 ID" defaultValue="ST-20261120-3F02" />
          <Select
            label="보류 사유"
            helper="정산 계좌 미등록 · 대회 취소 처리"
            items={REASONS}
            defaultValue="race-canceled"
          />
          {/* TODO(api): 보류 걸기 */}
          <Button className="w-full">보류 걸기</Button>
          <p className="text-caption text-text-tertiary">판매자 화면에 사유가 그대로 보여요.</p>
        </ConsoleCard>

        <ConsoleCard
          title={`회수 처리 ${CLAWBACKS.length}건`}
          className="min-w-130 flex-1 items-start"
        >
          <p className="text-body-s text-text-secondary">
            1차 정산을 지급한 뒤 대회가 취소된 건이에요. 1차 정산금 중 환불 몫을 돌려받아요.
          </p>
          <DataTable
            label="회수 대기"
            rows={CLAWBACKS}
            rowKey={(c) => c.settlementId}
            className="w-full"
            columns={[
              { key: 'seller', header: '판매자', render: (c) => c.seller },
              { key: 'settlementId', header: '정산 ID', render: (c) => c.settlementId },
              { key: 'amount', header: '회수할 금액', align: 'right', render: (c) => c.amount },
              {
                key: 'status',
                header: '상태',
                width: 'w-25',
                render: () => <Badge tone="warning">회수 대기</Badge>,
              },
            ]}
          />
          {/* TODO(api): 회수 완료 */}
          <Button variant="secondary">회수 완료로 바꾸기</Button>
          <p className="text-body-s text-text-secondary">
            회수가 끝나야 구매자에게 환불돼요. 1차에서는 상태만 바꿔요.
          </p>
        </ConsoleCard>
      </div>
    </ConsolePage>
  )
}
