'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { SAMPLE_PARTICIPANT, useBookingFlow, type BookingParticipant } from '@/features/booking'
import { RaceTicketCard, type RaceDetail, type RaceOption } from '@/entities/race'
import { cn, formatWon } from '@/shared/lib'
import {
  BottomCta,
  Button,
  Checkbox,
  Dialog,
  Divider,
  FOCUS_RING,
  KeyValue,
  Radio,
  RadioGroup,
  Section,
  Select,
  Stepper,
  TextField,
} from '@/shared/ui'

const SIZES = ['XS (85)', 'S (90)', 'M (95)', 'L (100)', 'XL (105)'].map((v) => ({
  value: v,
  label: v,
}))

const LINK = cn('rounded-sm text-label-m text-text-brand', FOCUS_RING)

// 피그마 V2 · M05 참가 신청 · 참가자 정보 (+ M05-1 장바구니 바꾸기 확인)
// TODO(api): 회원 정보·지난 신청 값으로 채우고, 제출은 react-hook-form + zod로 묶는다.
// 종목은 page.tsx에서 정원이 남은 종목 중에 골라 넘긴다.
export function RaceApplyPage({ race, option }: { race: RaceDetail; option: RaceOption }) {
  const router = useRouter()
  const refundDeadline = race.schedule.find((row) => row.label === '전액 환불 기한')?.value
  const savedParticipant = useBookingFlow((s) => s.participant)
  const submitApplication = useBookingFlow((s) => s.submitApplication)
  // 결제에서 돌아오면 앞서 입력한 값을 그대로 보여 준다.
  const [participant, setParticipant] = useState<BookingParticipant>(
    savedParticipant ?? SAMPLE_PARTICIPANT,
  )
  const edit = (patch: Partial<BookingParticipant>) => setParticipant((p) => ({ ...p, ...patch }))
  // 필수 동의는 사용자가 직접 체크해야 버튼이 켜진다.
  const [agreed, setAgreed] = useState(false)
  const [replaceOpen, setReplaceOpen] = useState(false)

  // 다음 단계(장바구니 · 결제)로 넘어가기 전에 고른 종목과 입력값을 예매 흐름 스토어에 남긴다.
  const submit = () => submitApplication({ raceId: race.id, optionId: option.id }, participant)

  return (
    <>
      <AppHeader type="sub" title="참가 신청" />
      <main className="flex flex-1 flex-col gap-2 bg-bg-surface">
        <div className="bg-bg-surface px-4 py-5">
          <Stepper steps={['종목 선택', '참가자 정보', '결제']} current={1} />
        </div>

        <Section title="선택한 대회">
          <RaceTicketCard
            image={race.images[0]}
            title={race.title}
            meta={`${race.startsAt} · ${race.place}`}
            ticket={`${option.name} · ${formatWon(option.price)}`}
            action={
              <Link href={`/races/${race.id}`} className={LINK}>
                변경
              </Link>
            }
          />
        </Section>

        <Section
          title="참가자 정보"
          aside={
            <Link href="/me/edit" className={LINK}>
              회원 정보에서 바꾸기 ›
            </Link>
          }
        >
          <div className="flex flex-col gap-2">
            <KeyValue label="이름" value={participant.name} />
            <KeyValue label="생년월일" value={participant.birth} />
          </div>
          <Divider />
          <fieldset className="flex flex-col gap-2">
            <legend className="mb-2 text-label-m">성별</legend>
            <RadioGroup
              value={participant.gender}
              onValueChange={(gender) => edit({ gender: gender as BookingParticipant['gender'] })}
              className="flex-row gap-6"
            >
              <Radio value="male" label="남성" />
              <Radio value="female" label="여성" />
            </RadioGroup>
          </fieldset>
          <Select
            label="기념 티셔츠 사이즈"
            items={SIZES}
            value={participant.shirtSize}
            onValueChange={(shirtSize) => shirtSize !== null && edit({ shirtSize })}
          />
          <TextField
            label="비상 연락처 이름"
            value={participant.emergencyName}
            onChange={(e) => edit({ emergencyName: e.target.value })}
          />
          <TextField
            label="비상 연락처 전화번호"
            helper="연락받을 분께 미리 알려 주세요"
            inputMode="tel"
            value={participant.emergencyPhone}
            onChange={(e) => edit({ emergencyPhone: e.target.value })}
          />
          <TextField
            label="키트 배송 주소"
            helper={`환불 기한(${refundDeadline})이 지난 뒤 이 주소로 키트를 보내요.`}
            value={participant.address}
            onChange={(e) => edit({ address: e.target.value })}
          />
        </Section>

        <Section>
          <div className="flex flex-col gap-4 rounded-lg bg-bg-surface p-5 shadow-sm inset-ring inset-ring-border-default">
            <Checkbox
              checked={agreed}
              onCheckedChange={setAgreed}
              label={<span className="font-bold">[필수] 개인정보 제3자 제공 동의</span>}
            />
            <div className="flex flex-col gap-2">
              <KeyValue label="받는 곳" value={`${race.seller.company} (판매자)`} />
              <KeyValue label="목적" value="대회 운영 · 보험 가입" />
              <KeyValue
                label="항목"
                value="이름 · 생년월일 · 성별 · 연락처 2개 · 사이즈 · 배송 주소"
              />
              <KeyValue label="보유 기간" value="대회 종료 후 1년" />
              <p className="text-caption text-text-tertiary">동의하지 않으면 신청할 수 없어요.</p>
            </div>
          </div>
          <p className="text-caption text-text-tertiary">
            입력한 정보는 다음 신청 때 자동으로 채워져요.
          </p>
          <p className="text-caption text-text-tertiary">
            참가권은 다른 사람에게 양도할 수 없어요. 꼭 본인 정보로 신청해 주세요.
          </p>
        </Section>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        secondary={
          // TODO(api): 장바구니가 비어 있으면 바로 담고 M06으로 간다. 지금은 다른 대회가 담겨 있다고 보고 확인 창을 띄운다.
          <Button
            variant="secondary"
            size="l"
            disabled={!agreed}
            onClick={() => {
              submit()
              setReplaceOpen(true)
            }}
          >
            장바구니 담기
          </Button>
        }
        primary={
          <Button
            size="l"
            disabled={!agreed}
            onClick={() => {
              submit()
              router.push('/checkout')
            }}
          >
            바로 결제
          </Button>
        }
      />
      <Dialog
        open={replaceOpen}
        onOpenChange={setReplaceOpen}
        title="장바구니의 대회를 바꿀까요?"
        body={
          <>
            장바구니에는 대회 1개만 담을 수 있어요.
            <br />
            담아 둔 「2026 JTBC 서울마라톤 · 10K」를 빼고 이 대회를 담아요.
          </>
        }
        confirmLabel="바꾸기"
        cancelLabel="그대로 두기"
        onConfirm={() => router.push('/cart')}
      />
    </>
  )
}
