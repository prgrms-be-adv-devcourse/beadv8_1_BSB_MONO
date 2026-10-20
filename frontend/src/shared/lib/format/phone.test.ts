import { describe, expect, it } from 'vitest'

import { formatPhoneInput, formatPhoneNumber } from './phone'

describe('formatPhoneNumber', () => {
  it('11자리는 3-4-4로 나눈다', () => {
    expect(formatPhoneNumber('01012345678')).toBe('010-1234-5678')
  })

  it('10자리(011 등)는 3-3-4로 나눈다', () => {
    expect(formatPhoneNumber('0111234567')).toBe('011-123-4567')
  })

  it('치는 중에는 자릿수에 맞춰 하이픈을 넣는다', () => {
    expect(formatPhoneNumber('010')).toBe('010')
    expect(formatPhoneNumber('0101')).toBe('010-1')
    expect(formatPhoneNumber('0101234')).toBe('010-1234')
    expect(formatPhoneNumber('01012345')).toBe('010-1234-5')
  })

  it('숫자 외 문자는 버리고 11자리까지만 받는다', () => {
    expect(formatPhoneNumber('010 1234.5678')).toBe('010-1234-5678')
    expect(formatPhoneNumber('010-1234-5678-9')).toBe('010-1234-5678')
    expect(formatPhoneNumber('abc')).toBe('')
  })
})

describe('formatPhoneInput', () => {
  it('끝에 숫자를 치면 커서가 끝에 남는다', () => {
    expect(formatPhoneInput('010', '0101', 4)).toEqual({ value: '010-1', caret: 5 })
  })

  it('붙여넣으면 하이픈을 넣고 커서를 끝으로 보낸다', () => {
    expect(formatPhoneInput('', '01012345678', 11)).toEqual({ value: '010-1234-5678', caret: 13 })
  })

  it('중간에 숫자를 넣어도 커서는 넣은 숫자 바로 뒤에 있다', () => {
    // "010-1234-567"의 '1' 앞(4번)에 9를 넣음
    expect(formatPhoneInput('010-1234-567', '010-91234-567', 5)).toEqual({
      value: '010-9123-4567',
      caret: 5,
    })
  })

  it('중간 숫자를 지워도 커서가 그 자리에 남는다', () => {
    // "010-1234-5678"에서 '2'(6번)를 지움
    expect(formatPhoneInput('010-1234-5678', '010-134-5678', 5, 'deleteContentBackward')).toEqual({
      value: '010-134-5678',
      caret: 5,
    })
  })

  it('하이픈 뒤에서 지우면 하이픈 앞 숫자를 지운다', () => {
    expect(formatPhoneInput('010-1234', '0101234', 3, 'deleteContentBackward')).toEqual({
      value: '011-234',
      caret: 2,
    })
  })

  it('하이픈 앞에서 Delete를 누르면 하이픈 뒤 숫자를 지운다', () => {
    expect(formatPhoneInput('010-1234', '0101234', 3, 'deleteContentForward')).toEqual({
      value: '010-234',
      caret: 3,
    })
  })

  it('숫자가 아닌 글자를 치면 값과 커서가 그대로다', () => {
    expect(formatPhoneInput('010-1', '010-1a', 6, 'insertText')).toEqual({
      value: '010-1',
      caret: 5,
    })
  })
})
