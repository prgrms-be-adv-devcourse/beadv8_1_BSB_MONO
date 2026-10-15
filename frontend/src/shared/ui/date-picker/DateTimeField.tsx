'use client'

import { Popover } from '@base-ui/react/popover'
import { useId, useState, type ReactNode } from 'react'

import { cn } from '@/shared/lib'

import { FIELD_BOX, FIELD_HELPER, FIELD_LABEL } from '../field/field-styles'
import { Select } from '../field/Select'
import { Icon } from '../icon/icons'
import { rangeDateText, singleDateText, timeItemsWith, timeOf, withTime } from './date-time'
import { DatePicker } from './DatePicker'
import type { DateRange } from './selection'

type CommonProps = {
  label?: ReactNode
  /** 입력 아래 안내 문구 */
  helper?: ReactNode
  /** 있으면 오류 상태가 되고 helper 자리에 이 문구를 보여 준다. */
  error?: ReactNode
  disabled?: boolean
  placeholder?: string
  /** 고를 수 없는 날(지난 날 등) */
  isDateDisabled?: (date: Date) => boolean
  startMonth?: Date
  endMonth?: Date
  className?: string
}

export type DateTimeFieldProps = CommonProps &
  (
    | {
        mode: 'single'
        defaultValue?: Date
        /** 날짜와 시간을 둘 다 골랐을 때 그 시각, 하나라도 비면 undefined */
        onValueChange?: (value: Date | undefined) => void
      }
    | {
        mode: 'range'
        defaultValue?: DateRange
        /** 시작·끝 각각 날짜와 시간을 둘 다 골랐을 때만 값이 들어간다 */
        onValueChange?: (value: DateRange) => void
      }
  )

// 피그마 DateTimeField. 날짜 칸을 누르면 DatePicker가 팝오버로 열리고 '선택 완료'로 확정한다.
// 시간은 옆의 Select(30분 단위 + 23:59)로 고른다. range는 시간 칸이 시작·끝 2개다.
export function DateTimeField(props: DateTimeFieldProps) {
  const { label, helper, error, disabled, placeholder = '날짜를 골라 주세요', className } = props
  const labelId = useId()
  const valueId = useId()
  const descriptionId = useId()
  const description = error ?? helper

  const initial: DateRange =
    props.mode === 'single'
      ? { from: props.defaultValue, to: undefined }
      : (props.defaultValue ?? { from: undefined, to: undefined })
  // 날짜(일)와 시간을 따로 들고 있다가, 둘 다 있을 때 합쳐서 알린다.
  const [dates, setDates] = useState<DateRange>(initial)
  const [startTime, setStartTime] = useState(timeOf(initial.from))
  const [endTime, setEndTime] = useState(timeOf(initial.to))
  const [open, setOpen] = useState(false)
  // 팝오버 안에서 고르는 중인 값. '선택 완료'를 눌러야 dates에 들어간다.
  const [draft, setDraft] = useState<DateRange>(initial)

  function emit(next: DateRange, start: string | null, end: string | null) {
    if (props.mode === 'single') {
      props.onValueChange?.(withTime(next.from, start))
    } else {
      props.onValueChange?.({ from: withTime(next.from, start), to: withTime(next.to, end) })
    }
  }

  function confirm() {
    // 기간에서 시작일만 누르고 확정하면 하루짜리 기간으로 본다
    const next =
      props.mode === 'range' && draft.from
        ? { from: draft.from, to: draft.to ?? draft.from }
        : draft
    setDates(next)
    setOpen(false)
    emit(next, startTime, endTime)
  }

  const dateText =
    props.mode === 'single'
      ? dates.from && singleDateText(dates.from)
      : dates.from && rangeDateText(dates)
  const items = timeItemsWith(startTime, endTime)
  const labelText = typeof label === 'string' ? label : '날짜'

  return (
    <div
      role="group"
      aria-labelledby={label ? labelId : undefined}
      aria-describedby={description ? descriptionId : undefined}
      data-invalid={error ? '' : undefined}
      data-disabled={disabled ? '' : undefined}
      className={cn('group flex w-full flex-col gap-2', className)}
    >
      {label && (
        <span id={labelId} className={FIELD_LABEL}>
          {label}
        </span>
      )}
      <div className="flex items-center gap-2">
        <Popover.Root
          open={open}
          onOpenChange={(next) => {
            if (next) {
              setDraft(dates)
            }
            setOpen(next)
          }}
        >
          <Popover.Trigger
            disabled={disabled}
            aria-labelledby={`${label ? labelId : ''} ${valueId}`.trim()}
            className={cn(
              FIELD_BOX,
              'flex min-w-0 flex-1 items-center gap-2 text-left',
              'focus-visible:inset-ring-2 focus-visible:inset-ring-border-focus',
              'data-popup-open:inset-ring-2 data-popup-open:inset-ring-border-focus',
            )}
          >
            <Icon
              name="Calendar"
              className="size-5 shrink-0 text-text-tertiary group-data-disabled:text-text-disabled"
            />
            <span
              id={valueId}
              className={cn('min-w-0 flex-1 truncate', !dateText && 'text-text-tertiary')}
            >
              {dateText || placeholder}
            </span>
          </Popover.Trigger>
          <Popover.Portal>
            <Popover.Positioner align="start" sideOffset={4} className="z-50 outline-none">
              <Popover.Popup
                aria-label={`${labelText} 고르기`}
                className="w-86 rounded-lg shadow-md outline-none"
              >
                {props.mode === 'single' ? (
                  <DatePicker
                    mode="single"
                    selected={draft.from}
                    onSelect={(date) => setDraft({ from: date, to: undefined })}
                    isDateDisabled={props.isDateDisabled}
                    startMonth={props.startMonth}
                    endMonth={props.endMonth}
                    footer={{ onConfirm: confirm }}
                  />
                ) : (
                  <DatePicker
                    mode="range"
                    selected={draft}
                    onSelect={setDraft}
                    isDateDisabled={props.isDateDisabled}
                    startMonth={props.startMonth}
                    endMonth={props.endMonth}
                    footer={{ onConfirm: confirm }}
                  />
                )}
              </Popover.Popup>
            </Popover.Positioner>
          </Popover.Portal>
        </Popover.Root>
        <Select
          items={items}
          value={startTime}
          onValueChange={(time) => {
            setStartTime(time)
            emit(dates, time, endTime)
          }}
          placeholder="시간"
          disabled={disabled}
          aria-label={props.mode === 'single' ? `${labelText} 시간` : `${labelText} 시작 시간`}
          popupClassName="w-36"
          className="w-28 shrink-0"
        />
        {props.mode === 'range' && (
          <>
            <span aria-hidden className="text-body-m text-text-secondary">
              ~
            </span>
            <Select
              items={items}
              value={endTime}
              onValueChange={(time) => {
                setEndTime(time)
                emit(dates, startTime, time)
              }}
              placeholder="시간"
              disabled={disabled}
              aria-label={`${labelText} 끝 시간`}
              popupClassName="w-36"
              className="w-28 shrink-0"
            />
          </>
        )}
      </div>
      {description && (
        <p id={descriptionId} className={FIELD_HELPER}>
          {description}
        </p>
      )}
    </div>
  )
}
