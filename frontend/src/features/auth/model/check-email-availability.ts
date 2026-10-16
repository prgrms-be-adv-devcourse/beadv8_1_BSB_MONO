import { memberApi, unwrapData } from '@/shared/api'

/**
 * 이메일을 아직 쓸 수 있는지 서버에 묻는다 (GET /api/v1/member/members/email-availability).
 * 토큰 없이 부르는 공개 API다. 중복이어도 오류가 아니라 available=false로 온다.
 */
export async function checkEmailAvailable(email: string): Promise<boolean> {
  const result = unwrapData(await memberApi.checkEmailAvailability({ email }))
  return result.available ?? false
}
