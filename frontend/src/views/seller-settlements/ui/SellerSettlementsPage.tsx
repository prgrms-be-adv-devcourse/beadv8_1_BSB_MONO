'use client'

import { useState } from 'react'

import { ConsoleCard, ConsolePage } from '@/widgets/console-page'
import { cn } from '@/shared/lib'
import {
  Alert,
  Badge,
  Button,
  DataTable,
  EmptyState,
  FOCUS_RING,
  Segmented,
  Select,
  StatTile,
  TextField,
  type BadgeTone,
} from '@/shared/ui'

export type SettlementTab = 'monthly' | 'orders' | 'holds' | 'clawbacks'

type Monthly = {
  id: string
  race: string
  kind: string
  sales: string
  refund: string
  fee: string
  payout: string
  paidAt: string
  status: string
  tone: BadgeTone
}

type Hold = {
  id: string
  settlementId: string
  amount: string
  reason: string
  status: string
  tone: BadgeTone
  heldAt: string
}

// TODO(api): 판매자 정산 API로 바꾼다. 지금은 시안 예시 값.
const MONTHLY: Monthly[] = [
  {
    id: 'ST-20261120-0A3F',
    race: '쿠키RUN',
    kind: '1차 정산',
    sales: '₩5,200,000',
    refund: '₩400,000',
    fee: '₩0',
    payout: '₩1,920,000',
    paidAt: '11/20',
    status: '지급 예정',
    tone: 'neutral',
  },
  {
    id: 'ST-20261020-09B2',
    race: '한강 나이트런',
    kind: '최종 정산',
    sales: '₩2,100,000',
    refund: '₩100,000',
    fee: '₩110,000',
    payout: '₩1,090,000',
    paidAt: '10/20',
    status: '지급 완료',
    tone: 'success',
  },
]

const HOLDS: Hold[] = [
  {
    id: 'HD-20261027-01A2',
    settlementId: 'ST-20261020-1C07',
    amount: '₩1,200,000',
    reason: '정산 계좌 미등록',
    status: '지급보류',
    tone: 'warning',
    heldAt: '10/27',
  },
  {
    id: 'HD-20260918-0B71',
    settlementId: 'ST-20260921-07C5',
    amount: '₩300,000',
    reason: '대회 취소 처리',
    status: '해제 · 10/2',
    tone: 'neutral',
    heldAt: '9/18',
  },
]

const PERIODS = [
  { value: '0915', label: '9/15 ~ 10/14' },
  { value: '1015', label: '10/15 ~ 11/14' },
]

