import { describe, expect, it } from 'vitest'

import { isProtectedPath } from './protected-path'

describe('isProtectedPath', () => {
  it.each([
    '/me',
    '/me/deposit',
    '/tickets',
    '/tickets/1',
    '/cart',
    '/checkout/complete',
    '/races/1/apply',
  ])('%s는 로그인이 필요하다', (path) => {
    expect(isProtectedPath(path)).toBe(true)
  })

  it.each(['/', '/races', '/races/1', '/login', '/signup', '/mercury', '/terms/service'])(
    '%s는 로그인 없이 열린다',
    (path) => {
      expect(isProtectedPath(path)).toBe(false)
    },
  )
})
