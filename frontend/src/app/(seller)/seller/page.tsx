import { SellerDashboardPage } from '@/views/seller-dashboard'

// ?preview=loading|offline 로 판매자·관리자 공통 상태 화면을 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/seller'>) {
  const { preview } = await searchParams
  const state = preview === 'loading' || preview === 'offline' ? preview : undefined
  return <SellerDashboardPage state={state} />
}
