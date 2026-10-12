import { SellerRaceChangePage } from '@/views/seller-race-change'

// ?preview=confirm 로 S09-1(대회 취소 확인 창)을 연 채로 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/seller/races/[raceId]/change'>) {
  const { preview } = await searchParams
  return <SellerRaceChangePage confirmOpen={preview === 'confirm'} />
}
