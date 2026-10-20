'use client'

import { ConsoleCard, ConsolePage } from '@/widgets/console-page'
import { useAuthStatus } from '@/features/auth'
import { formatSellerDate, SELLER_STATUS, SELLER_TYPE_LABEL } from '@/entities/member'
import { memberApi, unwrapData, type memberModel } from '@/shared/api'
import { Alert, Badge, Skeleton } from '@/shared/ui'

// 피그마 V2 · S11 판매자 설정 (세미: 정산 계좌는 준비 중 안내)
// 판매자 정보는 내 신청 조회(GET /api/v1/member/sellers/me)로 보여 준다. 바꾸려면 관리자 재확인이 필요해 보기 전용이다.
export function SellerSettingsPage() {
  const mine = memberApi.useFindMine({
    query: { select: unwrapData, enabled: useAuthStatus() === 'authenticated' },
  })

  return (
    <ConsolePage title="설정" description="판매자 정보와 정산 계좌를 확인해요.">
      {mine.data ? (
        <SellerInfoCard seller={mine.data} />
      ) : mine.isError ? (
        <Alert
          tone="danger"
          title="판매자 정보를 불러오지 못했어요"
          description="잠시 뒤 다시 시도해 주세요."
        />
      ) : (
        <Skeleton className="h-70 w-full" />
      )}

      {/* TODO(api): cash 서비스의 판매자 정산 계좌 API가 생기면 S11-1(계좌 입력 · 저장)로 바꾼다 */}
      <ConsoleCard title="정산 계좌">
        <Alert
          tone="warning"
          title="정산 계좌 등록은 준비 중이에요"
          description="등록 기능이 열리면 여기에서 등록할 수 있어요. 등록 전까지는 정산금 지급이 보류돼요."
        />
      </ConsoleCard>
    </ConsolePage>
  )
}

function SellerInfoCard({ seller }: { seller: memberModel.SellerDto }) {
  const status = SELLER_STATUS[seller.status]
  const business = seller.sellerType === 'BUSINESS'
  const fields: [string, string][] = [
    ['판매자 유형', SELLER_TYPE_LABEL[seller.sellerType]],
    ...(business
      ? ([
          ['사업자등록번호', seller.businessNumber ?? '-'],
          ['상호', seller.businessName ?? '-'],
          ['대표자', seller.representativeName],
        ] as [string, string][])
      : ([['성명', seller.representativeName]] as [string, string][])),
    seller.status === 'APPROVED' && seller.reviewedAt
      ? ['승인일', formatSellerDate(seller.reviewedAt)]
      : ['신청일', formatSellerDate(seller.requestedAt)],
  ]

  return (
    <ConsoleCard title="판매자 정보" aside={<Badge tone={status.tone}>{status.label}</Badge>}>
      <dl className="grid grid-cols-2 gap-x-4 gap-y-4">
        {fields.map(([label, value]) => (
          <div key={label} className="flex flex-col gap-0.5">
            <dt className="text-caption text-text-secondary">{label}</dt>
            <dd className="text-body-s text-text-primary">{value}</dd>
          </div>
        ))}
      </dl>
      <p className="text-caption text-text-tertiary">
        판매자 정보를 바꾸려면 관리자 확인이 필요해요.
      </p>
    </ConsoleCard>
  )
}
