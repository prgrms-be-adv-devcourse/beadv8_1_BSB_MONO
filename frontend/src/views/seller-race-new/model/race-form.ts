import { z } from 'zod'

import { parseRaceDateInput, parseRaceDateTimeInput } from '@/entities/race'
import { productModel, productZod } from '@/shared/api'

export const RACE_FORM_ERRORS = {
  titleRequired: '대회 이름을 입력해 주세요.',
  titleMax: `대회 이름은 ${productZod.registerBodyTitleMax}자 이하로 입력해 주세요.`,
  locationRequired: '장소를 입력해 주세요.',
  locationMax: `장소는 ${productZod.registerBodyLocationMax}자 이하로 입력해 주세요.`,
  sidoRequired: '시·도를 입력해 주세요.',
  sidoMax: `시·도는 ${productZod.registerBodyRegionSidoMax}자 이하로 입력해 주세요.`,
  sigunguRequired: '시·군·구를 입력해 주세요.',
  sigunguMax: `시·군·구는 ${productZod.registerBodyRegionSigunguMax}자 이하로 입력해 주세요.`,
  dateTimeFormat: '2026-11-01 08:00처럼 입력해 주세요.',
  dateFormat: '2026-10-31처럼 입력해 주세요.',
  closeAfterOpen: '접수 마감은 접수 시작보다 늦어야 해요.',
  closeBeforeRace: '접수 마감은 대회 일시보다 늦을 수 없어요.',
  refundBeforeRace: '환불 기한은 대회 날짜보다 늦을 수 없어요.',
  coursesRequired: '종목을 1개 이상 추가해 주세요.',
  coverRequired: '대표 이미지를 올려 주세요.',
} as const

/** 소개 이미지 최대 장수 (파일 서비스 DETAIL 용도의 개수 제한) */
export const MAX_INTRO_IMAGES = 5

/**
 * 크루 최소 인원. 세미는 크루 예매가 없어 쓰이지 않지만 API가 5 이상을 꼭 받는다.
 * 크루 예매(최종)를 열 때 입력 칸으로 바꾼다.
 */
const CREW_MIN_COUNT = productZod.registerBodyCrewMinCountMin

export type RaceCourseInput = Omit<productModel.RaceCourseSaveRequestDto, 'images'>

// 종목 한 줄은 orval이 만든 요청 스키마에서 이미지만 뺀 모양 (이미지는 등록할 때 올려서 붙인다)
const courseSchema = productZod.RegisterBody.shape.courses.element.omit({ images: true })

const dateTime = z
  .string()
  .refine((value) => parseRaceDateTimeInput(value) !== null, RACE_FORM_ERRORS.dateTimeFormat)

