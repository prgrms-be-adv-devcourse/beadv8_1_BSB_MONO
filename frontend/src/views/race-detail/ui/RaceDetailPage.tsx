'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useState } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { type RaceDetail, type RaceOption } from '@/entities/race'
import { cn, formatNumber, formatWon } from '@/shared/lib'
import {
  Alert,
  Badge,
  BottomCta,
  Button,
  buttonClassName,
  FOCUS_RING,
  Icon,
  IconButton,
  KeyValue,
  Section,
  Segmented,
  StatTile,
  useToast,
} from '@/shared/ui'

const ANCHORS = [
  { value: 'options', label: '종목' },
  { value: 'schedule', label: '일정' },
  { value: 'refund', label: '환불' },
  { value: 'info', label: '상세' },
] as const
type Anchor = (typeof ANCHORS)[number]['value']

export type RaceDetailPageProps = {
  race: RaceDetail
  /** 처음 골라 둘 종목. 없거나 맞지 않으면 첫 종목 */
  optionId?: string
  /** M02-1: 이 대회에 이미 신청한 회원 (한 사람은 한 대회에 1건만) */
  alreadyApplied?: boolean
}

// 피그마 V2 · M02 대회 상세 (+ M02-1 이미 신청함, 링크 복사 완료 토스트)
// TODO(api): raceId로 대회를 불러온다. 지금은 시안 예시 값.
export function RaceDetailPage({
  race,
  optionId: initialOptionId,
  alreadyApplied = false,
}: RaceDetailPageProps) {
  const toast = useToast()
  const [optionId, setOptionId] = useState(
    race.options.find((o) => o.id === initialOptionId)?.id ?? race.options[0].id,
  )
  const [anchor, setAnchor] = useState<Anchor>('options')
  const [infoTab, setInfoTab] = useState<'detail' | 'faq'>('detail')
  const minPrice = Math.min(...race.options.map((o) => o.price))

  function copyLink() {
    void navigator.clipboard?.writeText(window.location.href)
    toast({ message: '링크를 복사했어요', tone: 'success' })
  }

  function jumpTo(next: Anchor) {
    setAnchor(next)
    document.getElementById(`race-${next}`)?.scrollIntoView({ behavior: 'smooth' })
  }

  return (
    <>
      <AppHeader
        type="sub"
        title={race.title}
        action={
          <IconButton aria-label="링크 복사" onClick={copyLink}>
            <Icon name="Share" />
          </IconButton>
        }
      />
      <main className="flex flex-1 flex-col gap-2 bg-bg-surface">
        <div className="bg-bg-surface">
          <div className="relative h-84 overflow-hidden">
            <Image
              src={race.images[0]}
              alt=""
              fill
              priority
              sizes="480px"
              className="object-cover"
            />
            <div className="absolute inset-x-0 bottom-0 h-35 bg-linear-to-b from-[rgb(0_0_0/0)] to-[rgb(0_0_0/0.55)]" />
            <div className="absolute bottom-5.5 left-5.5 flex flex-col gap-1.5 text-text-on-hero">
              <p className="text-heading-m">{race.heroTitle}</p>
              <p className="text-label-m whitespace-pre opacity-85">{race.heroTags}</p>
            </div>
            <span className="absolute right-4 bottom-4.5 rounded-full bg-[rgb(0_0_0/0.4)] px-3 py-1 text-label-s text-text-on-hero">
              1 / {race.images.length}
            </span>
          </div>

          <div className="flex flex-col gap-3 px-4 py-5">
            <div className="flex gap-1">
              <Badge tone="brand">접수중</Badge>
            </div>
            <h2 className="text-heading-xl">{race.title}</h2>
            <p className="flex items-center gap-2 text-body-m text-text-secondary">
              <Icon name="Calendar" className="size-4" />
              {race.startsAt}
            </p>
            <p className="flex items-center gap-2 text-body-m text-text-secondary">
              <Icon name="MapPin" className="size-4" />
              {race.place}
            </p>
            <p className="flex items-baseline gap-2">
              <span className="text-heading-l">{formatWon(minPrice)}</span>
              <span className="text-body-s text-text-secondary">부터</span>
            </p>
          </div>

          <div className="flex gap-2 px-4 pb-5">
            <StatTile
              size="s"
              className="inset-ring inset-ring-border-default"
              label="접수 마감"
              value={race.dDay}
            />
            <StatTile
              size="s"
              className="inset-ring inset-ring-border-default"
              label="남은 자리"
              value={formatNumber(race.remaining)}
              unit="자리"
            />
            <StatTile
              size="s"
              className="inset-ring inset-ring-border-default"
              label="정원"
              value={formatNumber(race.capacity)}
              unit="명"
            />
          </div>
        </div>

        {alreadyApplied && (
          <div className="bg-bg-surface px-4 py-5">
            <Alert
              title="이 대회에 이미 신청했어요"
              description="한 사람은 한 대회에 1건만 가질 수 있어요. 내 참가 대회에서 확인해 주세요."
            />
          </div>
        )}

        <div className="sticky top-(--layout-header-height) z-[5] bg-bg-surface">
          <Segmented label="바로 가기" items={[...ANCHORS]} value={anchor} onChange={jumpTo} />
        </div>

        <Section title="종목 선택" className="scroll-mt-32">
          <div id="race-options" className="flex flex-col gap-4">
            {race.options.map((option) => (
              <OptionCard
                key={option.id}
                option={option}
                image={race.images[0]}
                selected={option.id === optionId}
                onSelect={() => setOptionId(option.id)}
              />
            ))}
          </div>
        </Section>

        <Section title="접수 일정">
          <div id="race-schedule" className="flex scroll-mt-32 flex-col gap-4">
            {race.schedule.map((row) => (
              <KeyValue key={row.label} label={row.label} value={row.value} />
            ))}
            <p className="text-caption text-text-tertiary">
              정원이 차면 접수가 일찍 끝날 수 있어요.
            </p>
          </div>
        </Section>

        <Section title="취소 · 환불">
          <div id="race-refund" className="flex scroll-mt-32 flex-col gap-4">
            {race.refund.map((row) => (
              <KeyValue key={row.label} label={row.label} value={row.value} />
            ))}
            <p className="text-caption text-text-tertiary">
              환불은 예치금으로 돌려드려요. 키트는 환불 기한이 지난 뒤 배송해요.
            </p>
            <p className="text-caption text-text-tertiary">
              참가권은 다른 사람에게 양도할 수 없어요.
            </p>
          </div>
        </Section>

        <Section className="pt-0">
          <div id="race-info" className="flex scroll-mt-32 flex-col gap-4">
            <Segmented
              label="상세 정보"
              items={[
                { value: 'detail', label: '상세 정보' },
                { value: 'faq', label: '자주 묻는 질문' },
              ]}
              value={infoTab}
              onChange={setInfoTab}
            />
            {infoTab === 'detail' ? (
              <>
                <div className="relative h-112 overflow-hidden rounded-[12px]">
                  <Image
                    src={race.images[0]}
                    alt="상세 정보 이미지"
                    fill
                    sizes="448px"
                    className="object-cover"
                  />
                </div>
                <p className="text-caption text-text-secondary">
                  판매자가 올린 코스 · 타임테이블 · 기념품 사이즈표 이미지가 이어져요
                </p>
              </>
            ) : (
              <FaqList items={race.faq} />
            )}
          </div>
        </Section>

        <section className="flex flex-col gap-2 bg-bg-surface px-4 py-6">
          <p className="text-label-m">{race.seller.name}</p>
          <p className="text-caption text-text-tertiary">{race.seller.businessNo}</p>
          <p className="text-caption text-text-tertiary">{race.seller.mailOrderNo}</p>
        </section>
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          alreadyApplied ? (
            <Button size="l" disabled>
              이미 신청했어요
            </Button>
          ) : (
            // TODO(auth): 로그인 전이면 로그인(M03)으로 보낸다
            <Link
              href={`/races/${race.id}/apply?option=${optionId}`}
              className={buttonClassName({ size: 'l' })}
            >
              신청하기
            </Link>
          )
        }
      />
    </>
  )
}

