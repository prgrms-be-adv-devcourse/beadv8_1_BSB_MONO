import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getAuthStatus } from '@/shared/api'
import { server } from '@/shared/testing'

import { AUTH_ERRORS } from '../model/schemas'
import { SignupStepProvider, type SignupStepInitial } from '../model/signup-store'
import { termChecks } from '../model/signup-terms'
import { SignupAccountPage, SignupProfilePage } from './SignupPages'

const push = vi.fn()
const replace = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push, back: vi.fn(), replace }),
  usePathname: () => '/signup',
}))

beforeEach(() => {
  push.mockClear()
  replace.mockClear()
})

// (auth) 레이아웃처럼 Provider는 그대로 두고 안쪽 페이지만 바꿔 단계 이동을 흉내 낸다.
function renderSignup() {
  const client = new QueryClient()
  const wrap = (page: ReactNode) => (
    <QueryClientProvider client={client}>
      <SignupStepProvider>{page}</SignupStepProvider>
    </QueryClientProvider>
  )
  const view = render(wrap(<SignupAccountPage />))
  const show = (page: 'account' | 'profile') =>
    view.rerender(wrap(page === 'account' ? <SignupAccountPage /> : <SignupProfilePage />))
  return { show }
}

// 약관 동의와 계정 입력을 마친 채로 회원 정보 단계를 연다.
const ACCOUNT_DONE: SignupStepInitial = {
  terms: termChecks(true),
  termsAgreed: true,
  account: {
    email: 'runner@crewrun.kr',
    password: 'password1234',
    passwordConfirm: 'password1234',
  },
}

function renderProfileStep() {
  render(
    <QueryClientProvider client={new QueryClient()}>
      <SignupStepProvider initialState={ACCOUNT_DONE}>
        <SignupProfilePage />
      </SignupStepProvider>
    </QueryClientProvider>,
  )
}

async function fillProfile(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('이름'), '김러너')
  await user.type(screen.getByLabelText('생년월일'), '1995.03.02')
  await user.type(screen.getByLabelText('휴대폰 번호'), '010-1234-5678')
  await user.type(screen.getByLabelText('닉네임'), '달리는곰')
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

  it('비밀번호가 9자면(백엔드 10자 이상) 오류가 뜨고 다음 버튼이 꺼진다', async () => {
    const user = userEvent.setup()
    renderSignup()
    await agreeRequiredTerms(user)

    await user.type(screen.getByLabelText('이메일'), 'runner@crewrun.kr')
    await user.type(screen.getByLabelText('비밀번호'), 'password1')
    await user.tab()

    expect(await screen.findByText(AUTH_ERRORS.passwordRule)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다음' })).toBeDisabled()
  })

  it('다음 단계로 갔다가 돌아와도 입력값과 약관 체크가 남아 있다', async () => {
    const user = userEvent.setup()
    const { show } = renderSignup()
    await agreeRequiredTerms(user)

    await user.type(screen.getByLabelText('이메일'), 'runner@crewrun.kr')
    await user.type(screen.getByLabelText('비밀번호'), 'password123')
    await user.type(screen.getByLabelText('비밀번호 확인'), 'password123')
    await user.click(screen.getByRole('button', { name: '다음' }))
    expect(push).toHaveBeenCalledWith('/signup/profile')

    show('profile')
    await user.type(screen.getByLabelText('닉네임'), '달리는곰')

    show('account')
    // 약관에 이미 동의했으므로 바텀시트 없이 계정 단계가 보인다.
    expect(screen.queryByRole('button', { name: '동의하고 계속하기' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('이메일')).toHaveValue('runner@crewrun.kr')
    expect(screen.getByLabelText('비밀번호 확인')).toHaveValue('password123')
    expect(await screen.findByRole('button', { name: '다음' })).toBeEnabled()

    show('profile')
    expect(screen.getByLabelText('닉네임')).toHaveValue('달리는곰')
  })

  it('약관 시트를 동의 없이 닫아도 다음을 누르면 시트가 다시 열린다', async () => {
    const user = userEvent.setup()
    renderSignup()

    await user.click(await screen.findByRole('button', { name: '닫기' }))
    expect(screen.queryByRole('button', { name: '동의하고 계속하기' })).not.toBeInTheDocument()

    await user.type(screen.getByLabelText('이메일'), 'runner@crewrun.kr')
    await user.type(screen.getByLabelText('비밀번호'), 'password123')
    await user.type(screen.getByLabelText('비밀번호 확인'), 'password123')
    await user.click(screen.getByRole('button', { name: '다음' }))

    expect(await screen.findByRole('button', { name: '동의하고 계속하기' })).toBeInTheDocument()
    expect(push).not.toHaveBeenCalled()
  })

  it('가입 완료를 누르면 가입 API에 입력값을 보내고, 바로 로그인해 홈으로 간다', async () => {
    let body: unknown
    server.use(
      http.post('*/api/v1/auth/join', async ({ request }) => {
        body = await request.json()
        return HttpResponse.json(
          { status: 201, message: 'Member Joined Successfully', data: null },
          { status: 201 },
        )
      }),
    )
    const user = userEvent.setup()
    renderProfileStep()

    await fillProfile(user)
    await user.click(screen.getByRole('button', { name: '가입 완료' }))

    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/'))
    expect(body).toEqual({
      email: 'runner@crewrun.kr',
      password: 'password1234',
      name: '김러너',
      nickname: '달리는곰',
      phone: '010-1234-5678',
      birthDate: '1995-03-02',
    })
    expect(getAuthStatus()).toBe('authenticated')
  })

  it('닉네임이 이미 쓰이면 닉네임 칸에 서버 문구를 띄우고 이동하지 않는다', async () => {
    server.use(
      http.post('*/api/v1/auth/join', () =>
        HttpResponse.json(
          {
            status: 409,
            code: 'MEMBER_DUPLICATE_NICKNAME',
            message: '이미 사용 중인 닉네임입니다.',
            data: null,
          },
          { status: 409 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderProfileStep()

    await fillProfile(user)
    await user.click(screen.getByRole('button', { name: '가입 완료' }))

    expect(await screen.findByText('이미 사용 중인 닉네임입니다.')).toBeInTheDocument()
    expect(replace).not.toHaveBeenCalled()
  })

  it('약관 동의와 계정 입력 없이 회원 정보 단계에 오면 첫 단계로 돌려보낸다', () => {
    render(
      <SignupStepProvider>
        <SignupProfilePage />
      </SignupStepProvider>,
    )

    expect(replace).toHaveBeenCalledWith('/signup')
    expect(screen.queryByRole('button', { name: '가입 완료' })).not.toBeInTheDocument()
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
