import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getAuthStatus, setAccessToken } from '@/shared/api'
import { memberMock, server } from '@/shared/testing'

import { MyPage } from './MyPage'

const replace = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn(), replace }),
  usePathname: () => '/me',
}))

beforeEach(() => replace.mockClear())

describe('MyPage 로그아웃', () => {
  it('로그아웃을 누르면 로그아웃 API를 부르고, 토큰·캐시를 비운 뒤 로그인 화면으로 보낸다', async () => {
    let authorization: string | null = null
    server.use(
      http.post('*/api/v1/auth/logout', ({ request }) => {
        authorization = request.headers.get('Authorization')
        return HttpResponse.json({ status: 200, message: '로그아웃 성공', data: null })
      }),
    )
    setAccessToken('access-token')
    const client = new QueryClient()
    client.setQueryData(['/api/v1/member/members/me'], { data: { nickname: '러너1' } })
    const user = userEvent.setup()
    render(
      <QueryClientProvider client={client}>
        <MyPage />
      </QueryClientProvider>,
    )

    await user.click(screen.getByRole('button', { name: '로그아웃' }))

    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/login'))
    expect(authorization).toBe('Bearer access-token')
    expect(getAuthStatus()).toBe('anonymous')
    expect(client.getQueryData(['/api/v1/member/members/me'])).toBeUndefined()
  })

  it('로그아웃 요청이 실패해도 이 브라우저에서는 로그아웃 상태로 바꾼다', async () => {
    server.use(http.post('*/api/v1/auth/logout', () => HttpResponse.error()))
    setAccessToken('access-token')
    const user = userEvent.setup()
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MyPage />
      </QueryClientProvider>,
    )

    await user.click(screen.getByRole('button', { name: '로그아웃' }))

    await vi.waitFor(() => expect(replace).toHaveBeenCalledWith('/login'))
    expect(getAuthStatus()).toBe('anonymous')
  })
})

describe('MyPage 회원 정보', () => {
  it('내 정보 조회 API의 닉네임 · 이메일 · 이름 · 휴대폰(가림) · 생년월일을 보여 준다', async () => {
    server.use(
      memberMock.getFindMeMockHandler({
        status: 200,
        message: '내 정보를 조회했습니다.',
        data: {
          id: 1,
          nickname: '달리는곰',
          email: 'bear@crewrun.kr',
          phone: '01098765432',
          name: '김곰',
          birthDate: '1990-01-31',
        },
      }),
    )
    setAccessToken('access-token')
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MyPage />
      </QueryClientProvider>,
    )

    expect(await screen.findByText('달리는곰')).toBeInTheDocument()
    expect(screen.getByText('bear@crewrun.kr')).toBeInTheDocument()
    expect(screen.getByText('김곰')).toBeInTheDocument()
    expect(screen.getByText('010-****-5432')).toBeInTheDocument()
    expect(screen.getByText('1990.01.31')).toBeInTheDocument()
  })

  it('내 정보를 불러오지 못하면 안내 문구를 띄운다', async () => {
    server.use(
      http.get('*/api/v1/member/members/me', () =>
        HttpResponse.json(
          { status: 404, message: '존재하지 않는 회원입니다.', data: null },
          { status: 404 },
        ),
      ),
    )
    setAccessToken('access-token')
    render(
      <QueryClientProvider
        client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
      >
        <MyPage />
      </QueryClientProvider>,
    )

    expect(await screen.findByRole('alert')).toHaveTextContent('회원 정보를 불러오지 못했어요')
  })
})

describe('MyPage 참가 대회 수', () => {
  const renderPage = () => {
    setAccessToken('access-token')
    render(
      <QueryClientProvider
        client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
      >
        <MyPage />
      </QueryClientProvider>,
    )
  }

  it('내 참가권 화면과 같은 목록(주문 API)의 개수를 보여 준다', async () => {
    renderPage()

    // 목의 주문 목록에서 참가권이 되는 것은 결제한 주문 1개다
    expect(await screen.findByText('1개')).toBeInTheDocument()
  })

  it('참가권이 없으면(방금 가입한 회원) 0개로 보여 준다', async () => {
    server.use(
      http.get('*/api/v1/order/orders', () =>
        HttpResponse.json({
          status: 200,
          message: '주문 내역을 조회했습니다.',
          data: { orders: [], page: 0, size: 100, totalElements: 0, totalPages: 0 },
        }),
      ),
    )
    renderPage()

    expect(await screen.findByText('0개')).toBeInTheDocument()
  })
})
