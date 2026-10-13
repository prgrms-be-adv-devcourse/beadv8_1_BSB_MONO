import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AUTH_ERRORS } from '../model/schemas'
import { PasswordResetPage } from './FindAccountPages'
import { LoginPage } from './LoginPage'
import { SignupProfilePage } from './SignupPages'

const push = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push, back: vi.fn(), replace: vi.fn() }),
  usePathname: () => '/',
}))

beforeEach(() => push.mockClear())

describe('LoginPage', () => {
  it('이메일 형식이 틀리면 필드 아래에 오류가 뜨고 버튼이 꺼진다', async () => {
    const user = userEvent.setup()
    render(<LoginPage />)

    await user.type(screen.getByLabelText('이메일'), 'runner')
    await user.type(screen.getByLabelText('비밀번호'), 'password1')

    expect(await screen.findByText(AUTH_ERRORS.emailFormat)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그인' })).toBeDisabled()
  })

  it('맞게 입력하면 버튼이 켜지고 홈으로 간다', async () => {
    const user = userEvent.setup()
    render(<LoginPage />)

    await user.type(screen.getByLabelText('이메일'), 'runner@crewrun.kr')
    await user.type(screen.getByLabelText('비밀번호'), 'password1')
    const button = screen.getByRole('button', { name: '로그인' })
    expect(button).toBeEnabled()

    await user.click(button)
    expect(push).toHaveBeenCalledWith('/')
  })

  it('시안 미리보기(failed)는 로그인 실패 문구를 보여 준다', () => {
    render(<LoginPage failed />)
    expect(screen.getByText(AUTH_ERRORS.loginFailed)).toBeInTheDocument()
  })
})

describe('SignupProfilePage', () => {
  it('닉네임이 짧으면 오류가 뜨고 가입 완료 버튼이 꺼진다', async () => {
    const user = userEvent.setup()
    render(<SignupProfilePage />)

    await user.type(screen.getByLabelText('이름'), '김러너')
    await user.type(screen.getByLabelText('생년월일'), '1995.03.02')
    await user.type(screen.getByLabelText('휴대폰 번호'), '010-1234-5678')
    await user.type(screen.getByLabelText('닉네임'), '곰')
    await user.tab()

    expect(await screen.findByText(AUTH_ERRORS.nicknameLength)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '가입 완료' })).toBeDisabled()
  })
})

describe('PasswordResetPage', () => {
  it('이메일이 맞으면 다음 화면으로 간다', async () => {
    const user = userEvent.setup()
    render(<PasswordResetPage />)

    const button = screen.getByRole('button', { name: '재설정 링크 보내기' })
    expect(button).toBeDisabled()

    await user.type(screen.getByLabelText('이메일'), 'runner@crewrun.kr')
    await user.click(button)
    expect(push).toHaveBeenCalledWith('/password-reset/new')
  })
})
