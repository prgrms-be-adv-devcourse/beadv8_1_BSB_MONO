'use client'

import type { Route } from 'next'
import Link from 'next/link'
import type { ReactNode } from 'react'

import { AppHeader } from '@/widgets/app-header'
import { BottomNav } from '@/widgets/bottom-nav'
import { useLogout } from '@/features/auth'
import { useMyBalance } from '@/entities/wallet'
import { cn, formatWon } from '@/shared/lib'
import { buttonClassName, FOCUS_RING, Icon, KeyValue, Switch, type IconName } from '@/shared/ui'

// 피그마 V2 · M30 마이 · 설정 (+ M30-2 판매자 승인 후)
// TODO(api): 회원 정보 · 알림 설정 · 판매자 상태를 불러온다
export function MyPage({ seller = false }: { seller?: boolean }) {
  const logout = useLogout()
  const { balance } = useMyBalance()
  return (
    <>
      <AppHeader />
      <main className="flex flex-1 flex-col gap-4 px-4 py-6">
        <div className="flex items-center gap-3">
          <span className="flex size-10 items-center justify-center rounded-full bg-bg-brand-subtle text-label-m text-text-brand">
            달
          </span>
          <div className="flex min-w-0 flex-1 flex-col gap-0.5">
            <p className="text-heading-m">달리는곰</p>
            <p className="text-body-s text-text-secondary">runner@example.com</p>
          </div>
          <Link href="/me/edit" className={buttonClassName({ variant: 'secondary', size: 's' })}>
            닉네임 수정
          </Link>
        </div>

        <section className="flex flex-col gap-2 rounded-lg bg-bg-subtle p-4">
          <KeyValue label="이름" value="김러너" />
          <KeyValue label="휴대폰" value="010-****-5678" />
          <KeyValue label="생년월일" value="1995.03.02" />
          <p className="text-caption text-text-secondary">
            가입할 때 직접 입력한 정보예요. 바뀌면 직접 고칠 수 있어요.
          </p>
          <Link
            href="/me/edit"
            className={buttonClassName({
              variant: 'secondary',
              size: 's',
              className: 'self-start',
            })}
          >
            정보 수정
          </Link>
        </section>

        <MenuCard>
          <MenuRow href="/tickets" icon="Calendar" label="내 참가 대회" value="2개" />
          <MenuRow
            href="/me/deposit"
            icon="Package"
            label="예치금"
            value={balance === undefined ? '-' : formatWon(balance)}
          />
        </MenuCard>

        <h2 className="pt-2 text-label-strong">알림 설정</h2>
        <section className="flex flex-col gap-4 rounded-lg bg-bg-surface px-4 py-4 shadow-sm inset-ring inset-ring-border-default">
          <SwitchRow
            label="마케팅 정보 받기"
            description="2025.3.2 동의 · 2년마다 다시 여쭤봐요"
            defaultChecked
          />
          <SwitchRow label="밤 9시~아침 8시 광고 받기" description="마케팅 동의와 따로 받아요" />
        </section>

        <MenuCard>
          {seller ? (
            <MenuRow
              href="/seller"
              icon="Dashboard"
              label="판매자 센터"
              description="대회 등록 · 판매 현황 · 정산을 관리해요"
            />
          ) : (
            <MenuRow
              href="/seller-apply/result"
              icon="Dashboard"
              label="판매자 등록 신청"
              description="대회를 열고 참가권을 팔 수 있어요"
            />
          )}
        </MenuCard>

        <section className="flex flex-col rounded-lg bg-bg-subtle px-4 pt-4 pb-2">
          <h2 className="pb-2 text-label-strong">계정 관리</h2>
          <MenuRow
            onClick={() => logout.mutate()}
            disabled={logout.isPending}
            icon="LogOut"
            label="로그아웃"
          />
          <MenuRow
            href="/me/withdraw"
            icon="Settings"
            label="계정 설정"
            description="계정과 개인정보 설정을 관리할 수 있어요"
          />
        </section>
      </main>
      <BottomNav />
    </>
  )
}

function MenuCard({ children }: { children: ReactNode }) {
  return (
    <section className="flex flex-col rounded-lg bg-bg-surface px-4 py-2 shadow-sm inset-ring inset-ring-border-default">
      {children}
    </section>
  )
}

type MenuRowProps = {
  icon: IconName
  label: string
  value?: string
  description?: string
} & ({ href: Route } | { onClick: () => void; disabled?: boolean })

// 다른 화면으로 가는 줄은 링크, 그 자리에서 실행하는 줄(로그아웃)은 같은 모양의 버튼
function MenuRow({ icon, label, value, description, ...action }: MenuRowProps) {
  const content = (
    <>
      <Icon name={icon} className="size-5" />
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="text-label-m">{label}</span>
        {description && <span className="text-caption text-text-secondary">{description}</span>}
      </span>
      {value && <span className="text-label-m">{value}</span>}
      <Icon name="ChevronRight" className="size-5" />
    </>
  )
  const className = cn('flex items-center gap-3 rounded-md py-3', FOCUS_RING)
  if ('href' in action) {
    return (
      <Link href={action.href} className={className}>
        {content}
      </Link>
    )
  }
  return (
    <button
      type="button"
      onClick={action.onClick}
      disabled={action.disabled}
      className={cn(className, 'w-full text-left')}
    >
      {content}
    </button>
  )
}

function SwitchRow({
  label,
  description,
  defaultChecked,
}: {
  label: string
  description: string
  defaultChecked?: boolean
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div className="flex flex-col gap-0.5">
        <span className="text-label-m">{label}</span>
        <span className="text-caption text-text-secondary">{description}</span>
      </div>
      <Switch aria-label={label} defaultChecked={defaultChecked} />
    </div>
  )
}
