import { SellerApplyResultPage, type SellerApplyStatus } from '@/views/seller-apply'

const STATUSES: SellerApplyStatus[] = ['pending', 'rejected', 'approved']

// ?status=pending|rejected|approved 로 신청 상태별 카드를 미리 본다. 없으면 심사 중.
// TODO(api): PRO-129 실제 신청 상태 조회로 바꾼다
export default async function Page({ searchParams }: PageProps<'/seller-apply/result'>) {
  const { status } = await searchParams
  const value = STATUSES.find((s) => s === status)
  return <SellerApplyResultPage status={value} />
}
