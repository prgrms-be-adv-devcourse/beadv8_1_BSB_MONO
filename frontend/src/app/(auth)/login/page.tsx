import { LoginPage } from '@/views/auth'

// ?preview=error 로 M03-E(로그인 실패)를 볼 수 있다
// ?next=<경로> 는 로그인 뒤 돌아갈 화면 (앱 내부 경로만 쓰고 아니면 홈)
export default async function Page({ searchParams }: PageProps<'/login'>) {
  const { preview, next } = await searchParams
  return (
    <LoginPage failed={preview === 'error'} next={typeof next === 'string' ? next : undefined} />
  )
}
