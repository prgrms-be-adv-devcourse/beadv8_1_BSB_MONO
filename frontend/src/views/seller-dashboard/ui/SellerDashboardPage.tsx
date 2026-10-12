import Link from 'next/link'

import { ConsoleCard, ConsoleLoading, ConsoleOffline, ConsolePage } from '@/widgets/console-page'
import { cn } from '@/shared/lib'
import {
  Badge,
  buttonClassName,
  DataTable,
  FOCUS_RING,
  ProgressBar,
  StatTile,
  type BadgeTone,
} from '@/shared/ui'

// TODO(api): 판매자 대시보드 API로 바꾼다. 지금은 시안 예시 값.
const RACES = [
  {
    title: '2026 용감한 쿠키RUN in 서울',
    period: '접수 10/14~10/31 · 대회 11/1',
    applied: 2410,
    capacity: 3000,
  },
  {
    title: '2026 춘천 나이트런',
    period: '접수 10/5~11/5 · 대회 12/6',
    applied: 1500,
    capacity: 5000,
  },
]

const TODOS = [
  {
    title: '배송 대기 키트',
    description: '환불 기한이 지난 주문 · 송장 번호를 넣으면 배송 중으로 바뀌어요',
    count: '12건',
  },
  { title: '접수 마감 임박', description: '쿠키RUN · 10/31 18:00 접수 마감', count: 'D-7' },
  { title: '지급보류 알림', description: 'HD-20261027-01A2 · 정산 계좌 미등록', count: '1건' },
]

type Order = {
  id: string
  nickname: string
  race: string
  course: string
  status: string
  tone: BadgeTone
  paidAt: string
}

const ORDERS: Order[] = [
  {
    id: 'R-20261019-0907',
    nickname: '주말장거리',
    race: '2026 용감한 쿠키RUN in 서울',
    course: '5K',
    status: '참가 확정',
    tone: 'brand',
    paidAt: '10/19',
  },
  {
    id: 'R-20261019-0911',
    nickname: '새벽런',
    race: '2026 춘천 나이트런',
    course: '10K',
    status: '참가 확정',
    tone: 'brand',
    paidAt: '10/19',
  },
  {
    id: 'R-20261018-0874',
    nickname: '한강조깅',
    race: '2026 춘천 나이트런',
    course: '5K',
    status: '취소 · 환불',
    tone: 'neutral',
    paidAt: '10/18',
  },
  {
    id: 'R-20261018-0850',
    nickname: '달리는쿠키',
    race: '2026 용감한 쿠키RUN in 서울',
    course: '10K',
    status: '참가 확정',
    tone: 'brand',
    paidAt: '10/18',
  },
]

const LINK = cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)

// 피그마 V2 · S05 판매자 홈 (+ 판매자 · 관리자 공통 로딩 · 연결 끊김)
export function SellerDashboardPage({ state }: { state?: 'loading' | 'offline' }) {
  return (
    <ConsolePage
      title="판매자 홈"
      description="오늘 처리할 일과 다음 정산을 한눈에 봐요."
      actions={
        <Link href="/seller/races/new" className={buttonClassName()}>
          대회 등록
        </Link>
      }
    >
      {state === 'loading' && <ConsoleLoading />}
      {state === 'offline' && <ConsoleOffline />}
      {!state && <DashboardBody />}
    </ConsolePage>
  )
}

function DashboardBody() {
  return (
    <>
      <div className="flex gap-4">
        <StatTile label="다음 정산일" value="11/20" unit="(금)" />
        <StatTile
          tone="accent"
          className="flex-2"
          label="정산 예정액"
          value="1,920,000"
          unit="원"
        />
        <StatTile label="판매 중 대회" value="2" unit="개" />
        <StatTile label="처리할 일" value="5" unit="건" />
      </div>

      <div className="flex flex-wrap items-start gap-6">
        <ConsoleCard
          title="진행 중 대회"
          aside={
            <Link href="/seller/races" className={LINK}>
              대회 관리
            </Link>
          }
          className="min-w-130 flex-1"
        >
          {RACES.map((race) => (
            <div key={race.title} className="flex flex-col gap-3 rounded-md bg-bg-subtle p-5">
              <div className="flex items-center gap-2">
                <p className="text-label-l">{race.title}</p>
                <Badge tone="brand">판매 중</Badge>
              </div>
              <p className="text-body-s text-text-secondary">{race.period}</p>
              <ProgressBar
                label="접수율"
                value={Math.round((race.applied / race.capacity) * 100)}
                valueText={`${race.applied.toLocaleString('ko-KR')}/${race.capacity.toLocaleString('ko-KR')}명`}
              />
            </div>
          ))}
        </ConsoleCard>

        <ConsoleCard title="처리할 일" className="w-100 shrink-0 gap-0">
          {TODOS.map((todo) => (
            <div
              key={todo.title}
              className="flex items-center gap-3 border-b border-border-default py-3 last:border-b-0"
            >
              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <p className="text-label-l">{todo.title}</p>
                <p className="text-body-s text-text-secondary">{todo.description}</p>
              </div>
              <Badge tone="warning">{todo.count}</Badge>
            </div>
          ))}
        </ConsoleCard>
      </div>

      <ConsoleCard
        title="최근 주문"
        aside={
          <Link href="/seller/sales" className={LINK}>
            주문 · 참가자에서 보기
          </Link>
        }
      >
        <DataTable
          label="최근 주문"
          rows={ORDERS}
          rowKey={(order) => order.id}
          columns={[
            { key: 'id', header: '주문번호', width: 'w-42.5', render: (o) => o.id },
            { key: 'nickname', header: '닉네임', width: 'w-58.75', render: (o) => o.nickname },
            { key: 'race', header: '대회', render: (o) => o.race },
            { key: 'course', header: '종목', width: 'w-30', render: (o) => o.course },
            {
              key: 'status',
              header: '상태',
              width: 'w-40',
              render: (o) => <Badge tone={o.tone}>{o.status}</Badge>,
            },
            {
              key: 'paidAt',
              header: '결제일',
              align: 'right',
              width: 'w-30',
              render: (o) => o.paidAt,
            },
          ]}
        />
      </ConsoleCard>
    </>
  )
}
