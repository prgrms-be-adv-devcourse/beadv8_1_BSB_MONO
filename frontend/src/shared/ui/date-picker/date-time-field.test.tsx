import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { rangeDateText, TIME_ITEMS, timeItemsWith, withTime } from './date-time'
import { DateTimeField } from './DateTimeField'

const day = (d: number, h = 0, m = 0) => new Date(2026, 9, d, h, m) // 2026년 10월

describe('시간 목록', () => {
  it('30분 단위 48개 뒤에 23:59가 온다', () => {
    expect(TIME_ITEMS).toHaveLength(49)
    expect(TIME_ITEMS.slice(0, 2).map((i) => i.value)).toEqual(['00:00', '00:30'])
    expect(TIME_ITEMS.slice(-2).map((i) => i.value)).toEqual(['23:30', '23:59'])
  })

  it('목록에 없는 기본 시각은 순서에 맞춰 끼워 넣는다', () => {
    const values = timeItemsWith('10:15', null).map((i) => i.value)
    expect(values.slice(values.indexOf('10:00'), values.indexOf('10:00') + 3)).toEqual([
      '10:00',
      '10:15',
      '10:30',
    ])
  })
})

describe('withTime', () => {
  it('날짜에 시각을 붙인다', () => {
    expect(withTime(day(31), '23:59')).toEqual(day(31, 23, 59))
  })

  it('날짜나 시간 중 하나라도 없으면 undefined', () => {
    expect(withTime(undefined, '10:00')).toBeUndefined()
    expect(withTime(day(1), null)).toBeUndefined()
  })
})

it('기간 글자는 M/d (요일) ~ M/d (요일)', () => {
  expect(rangeDateText({ from: day(14), to: day(31) })).toBe('10/14 (수) ~ 10/31 (토)')
})

describe('DateTimeField', () => {
  it('달력에서 날짜를 고르고 선택 완료를 누르면 시간을 붙여 알린다', async () => {
    const onValueChange = vi.fn()
    render(
      <DateTimeField
        mode="single"
        label="대회 일시"
        defaultValue={day(18, 8, 0)}
        onValueChange={onValueChange}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: /대회 일시 2026-10-18/ }))
    await userEvent.click(screen.getByRole('button', { name: /10월 20일/ }))
    await userEvent.click(screen.getByRole('button', { name: '선택 완료' }))

    expect(onValueChange).toHaveBeenLastCalledWith(day(20, 8, 0))
    expect(screen.getByRole('button', { name: /2026-10-20 \(화\)/ })).toBeInTheDocument()
  })

  it('선택 완료 없이 닫으면 고른 날짜를 버린다', async () => {
    const onValueChange = vi.fn()
    render(
      <DateTimeField
        mode="single"
        label="대회 일시"
        defaultValue={day(18, 8, 0)}
        onValueChange={onValueChange}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: /대회 일시/ }))
    await userEvent.click(screen.getByRole('button', { name: /10월 20일/ }))
    await userEvent.keyboard('{Escape}')

    expect(onValueChange).not.toHaveBeenCalled()
    expect(screen.getByRole('button', { name: /2026-10-18/ })).toBeInTheDocument()
  })

  it('기간은 시작·끝 시간 칸이 따로 있다', () => {
    render(
      <DateTimeField
        mode="range"
        label="접수 기간"
        defaultValue={{ from: day(14, 10, 0), to: day(31, 18, 0) }}
      />,
    )
    expect(screen.getByRole('combobox', { name: '접수 기간 시작 시간' })).toHaveTextContent('10:00')
    expect(screen.getByRole('combobox', { name: '접수 기간 끝 시간' })).toHaveTextContent('18:00')
  })
})
