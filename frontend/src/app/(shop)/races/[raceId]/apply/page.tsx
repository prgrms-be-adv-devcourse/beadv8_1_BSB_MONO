import { notFound } from 'next/navigation'

import { RaceApplyPage } from '@/views/race-apply'
import { getRaceDetail } from '@/entities/race'

export default async function Page({ params, searchParams }: PageProps<'/races/[raceId]/apply'>) {
  const [{ raceId }, { option }] = await Promise.all([params, searchParams])
  const race = getRaceDetail(raceId)
  if (!race) notFound()
  return <RaceApplyPage race={race} optionId={typeof option === 'string' ? option : undefined} />
}
