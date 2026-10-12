'use client'

import { useRouter } from 'next/navigation'

import { AppHeader } from '@/widgets/app-header'
import { BottomCta, Button, TextField } from '@/shared/ui'

// 피그마 V2 · M30-1 회원 정보 수정
// TODO(api): 회원 정보 수정 요청
export function ProfileEditPage() {
  const router = useRouter()
  return (
    <>
      <AppHeader type="sub" title="정보 수정" />
      <main className="flex flex-1 flex-col gap-5 px-4 py-6">
        <div className="flex flex-col gap-1">
          <h2 className="text-heading-m">회원 정보를 고쳐요</h2>
          <p className="text-body-s text-text-tertiary">바꾼 정보는 다음 대회 신청부터 쓰여요.</p>
        </div>
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
      </main>
      <BottomCta
        className="sticky bottom-0 z-10"
        primary={
          <Button size="l" onClick={() => router.push('/me')}>
            저장하기
          </Button>
        }
      />
    </>
  )
}
