import { CheckoutPage, type CheckoutState } from '@/views/checkout'

const STATES: CheckoutState[] = ['offline', 'timeout', 'soldout', 'failed']

// ?preview=offline|timeout|soldout|failed 로 M07의 다른 상태를 볼 수 있다
export default async function Page({ searchParams }: PageProps<'/checkout'>) {
  const { preview } = await searchParams
  const state = STATES.find((s) => s === preview) ?? 'default'
  return <CheckoutPage state={state} />
}
