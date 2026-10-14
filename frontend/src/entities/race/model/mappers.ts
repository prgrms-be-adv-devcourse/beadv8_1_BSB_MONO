import type { fileModel, productModel } from '@/shared/api'
import { formatNumber, formatWon } from '@/shared/lib'

import { daysUntil, formatDDay, formatRaceDate, formatRaceDateTime } from '../lib/race-date'
import { MAIL_ORDER_NOTICE, refundRows } from './copy'
import type { RaceBadge, RaceDetail, RaceListItem, RaceOption, RaceSummary } from './types'

type SummaryDto = productModel.RaceSummaryDto
type DetailDto = productModel.RaceDetailDto
type FileDto = fileModel.FileDto

/** 마감까지 이 날 수 이하로 남으면 '마감 임박' */
const CLOSING_SOON_DAYS = 3

function courseNames(courses: { name?: string }[] | undefined): string[] {
  return (courses ?? []).map((c) => c.name ?? '').filter(Boolean)
}

/** 판매 상태 · 남은 자리 · 마감일로 목록 배지를 정한다 */
export function raceBadge(
  saleStatus: string | undefined,
  courses: { status?: string }[] | undefined,
  applicationCloseAt: string | undefined,
): RaceBadge {
  if (saleStatus === 'SCHEDULED') return { label: '접수 예정', tone: 'neutral' }
  if (saleStatus !== 'ON_SALE') return { label: '마감', tone: 'neutral' }
  const list = courses ?? []
  if (list.length > 0 && list.every((c) => c.status === 'SOLD_OUT')) {
    return { label: '정원 마감', tone: 'neutral' }
  }
  if (applicationCloseAt && daysUntil(applicationCloseAt) <= CLOSING_SOON_DAYS) {
    return { label: '마감 임박', tone: 'warning' }
  }
  return { label: '접수중', tone: 'brand' }
}

function remainingSeats(courses: { individualRemaining?: number }[] | undefined): number {
  return (courses ?? []).reduce((sum, c) => sum + (c.individualRemaining ?? 0), 0)
}

export function toRaceListItem(race: SummaryDto): RaceListItem {
  const place = race.location ?? ''
  return {
    id: String(race.raceId),
    title: race.title ?? '',
    posterLabel: courseNames(race.courses).join(' · '),
    badge: raceBadge(race.saleStatus, race.courses, race.applicationCloseAt),
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

/** 지금 접수 중 카드: 대회마다 한 장 */
export function toOpenRaceCard(race: SummaryDto): RaceSummary {
  const names = courseNames(race.courses)
  return {
    id: String(race.raceId),
    title: race.title ?? '',
    posterLabel: names.join(' · '),
    schedule: race.applicationCloseAt
      ? `접수 중 · ~${formatRaceDateTime(race.applicationCloseAt)}`
      : '접수 중',
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
  const status = raceBadge(race.saleStatus, courses, race.applicationCloseAt)
  const refundDeadline = race.refundDeadline ? `${formatRaceDate(race.refundDeadline)} 23:59` : '-'
  const openAt = race.individualOpenAt ?? race.saleOpenAt
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
    startsAt: race.raceDate ? `${formatRaceDateTime(race.raceDate)} 출발` : '',
    place: [race.location, names.join(' / ')].filter(Boolean).join(' · '),
    description: race.description ?? undefined,
    dDay: closeAt ? formatDDay(closeAt) : '-',
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
