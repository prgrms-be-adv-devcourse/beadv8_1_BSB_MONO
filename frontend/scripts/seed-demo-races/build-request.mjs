// 데모 대회 시드의 순수 함수: 날짜 계산, 대회 등록 요청 본문 만들기, 서버 규칙 검사.
// 네트워크 · 파일을 건드리지 않아 테스트(build-request.test.ts)에서 그대로 부른다.
//
// 서버 규칙 출처 (product-service):
//   - RaceSaveRequestDto · RaceCourseSaveRequestDto · RaceImageRequestDto의 검증 애노테이션
//   - RaceRequestValidator (날짜 순서, 키트 금액, 크루 정원, 대표 이미지 1장, 같은 파일 두 번 금지)
//   - Race.checkSaleOpenDate (판매 시작일은 오늘로부터 이틀 뒤부터. 서버 시계는 TZ=Asia/Seoul)
// 15분 단위 시각은 서버가 아니라 판매자 화면(race-form.ts)의 규칙이지만, 화면에서 넣은 대회와 같게 맞춘다.

import { DISTANCE_KM, TIME_LIMIT_MINUTES } from './races.mjs'

const DAY_MS = 24 * 60 * 60 * 1000
const KST_OFFSET_MS = 9 * 60 * 60 * 1000

/** 판매 시작일은 오늘(서버 날짜)로부터 이 날수 뒤부터 (Race.SALE_OPEN_MIN_DAYS) */
export const SALE_OPEN_MIN_DAYS = 2
/** 크루 최소 인원. 세미는 크루 예매가 없지만 서버가 5 이상을 꼭 받는다 */
export const CREW_MIN_COUNT = 5
export const MIN_AGE = 14
export const TSHIRT_SIZES = ['S', 'M', 'L', 'XL']
export const REFUND_POLICY = '환불 기한까지 취소하면 전액 환불됩니다.'
/** Idempotency-Key 헤더 최대 길이 (IdempotencyKeyHeader.MAX_LENGTH) */
export const IDEMPOTENCY_KEY_MAX_LENGTH = 64

const COURSE_TYPES = ['KM_5', 'KM_10', 'HALF', 'FULL', 'ULTRA']
const LOCAL_DATE_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/

/** 지금 시각의 한국 날짜 ('2026-10-20') */
export function kstDate(now) {
  return new Date(now + KST_OFFSET_MS).toISOString().slice(0, 10)
}

/** 'yyyy-MM-dd'에 days일을 더한 날짜 */
export function addDays(date, days) {
  return new Date(Date.parse(`${date}T00:00:00Z`) + days * DAY_MS).toISOString().slice(0, 10)
}

/** 서버 규칙이 허용하는 가장 이른 개인 예매 오픈(= 접수 시작) 일시: 오늘 + 2일 00:00 */
export function earliestOpenAt(now) {
  return `${addDays(kstDate(now), SALE_OPEN_MIN_DAYS)}T00:00:00`
}

export function idempotencyKeyOf(race) {
  return `seed-demo-${race.slug}`
}

/**
 * 대회 하나의 등록 요청 본문을 만든다.
 * fileIds는 종목 순서대로 대표 이미지 파일 ID다 (한 파일은 한 종목에만 연결되므로 종목마다 따로 올린다).
 * 드라이런처럼 아직 올리지 않았으면 비워 두고, 그 자리는 0으로 채운다.
 * @param {(typeof import('./races.mjs').DEMO_RACES)[number]} race
 * @param {{ now?: number, fileIds?: number[] }} options now는 쓰지 않는다(접수 시작이 races.mjs에 고정)
 */
export function buildRaceRequest(race, { fileIds = [] }) {
  return {
    title: race.title,
    description: race.description,
    location: race.location,
    regionSido: race.regionSido,
    regionSigungu: race.regionSigungu,
    raceDate: race.raceDate,
    // 크루 예매(최종 범위)는 없다
    crewSaleStartAt: null,
    crewSaleEndAt: null,
    individualOpenAt: race.applicationOpenAt,
    applicationCloseAt: race.applicationCloseAt,
    // 환불 기준일은 접수 마감 날짜 (목과 같다)
    refundDeadline: race.applicationCloseAt.slice(0, 10),
    refundPolicy: REFUND_POLICY,
    crewMinCount: CREW_MIN_COUNT,
    crewMaxCount: null,
    courses: race.courses.map((course, i) => ({
      name: course.name,
      courseType: course.courseType,
      surface: 'ROAD',
      distanceKm: DISTANCE_KM[course.courseType],
      timeLimitMinutes: TIME_LIMIT_MINUTES[course.courseType],
      price: course.price,
      kitIncluded: true,
      kitPrice: 0,
      tshirtSizeOption: TSHIRT_SIZES,
      minAge: MIN_AGE,
      maxAge: null,
      crewQuota: 0,
      individualQuota: course.quota,
      images: [{ fileId: fileIds[i] ?? 0, fileType: 'THUMBNAIL', sortNo: 0 }],
    })),
  }
}

function isBlank(value) {
  return typeof value !== 'string' || value.trim() === ''
}

function isQuarterHour(dateTime) {
  return ['00', '15', '30', '45'].includes(dateTime.slice(14, 16)) && dateTime.slice(17) === '00'
}

/** 소수 둘째 자리까지, 정수 세 자리까지 (@Digits(integer = 3, fraction = 2)) */
function hasValidDigits(km) {
  return Number.isFinite(km) && km < 1000 && Math.round(km * 100) / 100 === km
}

