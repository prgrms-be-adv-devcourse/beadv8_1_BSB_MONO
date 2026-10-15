import type { fileModel, productModel } from '@/shared/api'
import { formatNumber, formatWon } from '@/shared/lib'

import {
  daysUntil,
  formatDDay,
  formatRaceDate,
  formatRaceDateTime,
  parseKstDateTime,
} from '../lib/race-date'
import { MAIL_ORDER_NOTICE, refundRows } from './copy'
import type {
  ApplicationPhase,
  RaceBadge,
  RaceDetail,
  RaceListItem,
  RaceOption,
  RaceSummary,
} from './types'

type SummaryDto = productModel.RaceSummaryDto
type DetailDto = productModel.RaceDetailDto
type FileDto = fileModel.FileDto

/** 마감까지 이 날 수 이하로 남으면 '마감 임박' */
const CLOSING_SOON_DAYS = 3

function courseNames(courses: { name?: string }[] | undefined): string[] {
  return (courses ?? []).map((c) => c.name ?? '').filter(Boolean)
}

/** 판매자가 멈췄거나 끝난 판매 상태. 시각과 상관없이 신청할 수 없다 */
const SALE_ENDED = new Set<string>(['STOPPED', 'CLOSED', 'CANCELED'])

type SaleWindow = {
  saleStatus?: string
  individualOpenAt?: string | null
  applicationCloseAt?: string | null
}

/**
 * 지금 신청할 수 있는지를 개인 예매 오픈 · 접수 마감 일시로 정한다.
 * 판매 상태(saleStatus)는 판매자 화면용이라(판매 시작일 0시에 판매 중) 오픈 시각 전에도 ON_SALE일 수 있다.
 * 세미 범위는 개인 예매만이라 크루 예매 기간은 보지 않는다(크루 예매 중이어도 개인에게는 오픈 예정).
 */
export function applicationPhase(race: SaleWindow, now: Date = new Date()): ApplicationPhase {
  if (race.saleStatus && SALE_ENDED.has(race.saleStatus)) return 'closed'
  if (race.applicationCloseAt && now >= parseKstDateTime(race.applicationCloseAt)) return 'closed'
  if (!race.individualOpenAt || now < parseKstDateTime(race.individualOpenAt)) return 'upcoming'
  return 'open'
}

/** 신청 기간 · 남은 자리 · 마감일로 목록 배지를 정한다 */
export function raceBadge(
  race: SaleWindow & { courses?: { status?: string }[] },
  now: Date = new Date(),
): RaceBadge {
  const phase = applicationPhase(race, now)
  if (phase === 'upcoming') return { label: '오픈 예정', tone: 'neutral' }
  if (phase === 'closed') return { label: '마감', tone: 'neutral' }
  const list = race.courses ?? []
  if (list.length > 0 && list.every((c) => c.status === 'SOLD_OUT')) {
    return { label: '정원 마감', tone: 'neutral' }
  }
  if (race.applicationCloseAt && daysUntil(race.applicationCloseAt, now) <= CLOSING_SOON_DAYS) {
    return { label: '마감 임박', tone: 'warning' }
  }
  return { label: '접수 중', tone: 'brand' }
}

function remainingSeats(courses: { individualRemaining?: number }[] | undefined): number {
  return (courses ?? []).reduce((sum, c) => sum + (c.individualRemaining ?? 0), 0)
}

export function toRaceListItem(race: SummaryDto, now: Date = new Date()): RaceListItem {
  const place = race.location ?? ''
  return {
    id: String(race.raceId),
    title: race.title ?? '',
    posterLabel: courseNames(race.courses).join(' · '),
    badge: raceBadge(race, now),
    meta: race.raceDate ? `${formatRaceDate(race.raceDate)} · ${place}` : place,
    price: `${formatWon(race.minPrice ?? 0)}~`,
    remaining: `${formatNumber(remainingSeats(race.courses))}자리 남음`,
  }
}

/** 곧 마감 카드: 종목마다 한 장 (종목을 골라 둔 채 상세로 간다) */
export function toClosingSoonCards(race: SummaryDto): RaceSummary[] {
  const dDay = race.applicationCloseAt ? formatDDay(race.applicationCloseAt) : ''
  return (race.courses ?? []).map((course) => ({
    id: String(race.raceId),
    option: String(course.raceCourseId),
    title: `${race.title ?? ''} (${course.name ?? ''})`,
    posterLabel: course.name ?? '',
    schedule: `${dDay} · 남은 ${formatNumber(course.individualRemaining ?? 0)}자리`,
    place: `${course.name ?? ''} · ${race.location ?? ''}`,
  }))
}

