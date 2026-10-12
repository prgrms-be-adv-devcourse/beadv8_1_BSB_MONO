import { RaceApplyPage } from '@/views/race-apply'

export default async function Page({ searchParams }: PageProps<'/races/[raceId]/apply'>) {
  const { option } = await searchParams
  return <RaceApplyPage optionId={typeof option === 'string' ? option : undefined} />
}
