import { LoginPage } from '@/views/auth'

// ?preview=error 로 M03-E(로그인 실패)를 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/login'>) {
  const { preview } = await searchParams
  return <LoginPage failed={preview === 'error'} />
}
