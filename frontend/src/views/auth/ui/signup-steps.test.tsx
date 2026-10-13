import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AUTH_ERRORS } from '../model/schemas'
import { SignupStepProvider } from '../model/signup-store'
import { SignupAccountPage, SignupProfilePage } from './SignupPages'

const push = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push, back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/signup',
}))

beforeEach(() => push.mockClear())

// (auth) 레이아웃처럼 Provider는 그대로 두고 안쪽 페이지만 바꿔 단계 이동을 흉내 낸다.
function renderSignup() {
  const view = render(
    <SignupStepProvider>
      <SignupAccountPage />
    </SignupStepProvider>,
  )
  const show = (page: 'account' | 'profile') =>
    view.rerender(
      <SignupStepProvider>
        {page === 'account' ? <SignupAccountPage /> : <SignupProfilePage />}
      </SignupStepProvider>,
    )
  return { show }
}

async function agreeRequiredTerms(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('checkbox', { name: '[필수] 서비스 이용약관' }))
  await user.click(screen.getByRole('checkbox', { name: '[필수] 개인정보 수집 · 이용' }))
  await user.click(screen.getByRole('checkbox', { name: '[필수] 만 14세 이상이에요' }))
  await user.click(screen.getByRole('button', { name: '동의하고 계속하기' }))
}

describe('회원가입 단계', () => {
  it('필수 약관을 체크하지 않으면 다음 단계로 넘어갈 수 없다', async () => {
    const user = userEvent.setup()
    renderSignup()

    const agree = await screen.findByRole('button', { name: '동의하고 계속하기' })
    expect(agree).toBeDisabled()

    await user.click(screen.getByRole('checkbox', { name: '[필수] 서비스 이용약관' }))
    await user.click(screen.getByRole('checkbox', { name: '[선택] 마케팅 정보 받기' }))
    expect(agree).toBeDisabled()
    expect(screen.getByRole('button', { name: '다음', hidden: true })).toBeDisabled()
  })

  it('비밀번호 규칙에 안 맞으면 오류가 뜨고 다음 버튼이 꺼진다', async () => {
    const user = userEvent.setup()
    renderSignup()
    await agreeRequiredTerms(user)

    await user.type(screen.getByLabelText('이메일'), 'runner@crewrun.kr')
    await user.type(screen.getByLabelText('비밀번호'), 'short')
    await user.tab()

    expect(await screen.findByText(AUTH_ERRORS.passwordRule)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다음' })).toBeDisabled()
  })

  it('다음 단계로 갔다가 돌아와도 입력값과 약관 체크가 남아 있다', async () => {
    const user = userEvent.setup()
    const { show } = renderSignup()
    await agreeRequiredTerms(user)

    await user.type(screen.getByLabelText('이메일'), 'runner@crewrun.kr')
    await user.type(screen.getByLabelText('비밀번호'), 'password1')
    await user.type(screen.getByLabelText('비밀번호 확인'), 'password1')
    await user.click(screen.getByRole('button', { name: '다음' }))
    expect(push).toHaveBeenCalledWith('/signup/profile')

    show('profile')
    await user.type(screen.getByLabelText('닉네임'), '달리는곰')

    show('account')
    // 약관에 이미 동의했으므로 바텀시트 없이 계정 단계가 보인다.
    expect(screen.queryByRole('button', { name: '동의하고 계속하기' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('이메일')).toHaveValue('runner@crewrun.kr')
    expect(screen.getByLabelText('비밀번호 확인')).toHaveValue('password1')
    expect(await screen.findByRole('button', { name: '다음' })).toBeEnabled()

    show('profile')
    expect(screen.getByLabelText('닉네임')).toHaveValue('달리는곰')
  })

  it('시안 미리보기(invalid)는 약관 없이 M04-1E 오류를 보여 준다', () => {
    render(
      <SignupStepProvider>
        <SignupAccountPage invalid />
      </SignupStepProvider>,
    )
    expect(screen.getByText(AUTH_ERRORS.emailTaken)).toBeInTheDocument()
    expect(screen.getByText(AUTH_ERRORS.passwordMismatch)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다음' })).toBeDisabled()
  })
})
