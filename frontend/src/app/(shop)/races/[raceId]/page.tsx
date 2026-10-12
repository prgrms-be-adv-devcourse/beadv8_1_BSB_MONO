import { RaceDetailPage } from '@/views/race-detail'

// ?preview=applied 로 M02-1(이미 신청함) 상태를 볼 수 있다. TODO(api): 신청 여부를 서버에서 받는다.
export default async function Page({ params, searchParams }: PageProps<'/races/[raceId]'>) {
  const [{ raceId }, { preview }] = await Promise.all([params, searchParams])
  return <RaceDetailPage raceId={raceId} alreadyApplied={preview === 'applied'} />
}
