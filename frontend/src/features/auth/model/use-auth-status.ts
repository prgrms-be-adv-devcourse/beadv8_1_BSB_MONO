import { useSyncExternalStore } from 'react'

import { getAuthStatus, subscribeAuthStatus, type AuthStatus } from '@/shared/api'

/**
 * 지금 로그인 상태. 메모리의 Access Token을 기준으로 한다.
 * 서버 렌더링과 첫 화면은 'pending'(확인 중)이고, 앱 시작 때 재발급이 끝나면 바뀐다.
 */
export function useAuthStatus(): AuthStatus {
  return useSyncExternalStore(subscribeAuthStatus, getAuthStatus, getServerStatus)
}

function getServerStatus(): AuthStatus {
  return 'pending'
}
