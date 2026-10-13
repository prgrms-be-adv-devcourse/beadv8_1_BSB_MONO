import { SignupAccountPage } from '@/views/auth'

// 들어오면 약관 동의(M04-0) 바텀시트부터 뜬다. ?preview=error 로 M04-1E(입력 오류)를 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/signup'>) {
  const { preview } = await searchParams
  return <SignupAccountPage invalid={preview === 'error'} />
}
