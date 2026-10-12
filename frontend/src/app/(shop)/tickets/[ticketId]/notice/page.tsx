import { RaceChangeNoticePage } from '@/views/ticket-detail'

export default async function Page({ params }: PageProps<'/tickets/[ticketId]/notice'>) {
  const { ticketId } = await params
  return <RaceChangeNoticePage ticketId={ticketId} />
}