export const raceFormSchema = z
  .object({
    title: z
      .string()
      .trim()
      .min(1, RACE_FORM_ERRORS.titleRequired)
      .max(productZod.registerBodyTitleMax, RACE_FORM_ERRORS.titleMax),
    location: z
      .string()
      .trim()
      .min(1, RACE_FORM_ERRORS.locationRequired)
      .max(productZod.registerBodyLocationMax, RACE_FORM_ERRORS.locationMax),
    regionSido: z
      .string()
      .trim()
      .min(1, RACE_FORM_ERRORS.sidoRequired)
      .max(productZod.registerBodyRegionSidoMax, RACE_FORM_ERRORS.sidoMax),
    regionSigungu: z
      .string()
      .trim()
      .min(1, RACE_FORM_ERRORS.sigunguRequired)
      .max(productZod.registerBodyRegionSigunguMax, RACE_FORM_ERRORS.sigunguMax),
    raceDate: dateTime,
    individualOpenAt: dateTime,
    applicationCloseAt: dateTime,
    refundDeadline: z
      .string()
      .refine((value) => parseRaceDateInput(value) !== null, RACE_FORM_ERRORS.dateFormat),
    courses: z.array(courseSchema).min(1, RACE_FORM_ERRORS.coursesRequired),
  })
  .superRefine((values, ctx) => {
    // 형식이 맞는 칸끼리만 순서를 본다. 시간대 없는 ISO 문자열이라 글자 비교가 곧 시각 비교다
    const raceDate = parseRaceDateTimeInput(values.raceDate)
    const openAt = parseRaceDateTimeInput(values.individualOpenAt)
    const closeAt = parseRaceDateTimeInput(values.applicationCloseAt)
    const refund = parseRaceDateInput(values.refundDeadline)
    if (openAt && closeAt && closeAt <= openAt) {
      ctx.addIssue({
        code: 'custom',
        path: ['applicationCloseAt'],
        message: RACE_FORM_ERRORS.closeAfterOpen,
      })
    }
    if (closeAt && raceDate && closeAt > raceDate) {
      ctx.addIssue({
        code: 'custom',
        path: ['applicationCloseAt'],
        message: RACE_FORM_ERRORS.closeBeforeRace,
      })
    }
    if (refund && raceDate && refund > raceDate.slice(0, 10)) {
      ctx.addIssue({
        code: 'custom',
        path: ['refundDeadline'],
        message: RACE_FORM_ERRORS.refundBeforeRace,
      })
    }
  })

export type RaceFormValues = z.input<typeof raceFormSchema>

// TODO(PRO-144): 종목 추가 창이 생기면 빈 목록에서 시작한다. 그 전까지는 시안의 두 종목을 기본으로 둔다.
const DRAFT_COURSES: RaceCourseInput[] = [
  {
    name: '10K',
    courseType: 'KM_10',
    surface: 'ROAD',
    distanceKm: 10,
    timeLimitMinutes: 90,
    price: 45000,
    kitIncluded: true,
    kitPrice: 0,
    tshirtSizeOption: ['S', 'M', 'L', 'XL', 'XXL'],
    minAge: 16,
    maxAge: 80,
    crewQuota: 0,
    individualQuota: 2000,
  },
  {
    name: '5K',
    courseType: 'KM_5',
    surface: 'ROAD',
    distanceKm: 5,
    timeLimitMinutes: 60,
    price: 35000,
    kitIncluded: true,
    kitPrice: 0,
    tshirtSizeOption: ['S', 'M', 'L', 'XL', 'XXL'],
    minAge: 14,
    maxAge: 80,
    crewQuota: 0,
    individualQuota: 1000,
  },
]

export const RACE_FORM_DEFAULTS: RaceFormValues = {
  title: '',
  location: '',
  regionSido: '',
  regionSigungu: '',
  raceDate: '',
  individualOpenAt: '',
  applicationCloseAt: '',
  refundDeadline: '',
  courses: DRAFT_COURSES,
}

/** 검사를 마친 입력과 종목별 이미지로 대회 등록 요청을 만든다. images는 courses와 같은 순서다. */
export function toRaceSaveRequest(
  values: RaceFormValues,
  images: productModel.RaceImageRequestDto[][],
): productModel.RaceSaveRequestDto {
  const dateTime = (text: string) => parseRaceDateTimeInput(text) ?? text
  return {
    title: values.title.trim(),
    location: values.location.trim(),
    regionSido: values.regionSido.trim(),
    regionSigungu: values.regionSigungu.trim(),
    raceDate: dateTime(values.raceDate),
    individualOpenAt: dateTime(values.individualOpenAt),
    applicationCloseAt: dateTime(values.applicationCloseAt),
    refundDeadline: parseRaceDateInput(values.refundDeadline) ?? values.refundDeadline,
    crewMinCount: CREW_MIN_COUNT,
    // 세미는 크루 예매가 없어 크루 예매 기간을 비우고 크루 몫 정원은 0으로 보낸다
    courses: values.courses.map((course, i) => ({
      ...course,
      crewQuota: 0,
      images: images[i] ?? [],
    })),
  }
}
