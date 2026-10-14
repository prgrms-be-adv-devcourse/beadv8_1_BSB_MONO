import { notFound } from 'next/navigation'

import { RaceApplyPage } from '@/views/race-apply'
import { fetchRaceDetail } from '@/entities/race'

// 대회 · 종목 정보는 상품 API에서 받는다. 신청 제출을 주문 API에 붙이는 것은 PRO-149.
export default async function Page({ params, searchParams }: PageProps<'/races/[raceId]/apply'>) {
  const [{ raceId }, { option }] = await Promise.all([params, searchParams])
  const race = await fetchRaceDetail(raceId)
  if (!race) notFound()
  return <RaceApplyPage race={race} optionId={typeof option === 'string' ? option : undefined} />
}
