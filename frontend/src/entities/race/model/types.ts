// 화면이 쓰는 대회 모양. 상품 API 응답(productModel)은 model/mappers.ts에서 이 모양으로 바꾼다.

export type RaceBadge = { label: string; tone: 'brand' | 'warning' | 'neutral' }

/** 홈 카드 (곧 마감 · 지금 접수 중) */
export type RaceSummary = {
  id: string
  /** 종목을 골라 둔 채 상세로 보낼 때 (곧 마감 카드는 종목별로 나온다) */
  option?: string
  title: string
  /** 없으면 대회 정보로 만든 색 포스터를 그린다 (목록 API는 이미지를 주지 않는다) */
  image?: string
  /** 포스터에 크게 쓰는 종목 (예: 10K · 5K) */
  posterLabel: string
  /** 카드 첫 줄 (접수 상태 · 마감) */
  schedule: string
  /** 카드 둘째 줄 (종목 · 장소) */
  place: string
}

/** 대회 찾기 목록의 한 줄 */
export type RaceListItem = {
  id: string
  title: string
  image?: string
  posterLabel: string
  badge: RaceBadge
  meta: string
  price: string
  remaining: string
}

export type RaceOption = {
  id: string
  name: string
  /** 1인 참가권 구성 */
  includes: string
  price: number
  /** 종목 대표 이미지 */
  image?: string
  soldOut?: boolean
}

export type RaceDetail = {
  id: string
  title: string
  /** 대표 이미지 → 소개 이미지 순. 비어 있으면 색 포스터를 그린다 */
  images: string[]
  heroTitle: string
  heroTags: string
  status: RaceBadge
  startsAt: string
  place: string
  /** 판매자가 쓴 대회 소개 */
  description?: string
  /** 접수 마감까지 남은 날 (D-12) */
  dDay: string
  remaining: number
  capacity: number
  applied: number
  options: RaceOption[]
  schedule: { label: string; value: string }[]
  refund: { label: string; value: string }[]
  /** 판매자가 대회 상세에 올리는 자주 묻는 질문 */
  faq: { question: string; answer: string }[]
  seller: {
    /** 개인정보 제3자 제공 동의의 '받는 곳' */
    company: string
    name: string
    businessNo: string
    mailOrderNo: string
  }
}
