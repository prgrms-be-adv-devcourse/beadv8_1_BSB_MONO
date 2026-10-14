import '@testing-library/jest-dom/vitest'

import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'

import { resetAuthSessionForTesting } from '@/shared/api/auth-session'
import { server } from '@/shared/mocks/node'

// 개발 서버와 같은 MSW 핸들러를 테스트에서도 쓴다.
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  server.resetHandlers()
  cleanup()
  // 메모리의 Access Token·재발급 상태가 다음 테스트로 넘어가지 않게 한다
  resetAuthSessionForTesting()
})
afterAll(() => server.close())
