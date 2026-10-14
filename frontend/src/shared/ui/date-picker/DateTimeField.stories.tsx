import type { Meta, StoryObj } from '@storybook/nextjs-vite'
import { isBefore, startOfToday } from 'date-fns'
import { fn } from 'storybook/test'

import { DateTimeField } from './DateTimeField'

// DateTimeFieldProps는 mode별 유니온이라 args 대신 render로 적는다.
const meta = {
  title: 'Inputs/DateTimeField',
  component: DateTimeField,
  decorators: [
    (Story) => (
      <div className="max-w-xl">
        <Story />
      </div>
    ),
  ],
} satisfies Meta

export default meta
type Story = StoryObj

const today = startOfToday()
const isPast = (date: Date) => isBefore(date, today)
const onValueChange = fn()

/** 날짜 칸을 누르면 달력이 열리고 '선택 완료'로 확정한다. 시간은 30분 단위, 마지막에 23:59. */
export const Single: Story = {
  render: () => (
    <DateTimeField
      mode="single"
      label="대회 일시"
      defaultValue={new Date(2026, 10, 1, 8, 0)}
      isDateDisabled={isPast}
      onValueChange={onValueChange}
    />
  ),
}

/** 기간은 시작 · 끝 시간 칸이 따로 있다. */
export const Range: Story = {
  render: () => (
    <DateTimeField
      mode="range"
      label="접수 기간"
      helper="시작일 → 마감일 순서로 눌러요"
      defaultValue={{ from: new Date(2026, 9, 14, 10, 0), to: new Date(2026, 9, 31, 18, 0) }}
      isDateDisabled={isPast}
      onValueChange={onValueChange}
    />
  ),
}

export const Empty: Story = {
  render: () => <DateTimeField mode="single" label="환불 기한" onValueChange={onValueChange} />,
}

export const Error: Story = {
  render: () => (
    <DateTimeField
      mode="single"
      label="환불 기한"
      defaultValue={new Date(2026, 10, 2, 23, 59)}
      error="환불 기한은 대회 일시보다 앞서야 해요"
    />
  ),
}

export const Disabled: Story = {
  render: () => (
    <DateTimeField
      mode="range"
      label="접수 기간"
      defaultValue={{ from: new Date(2026, 9, 14, 10, 0), to: new Date(2026, 9, 31, 18, 0) }}
      disabled
    />
  ),
}
