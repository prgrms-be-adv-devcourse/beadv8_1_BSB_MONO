import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import { server } from '@/shared/mocks/node'

import { ApiError } from './api-error'
import { getErrorMessage } from './error-message'
import { customFetch } from './fetcher'

// 서버가 돌려준 오류 응답을 customFetch로 받아 ApiError를 만든다.
async function fetchError(
  status: number,
  body: Record<string, string | number | null>,
): Promise<ApiError> {
  server.use(http.get('*/api/members/me', () => HttpResponse.json(body, { status })))
  const error = await customFetch('http://localhost/api/members/me').catch((e: unknown) => e)
  if (!(error instanceof ApiError)) throw new Error('ApiError가 아니다')
  return error
}

describe('getErrorMessage', () => {
  it.each([
    [
      409,
      'MEMBER_DUPLICATE_EMAIL',
      '이미 가입된 이메일이에요. 로그인하거나 비밀번호를 재설정해 주세요.',
    ],
    [409, 'MEMBER_DUPLICATE_NICKNAME', '이미 사용 중인 닉네임이에요.'],
    [409, 'MEMBER_DUPLICATE_PHONE', '이미 가입된 휴대폰 번호예요.'],
    [404, 'MEMBER_NOT_FOUND', '회원 정보를 찾을 수 없어요.'],
    [400, 'MEMBER_UNDER_AGE', '만 14세 미만은 가입할 수 없어요'],
    [401, 'MEMBER_LOGIN_FAILED', '이메일 또는 비밀번호가 맞지 않아요.'],
  ])('%i %s는 시안 문구로 바꾼다', async (status, code, message) => {
    const error = await fetchError(status, { status, code, message: '서버 문구', data: null })
    expect(getErrorMessage(error)).toBe(message)
  })

  it('표에 없는 코드는 상태 코드별 일반 문구로 바꾼다', async () => {
    const error = await fetchError(409, {
      status: 409,
      code: 'UNKNOWN_CODE',
      message: '?',
      data: null,
    })
    expect(getErrorMessage(error)).toBe('이미 처리된 요청이에요. 새로고침한 뒤 다시 확인해 주세요.')
  })

  it('코드 없는 500은 서버 오류 일반 문구로 바꾼다', async () => {
    const error = await fetchError(500, { status: 500, message: '서버 오류', data: null })
    expect(getErrorMessage(error)).toBe('일시적인 오류가 생겼어요. 잠시 후 다시 시도해 주세요.')
  })

  it('본문이 객체가 아니고 표에 없는 상태 코드면 기본 문구를 쓴다', () => {
    const error = new ApiError(418, 'teapot', '/api/tea')
    expect(getErrorMessage(error)).toBe('문제가 생겼어요. 다시 시도해 주세요.')
  })
})
