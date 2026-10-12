import { HomePage } from '@/views/home'

// ?preview=loading|offline 로 홈 로딩 · 공통 연결 끊김 화면을 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/'>) {
  const { preview } = await searchParams
  const state = preview === 'loading' || preview === 'offline' ? preview : undefined
  return <HomePage state={state} />
}
