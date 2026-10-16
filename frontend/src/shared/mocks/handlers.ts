import { http, HttpResponse, type RequestHandler } from 'msw'

import { getBukangCashAPIMock } from '@/shared/api/generated/cash/cash.msw'
import {
  getBukangFileAPIMock,
  getFindByRefMockHandler,
  getFindByRefResponseMock,
} from '@/shared/api/generated/file/file.msw'
import {
  getBukangMemberAPIMock,
  getLoginResponseMock,
  getReissueResponseMock,
} from '@/shared/api/generated/member/member.msw'
import {
  getBukangOrderAPIMock,
  getCheckoutMockHandler,
  getCheckoutResponseMock,
  getCreateMockHandler,
  getCreateResponseMock,
  getFindById1MockHandler,
  getFindById1ResponseMock,
  getFindCartMockHandler,
  getFindCartResponseMock,
  getFindMineMockHandler,
  getFindMineResponseMock,
  getPayMockHandler,
  getPayResponseMock,
  getRemoveItemMockHandler,
  getRemoveItemResponseMock,
} from '@/shared/api/generated/order/order.msw'
import { getBukangPayoutAPIMock } from '@/shared/api/generated/payout/payout.msw'
import { getBukangProductAPIMock } from '@/shared/api/generated/product/product.msw'

import { fileUploadHandlers } from './file-upload'
import {
  clearMockRefreshCookie,
  issueMockRefreshCookie,
  MOCK_REFRESH_COOKIE_NAME,
} from './mock-session'

/** 경로 변수(:raceId) 수. 적은 것부터 둬야 /races/suggestions가 /races/:raceId에 먼저 걸리지 않는다. */
function pathParamCount(handler: RequestHandler): number {
  const path = (handler.info as { path?: unknown }).path
  return typeof path === 'string' ? (path.match(/\/:/g) ?? []).length : 0
}

/**
 * 명세 예시의 이미지 주소는 실제로 없는 S3 주소라(next/image 허용 호스트도 아니다) 목에서는 public/mock 이미지로 바꾼다.
 * 생성된 핸들러보다 앞에 둬서 먼저 걸리게 한다.
 */
const fileImageHandler = getFindByRefMockHandler(() => {
  const example = getFindByRefResponseMock()
  return {
    ...example,
    data: example.data?.map((file) => ({ ...file, url: '/mock/races/cookierun.png' })),
  }
})

/**
 * 로그인 상태를 진짜처럼 흉내 내는 인증 목. 생성된 핸들러보다 앞에 둬서 먼저 걸리게 한다.
 * 진짜 서버처럼 로그인하면 Refresh Token 쿠키(가짜 이름)를 심고, 로그아웃하면 지운다.
 * 재발급은 요청 쿠키에 그 값이 있을 때만 성공하고(쿠키도 새로 바꿔 준다) 없으면 401.
 * 그래서 로그인하지 않고 새로고침하면 로그아웃 상태로 보인다.
 */
const authHandlers = [
  http.post('*/api/v1/auth/login', () =>
    HttpResponse.json(getLoginResponseMock(), {
      headers: { 'Set-Cookie': issueMockRefreshCookie() },
    }),
  ),
  http.post('*/api/v1/auth/reissue', ({ cookies }) => {
    if (!cookies[MOCK_REFRESH_COOKIE_NAME]) {
      return HttpResponse.json(
        { status: 401, message: 'Refresh Token이 없습니다. 다시 로그인해주세요.', data: null },
        { status: 401 },
      )
    }
    const example = getReissueResponseMock()
    return HttpResponse.json(
      {
        ...example,
        data: {
          ...example.data,
          accessToken: `${example.data?.accessToken}.${crypto.randomUUID()}`,
        },
      },
      { headers: { 'Set-Cookie': issueMockRefreshCookie() } },
    )
  }),
  http.post(
    '*/api/v1/auth/logout',
    () => new HttpResponse(null, { headers: { 'Set-Cookie': clearMockRefreshCookie() } }),
  ),
]