/** 카드 첫 줄: 오픈 예정이면 오픈 일시, 접수 중이면 마감 일시 */
function phaseSchedule(race: SummaryDto, now: Date): string {
  const phase = applicationPhase(race, now)
  if (phase === 'upcoming') {
    return race.individualOpenAt
      ? `오픈 예정 · ${formatRaceDateTime(race.individualOpenAt)}`
      : '오픈 예정'
  }
  if (phase === 'closed') return '마감'
  return race.applicationCloseAt
    ? `접수 중 · ~${formatRaceDateTime(race.applicationCloseAt)}`
    : '접수 중'
}

/** 지금 접수 중 카드: 대회마다 한 장 */
export function toOpenRaceCard(race: SummaryDto, now: Date = new Date()): RaceSummary {
  const names = courseNames(race.courses)
  return {
    id: String(race.raceId),
    title: race.title ?? '',
    posterLabel: names.join(' · '),
    schedule: phaseSchedule(race, now),
    place: [names.join(' / '), race.location].filter(Boolean).join(' · '),
  }
}

/** 종목별 파일 목록에서 대표 이미지 URL (없으면 첫 이미지) */
function courseImage(files: FileDto[] | undefined): string | undefined {
  const images = (files ?? []).filter((f) => f.url)
  return (images.find((f) => f.fileType === 'THUMBNAIL') ?? images[0])?.url ?? undefined
}

function includesText(course: productModel.RaceDetailCourseDto): string {
  const items = ['1인']
  if ((course.tshirtSizeOption ?? []).length > 0) items.push('기념 티셔츠')
  if (course.kitIncluded) items.push('기념품 키트')
  return items.join(' · ')
}

/**
 * 대회 상세 응답 + 종목별 이미지(file 서비스, 종목 ID → 파일 목록)를 상세 화면 모양으로 바꾼다.
 * API에 없는 자주 묻는 질문은 비워 둔다.
 */
export function toRaceDetail(
  race: DetailDto,
  courseFiles: Record<number, FileDto[]> = {},
  now: Date = new Date(),
): RaceDetail {
  const courses = race.courses ?? []
  const names = courseNames(courses)
  const options: RaceOption[] = courses.map((course) => ({
    id: String(course.raceCourseId),
    name: course.name ?? '',
    includes: includesText(course),
    price: course.price ?? 0,
    image: courseImage(courseFiles[course.raceCourseId ?? -1]),
    soldOut: course.status === 'SOLD_OUT',
    kitIncluded: course.kitIncluded ?? false,
    shirtSizes: course.tshirtSizeOption ?? [],
  }))
  // 대표 이미지(종목 순) → 나머지 이미지(소개·코스·키트) 순으로, 같은 주소는 한 번만
  const allFiles = courses.flatMap((c) => courseFiles[c.raceCourseId ?? -1] ?? [])
  const images = [
    ...options.map((o) => o.image),
    ...allFiles.filter((f) => f.fileType !== 'THUMBNAIL').map((f) => f.url),
  ].filter((url, i, list): url is string => !!url && list.indexOf(url) === i)

  const capacity = courses.reduce(
    (sum, c) => sum + (c.individualQuota ?? 0) + (c.crewQuota ?? 0),
    0,
  )
  const remaining = courses.reduce(
    (sum, c) => sum + (c.individualRemaining ?? 0) + (c.crewRemaining ?? 0),
    0,
  )
  const status = raceBadge(race, now)
  const refundDeadline = race.refundDeadline ? `${formatRaceDate(race.refundDeadline)} 23:59` : '-'
  // 판매 시작일(saleOpenDate)은 판매자 화면용 날짜라, 접수 기간은 개인 예매 오픈 일시부터 보여 준다
  const openAt = race.individualOpenAt
  const closeAt = race.applicationCloseAt

  const seller = race.seller
  const company = seller?.businessName ?? seller?.representativeName ?? ''

  return {
    id: String(race.raceId),
    title: race.title ?? '',
    images,
    heroTitle: `「${names.join(' · ')} ${status.label}」`,
    heroTags: names.map((n) => `#${n}`).join('  '),
    status,
    phase: applicationPhase(race, now),
    startsAt: race.raceDate ? `${formatRaceDateTime(race.raceDate)} 출발` : '',
    place: [race.location, names.join(' / ')].filter(Boolean).join(' · '),
    description: race.description ?? undefined,
    dDay: closeAt ? formatDDay(closeAt, now) : '-',
    remaining,
    capacity,
    applied: capacity - remaining,
    options,
    schedule: [
      {
        label: '접수 기간',
        value:
          openAt && closeAt
            ? `${formatRaceDateTime(openAt)} ~ ${formatRaceDateTime(closeAt)}`
            : '-',
      },
      { label: '전액 환불 기한', value: refundDeadline },
    ],
    refund: refundRows(refundDeadline),
    faq: [],
    seller: {
      company,
      name: `판매자 ${company}${seller?.representativeName ? ` · 대표 ${seller.representativeName}` : ''}`,
      businessNo: seller?.businessNumber ? `사업자 ${seller.businessNumber}` : '비사업자 판매자',
      mailOrderNo: MAIL_ORDER_NOTICE,
    },
  }
}
