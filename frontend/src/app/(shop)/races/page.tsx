import { RaceListPage, type RaceListState } from '@/views/race-list'

const STATES: RaceListState[] = ['loading', 'empty', 'menu']

// ?preview=loading|empty|menu 로 M18의 로딩 · 결과 없음 · 더보기 메뉴를 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/races'>) {
  const { preview } = await searchParams
  return <RaceListPage state={STATES.find((s) => s === preview)} />
}
