'use client'

import { cn } from '@/shared/lib'

import { FOCUS_RING } from '../focus-ring'

export type SegmentedItem<T extends string> = { value: T; label: string }

export type SegmentedProps<T extends string> = {
  items: SegmentedItem<T>[]
  value: T
  onChange: (value: T) => void
  label: string
  className?: string
}

// 피그마 화면의 묶음 탭: 회색(bg-segment) 판 위에 고른 칸만 흰 바탕으로 떠 있다. 칸 높이 40.
export function Segmented<T extends string>({
  items,
  value,
  onChange,
  label,
  className,
}: SegmentedProps<T>) {
  return (
    <div
      role="tablist"
      aria-label={label}
      className={cn('flex gap-1 rounded-md bg-bg-segment p-1', className)}
    >
      {items.map((item) => {
        const selected = item.value === value
        return (
          <button
            key={item.value}
            type="button"
            role="tab"
            aria-selected={selected}
            onClick={() => onChange(item.value)}
            className={cn(
              'h-10 min-w-0 flex-1 truncate rounded-[8px] px-2 text-label-l',
              selected
                ? 'bg-bg-surface font-bold text-text-primary shadow-sm'
                : 'text-text-tertiary',
              FOCUS_RING,
            )}
          >
            {item.label}
          </button>
        )
      })}
    </div>
  )
}
