import { notFound } from 'next/navigation'

import { RaceApplyPage } from '@/views/race-apply'
import { fetchRaceDetail } from '@/entities/race'

// 대회 · 종목 정보는 상품 API에서 받는다. 참가자 정보 미리 채우기와 신청 제출은 로그인이 필요해 화면(클라이언트)에서 부른다.
export default async function Page({ params, searchParams }: PageProps<'/races/[raceId]/apply'>) {
  const [{ raceId }, { option: optionId }] = await Promise.all([params, searchParams])
  const race = await fetchRaceDetail(raceId)
  if (!race) notFound()
  // 주소로 바로 들어와도 접수 중인 대회의 남은 종목만 고른다. 고를 종목이 없으면 신청할 수 없다.
  const open = race.options.filter((o) => !o.soldOut)
  const option = open.find((o) => o.id === optionId) ?? open[0]
  if (race.phase !== 'open' || !option) notFound()
  return <RaceApplyPage race={race} option={option} />
}
