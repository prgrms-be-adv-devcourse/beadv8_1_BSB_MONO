// TODO(api): 대회 API가 생기면 지운다. 시안(피그마 세미프로젝트 페이지)의 예시 값을 그대로 옮긴 화면용 임시 데이터.

export type RaceSummary = {
  id: string
  /** 종목을 골라 둔 채 상세로 보낼 때 (곧 마감 카드는 종목별로 나온다) */
  option?: string
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
    option: '10k',
    title: '2026 용감한 쿠키RUN in 서울 (10K)',
    image: '/mock/races/cookierun.png',
    schedule: 'D-12 · 남은 590자리',
    place: '10K · 서울숲',
  },
  {
    id: 'cookierun',
    option: '5k',
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

function refundRows(deadline: string) {
  return [
    { label: `환불 기한 ${deadline}까지`, value: '전액 환불' },
    { label: '환불 기한이 지난 뒤', value: '개인 사정 환불 불가' },
    { label: '대회 취소 · 일정 변경', value: '전액 환불' },
  ]
}

// 판매자가 올리는 자주 묻는 질문 예시. 정책(1인 1건, 양도 불가, 예치금 환불)과 맞춘다.
const FAQ_ROWS = [
  {
    question: '한 사람이 여러 종목에 신청할 수 있나요?',
    answer:
      '한 사람은 한 대회에 1건만 신청할 수 있어요. 다른 종목으로 바꾸려면 신청을 취소하고 다시 신청해 주세요.',
  },
  {
    question: '참가권을 친구에게 넘길 수 있나요?',
    answer: '참가권은 다른 사람에게 양도할 수 없어요. 신청한 본인만 대회에 참가할 수 있어요.',
  },
  {
    question: '환불하면 돈은 어디로 들어오나요?',
    answer:
      '환불 금액은 크루런 예치금으로 돌려드려요. 예치금은 다른 대회나 굿즈를 살 때 쓸 수 있어요.',
  },
  {
    question: '기념품 키트는 언제 받나요?',
    answer:
      '환불 기한이 지난 뒤 신청할 때 적은 주소로 배송해요. 배송이 시작되면 알림으로 알려 드려요.',
  },
  {
    question: '기념품 사이즈는 바꿀 수 있나요?',
    answer:
      '환불 기한 전까지는 내 참가 대회에서 사이즈를 바꿀 수 있어요. 사이즈표는 상세 정보 이미지에서 확인해 주세요.',
  },
]

const MAIL_ORDER_NOTICE = '크루런은 통신판매중개자이며 거래 당사자가 아니에요.'

export const RACE_DETAILS: Record<string, RaceDetail> = {
  cookierun: {
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
    refund: refundRows('10/31 (토) 23:59'),
    faq: FAQ_ROWS,
    seller: {
      company: '(주)데브시스터즈러닝',
      name: '판매자 (주)데브시스터즈러닝 · 대표 김주최',
      businessNo: '사업자 123-45-67890 · 통신판매업 2026-서울성동-0000',
      mailOrderNo: MAIL_ORDER_NOTICE,
    },
  },
  jtbc: {
    id: 'jtbc',
    title: '2026 JTBC 서울마라톤',
    images: ['/mock/races/jtbc.png'],
    heroTitle: '「풀코스 마감 임박」',
    heroTags: '#풀코스  #광화문출발  #잠실도착',
    startsAt: '11/1 (일) 08:00 출발',
    place: '광화문 → 잠실종합운동장 · 풀코스',
    dDay: 'D-13',
    remaining: 86,
    capacity: 30000,
    applied: 29914,
    options: [
      {
        id: 'full',
        name: '풀코스',
        includes: '1인 · 기념 티셔츠 · 완주 메달 · 기록칩',
        price: 80000,
      },
    ],
    schedule: [
      { label: '접수 기간', value: '9/1 (화) 10:00 ~ 10/25 (일) 18:00' },
      { label: '정보 수정 기한', value: '10/25 (일) 18:00' },
      { label: '전액 환불 기한', value: '10/25 (일) 23:59' },
    ],
    refund: refundRows('10/25 (일) 23:59'),
    faq: FAQ_ROWS,
    seller: {
      company: '(주)서울마라톤운영',
      name: '판매자 (주)서울마라톤운영 · 대표 박주최',
      businessNo: '사업자 234-56-78901 · 통신판매업 2026-서울종로-0000',
      mailOrderNo: MAIL_ORDER_NOTICE,
    },
  },
  youth10k: {
    id: 'youth10k',
    title: '2026 긍정 하프 마라톤 YOUTH 10K',
    images: ['/mock/races/youth10k.png'],
    heroTitle: '「오늘 18:00 접수 마감」',
    heroTags: '#10K  #청년부문  #접수마감임박',
    startsAt: '10/25 (일) 08:30 출발',
    place: '여의도 한강공원 · 10K',
    dDay: 'D-DAY',
    remaining: 420,
    capacity: 2000,
    applied: 1580,
    options: [{ id: '10k', name: '10K', includes: '1인 · 기념 티셔츠 · 완주 메달', price: 30000 }],
    schedule: [
      { label: '접수 기간', value: '8/20 (목) 12:00 ~ 10/12 (월) 18:00' },
      { label: '정보 수정 기한', value: '10/12 (월) 18:00' },
      { label: '전액 환불 기한', value: '10/18 (일) 23:59' },
    ],
    refund: refundRows('10/18 (일) 23:59'),
    faq: FAQ_ROWS,
    seller: {
      company: '(주)긍정러닝',
      name: '판매자 (주)긍정러닝 · 대표 이주최',
      businessNo: '사업자 345-67-89012 · 통신판매업 2026-서울영등포-0000',
      mailOrderNo: MAIL_ORDER_NOTICE,
    },
  },
  hyrox: {
    id: 'hyrox',
    title: 'POSITIVE HYROX',
    images: ['/mock/races/hyrox.png'],
    heroTitle: '「스웻 먼데이 접수 중」',
    heroTags: '#하이록스  #실내  #소규모',
    startsAt: '11/2 (월) 19:00 시작',
    place: '굿러너 하우스 0F · 하이록스',
    dDay: 'D-16',
    remaining: 18,
    capacity: 60,
    applied: 42,
    options: [{ id: 'single', name: '개인전', includes: '1인 · 기념 타월', price: 20000 }],
    schedule: [
      { label: '접수 기간', value: '10/1 (목) 10:00 ~ 10/28 (수) 19:00' },
      { label: '정보 수정 기한', value: '10/28 (수) 19:00' },
      { label: '전액 환불 기한', value: '10/28 (수) 23:59' },
    ],
    refund: refundRows('10/28 (수) 23:59'),
    faq: FAQ_ROWS,
    seller: {
      company: '굿러너컴퍼니',
      name: '판매자 굿러너컴퍼니 · 대표 최주최',
      businessNo: '사업자 456-78-90123 · 통신판매업 2026-서울마포-0000',
      mailOrderNo: MAIL_ORDER_NOTICE,
    },
  },
  daligi: {
    id: 'daligi',
    title: 'POSITIVE DALIGI',
    images: ['/mock/races/daligi.png'],
    heroTitle: '「함께 달리기 접수 중」',
    heroTags: '#5K  #저녁런  #함께뛰자',
    startsAt: '11/8 (일) 19:50 출발',
    place: '굿러너 하우스 1F · 5K',
    dDay: 'D-18',
    remaining: 40,
    capacity: 100,
    applied: 60,
    options: [{ id: '5k', name: '5K', includes: '1인 · 기념 음료', price: 15000 }],
    schedule: [
      { label: '접수 기간', value: '10/5 (월) 10:00 ~ 10/30 (금) 19:50' },
      { label: '정보 수정 기한', value: '10/30 (금) 19:50' },
      { label: '전액 환불 기한', value: '10/30 (금) 23:59' },
    ],
    refund: refundRows('10/30 (금) 23:59'),
    faq: FAQ_ROWS,
    seller: {
      company: '굿러너컴퍼니',
      name: '판매자 굿러너컴퍼니 · 대표 최주최',
      businessNo: '사업자 456-78-90123 · 통신판매업 2026-서울마포-0000',
      mailOrderNo: MAIL_ORDER_NOTICE,
    },
  },
}

/** 장바구니 · 참가 확정처럼 대회 id를 받지 않는 시안 화면이 쓰는 기본 대회 */
export const RACE_DETAIL = RACE_DETAILS.cookierun

export function getRaceDetail(raceId: string): RaceDetail | undefined {
  return Object.hasOwn(RACE_DETAILS, raceId) ? RACE_DETAILS[raceId] : undefined
}

export type RaceListItem = {
  id: string
  title: string
  image: string
  badge: { label: string; tone: 'brand' | 'warning' }
  meta: string
  price: string
  remaining: string
}

export const RACE_LIST: RaceListItem[] = [
  {
    id: 'cookierun',
    title: '2026 용감한 쿠키RUN in 서울',
    image: '/mock/races/cookierun.png',
    badge: { label: '접수중', tone: 'brand' },
    meta: '11/1 (일) · 서울숲 일대',
    price: '₩35,000~',
    remaining: '590자리 남음',
  },
  {
    id: 'jtbc',
    title: '2026 JTBC 서울마라톤',
    image: '/mock/races/jtbc.png',
    badge: { label: '마감 임박', tone: 'warning' },
    meta: '11/1 (일) · 광화문 → 잠실',
    price: '₩80,000~',
    remaining: '86자리 남음',
  },
  {
    id: 'youth10k',
    title: '2026 긍정 하프 마라톤 YOUTH 10K',
    image: '/mock/races/youth10k.png',
    badge: { label: '접수중', tone: 'brand' },
    meta: '10/25 (일) · 청년 부문',
    price: '₩30,000~',
    remaining: '420자리 남음',
  },
  {
    id: 'daligi',
    title: 'POSITIVE DALIGI',
    image: '/mock/races/daligi.png',
    badge: { label: '접수중', tone: 'brand' },
    meta: '11/8 (일) · 굿러너 하우스 1F',
    price: '₩15,000~',
    remaining: '40자리 남음',
  },
]
