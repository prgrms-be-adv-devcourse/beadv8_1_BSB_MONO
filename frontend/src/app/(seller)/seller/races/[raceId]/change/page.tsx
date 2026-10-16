import { notFound } from 'next/navigation'

import { SellerRaceChangePage } from '@/views/seller-race-change'

// ?preview=confirm 로 S09-1(대회 취소 확인 창)을 연 채로 볼 수 있다
export default async function Page({
  params,
  searchParams,
}: PageProps<'/seller/races/[raceId]/change'>) {
  const [{ raceId }, { preview }] = await Promise.all([params, searchParams])
  const id = Number(raceId)
  if (!Number.isInteger(id) || id <= 0) notFound()
  return <SellerRaceChangePage raceId={id} confirmOpen={preview === 'confirm'} />
}
