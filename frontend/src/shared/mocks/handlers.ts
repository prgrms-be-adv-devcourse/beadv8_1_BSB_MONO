import { http, HttpResponse, type RequestHandler } from 'msw'

import { getBukangCashAPIMock } from '@/shared/api/generated/cash/cash.msw'
import { getBukangFileAPIMock } from '@/shared/api/generated/file/file.msw'
import { getBukangMemberAPIMock } from '@/shared/api/generated/member/member.msw'
import { getBukangOrderAPIMock } from '@/shared/api/generated/order/order.msw'
import { getBukangPayoutAPIMock } from '@/shared/api/generated/payout/payout.msw'
import { getBukangProductAPIMock } from '@/shared/api/generated/product/product.msw'

/** 경로 변수(:raceId) 수. 적은 것부터 둬야 /races/suggestions가 /races/:raceId에 먼저 걸리지 않는다. */
function pathParamCount(handler: RequestHandler): number {
  const path = (handler.info as { path?: unknown }).path
  return typeof path === 'string' ? (path.match(/\/:/g) ?? []).length : 0
}

/** orval이 서비스별 명세(openapi/*.json)의 응답 예시로 만든 목 */
const generatedHandlers = [
  ...getBukangMemberAPIMock(),
  ...getBukangCashAPIMock(),
  ...getBukangPayoutAPIMock(),
  ...getBukangFileAPIMock(),
  ...getBukangOrderAPIMock(),
  ...getBukangProductAPIMock(),
].sort((a, b) => pathParamCount(a) - pathParamCount(b))

/**
 * 모든 MSW 핸들러 목록. 개발 서버(/api 라우트)와 테스트가 같이 쓴다.
 * 테스트에서 응답을 바꿀 때는 server.use(getXxxMockHandler(응답))로 덮어쓴다.
 */
export const handlers = [
  // 목 파이프라인이 살아있는지 확인하는 용도
  http.get('*/api/health', () => HttpResponse.json({ status: 'ok', mocked: true })),
  ...generatedHandlers,
]
