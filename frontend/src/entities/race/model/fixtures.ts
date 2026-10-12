// TODO(api): 대회 API가 생기면 지운다. 시안(피그마 세미프로젝트 페이지)의 예시 값을 그대로 옮긴 화면용 임시 데이터.

export type RaceSummary = {
  id: string
  title: string
  image: string
  /** 카드 첫 줄 (접수 상태 · 마감) */
  schedule: string
  /** 카드 둘째 줄 (종목 · 장소) */
  place: string
}

export type RaceBanner = {
  id: string
  image: string
  /** 글자가 박힌 배너 이미지는 제목을 비운다 */
  title?: string
  tags?: string
}

export const RACE_BANNERS: RaceBanner[] = [
  {
    id: 'cookierun',
    image: '/mock/races/cookierun.png',
    title: '「접수 중」\n용감한 쿠키RUN in 서울',
    tags: '#함께뛰자  #5K  #10K',
  },
  {
    id: 'youth10k',
    image: '/mock/races/youth10k.png',
    title: '긍정 하프 마라톤 YOUTH 10K\n10/12 (월) 18:00 접수 마감',
    tags: '#10K  #접수마감임박',
  },
  { id: 'jtbc', image: '/mock/races/jtbc.png' },
]

export const CLOSING_SOON_RACES: RaceSummary[] = [
  {
    id: 'cookierun',
    title: '2026 용감한 쿠키RUN in 서울 (10K)',
    image: '/mock/races/cookierun.png',
    schedule: 'D-12 · 남은 590자리',
    place: '10K · 서울숲',
  },
  {
    id: 'cookierun-5k',
    title: '2026 용감한 쿠키RUN in 서울 (5K)',
    image: '/mock/races/cookierun.png',
    schedule: 'D-12 · 남은 160자리',
    place: '5K · 서울숲',
  },
]

export const OPEN_RACES: RaceSummary[] = [
  {
    id: 'jtbc',
    title: '2026 JTBC 서울마라톤',
    image: '/mock/races/jtbc.png',
    schedule: '접수 중 · ~10/25',
    place: '풀 · 광화문 → 잠실',
  },
  {
    id: 'youth10k',
    title: '2026 긍정 하프 마라톤\nYOUTH 10K',
    image: '/mock/races/youth10k.png',
    schedule: '접수 중 · ~10/12 18:00',
    place: '10K · 청년 부문',
  },
  {
    id: 'hyrox',
    title: 'POSITIVE HYROX',
    image: '/mock/races/hyrox.png',
    schedule: '접수 중 · ~10/28 (수) 19:00',
    place: '굿러너 하우스 0F · 스웻 먼데이',
  },
  {
    id: 'daligi',
    title: 'POSITIVE DALIGI',
    image: '/mock/races/daligi.png',
    schedule: '접수 중 · ~10/30 (금) 19:50',
    place: '굿러너 하우스 1F · 함께 달리기',
  },
]

export type RaceOption = {
  id: string
  name: string
  /** 1인 참가권 구성 */
  includes: string
  price: number
}

export type RaceDetail = {
  id: string
  title: string
  images: string[]
  heroTitle: string
  heroTags: string
  startsAt: string
  place: string
  /** 접수 마감까지 남은 날 (D-12) */
  dDay: string
  remaining: number
  capacity: number
  applied: number
  options: RaceOption[]
  schedule: { label: string; value: string }[]
  refund: { label: string; value: string }[]
  seller: { name: string; businessNo: string; mailOrderNo: string }
}

export const RACE_DETAIL: RaceDetail = {
  id: 'cookierun',
  title: '2026 용감한 쿠키RUN in 서울',
  images: [
    '/mock/races/cookierun.png',
    '/mock/races/cookierun.png',
    '/mock/races/cookierun.png',
    '/mock/races/cookierun.png',
  ],
  heroTitle: '「10K · 5K 접수 중」',
  heroTags: '#함께뛰자  #5K  #10K',
  startsAt: '11/1 (일) 08:00 출발',
  place: '서울숲 일대 · 5K / 10K',
  dDay: 'D-12',
  remaining: 590,
  capacity: 3000,
  applied: 2410,
  options: [
    { id: '10k', name: '10K', includes: '1인 · 기념 티셔츠 · 완주 메달', price: 45000 },
    { id: '5k', name: '5K', includes: '1인 · 기념 티셔츠', price: 35000 },
  ],
  schedule: [
    { label: '접수 기간', value: '10/14 (수) 10:00 ~ 10/31 (토) 18:00' },
    { label: '정보 수정 기한', value: '10/31 (토) 18:00' },
    { label: '전액 환불 기한', value: '10/31 (토) 23:59' },
  ],
  refund: [
    { label: '환불 기한 10/31 (토) 23:59까지', value: '전액 환불' },
    { label: '환불 기한이 지난 뒤', value: '개인 사정 환불 불가' },
    { label: '대회 취소 · 일정 변경', value: '전액 환불' },
  ],
  seller: {
    name: '판매자 (주)데브시스터즈러닝 · 대표 김주최',
    businessNo: '사업자 123-45-67890 · 통신판매업 2026-서울성동-0000',
    mailOrderNo: '크루런은 통신판매중개자이며 거래 당사자가 아니에요.',
  },
}
