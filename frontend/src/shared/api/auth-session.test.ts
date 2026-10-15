import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { server } from '@/shared/mocks/node'

import {
  clearAccessToken,
  getAccessToken,
  getAuthStatus,
  onSessionExpired,
  setAccessToken,
} from './access-token'
import { ApiError } from './api-error'
import { reissueAccessToken, restoreSession } from './auth-session'
import { customFetch } from './fetcher'

const PROTECTED_URL = 'http://localhost/api/v1/order/orders'

/** Bearer <validToken>이 붙어야 200, 아니면 게이트웨이처럼 401로 답하는 주소 */
function protectedApi(validToken: string) {
  const authorizations: (string | null)[] = []
  server.use(
    http.get('*/api/v1/order/orders', ({ request }) => {
      const authorization = request.headers.get('Authorization')
      authorizations.push(authorization)
      if (authorization !== `Bearer ${validToken}`) {
        return HttpResponse.json(
          { status: 401, message: '만료된 토큰입니다.', data: null },
          { status: 401 },
        )
      }
      return HttpResponse.json({ status: 200, message: 'ok', data: [] })
    }),
  )
  return authorizations
}

/** 재발급 주소. 부른 횟수를 센다. */
function reissueApi(result: { token: string } | { status: number }) {
  const calls = { count: 0 }
  server.use(
    http.post('*/api/v1/auth/reissue', async () => {
      calls.count += 1
      // 여러 요청이 동시에 401을 받을 틈을 준다
      await new Promise((resolve) => setTimeout(resolve, 10))
      if ('token' in result) {
        return HttpResponse.json({
          status: 200,
          message: '토큰 재발급 성공',
          data: { accessToken: result.token },
        })
      }
      return HttpResponse.json(
        { status: result.status, message: 'Refresh Token이 만료되었습니다.', data: null },
        { status: result.status },
      )
    }),
  )
  return calls
}

describe('Access Token 붙이기', () => {
  it('메모리의 Access Token을 Authorization: Bearer 헤더로 보낸다', async () => {
    const authorizations = protectedApi('token-1')
    setAccessToken('token-1')

    await customFetch(PROTECTED_URL)
    expect(authorizations).toEqual(['Bearer token-1'])
  })

  it('토큰이 없으면 Authorization 헤더를 붙이지 않는다', async () => {
    const authorizations = protectedApi('token-1')
    reissueApi({ status: 401 })

    await customFetch(PROTECTED_URL).catch(() => undefined)
    expect(authorizations[0]).toBeNull()
  })
})

describe('401 → 재발급 → 다시 보내기', () => {
  it('401을 받으면 재발급한 토큰으로 원래 요청을 한 번 다시 보낸다', async () => {
    const authorizations = protectedApi('new-token')
    const reissue = reissueApi({ token: 'new-token' })
    setAccessToken('expired-token')

    await expect(customFetch(PROTECTED_URL)).resolves.toMatchObject({ status: 200 })
    expect(reissue.count).toBe(1)
    expect(authorizations).toEqual(['Bearer expired-token', 'Bearer new-token'])
    expect(getAccessToken()).toBe('new-token')
  })

  it('동시에 요청 3개가 401을 받아도 재발급은 한 번만 보낸다 (single-flight)', async () => {
    protectedApi('new-token')
    const reissue = reissueApi({ token: 'new-token' })
    setAccessToken('expired-token')

    const results = await Promise.all([
      customFetch(PROTECTED_URL),
      customFetch(PROTECTED_URL),
      customFetch(PROTECTED_URL),
    ])

    expect(reissue.count).toBe(1)
    expect(results).toHaveLength(3)
    results.forEach((result) => expect(result).toMatchObject({ status: 200 }))
  })

  it('요청을 보낸 뒤 다른 요청이 이미 재발급했다면 다시 재발급하지 않고 새 토큰으로 보낸다', async () => {
    const authorizations = protectedApi('new-token')
    const reissue = reissueApi({ token: 'other-token' })
    setAccessToken('expired-token')

    const request = customFetch(PROTECTED_URL)
    // 옛 토큰으로 보낸 요청이 401을 받기 전에 다른 곳에서 재발급이 끝난 상황
    setAccessToken('new-token')

    await expect(request).resolves.toMatchObject({ status: 200 })
    expect(reissue.count).toBe(0)
    expect(authorizations).toEqual(['Bearer expired-token', 'Bearer new-token'])
  })

  it('재발급도 실패하면 토큰을 비우고 로그아웃 상태가 되며 처음 받은 401을 던진다', async () => {
    const authorizations = protectedApi('new-token')
    reissueApi({ status: 401 })
    const expired = vi.fn()
    onSessionExpired(expired)
    setAccessToken('expired-token')

    const error = await customFetch(PROTECTED_URL).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 401 })
    expect(authorizations).toHaveLength(1)
    expect(getAccessToken()).toBeNull()
    expect(getAuthStatus()).toBe('anonymous')
    expect(expired).toHaveBeenCalledTimes(1)
  })

  it('로그인 API의 401(비밀번호 불일치)은 재발급하지 않는다', async () => {
    const reissue = reissueApi({ token: 'new-token' })
    server.use(
      http.post('*/api/v1/auth/login', () =>
        HttpResponse.json(
          { status: 401, message: '이메일 또는 비밀번호가 올바르지 않습니다.', data: null },
          { status: 401 },
        ),
      ),
    )

    const error = await customFetch('http://localhost/api/v1/auth/login', {
      method: 'POST',
    }).catch((e: unknown) => e)
    expect(error).toMatchObject({ status: 401 })
    expect(reissue.count).toBe(0)
  })

  it('재발급을 기다리는 사이 로그아웃하면 재발급 결과로 토큰을 되살리지 않는다', async () => {
    protectedApi('new-token')
    reissueApi({ token: 'new-token' })
    setAccessToken('expired-token')

    const request = customFetch(PROTECTED_URL).catch((e: unknown) => e)
    // 요청이 401을 받고 재발급을 기다리는 동안 로그아웃
    await new Promise((resolve) => setTimeout(resolve, 5))
    clearAccessToken()

    expect(await request).toMatchObject({ status: 401 })
    expect(getAccessToken()).toBeNull()
    expect(getAuthStatus()).toBe('anonymous')
  })
})

