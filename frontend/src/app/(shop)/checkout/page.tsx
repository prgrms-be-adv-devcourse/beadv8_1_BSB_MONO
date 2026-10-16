import { notFound } from 'next/navigation'

import { CheckoutPage, type CheckoutState } from '@/views/checkout'

const STATES: CheckoutState[] = ['offline', 'timeout', 'soldout', 'failed']

// /checkout?orderId=1 로 그 주문을 결제한다. 주문은 로그인이 필요해 화면(클라이언트)에서 불러온다.
// ?preview=offline|timeout|soldout|failed 로 M07의 다른 상태를 볼 수 있다
// ?result=timeout&raceId=1 은 대기열 입장 시간이 끝나 넘어온 시간 초과 화면(M07-1)이다
export default async function Page({ searchParams }: PageProps<'/checkout'>) {
  const { preview, result, orderId, raceId } = await searchParams
  const state = result === 'timeout' ? 'timeout' : (STATES.find((s) => s === preview) ?? 'default')
  const id = Number(orderId)
  if (state === 'default' && !(Number.isInteger(id) && id > 0)) notFound()
  const race = Number(raceId)
  return (
    <CheckoutPage
      state={state}
      orderId={state === 'default' ? id : undefined}
      raceId={Number.isInteger(race) && race > 0 ? race : undefined}
    />
  )
}
