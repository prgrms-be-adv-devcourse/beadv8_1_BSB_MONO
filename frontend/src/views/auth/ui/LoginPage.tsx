'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { Fragment } from 'react'
import { useForm } from 'react-hook-form'

import { AppHeader } from '@/widgets/app-header'
import { BottomNav } from '@/widgets/bottom-nav'
import { cn } from '@/shared/lib'
import { Button, FOCUS_RING, Logo, TextField } from '@/shared/ui'

import { AUTH_ERRORS, loginSchema, type LoginValues } from '../model/schemas'

const LINK = cn('rounded-sm', FOCUS_RING)

// 피그마 V2 · M03 로그인 (+ M03-E 로그인 실패)
// failed는 시안 미리보기(?preview=error)용. 실패 문구를 띄운 채로 보여 준다.
export function LoginPage({ failed = false }: { failed?: boolean }) {
  const router = useRouter()
  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    mode: 'onTouched',
    defaultValues: failed
      ? { email: 'runner@crewrun.kr', password: 'password1' }
      : { email: '', password: '' },
  })
  return (
    <>
      <AppHeader type="sub" title="로그인" />
      <main className="flex flex-1 flex-col bg-bg-surface">
        <div className="flex-1" />
        <form
          className="flex flex-col items-center px-4"
          noValidate
          onSubmit={handleSubmit(() => {
            // TODO(api): ApiV1AuthController 로그인에 연결한다. 실패하면 AUTH_ERRORS.loginFailed를 띄운다.
            router.push('/')
          })}
        >
          <Logo className="h-11 w-[150px]" />
          <p className="mt-3 text-center text-heading-s">러닝 대회, 여기서 바로 신청해요</p>
          <div className="mt-9 flex w-full flex-col gap-3">
            <TextField
              aria-label="이메일"
              type="email"
              autoComplete="email"
              placeholder="이메일"
              error={errors.email?.message}
              {...register('email')}
            />
            <TextField
              aria-label="비밀번호"
              type="password"
              autoComplete="current-password"
              placeholder="비밀번호"
              error={errors.password?.message ?? (failed ? AUTH_ERRORS.loginFailed : undefined)}
              {...register('password')}
            />
            <Button type="submit" size="l" className="w-full" disabled={!isValid}>
              로그인
            </Button>
            <LinkRow
              className="text-label-s text-text-secondary"
              links={[
                { href: '/signup', label: '회원가입' },
                { href: '/find-email', label: '이메일 찾기' },
                { href: '/password-reset', label: '비밀번호 재설정' },
              ]}
            />
          </div>
        </form>
        <div className="flex-1" />
        <footer className="flex flex-col items-center gap-2 pt-4 pb-6 text-center">
          <LinkRow
            className="text-label-s text-text-secondary"
            links={[
              { href: '/terms/service', label: '이용약관' },
              { href: '/terms/privacy', label: '개인정보처리방침' },
              { href: '/me', label: '고객센터' },
            ]}
          />
          <p className="text-caption text-text-tertiary">
            크루런은 통신판매중개자이며 거래 당사자가 아니에요.
          </p>
        </footer>
      </main>
      <BottomNav />
    </>
  )
}

type RowLink = {
  href: '/signup' | '/find-email' | '/password-reset' | '/terms/service' | '/terms/privacy' | '/me'
  label: string
}

// "회원가입   |   이메일 찾기" 처럼 세로선으로 나눈 링크 줄
function LinkRow({ links, className }: { links: RowLink[]; className?: string }) {
  return (
    <p className={cn('flex justify-center gap-3', className)}>
      {links.map((link, i) => (
        <Fragment key={link.href}>
          {i > 0 && <span aria-hidden>|</span>}
          <Link href={link.href} className={LINK}>
            {link.label}
          </Link>
        </Fragment>
      ))}
    </p>
  )
}
