import { useMutation, useQueryClient } from '@tanstack/react-query'

import { ApiError, clearAccessToken, memberApi } from '@/shared/api'

/**
 * 회원 탈퇴 (DELETE /api/v1/member/members/me).
 * 성공하면 이 브라우저를 로그아웃 상태로 바꾼다: 메모리의 Access Token을 지우고, 앞 사람의 서버 데이터 캐시도 비운다.
 * Refresh Token은 서버가 이미 지워 두었다. 화면 이동은 부르는 쪽이 정한다.
 */
export function useWithdraw() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => memberApi.withdrawMe(),
    onSuccess: () => {
      clearAccessToken()
      queryClient.removeQueries()
    },
  })
}

/** 탈퇴 실패 이유를 서버 code로 가른다. 모르는 code는 other. */
export type WithdrawFailure = 'cash-left' | 'ticket-left' | 'check-failed' | 'other'

export function withdrawFailure(error: unknown): WithdrawFailure {
  if (!(error instanceof ApiError)) return 'other'
  switch (error.code) {
    case 'MEMBER_WITHDRAW_CASH_LEFT':
      return 'cash-left'
    case 'MEMBER_WITHDRAW_TICKET_LEFT':
      return 'ticket-left'
    case 'MEMBER_WITHDRAW_CHECK_FAILED':
      return 'check-failed'
    default:
      return 'other'
  }
}
