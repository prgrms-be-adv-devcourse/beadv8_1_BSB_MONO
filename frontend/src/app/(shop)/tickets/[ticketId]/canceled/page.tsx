import { TicketCanceledPage } from '@/views/ticket-detail'

export default async function Page({ params }: PageProps<'/tickets/[ticketId]/canceled'>) {
  const { ticketId } = await params
  return <TicketCanceledPage ticketId={ticketId} />
}
