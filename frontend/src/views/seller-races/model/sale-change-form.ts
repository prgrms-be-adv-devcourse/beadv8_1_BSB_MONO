import { z } from 'zod'

import { isSaleStarted, parseRaceDateInput, type RaceForEdit } from '@/entities/race'
import type { productModel } from '@/shared/api'

export const SALE_CHANGE_ERRORS = {
  number: '숫자로 입력해 주세요.',
  quotaDecreased: '판매가 시작된 종목은 정원을 줄일 수 없어요.',
  priceChanged: '판매가 시작된 종목은 참가비를 바꿀 수 없어요.',
  dateFormat: '2026-10-31처럼 입력해 주세요.',
  refundAdvanced: '판매가 시작된 대회는 환불 기한을 앞당길 수 없어요.',
} as const

export type SaleChangeValues = {
  courses: { individualQuota: string; price: string }[]
  refundDeadline: string
}

const wholeNumber = z.string().trim().regex(/^\d+$/, SALE_CHANGE_ERRORS.number)

/** 지금 값으로 입력 칸을 채운다. */
export function toSaleChangeValues({ request }: RaceForEdit): SaleChangeValues {
  return {
    courses: request.courses.map((c) => ({
      individualQuota: String(c.individualQuota),
      price: String(c.price),
    })),
    refundDeadline: request.refundDeadline,
  }
}

/**
 * 판매 중 변경 검사. 판매가 시작된 대회는 백엔드 규칙(PRO-141)대로
 * 정원은 늘리기만, 참가비는 그대로, 환불 기한은 늦추기만 된다. 서버도 같은 규칙으로 409를 준다.
 */
export function saleChangeSchema({ race, request }: RaceForEdit) {
  const started = isSaleStarted(race.saleStatus)
  return z
    .object({
      courses: z.array(z.object({ individualQuota: wholeNumber, price: wholeNumber })),
      refundDeadline: z
        .string()
        .refine((value) => parseRaceDateInput(value) !== null, SALE_CHANGE_ERRORS.dateFormat),
    })
    .superRefine((values, ctx) => {
      if (!started) return
      values.courses.forEach((course, i) => {
        const before = request.courses[i]
        if (!before) return
        if (
          /^\d+$/.test(course.individualQuota) &&
          Number(course.individualQuota) < before.individualQuota
        ) {
          ctx.addIssue({
            code: 'custom',
            path: ['courses', i, 'individualQuota'],
            message: SALE_CHANGE_ERRORS.quotaDecreased,
          })
        }
        if (/^\d+$/.test(course.price) && Number(course.price) !== before.price) {
          ctx.addIssue({
            code: 'custom',
            path: ['courses', i, 'price'],
            message: SALE_CHANGE_ERRORS.priceChanged,
          })
        }
      })
      const refund = parseRaceDateInput(values.refundDeadline)
      if (refund && refund < request.refundDeadline) {
        ctx.addIssue({
          code: 'custom',
          path: ['refundDeadline'],
          message: SALE_CHANGE_ERRORS.refundAdvanced,
        })
      }
    })
}

/** 지금 상태의 수정 요청에 바꾼 값만 덮어쓴다. */
export function toUpdateRequest(
  { request }: RaceForEdit,
  values: SaleChangeValues,
): productModel.RaceSaveRequestDto {
  return {
    ...request,
    refundDeadline: parseRaceDateInput(values.refundDeadline) ?? request.refundDeadline,
    courses: request.courses.map((course, i) => ({
      ...course,
      individualQuota: Number(values.courses[i]?.individualQuota ?? course.individualQuota),
      price: Number(values.courses[i]?.price ?? course.price),
    })),
  }
}
