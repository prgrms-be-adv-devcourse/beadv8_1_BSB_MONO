// 가입 약관 동의(M04-0) 항목. 필수를 모두 체크해야 다음 단계로 넘어간다.
export const SIGNUP_TERMS = [
  { id: 'service', label: '[필수] 서비스 이용약관', required: true, href: '/terms/service' },
  { id: 'privacy', label: '[필수] 개인정보 수집 · 이용', required: true, href: '/terms/privacy' },
  { id: 'age', label: '[필수] 만 14세 이상이에요', required: true },
  { id: 'marketing', label: '[선택] 마케팅 정보 받기', required: false, href: '/terms/marketing' },
] as const

export type SignupTermId = (typeof SIGNUP_TERMS)[number]['id']
export type SignupTermChecks = Record<SignupTermId, boolean>

export function termChecks(value: boolean): SignupTermChecks {
  return Object.fromEntries(SIGNUP_TERMS.map((t) => [t.id, value])) as SignupTermChecks
}

export function requiredTermsChecked(checks: SignupTermChecks): boolean {
  return SIGNUP_TERMS.every((t) => !t.required || checks[t.id])
}
