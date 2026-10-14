import { HomePage } from '@/views/home'
import { fetchRacePage, RaceSort, toClosingSoonCards, toOpenRaceCard } from '@/entities/race'

/** 곧 마감: 마감 임박순 대회의 종목 카드 */
const CLOSING_SOON_RACES = 3
const CLOSING_SOON_CARDS = 6
/** 지금 접수 중: 최신 등록순 4개 (2열) */
const OPEN_RACES = 4

// ?preview=loading|offline 로 홈 로딩 · 공통 연결 끊김 화면을 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/'>) {
  const { preview } = await searchParams
  if (preview === 'loading' || preview === 'offline') {
    return <HomePage state={preview} closingSoon={[]} openRaces={[]} />
  }

  const lists = await Promise.all([
    fetchRacePage({ sort: RaceSort.DEADLINE, page: 0, size: CLOSING_SOON_RACES }),
    fetchRacePage({ sort: RaceSort.LATEST, page: 0, size: OPEN_RACES }),
  ]).catch(() => null)
  // 백엔드에 닿지 못하면 공통 연결 끊김 화면
  if (!lists) return <HomePage state="offline" closingSoon={[]} openRaces={[]} />

  const [closing, open] = lists
  return (
    <HomePage
      closingSoon={(closing.races ?? []).flatMap(toClosingSoonCards).slice(0, CLOSING_SOON_CARDS)}
      openRaces={(open.races ?? []).map(toOpenRaceCard)}
    />
  )
}
