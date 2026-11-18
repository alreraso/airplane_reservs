import { createContext, useContext, useEffect, useRef } from 'react'

/**
 * 'open' fires every time the connection is (re)established. Events missed while
 * disconnected are NOT replayed by the server, so listeners use it to reload from REST.
 */
export type StreamEventName = 'open' | 'seat' | 'stats' | 'flight'

export type Subscribe = (event: StreamEventName, handler: (data: unknown) => void) => () => void

export const StreamContext = createContext<Subscribe | null>(null)
export const ConnectionContext = createContext(false)

/** Runs `handler` for every event of this type. Always calls the latest handler version. */
export function useStreamEvent<T = unknown>(event: StreamEventName, handler: (data: T) => void) {
  const subscribe = useContext(StreamContext)
  const latest = useRef(handler)

  useEffect(() => {
    latest.current = handler
  })

  useEffect(() => {
    if (!subscribe) return
    return subscribe(event, (data) => latest.current(data as T))
  }, [subscribe, event])
}

export function useConnected(): boolean {
  return useContext(ConnectionContext)
}
