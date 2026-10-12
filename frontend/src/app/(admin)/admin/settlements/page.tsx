import { AdminSettlementsPage } from '@/views/admin-settlements'

// ?preview=confirm 로 A02-1(전체 지급 완료 확인 창)을 연 채로 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/admin/settlements'>) {
  const { preview } = await searchParams
  return <AdminSettlementsPage confirmOpen={preview === 'confirm'} />
}
