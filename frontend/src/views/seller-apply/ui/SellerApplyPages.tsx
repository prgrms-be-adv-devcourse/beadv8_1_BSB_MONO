'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState, type ReactNode } from 'react'

import { ConsoleCard } from '@/widgets/console-page'
import { cn } from '@/shared/lib'
import {
  Alert,
  Badge,
  Button,
  buttonClassName,
  Checkbox,
  FOCUS_RING,
  Radio,
  RadioGroup,
  Select,
  TextField,
  type BadgeTone,
} from '@/shared/ui'

const BANKS = ['국민은행', '신한은행', '우리은행', '하나은행', '카카오뱅크', '토스뱅크'].map(
  (b) => ({
    value: b,
    label: b,
  }),
)

// 데스크톱 폼 화면의 본문: 위 48 · 아래 64, 가운데 고정 폭 칸
function FormBody({
  width,
  title,
  description,
  children,
}: {
  width: string
  title: string
  description: string
  children: ReactNode
}) {
  return (
    <main className="flex justify-center pt-12 pb-16">
      <div className={cn('flex flex-col gap-6', width)}>
        <div className="flex flex-col gap-2 pb-2">
          <h1 className="text-heading-xl">{title}</h1>
          <p className="text-body-m text-text-secondary">{description}</p>
        </div>
        {children}
      </div>
    </main>
  )
}

// 피그마 V2 · S01 판매자 등록 신청
// TODO(api): 판매자 신청 API에 연결하고 react-hook-form + zod로 검사한다.
export function SellerApplyPage() {
  const router = useRouter()
  const [business, setBusiness] = useState<'business' | 'personal'>('business')
  const [agreed, setAgreed] = useState(true)

  return (
    <FormBody
      width="w-160"
      title="판매자 등록 신청"
      description="승인되면 대회를 등록할 수 있어요. 결과는 알림으로 알려 드려요."
    >
      <ConsoleCard title="판매자 정보">
        <RadioGroup
          value={business}
          onValueChange={(value) => setBusiness(value as typeof business)}
          className="flex-row gap-6"
        >
          <Radio value="business" label="사업자예요" />
          <Radio value="personal" label="사업자가 아니에요" />
        </RadioGroup>
        {business === 'business' ? (
          <>
            <div className="flex gap-4">
              <TextField label="사업자등록번호" defaultValue="123-45-67890" className="flex-1" />
              <TextField label="상호" defaultValue="(주)데브시스터즈러닝" className="flex-1" />
            </div>
            <TextField label="대표자" defaultValue="김주최" />
          </>
        ) : (
          <div className="flex gap-4">
            <TextField label="성명" defaultValue="김주최" className="flex-1" />
            <TextField label="연락처" type="tel" defaultValue="010-1234-5678" className="flex-1" />
          </div>
        )}
        <p className="text-caption text-text-tertiary">사업자가 아니면 성명 · 연락처만 받아요.</p>
      </ConsoleCard>

      <ConsoleCard title="정산 계좌">
        <div className="flex gap-4">
          <Select label="은행" items={BANKS} defaultValue="국민은행" className="flex-1" />
          <TextField label="예금주" defaultValue="(주)데브시스터즈러닝" className="flex-1" />
        </div>
        <TextField label="계좌번호" inputMode="numeric" defaultValue="123456-01-234567" />
        <Alert
          tone="warning"
          title="정산 계좌가 없으면 정산이 지급보류돼요"
          description="나중에 등록해도 되지만, 등록 전까지는 정산금을 받을 수 없어요."
        />
      </ConsoleCard>

      <ConsoleCard title="약관 동의">
        <div className="flex items-center justify-between">
          <Checkbox
            checked={agreed}
            onCheckedChange={setAgreed}
            label="[필수] 판매자 이용약관에 동의해요"
          />
          <Link
            href="/terms/service"
            className={cn('rounded-sm text-body-s text-text-tertiary underline', FOCUS_RING)}
          >
            보기
          </Link>
        </div>
        <p className="text-caption text-text-tertiary">동의한 약관 버전과 시각을 기록해요.</p>
      </ConsoleCard>

      <Alert
        title="크루런은 통신판매중개자예요"
        description="거래 당사자는 판매자님이에요. 판매자 정보는 대회 상세 화면에서 구매자에게 보여요."
      />

      <div className="flex justify-end gap-2">
        <Button variant="secondary" size="l">
          임시 저장
        </Button>
        <Button size="l" disabled={!agreed} onClick={() => router.push('/seller-apply/result')}>
          신청하기
        </Button>
      </div>
    </FormBody>
  )
}

type ResultCard = {
  tone: BadgeTone
  badge: string
  title: string
  description: string
  actions: ReactNode
}

const RESULTS: ResultCard[] = [
  {
    tone: 'neutral',
    badge: '심사 중',
    title: '관리자가 신청 내용을 확인하고 있어요',
    description: '결과가 나오면 알림(M25)으로 알려 드려요. 심사 중에는 신청 내용을 고칠 수 없어요.',
    actions: (
      <Link href="/seller-apply" className={buttonClassName({ variant: 'secondary' })}>
        신청 내용 보기
      </Link>
    ),
  },
  {
    tone: 'danger',
    badge: '반려',
    title: '신청이 반려됐어요',
    description:
      '사유: 사업자등록번호와 상호가 맞지 않아요. 사업자등록증의 상호로 다시 신청해 주세요.',
    actions: (
      <Link href="/seller-apply" className={buttonClassName()}>
        고쳐서 다시 신청하기
      </Link>
    ),
  },
  {
    tone: 'success',
    badge: '승인',
    title: '판매자로 승인됐어요',
    description:
      '이제 대회를 등록할 수 있어요. 정산 계좌가 없으면 정산금 지급이 보류되니 먼저 확인해 주세요.',
    actions: (
      <>
        <Link href="/seller" className={buttonClassName()}>
          판매자 홈으로
        </Link>
        <Link href="/seller/settings" className={buttonClassName({ variant: 'secondary' })}>
          정산 계좌 확인
        </Link>
      </>
    ),
  },
]

// 피그마 V2 · S01-1 판매자 등록 신청 · 결과 (심사 중 · 반려 · 승인 세 가지)
// TODO(api): 신청 상태에 맞는 카드 하나만 보여 준다. 지금은 시안처럼 셋 다 보여 준다.
export function SellerApplyResultPage() {
  return (
    <FormBody
      width="w-180"
      title="판매자 등록 신청 결과"
      description={
        '마이 > 설정(M30)의 "판매자 등록"을 누르면 신청 상태에 따라 아래 셋 중 하나를 보여 줘요.'
      }
    >
      {RESULTS.map((result) => (
        <section
          key={result.badge}
          className="flex flex-col items-start gap-3 rounded-lg bg-bg-surface p-5 inset-ring inset-ring-border-default"
        >
          <Badge tone={result.tone}>{result.badge}</Badge>
          <h2 className="text-heading-s">{result.title}</h2>
          <p className="text-body-s text-text-secondary">{result.description}</p>
          <div className="flex gap-2">{result.actions}</div>
        </section>
      ))}
    </FormBody>
  )
}
