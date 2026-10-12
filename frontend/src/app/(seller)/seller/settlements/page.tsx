import { SellerSettlementsPage } from '@/views/seller-settlements'

// ?tab=holds 로 S10(지급보류 내역)을 바로 연다
export default async function Page({ searchParams }: PageProps<'/seller/settlements'>) {
  const { tab } = await searchParams
  return <SellerSettlementsPage initialTab={tab === 'holds' ? 'holds' : 'monthly'} />
}
