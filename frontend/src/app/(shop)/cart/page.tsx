import { CartPage } from '@/views/cart'

// ?preview=empty 로 빈 장바구니를 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/cart'>) {
  const { preview } = await searchParams
  return <CartPage empty={preview === 'empty'} />
}
