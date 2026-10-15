import { notFound } from 'next/navigation'

import { CheckoutCompletePage } from '@/views/checkout-complete'

// /checkout/complete?orderId=1 로 결제한 주문을 보여 준다
export default async function Page({ searchParams }: PageProps<'/checkout/complete'>) {
  const { orderId } = await searchParams
  const id = Number(orderId)
  if (!Number.isInteger(id) || id <= 0) notFound()
  return <CheckoutCompletePage orderId={id} />
}
