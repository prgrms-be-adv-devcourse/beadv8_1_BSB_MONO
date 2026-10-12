'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'

import { AppHeader } from '@/widgets/app-header'
import { BottomCta, Button, buttonClassName, EmptyState, TextField } from '@/shared/ui'

import { AuthIntro, CenterBody } from './parts'

// 피그마 V2 · M03-1 비밀번호 재설정 · 이메일
// TODO(api): 재설정 메일 보내기. 가입 여부와 상관없이 같은 안내를 보여 준다.
export function PasswordResetPage() {
  const router = useRouter()
  return (
    <>
      <AppHeader type="sub" title="비밀번호 재설정" />
      <CenterBody>
        <AuthIntro title="비밀번호를 잊으셨나요?" />
        <TextField
          label="이메일"
          type="email"
          autoComplete="email"
          defaultValue="runner@crewrun.kr"
        />
        <p className="text-caption text-text-tertiary">
          가입하지 않은 이메일을 적어도 같은 안내가 보여요. 다른 사람이 가입 여부를 알아낼 수 없게
          하려는 거예요.
        </p>
      </CenterBody>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" onClick={() => router.push('/password-reset/new')}>
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
          defaultValue="password1"
        />
        <TextField
          label="새 비밀번호 확인"
          type="password"
          autoComplete="new-password"
          defaultValue="password1"
        />
        <p className="text-caption text-text-tertiary">
          바꾸면 다른 기기에서 로그인된 상태도 모두 풀려요.
        </p>
      </CenterBody>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" onClick={() => router.push('/login')}>
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
  return (
    <>
      <AppHeader type="sub" title="이메일 찾기" />
      <CenterBody>
        <h2 className="text-heading-m">가입할 때 적은 정보를 입력해 주세요</h2>
        <TextField label="이름" autoComplete="name" defaultValue="김러너" />
        <TextField label="생년월일" inputMode="numeric" defaultValue="1995.03.02" />
        <TextField label="휴대폰 번호" type="tel" autoComplete="tel" defaultValue="010-1234-5678" />
        <p className="text-caption text-text-tertiary">
          세 가지가 모두 맞는 계정이 있으면 이메일 일부를 가려서 보여 드려요.
        </p>
      </CenterBody>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" onClick={() => router.push('/find-email/result')}>
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