function OptionCard({
  option,
  image,
  selected,
  onSelect,
}: {
  option: RaceOption
  image: string
  selected: boolean
  onSelect: () => void
}) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onSelect}
      className={cn(
        'flex h-27 items-center gap-4 rounded-lg p-4 text-left',
        selected
          ? 'bg-bg-brand-subtle inset-ring-2 inset-ring-border-brand'
          : 'bg-bg-surface inset-ring inset-ring-border-default',
        FOCUS_RING,
      )}
    >
      <Image
        src={image}
        alt=""
        width={72}
        height={72}
        className="size-18 rounded-md object-cover"
      />
      <span className="flex min-w-0 flex-1 flex-col gap-1">
        <span className="text-label-strong">{option.name}</span>
        <span className="text-caption text-text-secondary">{option.includes}</span>
      </span>
      <span className={cn('text-label-strong', selected ? 'text-text-brand' : 'text-text-primary')}>
        {formatWon(option.price)}
      </span>
    </button>
  )
}

function FaqList({ items }: { items: RaceDetail['faq'] }) {
  if (items.length === 0) {
    return <p className="py-6 text-center text-body-s text-text-tertiary">등록된 질문이 없어요</p>
  }

  return (
    <ul className="flex flex-col divide-y divide-border-default">
      {items.map((item) => (
        <li key={item.question}>
          <details className="group">
            <summary
              className={cn(
                'flex cursor-pointer list-none items-center gap-3 rounded-md py-4 [&::-webkit-details-marker]:hidden',
                FOCUS_RING,
              )}
            >
              <span className="text-label-strong text-text-brand">Q</span>
              <span className="flex-1 text-label-m">{item.question}</span>
              <Icon
                name="ChevronDown"
                className="size-5 text-text-tertiary transition-transform group-open:rotate-180"
              />
            </summary>
            <p className="pb-4 pl-6 text-body-s text-text-secondary">{item.answer}</p>
          </details>
        </li>
      ))}
    </ul>
  )
}
