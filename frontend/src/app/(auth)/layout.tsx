import { SignupStepProvider } from '@/views/auth'

import { MobileColumn } from '../_shell/MobileColumn'

// 회원가입 단계 스토어는 signup이 아니라 (auth) 레이아웃에 둔다.
// 약관 시트에서 /terms/* 를 보고 돌아와도 약관 체크와 입력값이 남게 하려는 것이다.
// 레이아웃은 하위 페이지를 오갈 때 다시 그려지지 않으므로 스토어가 유지되고, (auth)를 벗어나면 사라진다.
export default function AuthLayout({ children }: LayoutProps<'/'>) {
  return (
    <MobileColumn>
      <SignupStepProvider>{children}</SignupStepProvider>
    </MobileColumn>
  )
}