/**
 * 내 참가권 목. 생성된 주문 목록 예시는 결제 대기 · 기한 만료 주문뿐이라 참가권이 하나도 안 보인다.
 * 결제한 주문(주문 상세 예시와 같은 1번)을 맨 앞에 더한다. 생성된 핸들러보다 앞에 둔다.
 */
const myOrdersHandler = getFindMineMockHandler(() => {
  const example = getFindMineResponseMock()
  const paid = {
    id: 1,
    createDate: '2026-10-12T10:00:00',
    orderNo: 'OD-20261012-0001',
    status: 'PAID' as const,
    totalAmount: 50000,
    paymentDueAt: '2026-10-12T10:10:00',
    itemName: '2026 서울 하프 마라톤 10K',
    itemCount: 1,
  }
  return { ...example, data: { ...example.data, orders: [paid, ...(example.data?.orders ?? [])] } }
})
/** 지금부터 minutes분 뒤의 한국 시간 (백엔드처럼 시간대 없는 2026-10-12T10:10:00 모양) */
function kstMinutesFromNow(minutes: number): string {
  return new Date(Date.now() + (9 * 60 + minutes) * 60 * 1000).toISOString().slice(0, 19)
}

/**
 * 명세 예시는 주문 생성 · 장바구니 주문 · 결제 · 조회가 모두 주문 1이다. 그런데 결제 화면은 주문 1이 결제 대기여야 하고,
 * 내 참가권(위 myOrdersHandler)은 주문 1이 결제한 주문이어야 한다.
 * 그래서 진짜 서버처럼 주문을 만들면 결제 대기로, 결제하면 결제 완료로 바꿔 두고 조회는 그 상태대로 돌려준다.
 * 처음에는 결제 완료(내 참가권에 보이는 주문)다.
 */
let mockOrderAwaitingPayment = false

/** 테스트 사이에 주문 1의 상태가 넘어가지 않게 처음(결제 완료)으로 돌린다. */
export function resetMockOrderForTesting() {
  mockOrderAwaitingPayment = false
}

/**
 * 명세 예시의 주문은 결제 기한이 이미 지난 시각이라 결제 화면이 바로 시간 초과가 된다.
 * 가짜 응답 모드에서 결제 화면을 볼 수 있게 주문 생성 · 결제 대기 주문 조회는 결제 대기 · 기한 10분 뒤로 돌려준다.
 * 생성된 핸들러보다 앞에 둬서 먼저 걸리게 한다.
 */
const orderHandlers = [
  getCreateMockHandler(() => {
    mockOrderAwaitingPayment = true
    const example = getCreateResponseMock()
    return { ...example, data: { ...example.data, paymentDueAt: kstMinutesFromNow(10) } }
  }),
  getCheckoutMockHandler(() => {
    mockOrderAwaitingPayment = true
    return getCheckoutResponseMock()
  }),
  // 장바구니에는 한 종목만 담긴다. 명세 예시는 두 항목이라, 대회 1의 10K(종목 1) 하나만 남겨 돌려준다.
  getFindCartMockHandler(() => {
    const example = getFindCartResponseMock()
    const items = (example.data?.items ?? []).filter((item) => item.raceCourseId === 1)
    return { ...example, data: { ...example.data, items } }
  }),
  getRemoveItemMockHandler(() => {
    const example = getRemoveItemResponseMock()
    return { ...example, data: { ...example.data, items: [] } }
  }),
  getPayMockHandler(() => {
    mockOrderAwaitingPayment = false
    return getPayResponseMock()
  }),
  getFindById1MockHandler(() => {
    if (!mockOrderAwaitingPayment) return getFindById1ResponseMock()
    const example = getCreateResponseMock()
    return { ...example, data: { ...example.data, paymentDueAt: kstMinutesFromNow(10) } }
  }),
]

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
  fileImageHandler,
  ...authHandlers,
  ...fileUploadHandlers,
  myOrdersHandler,
  ...orderHandlers,
  ...generatedHandlers,
]
