'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState, type ReactNode } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'

import { ConsoleCard } from '@/widgets/console-page'
import { useAuthStatus } from '@/features/auth'
import { memberApi, unwrapData, type memberModel } from '@/shared/api'
import { cn } from '@/shared/lib'
import {
  Alert,
  Badge,
  Button,
  buttonClassName,
  Checkbox,
  FOCUS_RING,
  Radio,
  RadioGroup,
  Select,
  Skeleton,
  TextField,
  type BadgeTone,
} from '@/shared/ui'

import {
  applyErrorMessage,
  isNoApplication,
  SELLER_API_ERRORS,
  toApplyRequest,
  toApplyStatus,
  type SellerApplyStatus,
} from '../model/seller-apply-api'
import {
  EMPTY_SELLER_APPLY_FORM,
  sellerApplyFormSchema,
  type SellerApplyFormValues,
} from '../model/seller-apply-form'

const BANKS = ['국민은행', '신한은행', '우리은행', '하나은행', '카카오뱅크', '토스뱅크'].map(
  (b) => ({
    value: b,
    label: b,
  }),
)

// 데스크톱 폼 화면의 본문: 위 48 · 아래 64, 가운데 고정 폭 칸
function FormBody({
  width,
  title,
  description,
  children,
}: {
  width: string
  title: string
  description?: string
  children: ReactNode
}) {
  return (
    <main className="flex justify-center pt-12 pb-16">
      <div className={cn('flex flex-col gap-6', width)}>
        <div className="flex flex-col gap-2 pb-2">
          <h1 className="text-heading-xl">{title}</h1>
          {description && <p className="text-body-m text-text-secondary">{description}</p>}
        </div>
        {children}
      </div>
    </main>
  )
}

// 피그마 V2 · S01 판매자 등록 신청
// 검사를 통과하면 판매자 신청 API(POST /api/v1/member/sellers)로 보내고, 성공하면 결과 화면으로 간다.
export function SellerApplyPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const apply = memberApi.useApply()
  const [formError, setFormError] = useState<string>()
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<SellerApplyFormValues>({
    resolver: zodResolver(sellerApplyFormSchema),
    defaultValues: EMPTY_SELLER_APPLY_FORM,
  })
  const business = useWatch({ control, name: 'business' })
  const agreed = useWatch({ control, name: 'agreed' })

  const onSubmit = handleSubmit((values) => {
    setFormError(undefined)
    apply.mutate(
      { data: toApplyRequest(values) },
      {
        onSuccess: (response) => {
          // 응답이 방금 낸 신청이라 결과 화면이 다시 부르지 않고 바로 쓴다.
          queryClient.setQueryData(memberApi.getFindMineQueryKey(), response)
          router.push('/seller-apply/result')
        },
        onError: (error) => setFormError(applyErrorMessage(error)),
      },
    )
  })

  return (
    <form onSubmit={onSubmit} noValidate>
      <FormBody
        width="w-160"
        title="판매자 등록 신청"
        description="승인되면 대회를 등록할 수 있어요. 결과는 알림으로 알려 드려요."
      >
        <ConsoleCard title="판매자 정보">
          <Controller
            control={control}
            name="business"
            render={({ field }) => (
              <RadioGroup
                value={field.value}
                onValueChange={(value) => field.onChange(value)}
                className="flex-row gap-6"
              >
                <Radio value="business" label="사업자예요" />
                <Radio value="personal" label="사업자가 아니에요" />
              </RadioGroup>
            )}
          />
          {business === 'business' ? (
            <>
              <div className="flex gap-4">
                <TextField
                  label="사업자등록번호"
                  className="flex-1"
                  error={errors.businessNumber?.message}
                  {...register('businessNumber')}
                />
                <TextField
                  label="상호"
                  className="flex-1"
                  error={errors.businessName?.message}
                  {...register('businessName')}
                />
              </div>
              <TextField
                label="대표자"
                error={errors.representative?.message}
                {...register('representative')}
              />
            </>
          ) : (
            <div className="flex gap-4">
              <TextField
                label="성명"
                className="flex-1"
                error={errors.name?.message}
                {...register('name')}
              />
              <TextField
                label="연락처"
                type="tel"
                className="flex-1"
                error={errors.phone?.message}
                {...register('phone')}
              />
            </div>
          )}
          <p className="text-caption text-text-tertiary">사업자가 아니면 성명 · 연락처만 받아요.</p>
        </ConsoleCard>

        <ConsoleCard title="정산 계좌">
          <div className="flex gap-4">
            <Controller
              control={control}
              name="bank"
              render={({ field }) => (
                <Select
                  label="은행"
                  items={BANKS}
                  value={field.value || null}
                  onValueChange={(bank) => field.onChange(bank ?? '')}
                  className="flex-1"
                />
              )}
            />
            <TextField label="예금주" className="flex-1" {...register('accountHolder')} />
          </div>
          <TextField label="계좌번호" inputMode="numeric" {...register('accountNumber')} />
          <Alert
            tone="warning"
            title="정산 계좌가 없으면 정산이 지급보류돼요"
            description="나중에 등록해도 되지만, 등록 전까지는 정산금을 받을 수 없어요."
          />
        </ConsoleCard>

        <ConsoleCard title="약관 동의">
          <div className="flex items-center justify-between">
            <Controller
              control={control}
              name="agreed"
              render={({ field }) => (
                <Checkbox
                  checked={field.value}
                  onCheckedChange={field.onChange}
                  label="[필수] 판매자 이용약관에 동의해요"
                />
              )}
            />
            <Link
              href="/terms/service"
              className={cn('rounded-sm text-body-s text-text-tertiary underline', FOCUS_RING)}
            >
              보기
            </Link>
          </div>
          <p className="text-caption text-text-tertiary">동의한 약관 버전과 시각을 기록해요.</p>
        </ConsoleCard>

        <Alert
          title="크루런은 통신판매중개자예요"
          description="거래 당사자는 판매자님이에요. 판매자 정보는 대회 상세 화면에서 구매자에게 보여요."
        />

        {formError && <Alert tone="danger" title={formError} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" size="l">
            임시 저장
          </Button>
          <Button type="submit" size="l" disabled={!agreed} loading={apply.isPending}>
            신청하기
          </Button>
        </div>
      </FormBody>
    </form>
  )
}

