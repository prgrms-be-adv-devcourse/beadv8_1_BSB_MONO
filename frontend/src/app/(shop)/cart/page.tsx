import { CartPage } from '@/views/cart'

// ?preview=empty 로 빈 장바구니를 볼 수 있다
// ?preview=queue 로 M06-Q(대기열 입장 전 결제 창)를 연 채로 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/cart'>) {
  const { preview } = await searchParams
  return <CartPage empty={preview === 'empty'} queueDialogOpen={preview === 'queue'} />
}
