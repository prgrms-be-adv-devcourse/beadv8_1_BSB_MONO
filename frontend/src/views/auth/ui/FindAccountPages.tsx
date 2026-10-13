'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'

import { AppHeader } from '@/widgets/app-header'
import { BottomCta, Button, buttonClassName, EmptyState, TextField } from '@/shared/ui'

import {
  findEmailSchema,
  passwordNewSchema,
  passwordResetSchema,
  type FindEmailValues,
  type PasswordNewValues,
  type PasswordResetValues,
} from '../model/schemas'
import { AuthIntro, CenterBody } from './parts'

// 피그마 V2 · M03-1 비밀번호 재설정 · 이메일
export function PasswordResetPage() {
  const router = useRouter()
  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<PasswordResetValues>({
    resolver: zodResolver(passwordResetSchema),
    mode: 'onTouched',
    defaultValues: { email: '' },
  })
  // TODO(api): 재설정 메일 보내기. 가입 여부와 상관없이 같은 안내를 보여 준다.
  const onSubmit = handleSubmit(() => router.push('/password-reset/new'))

  return (
    <>
      <AppHeader type="sub" title="비밀번호 재설정" />
      <CenterBody>
        <AuthIntro title="비밀번호를 잊으셨나요?" />
        <TextField
          label="이메일"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <p className="text-caption text-text-tertiary">
          가입하지 않은 이메일을 적어도 같은 안내가 보여요. 다른 사람이 가입 여부를 알아낼 수 없게
          하려는 거예요.
        </p>
      </CenterBody>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" disabled={!isValid} onClick={onSubmit}>
            재설정 링크 보내기
          </Button>
        }
      />
    </>
  )
}

// 피그마 V2 · M03-2 새 비밀번호 정하기 (메일 링크로 들어온다)
export function PasswordNewPage() {
  const router = useRouter()
  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<PasswordNewValues>({
    resolver: zodResolver(passwordNewSchema),
    mode: 'onTouched',
    defaultValues: { password: '', passwordConfirm: '' },
  })
  // TODO(api): 메일 링크의 토큰과 함께 새 비밀번호를 보낸다.
  const onSubmit = handleSubmit(() => router.push('/login'))

  return (
    <>
      <AppHeader type="sub" title="비밀번호 재설정" />
      <CenterBody>
        <AuthIntro title="새 비밀번호를 정해 주세요" />
        <TextField
          label="새 비밀번호"
          type="password"
          autoComplete="new-password"
          helper="영문 · 숫자 포함 8자 이상"
          error={errors.password?.message}
          {...register('password')}
        />
        <TextField
          label="새 비밀번호 확인"
          type="password"
          autoComplete="new-password"
          error={errors.passwordConfirm?.message}
          {...register('passwordConfirm')}
        />
        <p className="text-caption text-text-tertiary">
          바꾸면 다른 기기에서 로그인된 상태도 모두 풀려요.
        </p>
      </CenterBody>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" disabled={!isValid} onClick={onSubmit}>
            비밀번호 바꾸기
          </Button>
        }
      />
    </>
  )
}

// 피그마 V2 · M03-3 이메일 찾기 · 정보 입력
export function FindEmailPage() {
  const router = useRouter()
  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<FindEmailValues>({
    resolver: zodResolver(findEmailSchema),
    mode: 'onTouched',
    defaultValues: { name: '', birth: '', phone: '' },
  })
  // TODO(api): 이름 · 생년월일 · 휴대폰 번호로 이메일을 찾는다.
  const onSubmit = handleSubmit(() => router.push('/find-email/result'))

  return (
    <>
      <AppHeader type="sub" title="이메일 찾기" />
      <CenterBody>
        <h2 className="text-heading-m">가입할 때 적은 정보를 입력해 주세요</h2>
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
        <p className="text-caption text-text-tertiary">
          세 가지가 모두 맞는 계정이 있으면 이메일 일부를 가려서 보여 드려요.
        </p>
      </CenterBody>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" disabled={!isValid} onClick={onSubmit}>
            이메일 찾기
          </Button>
        }
      />
    </>
  )
}

// 피그마 V2 · M03-4 이메일 찾기 · 결과
export function FindEmailResultPage() {
  return (
    <>
      <AppHeader type="sub" title="이메일 찾기" />
      <main className="flex flex-1 items-center justify-center px-4 py-6">
        <EmptyState
          icon="CircleCheck"
          title="가입한 이메일을 찾았어요"
          description="ru****@crewrun.kr · 2026.09.12 가입"
          action={
            <Link href="/password-reset" className={buttonClassName({ variant: 'secondary' })}>
              비밀번호 재설정
            </Link>
          }
        />
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Link href="/login" className={buttonClassName({ size: 'l' })}>
            로그인하기
          </Link>
        }
      />
    </>
  )
}
