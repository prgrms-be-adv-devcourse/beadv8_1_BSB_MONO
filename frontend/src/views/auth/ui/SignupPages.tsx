'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

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

import { CenterBody, StepIntro } from './parts'

const STEPS = ['약관', '계정', '회원 정보']

type Term = {
  id: string
  label: string
  required: boolean
  href?: '/terms/service' | '/terms/privacy' | '/terms/marketing'
}

const TERMS: Term[] = [
  { id: 'service', label: '[필수] 서비스 이용약관', required: true, href: '/terms/service' },
  { id: 'privacy', label: '[필수] 개인정보 수집 · 이용', required: true, href: '/terms/privacy' },
  { id: 'age', label: '[필수] 만 14세 이상이에요', required: true },
  { id: 'marketing', label: '[선택] 마케팅 정보 받기', required: false, href: '/terms/marketing' },
]

// 피그마 V2 · M04-0 약관 동의(바텀시트) → M04-1 계정(이메일 · 비밀번호) (+ M04-1E 입력 오류)
// TODO(api): ApiV1MemberController 가입에 연결하고 react-hook-form + zod로 검사한다.
export function SignupAccountPage({ invalid = false }: { invalid?: boolean }) {
  const router = useRouter()
  const [termsOpen, setTermsOpen] = useState(!invalid)
  const [checked, setChecked] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(TERMS.map((t) => [t.id, true])),
  )
  const allChecked = TERMS.every((t) => checked[t.id])
  const requiredChecked = TERMS.every((t) => !t.required || checked[t.id])
  const agreed = !termsOpen && requiredChecked

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
          defaultValue={agreed ? 'runner@crewrun.kr' : undefined}
          error={
            invalid
              ? '이미 가입된 이메일이에요. 로그인하거나 비밀번호를 재설정해 주세요.'
              : undefined
          }
        />
        <TextField
          label="비밀번호"
          type="password"
          autoComplete="new-password"
          placeholder="비밀번호를 입력해 주세요"
          helper="영문 · 숫자 포함 8자 이상"
          defaultValue={agreed ? 'password1' : undefined}
        />
        <TextField
          label="비밀번호 확인"
          type="password"
          autoComplete="new-password"
          placeholder="비밀번호를 한 번 더 입력해 주세요"
          defaultValue={agreed ? 'password2' : undefined}
          error={invalid ? '비밀번호가 서로 달라요.' : undefined}
        />
      </CenterBody>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button
            size="l"
            disabled={!agreed || invalid}
            onClick={() => router.push('/signup/profile')}
          >
            다음
          </Button>
        }
      />
      <BottomSheet
        open={termsOpen}
        onOpenChange={setTermsOpen}
        title="약관에 동의해 주세요"
        footer={
          <Button size="l" disabled={!requiredChecked} onClick={() => setTermsOpen(false)}>
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
              onCheckedChange={(value) =>
                setChecked(Object.fromEntries(TERMS.map((t) => [t.id, value])))
              }
              label={<span className="font-bold">모두 동의해요 (선택 포함)</span>}
            />
          </div>
          <div className="flex flex-col px-1">
            {TERMS.map((term) => (
              <div key={term.id} className="flex min-h-11 items-center justify-between py-3">
                <Checkbox
                  checked={checked[term.id]}
                  onCheckedChange={(value) => setChecked((prev) => ({ ...prev, [term.id]: value }))}
                  label={term.label}
                />
                {term.href && (
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
export function SignupProfilePage() {
  const router = useRouter()
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
        <TextField label="이름" autoComplete="name" defaultValue="김러너" />
        <TextField
          label="생년월일"
          inputMode="numeric"
          helper="만 14세 미만은 가입할 수 없어요"
          defaultValue="1995.03.02"
        />
        <TextField label="휴대폰 번호" type="tel" autoComplete="tel" defaultValue="010-1234-5678" />
        <TextField
          label="닉네임"
          helper="다른 사람에게는 실명 대신 닉네임이 보여요. 2~10자"
          defaultValue="달리는곰"
        />
        <p className="text-caption text-text-tertiary">
          이름 · 생년월일 · 휴대폰 번호는 대회 신청 때 참가자 정보로 그대로 가져와요.
        </p>
      </CenterBody>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" onClick={() => router.push('/')}>
            가입 완료
          </Button>
        }
      />
    </>
  )
}