describe('restoreSession (새로고침 복구)', () => {
  it('재발급에 성공하면 토큰을 되살려 로그인 상태가 된다', async () => {
    reissueApi({ token: 'restored-token' })
    expect(getAuthStatus()).toBe('pending')

    await restoreSession()

    expect(getAccessToken()).toBe('restored-token')
    expect(getAuthStatus()).toBe('authenticated')
  })

  it('재발급에 실패하면 로그아웃 상태가 되고 세션 만료로 알리지 않는다', async () => {
    reissueApi({ status: 401 })
    const expired = vi.fn()
    onSessionExpired(expired)

    await restoreSession()

    expect(getAuthStatus()).toBe('anonymous')
    expect(expired).not.toHaveBeenCalled()
  })

  it('여러 번 불러도 재발급은 한 번만 보낸다', async () => {
    const reissue = reissueApi({ token: 'restored-token' })

    await Promise.all([restoreSession(), restoreSession()])
    await restoreSession()

    expect(reissue.count).toBe(1)
  })

  it('서버에 닿지 못해도 로그아웃 상태로 정리한다', async () => {
    server.use(http.post('*/api/v1/auth/reissue', () => HttpResponse.error()))

    await restoreSession()

    expect(getAuthStatus()).toBe('anonymous')
  })
})

/**
 * 탭 사이에서 공유되는 navigator.locks를 흉내 낸다. 같은 이름의 잠금은 앞의 것이 끝나야 다음 것이 실행된다.
 * hold()는 다른 탭이 잠금을 잡고 있는 상황을 만들고, 돌려준 함수를 부르면 푼다.
 */
function fakeLocks() {
  const requested: string[] = []
  let held = 0
  let tail: Promise<unknown> = Promise.resolve()
  const run = <T>(callback: () => Promise<T>): Promise<T> => {
    const result = tail.then(async () => {
      held += 1
      try {
        return await callback()
      } finally {
        held -= 1
      }
    })
    tail = result.catch(() => undefined)
    return result
  }
  const locks = {
    request: (name: string, callback: () => Promise<unknown>) => {
      requested.push(name)
      return run(callback)
    },
  }
  Object.defineProperty(navigator, 'locks', { value: locks, configurable: true })
  return {
    requested,
    isHeld: () => held > 0,
    hold: () => {
      const { promise: released, resolve: release } = Promise.withResolvers<void>()
      void run(() => released)
      return release
    },
  }
}

describe('탭 사이 재발급 잠금 (Web Locks)', () => {
  afterEach(() => {
    Reflect.deleteProperty(navigator, 'locks')
  })

  it('navigator.locks가 있으면 재발급을 잠금 안에서 보낸다', async () => {
    const locks = fakeLocks()
    const heldDuringReissue: boolean[] = []
    server.use(
      http.post('*/api/v1/auth/reissue', () => {
        heldDuringReissue.push(locks.isHeld())
        return HttpResponse.json({
          status: 200,
          message: '토큰 재발급 성공',
          data: { accessToken: 'new-token' },
        })
      }),
    )

    await expect(reissueAccessToken()).resolves.toBe('new-token')

    expect(locks.requested).toEqual(['auth:reissue'])
    expect(heldDuringReissue).toEqual([true])
  })

  it('다른 탭이 잠금을 잡고 있으면 풀릴 때까지 기다렸다가 보낸다', async () => {
    const locks = fakeLocks()
    const reissue = reissueApi({ token: 'new-token' })
    const releaseOtherTab = locks.hold()

    const result = reissueAccessToken()
    await new Promise((resolve) => setTimeout(resolve, 30))
    expect(reissue.count).toBe(0)

    releaseOtherTab()
    await expect(result).resolves.toBe('new-token')
    expect(reissue.count).toBe(1)
  })

  it('잠금을 기다리는 사이 로그아웃하면 재발급 결과로 토큰을 되살리지 않는다', async () => {
    const locks = fakeLocks()
    reissueApi({ token: 'new-token' })
    setAccessToken('expired-token')
    const releaseOtherTab = locks.hold()

    const result = reissueAccessToken()
    clearAccessToken()
    releaseOtherTab()

    await expect(result).resolves.toBeNull()
    expect(getAccessToken()).toBeNull()
    expect(getAuthStatus()).toBe('anonymous')
  })

  it('navigator.locks가 없으면 잠금 없이 지금처럼 보낸다', async () => {
    expect('locks' in navigator).toBe(false)
    const reissue = reissueApi({ token: 'new-token' })

    await expect(reissueAccessToken()).resolves.toBe('new-token')
    expect(reissue.count).toBe(1)
  })
})
