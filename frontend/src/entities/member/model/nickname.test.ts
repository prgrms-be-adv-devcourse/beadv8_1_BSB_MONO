import { describe, expect, it } from 'vitest'

import { isNicknameWidthValid, nicknameWidth } from './nickname'

describe('닉네임 칸 수', () => {
  it('한글은 2칸, 영문 · 숫자는 1칸으로 센다', () => {
    expect(nicknameWidth('러너abc')).toBe(7)
  })

  it.each([
    ['러너', true],
    ['abcd', true],
    ['가나다라마바사아', true],
    ['abcdefghijklmnop', true],
    ['곰', false],
    ['abc', false],
    ['가나다라마바사아자', false],
    ['abcdefghijklmnopq', false],
  ])('4~16칸만 허용한다: %s → %s', (nickname, valid) => {
    expect(isNicknameWidthValid(nickname)).toBe(valid)
  })
})
