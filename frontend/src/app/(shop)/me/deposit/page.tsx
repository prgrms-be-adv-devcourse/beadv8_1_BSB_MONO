import { DepositPage } from '@/views/deposit'

// ?preview=refunding 로 M17-1(돌려받는 중)을 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/me/deposit'>) {
  const { preview } = await searchParams
  return <DepositPage refunding={preview === 'refunding'} />
}
