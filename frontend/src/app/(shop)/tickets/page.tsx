import { MyTicketsPage } from '@/views/my-tickets'

// ?preview=loading|empty 로 M15의 로딩 · 없음 상태를 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/tickets'>) {
  const { preview } = await searchParams
  const state = preview === 'loading' || preview === 'empty' ? preview : undefined
  return <MyTicketsPage state={state} />
}
