import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'

import { clearAccessToken, memberApi } from '@/shared/api'

/**
 * 로그아웃한다 (POST /api/v1/auth/logout). 서버가 Refresh Token을 지우고 쿠키를 만료시킨다.
 * 요청이 실패해도 이 브라우저에서는 로그아웃 상태로 바꾸고 로그인 화면으로 보낸다.
 * 다음 사람이 앞사람의 데이터를 보지 않도록 불러 둔 서버 데이터 캐시도 지운다.
 */
export function useLogout() {
  const queryClient = useQueryClient()
  const router = useRouter()
  return useMutation({
    mutationFn: () => memberApi.logout(),
    onSettled: () => {
      clearAccessToken()
      queryClient.removeQueries()
      router.replace('/login')
    },
  })
}
