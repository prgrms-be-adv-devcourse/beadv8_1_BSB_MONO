import { z } from 'zod'

// 판매자 등록 신청(S01) 입력 규칙. 시안에 적힌 입력 칸·안내문에서 확실한 것만 검사한다.
// 길이 제한은 서버(SellerApplyRequestDto)와 같다: 상호 100자, 대표자·성명 50자.

export const SELLER_APPLY_ERRORS = {
  businessNumberRequired: '사업자등록번호를 입력해 주세요.',
  businessNumberFormat: '사업자등록번호를 123-45-67890처럼 입력해 주세요.',
  businessNameRequired: '상호를 입력해 주세요.',
  businessNameTooLong: '상호는 100자 이하로 입력해 주세요.',
  representativeRequired: '대표자를 입력해 주세요.',
  representativeTooLong: '대표자는 50자 이하로 입력해 주세요.',
  nameRequired: '성명을 입력해 주세요.',
  nameTooLong: '성명은 50자 이하로 입력해 주세요.',
  phoneRequired: '연락처를 입력해 주세요.',
  agreedRequired: '판매자 이용약관에 동의해 주세요.',
} as const

const BUSINESS_NUMBER = /^\d{3}-\d{2}-\d{5}$/

/** 화면 입력값. 정산 계좌는 나중에 등록해도 되므로 받지 않아도 된다. */
export type SellerApplyFormValues = {
  business: 'business' | 'personal'
  businessNumber: string
  businessName: string
  representative: string
  name: string
  phone: string
  bank: string
  accountHolder: string
  accountNumber: string
  agreed: boolean
}

export const EMPTY_SELLER_APPLY_FORM: SellerApplyFormValues = {
  business: 'business',
  businessNumber: '',
  businessName: '',
  representative: '',
  name: '',
  phone: '',
  bank: '',
  accountHolder: '',
  accountNumber: '',
  agreed: false,
}

// 사업자면 사업자등록번호·상호·대표자를, 아니면 성명·연락처를 꼭 받는다.
export const sellerApplyFormSchema = z
  .object({
    business: z.enum(['business', 'personal']),
    businessNumber: z.string().trim(),
    businessName: z.string().trim(),
    representative: z.string().trim(),
    name: z.string().trim(),
    phone: z.string().trim(),
    bank: z.string(),
    accountHolder: z.string().trim(),
    accountNumber: z.string().trim(),
    agreed: z.boolean().refine((v) => v, SELLER_APPLY_ERRORS.agreedRequired),
  })
  .superRefine((values, ctx) => {
    const need = (path: keyof SellerApplyFormValues, message: string) =>
      ctx.addIssue({ code: 'custom', path: [path], message })
    if (values.business === 'business') {
      if (!values.businessNumber) {
        need('businessNumber', SELLER_APPLY_ERRORS.businessNumberRequired)
      } else if (!BUSINESS_NUMBER.test(values.businessNumber)) {
        need('businessNumber', SELLER_APPLY_ERRORS.businessNumberFormat)
      }
      if (!values.businessName) {
        need('businessName', SELLER_APPLY_ERRORS.businessNameRequired)
      } else if (values.businessName.length > 100) {
        need('businessName', SELLER_APPLY_ERRORS.businessNameTooLong)
      }
      if (!values.representative) {
        need('representative', SELLER_APPLY_ERRORS.representativeRequired)
      } else if (values.representative.length > 50) {
        need('representative', SELLER_APPLY_ERRORS.representativeTooLong)
      }
    } else {
      if (!values.name) {
        need('name', SELLER_APPLY_ERRORS.nameRequired)
      } else if (values.name.length > 50) {
        need('name', SELLER_APPLY_ERRORS.nameTooLong)
      }
      if (!values.phone) {
        need('phone', SELLER_APPLY_ERRORS.phoneRequired)
      }
    }
  }) satisfies z.ZodType<SellerApplyFormValues, SellerApplyFormValues>
