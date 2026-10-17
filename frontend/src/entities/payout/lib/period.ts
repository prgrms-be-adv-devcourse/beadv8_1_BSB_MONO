import { fromIsoDate, toIsoDate } from '@/shared/lib'

// 판매자 정산 조회 기간 (백엔드 PayoutQueryPeriod와 같은 한도)
// - 건별 정산 · 지급보류: 한 번에 1개월 미만 (끝 날 < 시작 날 + 1개월). 예: 9/16 ~ 10/15
// - 월별 정산: 정산 월로 최대 12개월

export type DetailPeriod = {
  /** Select 값. 'YYYY-MM-DD~YYYY-MM-DD' */
  value: string
  /** '9/16 ~ 10/15' */
  label: string
  from: string
  to: string
}

/** 'YYYY-MM-DD'(또는 날짜시간 앞부분) → '10/20' */
export function formatMonthDay(isoDate: string): string {
  const date = fromIsoDate(isoDate.slice(0, 10))
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

/** 'YYYY-MM-DD' → '금' */
export function formatWeekday(isoDate: string): string {
  return WEEKDAYS[fromIsoDate(isoDate).getDay()]
}

/** 자바 LocalDate.plusMonths(1)과 같게: 다음 달에 같은 날이 없으면 그 달 마지막 날 */
function plusOneMonth(date: Date): Date {
  const lastDay = new Date(date.getFullYear(), date.getMonth() + 2, 0).getDate()
  return new Date(date.getFullYear(), date.getMonth() + 1, Math.min(date.getDate(), lastDay))
}

/**
 * 오늘로 끝나는 1개월 기간부터 한 달씩 앞으로 count개.
 * 10/15가 오늘이면 9/16 ~ 10/15, 8/16 ~ 9/15, ...
 */
export function detailPeriods(today: Date, count = 3): DetailPeriod[] {
  const periods: DetailPeriod[] = []
  let to = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  for (let i = 0; i < count; i++) {
    // 끝 날의 한 달 전 다음 날. 3/31처럼 한 달 전에 같은 날이 없으면 달 끝으로 맞춘다
    const monthBefore = new Date(to.getFullYear(), to.getMonth() - 1, 1)
    const lastDay = new Date(monthBefore.getFullYear(), monthBefore.getMonth() + 1, 0).getDate()
    monthBefore.setDate(Math.min(to.getDate(), lastDay))
    let from = new Date(
      monthBefore.getFullYear(),
      monthBefore.getMonth(),
      monthBefore.getDate() + 1,
    )
    // 백엔드는 시작 날 + 1개월이 달 끝으로 잘리므로(3/31 + 1개월 = 4/30) 끝 날이 그보다 이르도록 시작을 늦춘다
    while (to.getTime() >= plusOneMonth(from).getTime()) {
      from = new Date(from.getFullYear(), from.getMonth(), from.getDate() + 1)
    }

    const fromIso = toIsoDate(from)
    const toIso = toIsoDate(to)
    periods.push({
      value: `${fromIso}~${toIso}`,
      label: `${formatMonthDay(fromIso)} ~ ${formatMonthDay(toIso)}`,
      from: fromIso,
      to: toIso,
    })
    to = new Date(from.getFullYear(), from.getMonth(), from.getDate() - 1)
  }
  return periods
}

/** 이번 달까지 12개월 (yyyy-MM). 월별 정산 조회 범위 */
export function recentPayoutMonths(today: Date): { fromMonth: string; toMonth: string } {
  const month = (date: Date) => toIsoDate(date).slice(0, 7)
  return {
    fromMonth: month(new Date(today.getFullYear(), today.getMonth() - 11, 1)),
    toMonth: month(new Date(today.getFullYear(), today.getMonth(), 1)),
  }
}
