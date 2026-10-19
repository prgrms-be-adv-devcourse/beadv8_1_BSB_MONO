/** 로그인해야 열리는 회원 화면의 앞부분. 가짜 응답 모드에서 비로그인으로 열면 로그인 화면으로 보낼 때 쓴다. */
const PROTECTED_PREFIXES = ['/me', '/tickets', '/cart', '/checkout', '/notifications']

/** 대회 신청 화면(/races/1/apply)도 로그인이 필요하다. */
const APPLY_PATH = /^\/races\/[^/]+\/apply(\/|$)/

export function isProtectedPath(pathname: string): boolean {
  return (
    APPLY_PATH.test(pathname) ||
    PROTECTED_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`))
  )
}
