import { NotificationsPage } from '@/views/notifications'

// ?preview=empty 로 알림 없음 화면을 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/notifications'>) {
  const { preview } = await searchParams
  return <NotificationsPage empty={preview === 'empty'} />
}