/**
 * 서버가 400으로 돌려보낼 값을 미리 찾는다. 문제마다 한 줄씩 돌려주고, 없으면 빈 배열이다.
 * requireFiles가 false면 파일 ID가 아직 없는 드라이런으로 보고 파일 ID 검사를 건너뛴다.
 * @param {Record<string, any>} request
 * @param {{ now: number, requireFiles?: boolean }} options
 * @returns {string[]}
 */
export function validateRaceRequest(request, { now, requireFiles = true }) {
  const errors = []
  const check = (ok, message) => {
    if (!ok) errors.push(message)
  }

  check(!isBlank(request.title) && request.title.length <= 200, '대회 이름: 1~200자')
  check(!isBlank(request.location) && request.location.length <= 255, '장소: 1~255자')
  check(!isBlank(request.regionSido) && request.regionSido.length <= 20, '시·도: 1~20자')
  check(!isBlank(request.regionSigungu) && request.regionSigungu.length <= 30, '시·군·구: 1~30자')
  check(
    Number.isInteger(request.crewMinCount) && request.crewMinCount >= 5,
    '크루 최소 인원: 5 이상',
  )

  const dateTimes = ['raceDate', 'individualOpenAt', 'applicationCloseAt']
  for (const field of dateTimes) {
    check(LOCAL_DATE_TIME.test(request[field] ?? ''), `${field}: yyyy-MM-ddTHH:mm:ss 형식`)
  }
  check(/^\d{4}-\d{2}-\d{2}$/.test(request.refundDeadline ?? ''), 'refundDeadline: yyyy-MM-dd 형식')
  if (errors.length > 0) return errors

  for (const field of dateTimes) {
    check(isQuarterHour(request[field]), `${field}: 15분 단위(00·15·30·45분)`)
  }
  // 시간대 없는 ISO 문자열이라 글자 비교가 곧 시각 비교다
  check(
    request.individualOpenAt.slice(0, 10) >= addDays(kstDate(now), SALE_OPEN_MIN_DAYS),
    `개인 예매 오픈: 오늘로부터 이틀 뒤(${addDays(kstDate(now), SALE_OPEN_MIN_DAYS)})부터`,
  )
  check(request.individualOpenAt < request.applicationCloseAt, '접수 마감: 개인 예매 오픈보다 늦게')
  check(request.applicationCloseAt <= request.raceDate, '접수 마감: 대회 일시보다 늦을 수 없음')
  check(
    request.refundDeadline <= request.raceDate.slice(0, 10),
    '환불 기준일: 대회 날짜보다 늦을 수 없음',
  )

  check(Array.isArray(request.courses) && request.courses.length > 0, '종목: 1개 이상')
  const fileIds = new Set()
  for (const course of request.courses ?? []) {
    const at = `종목 ${course.name}`
    check(!isBlank(course.name) && course.name.length <= 50, `${at}: 종목명 1~50자`)
    check(COURSE_TYPES.includes(course.courseType), `${at}: 종목 구분은 ${COURSE_TYPES.join('·')}`)
    check(['ROAD', 'TRAIL'].includes(course.surface), `${at}: 노면은 ROAD·TRAIL`)
    check(
      course.distanceKm > 0 && hasValidDigits(course.distanceKm),
      `${at}: 거리 0 초과, 소수 둘째 자리까지`,
    )
    check(
      Number.isInteger(course.timeLimitMinutes) && course.timeLimitMinutes > 0,
      `${at}: 제한 시간 0분 초과`,
    )
    check(Number.isInteger(course.price) && course.price > 0, `${at}: 참가비 0원 초과`)
    check(Number.isInteger(course.kitPrice) && course.kitPrice >= 0, `${at}: 키트 금액 0원 이상`)
    check(course.kitPrice <= course.price, `${at}: 키트 금액은 참가비 이하`)
    check(
      Number.isInteger(course.minAge) && course.minAge >= MIN_AGE,
      `${at}: 최소 나이 ${MIN_AGE}세 이상`,
    )
    check(
      course.maxAge == null || course.maxAge >= course.minAge,
      `${at}: 최대 나이는 최소 나이 이상`,
    )
    check(course.crewQuota === 0, `${at}: 크루 예매가 없으면 크루 정원 0`)
    check(
      Number.isInteger(course.individualQuota) && course.individualQuota > 0,
      `${at}: 개인 정원 1명 이상`,
    )
    const sizes = course.tshirtSizeOption ?? []
    check(new Set(sizes).size === sizes.length, `${at}: 티셔츠 사이즈 중복 없음`)
    check(sizes.length === 0 || course.kitIncluded, `${at}: 키트가 없으면 티셔츠 사이즈 없음`)

    const thumbnails = (course.images ?? []).filter((image) => image.fileType === 'THUMBNAIL')
    check(thumbnails.length === 1, `${at}: 대표 이미지 1장`)
    if (requireFiles) {
      for (const image of course.images ?? []) {
        check(Number.isInteger(image.fileId) && image.fileId > 0, `${at}: 파일 ID는 0보다 큼`)
        check(
          !fileIds.has(image.fileId),
          `${at}: 같은 파일(${image.fileId})을 두 번 연결할 수 없음`,
        )
        fileIds.add(image.fileId)
      }
    }
  }
  return errors
}
