import { addDays, format } from 'date-fns'
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
  // 달력 칸에서 날짜를 고르고, 옆 칸에 시간을 친다
  dateRequired: '날짜를 골라 주세요.',
  timeRequired: '시간을 입력해 주세요.',
  timeFormat: '시간을 18:30처럼 24시간(00:00~23:45)으로 입력해 주세요.',
  timeQuarter: '15분 단위(00·15·30·45분)로 입력해 주세요.',
  closeAfterOpen: '접수 마감은 접수 시작보다 늦어야 해요.',
  closeBeforeRace: '접수 마감은 대회 일시보다 늦을 수 없어요.',
  refundBeforeRace: '환불 기한은 대회 날짜보다 늦을 수 없어요.',
  coursesRequired: '종목을 1개 이상 추가해 주세요.',
  coverRequired: '대표 이미지를 올려 주세요.',
} as const

/** 판매 시작일(접수 시작)은 오늘로부터 이 날 수 뒤부터 (서버 Race.SALE_OPEN_MIN_DAYS와 같다) */
export const SALE_OPEN_MIN_DAYS = 2

/** 접수 시작으로 고를 수 있는 가장 이른 날 */
export function earliestSaleOpenDate(today: Date): Date {
  return addDays(today, SALE_OPEN_MIN_DAYS)
}

export function saleOpenTooEarlyMessage(earliest: Date): string {
  return `접수 시작은 오늘로부터 이틀 뒤(${format(earliest, 'M/d')})부터 정할 수 있어요.`
}

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

// 시는 00~23, 분은 00 · 15 · 30 · 45만 받는다
const TIME_FORMAT = /^([01]\d|2[0-3]):([0-5]\d)$/
const QUARTER_MINUTES = new Set(['00', '15', '30', '45'])

/** 시간 칸 하나('HH:mm')의 규칙 */
export const raceTimeSchema = z
  .string()
  .min(1, RACE_FORM_ERRORS.timeRequired)
  .regex(TIME_FORMAT, RACE_FORM_ERRORS.timeFormat)
  .refine((time) => QUARTER_MINUTES.has(time.slice(3)), RACE_FORM_ERRORS.timeQuarter)

/** 날짜 · 시간 칸('yyyy-MM-dd HH:mm'). 날짜가 비면 날짜를, 아니면 시간 규칙을 알린다 */
const dateTime = z.string().superRefine((value, ctx) => {
  const [date = '', time = ''] = value.split(' ')
  if (!date) {
    ctx.addIssue({ code: 'custom', message: RACE_FORM_ERRORS.dateRequired })
    return
  }
  const timeResult = raceTimeSchema.safeParse(time)
  if (!timeResult.success) {
    ctx.addIssue({ code: 'custom', message: timeResult.error.issues[0].message })
    return
  }
  if (parseRaceDateTimeInput(value) === null) {
    ctx.addIssue({ code: 'custom', message: RACE_FORM_ERRORS.dateRequired })
  }
})

/**
 * 대회 등록 폼 규칙. 칸끼리의 순서는 서버(RaceRequestValidator · Race)와 같게 본다.
 * today를 주면 "판매 시작일은 오늘로부터 이틀 뒤부터" 규칙도 본다(화면은 오늘을 넘긴다).
 */
export function createRaceFormSchema({ today }: { today?: Date } = {}) {
  return z
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
      // 서버는 날짜만 받지만 칸 모양이 같아 시간도 같은 규칙으로 본다
      refundDeadline: dateTime,
      courses: z.array(courseSchema).min(1, RACE_FORM_ERRORS.coursesRequired),
    })
    .superRefine((values, ctx) => {
      // 형식이 맞는 칸끼리만 순서를 본다. 시간대 없는 ISO 문자열이라 글자 비교가 곧 시각 비교다
      const raceDate = parseRaceDateTimeInput(values.raceDate)
      const openAt = parseRaceDateTimeInput(values.individualOpenAt)
      const closeAt = parseRaceDateTimeInput(values.applicationCloseAt)
      const refund = parseRaceDateInput(values.refundDeadline.slice(0, 10))
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
      // 세미는 크루 예매가 없어 개인 예매 오픈(접수 시작) 날짜가 판매 시작일이다
      if (today && openAt) {
        const earliest = earliestSaleOpenDate(today)
        if (openAt.slice(0, 10) < format(earliest, 'yyyy-MM-dd')) {
          ctx.addIssue({
            code: 'custom',
            path: ['individualOpenAt'],
            message: saleOpenTooEarlyMessage(earliest),
          })
        }
      }
    })
}

/** 오늘 규칙 없이 칸 형식과 순서만 보는 규칙 (타입과 테스트용) */
export const raceFormSchema = createRaceFormSchema()

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
    // 환불 기한은 날짜만 보낸다 (칸의 시간은 보내지 않는다)
    refundDeadline: parseRaceDateInput(values.refundDeadline.slice(0, 10)) ?? values.refundDeadline,
    crewMinCount: CREW_MIN_COUNT,
    // 세미는 크루 예매가 없어 크루 예매 기간을 비우고 크루 몫 정원은 0으로 보낸다
    courses: values.courses.map((course, i) => ({
      ...course,
      crewQuota: 0,
      images: images[i] ?? [],
    })),
  }
}
