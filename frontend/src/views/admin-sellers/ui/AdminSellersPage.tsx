'use client'

import { useState } from 'react'

import { ConsoleCard, ConsolePage } from '@/widgets/console-page'
import { Alert, Button, DataTable, Dialog, Segmented, Textarea, TextField } from '@/shared/ui'

type Application = {
  id: string
  appliedAt: string
  seller: string
  kind: string
  businessNo: string
  account: string
  owner: string
  email: string
  terms: string
}

// TODO(api): 판매자 신청 목록 API로 바꾼다. 시안 예시 값.
const APPLICATIONS: Application[] = [
  {
    id: 'devsisters',
    appliedAt: '10/4',
    seller: '데브시스터즈러닝',
    kind: '사업자',
    businessNo: '123-45-67890',
    account: '국민 ****4567',
    owner: '김주최',
    email: 'run@devsisters.kr',
    terms: 'v1.0 동의 · 10/4 10:12',
  },
  {
    id: 'chuncheon',
    appliedAt: '10/5',
    seller: '춘천러닝클럽',
    kind: '비사업자',
    businessNo: '—',
    account: '신한 ****5678',
    owner: '이춘천',
    email: 'club@chuncheon.run',
    terms: 'v1.0 동의 · 10/5 09:40',
  },
  {
    id: 'nightrun',
    appliedAt: '10/5',
    seller: '나이트런컴퍼니',
    kind: '사업자',
    businessNo: '220-81-12345',
    account: '미등록',
    owner: '박야간',
    email: 'tax@nightrun.kr',
    terms: 'v1.0 동의 · 10/5 14:32',
  },
]

type Tab = 'pending' | 'approved' | 'rejected'

// 피그마 V2 · A01 판매자 승인 (+ A01-1 반려 사유 입력)
export function AdminSellersPage({ rejectOpen = false }: { rejectOpen?: boolean }) {
  const [tab, setTab] = useState<Tab>('pending')
  const [selectedId, setSelectedId] = useState('nightrun')
  const [dialogOpen, setDialogOpen] = useState(rejectOpen)
  const app = APPLICATIONS.find((a) => a.id === selectedId) ?? APPLICATIONS[0]
  const noAccount = app.account === '미등록'

  return (
    <ConsolePage
      title="판매자 승인"
      description="승인하면 대회를 등록할 수 있어요. 반려할 때는 사유를 꼭 적어요."
    >
      <Segmented
        label="신청 상태"
        layout="hug"
        size="l"
        value={tab}
        onChange={setTab}
        items={[
          { value: 'pending', label: '승인 대기', count: APPLICATIONS.length },
          { value: 'approved', label: '승인' },
          { value: 'rejected', label: '반려' },
        ]}
      />
      <div className="flex flex-wrap items-start gap-6">
        <ConsoleCard className="min-w-130 flex-1">
          <DataTable
            label="판매자 신청"
            rows={APPLICATIONS}
            rowKey={(a) => a.id}
            selected={selectedId}
            onRowClick={(a) => setSelectedId(a.id)}
            columns={[
              { key: 'appliedAt', header: '신청일', align: 'right', render: (a) => a.appliedAt },
              { key: 'seller', header: '판매자', render: (a) => a.seller },
              { key: 'kind', header: '구분', render: (a) => a.kind },
              { key: 'businessNo', header: '사업자등록번호', render: (a) => a.businessNo },
              { key: 'account', header: '정산 계좌', render: (a) => a.account },
            ]}
          />
        </ConsoleCard>

        <ConsoleCard title={app.seller} className="w-100 shrink-0">
          <dl className="flex flex-col gap-3">
            {[
              ['대표자', app.owner],
              ['이메일', app.email],
              ['정산 계좌', app.account],
              ['판매자 이용약관', app.terms],
            ].map(([label, value]) => (
              <div key={label} className="flex items-start justify-between gap-4">
                <dt className="text-body-s text-text-secondary">{label}</dt>
                <dd
                  className={
                    noAccount && label === '정산 계좌'
                      ? 'text-label-m text-text-warning'
                      : 'text-label-m'
                  }
                >
                  {value}
                </dd>
              </div>
            ))}
          </dl>
          {noAccount && (
            <Alert
              tone="warning"
              title="정산 계좌가 없어요"
              description="승인은 할 수 있지만, 계좌를 등록할 때까지 정산이 지급보류돼요."
            />
          )}
          <TextField label="반려 사유" helper="반려하면 이 사유가 신청자에게 보여요" />
          <div className="flex gap-2">
            <Button variant="secondary" className="flex-1" onClick={() => setDialogOpen(true)}>
              반려
            </Button>
            {/* TODO(api): 판매자 승인 */}
            <Button className="flex-1">승인</Button>
          </div>
        </ConsoleCard>
      </div>

      <Dialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        type="danger"
        title="신청을 반려할까요?"
        body={
          <div className="flex flex-col gap-4">
            <p>
              반려하면 신청자에게 알림이 가고, 사유가 신청자 화면(S01-1)에 그대로 보여요. 신청자는
              고쳐서 다시 신청할 수 있어요.
            </p>
            <Textarea
              label="반려 사유 (필수)"
              helper="신청자 화면에 그대로 보여요"
              showCounter
              maxLength={200}
              defaultValue="사업자등록번호와 상호가 맞지 않아요. 사업자등록증의 상호로 다시 신청해 주세요."
            />
          </div>
        }
        confirmLabel="반려하기"
        cancelLabel="취소"
        onConfirm={() => setDialogOpen(false)}
      />
    </ConsolePage>
  )
}
