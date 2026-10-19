'use client'

import { useCallback, useRef } from 'react'

import { createIdempotencyKey } from './idempotency'

/**
 * 두 번 처리되면 안 되는 요청(대회 등록, 주문 생성, 장바구니 결제)에 보낼 Idempotency-Key를 고른다.
 * 바로 전에 보낸 입력과 같으면(두 번 클릭 · 실패 뒤 재시도) 같은 키를, 입력이 바뀌면 새 키를 돌려준다.
 * 입력은 JSON으로 비교하므로 File처럼 JSON에 내용이 담기지 않는 값은 이름 · 크기 같은 값으로 바꿔 넘긴다.
 *
 * 돌려받은 키는 `customFetch`의 `idempotencyKey` 옵션으로 넘기면 `Idempotency-Key` 헤더로 붙는다.
 */
export function useIdempotencyKey(): (input: unknown) => string {
  const last = useRef<{ input: string; key: string } | null>(null)
  return useCallback((input: unknown) => {
    const serialized = JSON.stringify(input)
    if (last.current?.input !== serialized) {
      last.current = { input: serialized, key: createIdempotencyKey() }
    }
    return last.current.key
  }, [])
}