type ResultCard = {
  tone: BadgeTone
  badge: string
  title: string
  description: string
  actions: ReactNode
}

// 반려 사유는 서버가 준 값(rejectReason)을 그대로 보여 준다.
function resultCard(seller: memberModel.SellerDto): ResultCard {
  const status: SellerApplyStatus = toApplyStatus(seller.status)
  if (status === 'rejected') {
    return {
      tone: 'danger',
      badge: '반려',
      title: '신청이 반려됐어요',
      description: seller.rejectReason ? `사유: ${seller.rejectReason}` : '',
      actions: (
        <Link href="/seller-apply" className={buttonClassName()}>
          고쳐서 다시 신청하기
        </Link>
      ),
    }
  }
  if (status === 'approved') {
    return {
      tone: 'success',
      badge: '승인',
      title: '판매자로 승인됐어요',
      description:
        '이제 대회를 등록할 수 있어요. 정산 계좌가 없으면 정산금 지급이 보류되니 먼저 확인해 주세요.',
      actions: (
        <>
          <Link href="/seller" className={buttonClassName()}>
            판매자 홈으로
          </Link>
          <Link href="/seller/settings" className={buttonClassName({ variant: 'secondary' })}>
            정산 계좌 확인
          </Link>
        </>
      ),
    }
  }
  return {
    tone: 'neutral',
    badge: '심사 중',
    title: '관리자가 신청 내용을 확인하고 있어요',
    description: '결과가 나오면 알림(M25)으로 알려 드려요. 심사 중에는 신청 내용을 고칠 수 없어요.',
    // TODO(design): 신청 내용을 다시 보는 화면은 시안에 없다. 지금은 신청 폼으로 연결한다.
    actions: (
      <Link href="/seller-apply" className={buttonClassName({ variant: 'secondary' })}>
        신청 내용 보기
      </Link>
    ),
  }
}

// 피그마 V2 · S01-1 판매자 등록 신청 · 결과 (심사 중 · 반려 · 승인 중 내 신청 상태에 맞는 카드 하나)
// 내 신청 조회(GET /api/v1/member/sellers/me)로 상태를 가져온다. 신청 내역이 없으면(404 SELLER_NOT_FOUND) 신청하러 가도록 안내한다.
export function SellerApplyResultPage() {
  const mine = memberApi.useFindMine({
    query: {
      select: unwrapData,
      enabled: useAuthStatus() === 'authenticated',
      // 신청 내역이 없는 404는 다시 불러도 같아서 재시도하지 않는다.
      retry: false,
    },
  })

  return (
    <FormBody width="w-180" title="판매자 등록 신청 결과">
      {mine.data ? (
        <ResultSection card={resultCard(mine.data)} />
      ) : mine.isError ? (
        isNoApplication(mine.error) ? (
          <section className="flex flex-col items-start gap-3 rounded-lg bg-bg-surface p-5 inset-ring inset-ring-border-default">
            <h2 className="text-heading-s">아직 판매자 신청을 하지 않았어요</h2>
            <Link href="/seller-apply" className={buttonClassName()}>
              판매자 신청하러 가기
            </Link>
          </section>
        ) : (
          <Alert tone="danger" title={SELLER_API_ERRORS.loadFailed} />
        )
      ) : (
        <Skeleton shape="rect" className="h-40 w-full" />
      )}
    </FormBody>
  )
}

function ResultSection({ card }: { card: ResultCard }) {
  return (
    <section className="flex flex-col items-start gap-3 rounded-lg bg-bg-surface p-5 inset-ring inset-ring-border-default">
      <Badge tone={card.tone}>{card.badge}</Badge>
      <h2 className="text-heading-s">{card.title}</h2>
      {card.description && <p className="text-body-s text-text-secondary">{card.description}</p>}
      <div className="flex gap-2">{card.actions}</div>
    </section>
  )
}
