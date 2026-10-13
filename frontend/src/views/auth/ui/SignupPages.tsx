'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'

import { AppHeader } from '@/widgets/app-header'
import { cn } from '@/shared/lib'
import {
  BottomCta,
  BottomSheet,
  Button,
  Checkbox,
  FOCUS_RING,
  Icon,
  Stepper,
  TextField,
} from '@/shared/ui'

import {
  AUTH_ERRORS,
  signupAccountSchema,
  signupProfileSchema,
  type SignupAccountValues,
  type SignupProfileValues,
} from '../model/schemas'
import { useSignupStep } from '../model/signup-store'
import { requiredTermsChecked, SIGNUP_TERMS } from '../model/signup-terms'
import { CenterBody, StepIntro } from './parts'

const STEPS = ['약관', '계정', '회원 정보']

// 피그마 V2 · M04-0 약관 동의(바텀시트) → M04-1 계정(이메일 · 비밀번호) (+ M04-1E 입력 오류)
// invalid는 시안 미리보기(?preview=error)용. 약관을 건너뛰고 M04-1E 오류를 띄운 채로 보여 준다.
// 입력값과 약관 체크는 SignupStepProvider((auth) 레이아웃)에 남아 회원 정보 단계에서 돌아와도 그대로다.
export function SignupAccountPage({ invalid = false }: { invalid?: boolean }) {
  const router = useRouter()
  const terms = useSignupStep((s) => s.terms)
  const termsAgreed = useSignupStep((s) => s.termsAgreed)
  const setTerm = useSignupStep((s) => s.setTerm)
  const setAllTerms = useSignupStep((s) => s.setAllTerms)
  const agreeTerms = useSignupStep((s) => s.agreeTerms)
  const account = useSignupStep((s) => s.account)
  const setAccount = useSignupStep((s) => s.setAccount)

  const [termsOpen, setTermsOpen] = useState(!invalid && !termsAgreed)
  const allChecked = SIGNUP_TERMS.every((t) => terms[t.id])
  const requiredChecked = requiredTermsChecked(terms)
  const agreed = invalid || termsAgreed

  const {
    register,
    handleSubmit,
    subscribe,
    getValues,
    trigger,
    formState: { errors, isValid },
  } = useForm<SignupAccountValues>({
    resolver: zodResolver(signupAccountSchema),
    mode: 'onTouched',
    defaultValues: invalid
      ? { email: 'runner@crewrun.kr', password: 'password1', passwordConfirm: 'password2' }
      : account,
  })
  // 입력할 때마다 스토어에 옮겨 둔다. 뒤로 왔다가 다시 와도 값이 남는다.
  useEffect(() => {
    if (invalid) return
    return subscribe({
      formState: { values: true },
      callback: ({ values }) => setAccount(values),
    })
  }, [invalid, subscribe, setAccount])
  // 비밀번호를 고치면 이미 채운 확인 칸도 다시 검사한다. 비어 있으면 "다르다" 오류를 먼저 띄우지 않는다.
  const revalidateConfirm = () => {
    if (getValues('passwordConfirm')) void trigger('passwordConfirm')
  }
  // TODO(api): ApiV1MemberController로 이메일 중복을 확인한다. 중복이면 AUTH_ERRORS.emailTaken.
  const onNext = handleSubmit(() => router.push('/signup/profile'))
  // 약관 시트를 동의 없이 닫았다면 "다음"이 시트를 다시 연다. 동의해야 다음 단계로 간다.
  const onNextClick = termsAgreed || invalid ? onNext : () => setTermsOpen(true)

  return (
    <>
      <AppHeader type="sub" title="가입하기" />
      {agreed && (
        <div className="bg-bg-surface px-4 pt-3 pb-1">
          <Stepper steps={STEPS} current={1} />
        </div>
      )}
      <CenterBody center={false}>
        <StepIntro
          title="계정을 만들어요"
          description="이메일과 비밀번호를 정해요. 다음은 회원 정보예요."
        />
        <TextField
          label="이메일"
          type="email"
          autoComplete="email"
          placeholder="이메일을 입력해 주세요"
          error={errors.email?.message ?? (invalid ? AUTH_ERRORS.emailTaken : undefined)}
          {...register('email')}
        />
        <TextField
          label="비밀번호"
          type="password"
          autoComplete="new-password"
          placeholder="비밀번호를 입력해 주세요"
          helper="영문 · 숫자 포함 8자 이상"
          error={errors.password?.message}
          {...register('password', { onChange: revalidateConfirm })}
        />
        <TextField
          label="비밀번호 확인"
          type="password"
          autoComplete="new-password"
          placeholder="비밀번호를 한 번 더 입력해 주세요"
          error={
            errors.passwordConfirm?.message ?? (invalid ? AUTH_ERRORS.passwordMismatch : undefined)
          }
          {...register('passwordConfirm')}
        />
      </CenterBody>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" disabled={!isValid || invalid} onClick={onNextClick}>
            다음
          </Button>
        }
      />
      <BottomSheet
        open={termsOpen}
        onOpenChange={setTermsOpen}
        title="약관에 동의해 주세요"
        footer={
          <Button
            size="l"
            disabled={!requiredChecked}
            onClick={() => {
              if (agreeTerms()) setTermsOpen(false)
            }}
          >
            동의하고 계속하기
          </Button>
        }
      >
        <div className="flex flex-col gap-3 px-2 pt-1 pb-3">
          <p className="text-body-m text-text-secondary">
            대회 신청과 거래를 위해 꼭 필요한 약관이에요.
          </p>
          <div className="rounded-[12px] bg-bg-subtle p-4">
            <Checkbox
              checked={allChecked}
              onCheckedChange={setAllTerms}
              label={<span className="font-bold">모두 동의해요 (선택 포함)</span>}
            />
          </div>
          <div className="flex flex-col px-1">
            {SIGNUP_TERMS.map((term) => (
              <div key={term.id} className="flex min-h-11 items-center justify-between py-3">
                <Checkbox
                  checked={terms[term.id]}
                  onCheckedChange={(value) => setTerm(term.id, value)}
                  label={term.label}
                />
                {'href' in term && (
                  <Link
                    href={term.href}
                    aria-label={`${term.label} 보기`}
                    className={cn('rounded-sm text-text-primary', FOCUS_RING)}
                  >
                    <Icon name="ChevronRight" className="size-5" />
                  </Link>
                )}
              </div>
            ))}
          </div>
          <p className="text-caption text-text-tertiary">
            마케팅 정보 동의는 2년마다 다시 여쭤봐요. 마이에서 언제든 끌 수 있어요.
          </p>
        </div>
      </BottomSheet>
    </>
  )
}

