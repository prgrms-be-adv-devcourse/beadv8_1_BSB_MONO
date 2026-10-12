import { TicketDetailPage } from '@/views/ticket-detail'

// ?preview=confirm 로 M16-1(참가 취소 확인 창)을 연 채로 볼 수 있다
export default async function Page({ params, searchParams }: PageProps<'/tickets/[ticketId]'>) {
  const [{ ticketId }, { preview }] = await Promise.all([params, searchParams])
  return <TicketDetailPage ticketId={ticketId} confirmOpen={preview === 'confirm'} />
}
