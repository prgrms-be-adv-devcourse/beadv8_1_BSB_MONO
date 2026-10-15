import { useMutation, useQueryClient } from '@tanstack/react-query'

import { memberApi, memberModel, setAccessToken, unwrapData } from '@/shared/api'

/**
 * 이메일·비밀번호로 로그인한다 (POST /api/v1/auth/login).
 * Access Token은 메모리에 두고, Refresh Token은 서버가 httpOnly 쿠키로 심는다.
 * 응답의 회원 정보는 내 정보 조회 캐시에 넣어 바로 다시 부르지 않게 한다.
 */
export function useLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (values: memberModel.MemberLoginRequestDto) => {
      const { accessToken, member } = unwrapData(await memberApi.login(values))
      if (!accessToken) throw new Error('로그인 응답에 Access Token이 없습니다.')
      return { accessToken, member }
    },
    onSuccess: ({ accessToken, member }) => {
      setAccessToken(accessToken)
      if (member) {
        queryClient.setQueryData<memberModel.RsDataMemberDto>(memberApi.getFindMeQueryKey(), {
          status: 200,
          message: '로그인 성공',
          data: member,
        })
      }
    },
  })
}