// 피그마 V2 · M04-2 가입 · 회원 정보
// 약관 동의와 계정 입력을 마치지 않고 들어오면(주소 직접 입력, 새로고침) 첫 단계로 돌려보낸다.
export function SignupProfilePage() {
  const router = useRouter()
  const termsAgreed = useSignupStep((s) => s.termsAgreed)
  const account = useSignupStep((s) => s.account)
  const ready = termsAgreed && signupAccountSchema.safeParse(account).success
  useEffect(() => {
    if (!ready) router.replace('/signup')
  }, [ready, router])

  return ready ? <SignupProfileForm /> : null
}

function SignupProfileForm() {
  const router = useRouter()
  const profile = useSignupStep((s) => s.profile)
  const setProfile = useSignupStep((s) => s.setProfile)
  const {
    register,
    handleSubmit,
    subscribe,
    formState: { errors, isValid },
  } = useForm<SignupProfileValues>({
    resolver: zodResolver(signupProfileSchema),
    mode: 'onTouched',
    defaultValues: profile,
  })
  useEffect(
    () =>
      subscribe({
        formState: { values: true },
        callback: ({ values }) => setProfile(values),
      }),
    [subscribe, setProfile],
  )
  // TODO(api): ApiV1MemberController 가입에 스토어의 약관 · 계정과 회원 정보를 함께 보낸다.
  const onSubmit = handleSubmit(() => router.push('/'))

  return (
    <>
      <AppHeader type="sub" title="가입하기" />
      <div className="bg-bg-surface px-4 pt-3 pb-1">
        <Stepper steps={STEPS} current={2} />
      </div>
      <CenterBody center={false}>
        <StepIntro
          title="회원 정보를 적어요"
          description="마지막 단계예요. 다음은 가입 완료예요."
        />
        <TextField
          label="이름"
          autoComplete="name"
          error={errors.name?.message}
          {...register('name')}
        />
        <TextField
          label="생년월일"
          inputMode="numeric"
          placeholder="1995.03.02"
          helper="만 14세 미만은 가입할 수 없어요"
          error={errors.birth?.message}
          {...register('birth')}
        />
        <TextField
          label="휴대폰 번호"
          type="tel"
          autoComplete="tel"
          placeholder="010-1234-5678"
          error={errors.phone?.message}
          {...register('phone')}
        />
        <TextField
          label="닉네임"
          helper="다른 사람에게는 실명 대신 닉네임이 보여요. 2~10자"
          error={errors.nickname?.message}
          {...register('nickname')}
        />
        <p className="text-caption text-text-tertiary">
          이름 · 생년월일 · 휴대폰 번호는 대회 신청 때 참가자 정보로 그대로 가져와요.
        </p>
      </CenterBody>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" disabled={!isValid} onClick={onSubmit}>
            가입 완료
          </Button>
        }
      />
    </>
  )
}
