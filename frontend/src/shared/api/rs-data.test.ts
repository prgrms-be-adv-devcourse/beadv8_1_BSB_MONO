import { describe, expect, it } from 'vitest'

import { unwrapData } from './rs-data'

describe('unwrapData', () => {
  it('RsData에서 data를 꺼낸다', () => {
    expect(unwrapData({ status: 200, message: 'ok', data: { raceId: 1 } })).toEqual({ raceId: 1 })
  })

  it('data가 없으면 에러를 던진다', () => {
    expect(() => unwrapData({ data: null })).toThrow()
  })
})
