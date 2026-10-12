import { SellerRacesPage } from '@/views/seller-races'

// 대회 등록(S02) 뒤 ?registered=1 로 들어오면 등록 완료 토스트(S02-4)를 띄운다
export default async function Page({ searchParams }: PageProps<'/seller/races'>) {
  const { registered } = await searchParams
  return <SellerRacesPage registered={registered === '1'} />
}
