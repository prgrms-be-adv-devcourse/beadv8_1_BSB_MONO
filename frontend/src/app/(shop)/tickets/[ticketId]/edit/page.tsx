import { TicketEditPage } from '@/views/ticket-detail'

export default async function Page({ params }: PageProps<'/tickets/[ticketId]/edit'>) {
  const { ticketId } = await params
  return <TicketEditPage ticketId={ticketId} />
}
