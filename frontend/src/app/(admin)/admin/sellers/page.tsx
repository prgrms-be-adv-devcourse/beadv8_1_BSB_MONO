import { AdminSellersPage } from '@/views/admin-sellers'

// ?preview=reject 로 A01-1(반려 사유 입력 창)을 연 채로 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/admin/sellers'>) {
  const { preview } = await searchParams
  return <AdminSellersPage rejectOpen={preview === 'reject'} />
}
