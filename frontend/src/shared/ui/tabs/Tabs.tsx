'use client'

import { Tabs as BaseTabs } from '@base-ui/react/tabs'
import type { ReactNode } from 'react'

import { cn } from '@/shared/lib'

import { FOCUS_RING } from '../focus-ring'

export const Tabs = BaseTabs.Root
export const TabPanel = BaseTabs.Panel

export type TabBarProps = {
  /** fill: 모바일에서 탭이 폭을 똑같이 나눈다. hug: 태블릿·데스크톱에서 왼쪽부터 내용 폭만큼 나열한다. */
  layout?: 'fill' | 'hug'
  children: ReactNode
  className?: string
}

// 탭 묶음 막대. 회색(bg-segment) 판 안에 탭이 4 간격으로 놓인다. 높이 48 = 여백 4 + 탭 40 + 여백 4.
export function TabBar({ layout = 'fill', children, className }: TabBarProps) {
  return (
    <BaseTabs.List
      className={cn(
        'flex gap-1 rounded-md bg-bg-segment p-1',
        layout === 'fill' && '*:flex-1',
        className,
      )}
    >
      {children}
    </BaseTabs.List>
  )
}

export type TabProps = {
  value: string
  children: ReactNode
  /** 개수(예: 후기 128) */
  count?: ReactNode
  disabled?: boolean
}

// 탭 하나. 높이 40, 고른 탭만 흰 바탕으로 떠 있고 글자가 굵어진다(Label/L Strong).
export function Tab({ value, children, count, disabled }: TabProps) {
  return (
    <BaseTabs.Tab
      value={value}
      disabled={disabled}
      className={cn(
        'group flex h-10 min-w-0 items-center justify-center gap-1 rounded-[8px] px-4 text-label-l text-text-tertiary',
        'data-active:bg-bg-surface data-active:font-bold data-active:text-text-primary data-active:shadow-sm',
        'data-disabled:text-text-disabled',
        FOCUS_RING,
      )}
    >
      <span className="truncate whitespace-nowrap">{children}</span>
      {count !== undefined && (
        <span className="text-label-s group-data-active:text-text-brand group-data-disabled:text-text-disabled">
          {count}
        </span>
      )}
    </BaseTabs.Tab>
  )
}
