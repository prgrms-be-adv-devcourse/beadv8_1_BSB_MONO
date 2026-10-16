import { DepositChargeReturnPage } from '@/views/deposit'

const text = (v: string | string[] | undefined) => (typeof v === 'string' ? v : undefined)

function positiveInt(v: string | string[] | undefined) {
  const n = Number(text(v))
  return Number.isInteger(n) && n > 0 ? n : undefined
}

// 토스 결제창이 돌려보내는 주소. successUrl은 ?result=success&paymentKey&orderId&amount,
// failUrl은 ?result=fail&code&message&orderId 를 붙여 온다. returnOrderId는 결제 중 충전일 때 우리가 붙인 주문 ID다.
export default async function Page({ searchParams }: PageProps<'/me/deposit/charge/return'>) {
  const params = await searchParams
  return (
    <DepositChargeReturnPage
      result={text(params.result)}
      paymentKey={text(params.paymentKey)}
      orderId={text(params.orderId)}
      amount={positiveInt(params.amount)}
      code={text(params.code)}
      returnOrderId={positiveInt(params.returnOrderId)}
    />
  )
}
