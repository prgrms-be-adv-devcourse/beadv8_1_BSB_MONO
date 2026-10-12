'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'

import { AppHeader } from '@/widgets/app-header'
import { cn } from '@/shared/lib'
import {
  BottomCta,
  Button,
  Divider,
  FOCUS_RING,
  KeyValue,
  Radio,
  RadioGroup,
  Section,
  Select,
  TextField,
} from '@/shared/ui'

const SIZES = ['XS (85)', 'S (90)', 'M (95)', 'L (100)', 'XL (105)'].map((v) => ({
  value: v,
  label: v,
}))

// 피그마 V2 · M23 참가자 정보 수정
// TODO(api): 참가자 정보 수정 요청. 접수 마감 뒤에는 막는다.
export function TicketEditPage({ ticketId }: { ticketId: string }) {
  const router = useRouter()
  return (
    <>
      <AppHeader type="sub" title="참가자 정보 수정" />
      <main className="flex flex-1 flex-col gap-2 bg-bg-subtle">
        <Section>
          <div className="flex flex-col gap-1">
            <p className="text-heading-s">2026 용감한 쿠키RUN in 서울 · 10K</p>
            <p className="flex items-baseline gap-2">
              <span className="font-display text-display-m">D-5</span>
              <span className="text-label-m text-text-secondary">접수 마감까지</span>
            </p>
            <p className="text-body-s text-text-secondary">
              10/31 (토) 18:00 접수 마감 전까지 수정할 수 있어요.
            </p>
          </div>
        </Section>

        <Section
          title="참가자 정보"
          aside={
            <Link
              href="/me/edit"
              className={cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)}
            >
              회원 정보에서 바꾸기 ›
            </Link>
          }
        >
          <div className="flex flex-col gap-2">
            <KeyValue label="이름" value="김러너" />
            <KeyValue label="생년월일" value="1995.03.02" />
          </div>
          <Divider />
          <fieldset className="flex flex-col gap-2">
            <legend className="mb-2 text-label-m">성별</legend>
            <RadioGroup defaultValue="male" className="flex-row gap-6">
              <Radio value="male" label="남성" />
              <Radio value="female" label="여성" />
            </RadioGroup>
          </fieldset>
          <Select label="기념품 티셔츠 사이즈" items={SIZES} defaultValue="M (95)" />
        </Section>

        <Section title="비상 연락처">
          <p className="text-body-s text-text-secondary">
            연락받을 분께 먼저 알린 뒤 입력해 주세요. 대회 중 사고가 나면 주최 측이 연락해요.
          </p>
          <TextField label="이름" defaultValue="김가족" />
          <TextField label="연락처" type="tel" defaultValue="010-1234-5678" autoFocus />
        </Section>

        <Section title="키트 배송 주소">
          <p className="text-body-s text-text-secondary">
            환불 기한(10/31)이 지난 뒤 이 주소로 키트를 보내요.
          </p>
          <TextField label="주소" defaultValue="서울 성동구 왕십리로 00, 101동 1001호" />
        </Section>

        <p className="px-4 pt-2 pb-6 text-caption text-text-tertiary">
          참가자 정보는 대회 주최자에게 전달돼요.
        </p>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" onClick={() => router.push(`/tickets/${ticketId}`)}>
            저장하기
          </Button>
        }
      />
    </>
  )
}
