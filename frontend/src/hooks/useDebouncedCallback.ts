import { useEffect, useMemo, useRef } from 'react'

/**
 * 인자를 받는 콜백을 디바운스한다. 최신 콜백 참조를 유지하므로
 * 의존성 변화로 인한 디바운스 타이머 재생성이 발생하지 않는다.
 */
export function useDebouncedCallback<A extends unknown[]>(
  callback: (...args: A) => void,
  delay: number,
): (...args: A) => void {
  const callbackRef = useRef(callback)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    callbackRef.current = callback
  }, [callback])

  useEffect(
    () => () => {
      if (timerRef.current) clearTimeout(timerRef.current)
    },
    [],
  )

  return useMemo(
    () =>
      (...args: A) => {
        if (timerRef.current) clearTimeout(timerRef.current)
        timerRef.current = setTimeout(() => callbackRef.current(...args), delay)
      },
    [delay],
  )
}
