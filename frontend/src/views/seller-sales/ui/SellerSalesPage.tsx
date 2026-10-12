'use client'

import Link from 'next/link'

import { ConsoleCard, ConsolePage } from '@/widgets/console-page'
import { cn } from '@/shared/lib'
import {
  Alert,
  Badge,
  Button,
  buttonClassName,
  DataTable,
  FOCUS_RING,
  Select,
  StatTile,
  TextField,
  type BadgeTone,
} from '@/shared/ui'

type Order = {
  id: string
  nickname: string
  course: string
  paidAt: string
  amount: string
  status: string
  tone: BadgeTone
}

// TODO(api): 대회별 주문 · 참가자 API로 바꾼다. 지금은 시안 예시 값.
const ORDERS: Order[] = [
  {
    id: 'R-20261014-0012',
    nickname: '달리는쿠키',
    course: '10K',
    paidAt: '10/14 10:03',
    amount: '₩45,000',
    status: '참가 확정',
    tone: 'brand',
  },
  {
    id: 'R-20261014-0158',
    nickname: '새벽러너',
    course: '5K',
    paidAt: '10/14 10:21',
    amount: '₩35,000',
    status: '참가 확정',
    tone: 'brand',
  },
  {
    id: 'R-20261016-0421',
    nickname: '한강조깅',
    course: '10K',
    paidAt: '10/16 21:40',
    amount: '₩45,000',
    status: '취소 · 환불',
    tone: 'neutral',
  },
  {
    id: 'R-20261019-0907',
    nickname: '주말장거리',
    course: '5K',
    paidAt: '10/19 08:15',
    amount: '₩35,000',
    status: '참가 확정',
    tone: 'brand',
  },
]

const SCOPES = [
  { value: 'confirmed', label: '참가 확정자 전체 (2,410명)' },
  { value: '10k', label: '10K 참가 확정자' },
  { value: '5k', label: '5K 참가 확정자' },
]

// 피그마 V2 · S03 판매 현황 (주문 · 참가자)
export function SellerSalesPage() {
  return (
    <ConsolePage
      title="2026 용감한 쿠키RUN in 서울"
      description="판매 중 · 접수 10/14~10/31 · 대회 11/1"
      actions={
        <>
          <a href="#download" className={buttonClassName({ variant: 'secondary' })}>
            명단 내려받기
          </a>
          <Link href="/seller/races" className={buttonClassName({ variant: 'secondary' })}>
            대회 수정
          </Link>
        </>
      }
    >
      <div className="flex gap-6">
        <StatTile label="전체 판매" value="2,410" unit="/ 3,000명" />
        <StatTile tone="accent" label="남은 자리" value="590" unit="/ 3,000명" />
        <StatTile label="접수 마감" value="D-7" unit="10/31 18:00" />
        <StatTile label="환불 기한" value="D-7" unit="10/31 23:59" />
      </div>

      <Alert
        title="환불 기한(10/31 23:59)까지는 취소가 생길 수 있어요"
        description="취소로 풀린 자리는 남은 자리로 자동으로 돌아가 다시 판매돼요. 환불 기한이 지나면 1차 정산 대상이 돼요."
      />

      <ConsoleCard
        title="참가자 2,410명"
        aside={
          <button
            type="button"
            className={cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)}
          >
            주문번호로 찾기
          </button>
        }
      >
        <DataTable
          label="참가자"
          rows={ORDERS}
          rowKey={(o) => o.id}
          columns={[
            { key: 'id', header: '주문번호', render: (o) => o.id },
            { key: 'nickname', header: '닉네임', render: (o) => o.nickname },
            { key: 'course', header: '종목', render: (o) => o.course },
            { key: 'paidAt', header: '결제일', align: 'right', render: (o) => o.paidAt },
            { key: 'amount', header: '결제 금액', align: 'right', render: (o) => o.amount },
            {
              key: 'status',
              header: '상태',
              render: (o) => <Badge tone={o.tone}>{o.status}</Badge>,
            },
          ]}
        />
        <p className="text-caption text-text-tertiary">
          참가권 결제 한 건이 주문 1건이에요. 구매자는 닉네임으로 보여 줘요. 취소한 주문의 자리는
          남은 자리로 자동 복구돼요.
        </p>
      </ConsoleCard>

      <div id="download">
        <ConsoleCard title="명단 내려받기">
          <div className="flex items-end gap-4">
            <Select label="범위" items={SCOPES} defaultValue="confirmed" className="flex-1" />
            <TextField
              label="내려받는 사유"
              defaultValue="배번 배정 · 보험 가입"
              className="flex-1"
            />
            {/* TODO(api): 사유를 남기고 엑셀 파일을 받는다 */}
            <Button>엑셀로 내려받기</Button>
          </div>
          <p className="text-caption text-text-tertiary">
            내려받기 기록은 1년 보관돼요. 참가자 정보는 대회가 끝나고 1년 뒤 파기해야 해요.
          </p>
        </ConsoleCard>
      </div>
    </ConsolePage>
  )
}
