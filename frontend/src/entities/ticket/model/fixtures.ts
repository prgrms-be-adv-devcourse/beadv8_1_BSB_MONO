// TODO(api): 주문 · 참가권 API가 생기면 지운다. 시안 예시 값.

export type Ticket = {
  id: string
  raceTitle: string
  eyebrow: string
  bib: string
  course: string
  runner: string
  /** 띠에 깔 대회 이미지. 없으면 코발트 띠 */
  image?: string
  /** 카드 아래 한 줄 (대회일 · 환불 안내) */
  summary: string
  status: 'confirmed' | 'finished' | 'canceled'
}

export const UPCOMING_TICKETS: Ticket[] = [
  {
    id: 'R-20261005-0012',
    raceTitle: '2026 용감한 쿠키RUN in 서울',
    eyebrow: 'COOKIE RUN 2026',
    bib: '1024',
    course: '10K',
    runner: '김러너',
    image: '/mock/races/cookierun.png',
    summary: '11/1 (일) · 10/31까지 취소하면 ₩45,000 전액 환불',
    status: 'confirmed',
  },
  {
    id: 'R-20261002-0388',
    raceTitle: '2026 춘천 나이트런',
    eyebrow: 'CHUNCHEON 10K 2026',
    bib: '0388',
    course: '10K',
    runner: '김러너',
    summary: '12/13 (일) · 10/12까지 취소하면 ₩45,000 전액 환불',
    status: 'confirmed',
  },
]

export const PAST_TICKETS: Ticket[] = [
  {
    id: 'R-20260402-0101',
    raceTitle: '2026 봄 한강 10K',
    eyebrow: 'HANGANG 10K 2026',
    bib: '0101',
    course: '10K',
    runner: '김러너',
    summary: '4/12 (일) · 완주',
    status: 'finished',
  },
  {
    id: 'R-20251011-0777',
    raceTitle: '2025 가을 트레일런',
    eyebrow: 'AUTUMN TRAIL 2025',
    bib: '0777',
    course: '하프',
    runner: '김러너',
    summary: '11/22 (토) · 완주',
    status: 'finished',
  },
  {
    id: 'R-20250902-0520',
    raceTitle: '2025 부산 바다 10K',
    eyebrow: 'BUSAN SEA 10K 2025',
    bib: '0520',
    course: '10K',
    runner: '김러너',
    summary: '3/14 (토) · 참가 취소 · 전액 환불',
    status: 'canceled',
  },
]
