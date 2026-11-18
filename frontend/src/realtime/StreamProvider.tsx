import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { ConnectionContext, StreamContext, type StreamEventName, type Subscribe } from './stream'

type Handler = (data: unknown) => void

const DATA_EVENTS = ['seat', 'stats', 'flight'] as const

/**
 * Opens ONE EventSource for the whole app and fans events out to subscribers.
 * The browser reconnects by itself if the link drops.
 */
export function StreamProvider({ children }: { children: ReactNode }) {
  const listeners = useRef(new Map<StreamEventName, Set<Handler>>())
  const [connected, setConnected] = useState(false)

  // Stable identity: consumers never resubscribe because the connection state changed.
  const subscribe = useCallback<Subscribe>((event, handler) => {
    const set = listeners.current.get(event) ?? new Set<Handler>()
    set.add(handler)
    listeners.current.set(event, set)
    return () => {
      set.delete(handler)
    }
  }, [])

  useEffect(() => {
    const emit = (event: StreamEventName, data: unknown) => {
      listeners.current.get(event)?.forEach((handler) => handler(data))
    }

    const source = new EventSource('/api/stream')
    source.onopen = () => {
      setConnected(true)
      emit('open', null)
    }
    source.onerror = () => setConnected(false)

    for (const name of DATA_EVENTS) {
      source.addEventListener(name, (event) => {
        emit(name, JSON.parse((event as MessageEvent<string>).data))
      })
    }

    return () => source.close()
  }, [])

  return (
    <StreamContext.Provider value={subscribe}>
      <ConnectionContext.Provider value={connected}>{children}</ConnectionContext.Provider>
    </StreamContext.Provider>
  )
}
