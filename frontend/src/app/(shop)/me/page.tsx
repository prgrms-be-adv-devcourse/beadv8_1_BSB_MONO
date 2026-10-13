import { MyPage } from '@/views/my-page'

// ?preview=seller 로 M30-2(판매자 승인 후)를 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/me'>) {
  const { preview } = await searchParams
  return <MyPage seller={preview === 'seller'} />
}
