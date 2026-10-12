import { notFound } from 'next/navigation'

import { RaceDetailPage } from '@/views/race-detail'
import { getRaceDetail } from '@/entities/race'

// ?preview=applied 로 M02-1(이미 신청함) 상태를 볼 수 있다. TODO(api): 신청 여부를 서버에서 받는다.
// ?option=5k 처럼 종목을 골라 둔 채로 열 수 있다(곧 마감 카드).
export default async function Page({ params, searchParams }: PageProps<'/races/[raceId]'>) {
  const [{ raceId }, { preview, option }] = await Promise.all([params, searchParams])
  const race = getRaceDetail(raceId)
  if (!race) notFound()
  return (
    <RaceDetailPage
      race={race}
      optionId={typeof option === 'string' ? option : undefined}
      alreadyApplied={preview === 'applied'}
    />
  )
}
