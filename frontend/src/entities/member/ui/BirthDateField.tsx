'use client'

import { TextField, type TextFieldProps } from '@/shared/ui'

import { BIRTH_MIN, latestJoinableBirth } from '../model/birth'

export type BirthDateFieldProps = Omit<TextFieldProps, 'type' | 'min' | 'max'>

// 생년월일 칸. 브라우저 날짜 선택기(<input type="date">)라 모바일에서는 OS 휠 · 달력이 뜬다.
// 값은 "YYYY-MM-DD"로 API birthDate와 같다. 고를 수 있는 날은 1900-01-01부터 만 14세가 되는 날까지다.
// max는 고르는 범위만 좁힐 뿐 키보드로 친 값은 막지 않으므로, 만 14세 검사는 폼 스키마가 그대로 한다.
export function BirthDateField(props: BirthDateFieldProps) {
  return <TextField type="date" min={BIRTH_MIN} max={latestJoinableBirth()} {...props} />
}
