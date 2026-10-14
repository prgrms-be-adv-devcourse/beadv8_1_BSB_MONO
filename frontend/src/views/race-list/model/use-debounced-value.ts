import { useEffect, useState } from 'react'

/** 입력이 멈추고 delay(ms)가 지나면 바뀌는 값. 글자마다 API를 부르지 않게 한다. */
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debounced
}
