import { notFound } from 'next/navigation'

import { RaceQueuePage } from '@/views/race-queue'
import { fetchRaceDetail } from '@/entities/race'

// 대기열 · 차례 기다리기(M05-Q1). /races/1/queue?option=1 처럼 줄을 선 종목을 붙여 온다.
// 입장 여부는 로그인이 필요해 화면(클라이언트)이 대기열 API로 묻는다.
// ?preview=admitted 로 M05-Q2(내 차례)를 시안의 남은 시간(09:43)으로 볼 수 있다.
const PREVIEW_REMAINING_SECONDS = 583

export default async function Page({ params, searchParams }: PageProps<'/races/[raceId]/queue'>) {
  const [{ raceId }, { option: optionId, preview }] = await Promise.all([params, searchParams])
  const race = await fetchRaceDetail(raceId)
  if (!race) notFound()
  // 신청 화면(M05)과 같이 접수 중인 대회의 남은 종목만 줄을 설 수 있다. 종목이 없으면 첫 남은 종목으로 선다.
  const open = race.options.filter((o) => !o.soldOut)
  const option = open.find((o) => o.id === optionId) ?? open[0]
  if (race.phase !== 'open' || !option) notFound()
  return (
    <RaceQueuePage
      raceId={race.id}
      optionId={option.id}
      previewRemainingSeconds={preview === 'admitted' ? PREVIEW_REMAINING_SECONDS : undefined}
    />
  )
}
