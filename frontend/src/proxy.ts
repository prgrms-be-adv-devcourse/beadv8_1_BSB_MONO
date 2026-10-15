import { NextResponse } from 'next/server'

/**
 * 판매자·관리자 화면 접근 1차 확인 자리. 지금은 모두 통과시킨다.
 * Refresh Token 쿠키는 Path=/api/v1/auth라 이 요청에 실리지 않아 로그인 여부를 알 수 없다.
 * 로그인 전 진입은 브라우저의 AuthSessionRestorer가 재발급 실패 시 /login으로 보내고,
 * 실제 권한 검사는 백엔드가 한다.
 */
export function proxy() {
  return NextResponse.next()
}

export const config = {
  matcher: ['/seller/:path*', '/admin/:path*'],
}