// 피그마 V2 · S04 정산(월별 정산) · S10 정산 내역 · 지급보류
export function SellerSettlementsPage({ initialTab = 'monthly' }: { initialTab?: SettlementTab }) {
  const [tab, setTab] = useState<SettlementTab>(initialTab)
  const isHolds = tab === 'holds'

  return (
    <ConsolePage
      title={isHolds ? '정산 내역' : '정산'}
      description={
        isHolds
          ? '매월 20일(휴일이면 다음 영업일)에 전월 정산 대상분을 지급해요. 보류가 풀리면 다음 20일 묶음에 넣어 지급해요.'
          : '매월 20일(휴일이면 다음 영업일)에 전월 1일~말일에 정산 대상이 된 건을 대회별로 묶어 지급해요.'
      }
      // TODO(api): 정산 내역 엑셀 내려받기
      actions={<Button variant="secondary">엑셀로 내려받기</Button>}
    >
      <div className="flex gap-6">
        <StatTile label="다음 정산일" value="11/20" unit="(금)" />
        <StatTile tone="accent" label="정산 예정액" value="1,920,000" unit="원" />
        <StatTile label="지급보류" value="1" unit="건" />
      </div>

      <Segmented
        label="정산 보기"
        layout="hug"
        value={tab}
        onChange={setTab}
        items={[
          { value: 'monthly', label: '월별 정산' },
          { value: 'orders', label: '건별 정산' },
          { value: 'holds', label: '지급보류 내역', count: 1 },
          { value: 'clawbacks', label: '회수 내역' },
        ]}
      />

      {tab === 'monthly' && (
        <ConsoleCard
          title="대회별 월 정산"
          aside={
            <button
              type="button"
              className={cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)}
            >
              정산 ID로 찾기
            </button>
          }
        >
          <DataTable
            label="대회별 월 정산"
            rows={MONTHLY}
            rowKey={(r) => r.id}
            columns={[
              { key: 'id', header: '정산 ID', width: 'w-42.5', render: (r) => r.id },
              { key: 'race', header: '대회', render: (r) => r.race },
              { key: 'kind', header: '구분', render: (r) => r.kind },
              { key: 'sales', header: '판매액', align: 'right', render: (r) => r.sales },
              { key: 'refund', header: '환불액', align: 'right', render: (r) => r.refund },
              { key: 'fee', header: '수수료', align: 'right', render: (r) => r.fee },
              { key: 'payout', header: '지급액', align: 'right', render: (r) => r.payout },
              { key: 'paidAt', header: '지급일', align: 'right', render: (r) => r.paidAt },
              {
                key: 'status',
                header: '상태',
                render: (r) => <Badge tone={r.tone}>{r.status}</Badge>,
              },
            ]}
          />
          <p className="text-caption text-text-tertiary">
            환불 기한이 지나면 1차 정산(판매액의 40%, 수수료 없음), 대회가 끝나면 최종 정산.
            수수료는 (판매액 − 환불액)의 5.5%(부가세 포함), 최종 정산에서만 떼요.
          </p>
        </ConsoleCard>
      )}

      {isHolds && (
        <ConsoleCard title="지급보류 내역">
          <div className="flex items-start gap-4">
            <Select
              label="조회 기간"
              helper="건별 정산·지급보류는 한 번에 최대 1개월까지"
              items={PERIODS}
              defaultValue="0915"
              className="flex-1"
            />
            <TextField
              label="ID로 찾기"
              helper="지급보류 ID·정산 ID·주문번호로 찾으면 기간과 관계없이 찾아요"
              className="flex-1"
            />
          </div>
          <DataTable
            label="지급보류 내역"
            rows={HOLDS}
            rowKey={(r) => r.id}
            columns={[
              { key: 'id', header: '지급보류 ID', width: 'w-47.5', render: (r) => r.id },
              {
                key: 'settlementId',
                header: '정산 ID',
                width: 'w-47.5',
                render: (r) => r.settlementId,
              },
              {
                key: 'amount',
                header: '금액',
                align: 'right',
                width: 'w-35',
                render: (r) => r.amount,
              },
              { key: 'reason', header: '사유', render: (r) => r.reason },
              {
                key: 'status',
                header: '상태',
                width: 'w-30',
                render: (r) => <Badge tone={r.tone}>{r.status}</Badge>,
              },
              {
                key: 'heldAt',
                header: '보류일',
                align: 'right',
                width: 'w-32.5',
                render: (r) => r.heldAt,
              },
            ]}
          />
          <Alert
            title="보류 사유는 두 가지예요"
            description="정산 계좌 미등록 · 대회 취소 처리. 보류된 금액은 지급 완료로 바뀌지 않고, 풀리면 해제 뒤 돌아오는 20일 묶음에 넣어 지급해요."
          />
        </ConsoleCard>
      )}

      {/* 건별 정산 · 회수 내역은 시안이 없어 빈 화면만 둔다 */}
      {(tab === 'orders' || tab === 'clawbacks') && (
        <ConsoleCard>
          <EmptyState
            title={tab === 'orders' ? '이 기간에 건별 정산이 없어요' : '회수 내역이 없어요'}
            description="조회 기간을 바꿔 다시 찾아보세요."
          />
        </ConsoleCard>
      )}
    </ConsolePage>
  )
}
