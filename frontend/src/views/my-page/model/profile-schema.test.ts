import { describe, expect, it } from 'vitest'

import { PROFILE_ERRORS, profileSchema } from './profile-schema'

const valid = { name: '김러너', birth: '1995-03-02', phone: '010-1234-5678', nickname: '러너1' }

function birthError(birth: string) {
  const result = profileSchema.safeParse({ ...valid, birth })
  if (result.success) return null
  return result.error.issues.find((issue) => issue.path[0] === 'birth')?.message ?? null
}

describe('profileSchema 생년월일', () => {
  it('정상 값은 통과한다', () => {
    expect(profileSchema.safeParse(valid).success).toBe(true)
  })

  it('만 14세 미만은 가입 화면과 같은 문구로 거절한다', () => {
    const year = new Date().getFullYear() - 10
    expect(birthError(`${year}-01-01`)).toBe(PROFILE_ERRORS.birthUnder14)
  })

  it('미래 날짜는 거절한다', () => {
    const year = new Date().getFullYear() + 1
    expect(birthError(`${year}-01-01`)).toBe(PROFILE_ERRORS.birthUnder14)
  })

  it('비었거나 없는 날짜면 형식 문구를 보여 준다', () => {
    expect(birthError('')).toBe(PROFILE_ERRORS.birth)
    expect(birthError('1995-02-30')).toBe(PROFILE_ERRORS.birth)
  })
})
