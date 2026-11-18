import { useState } from 'react'
import { StreamProvider } from './realtime/StreamProvider'
import { useConnected } from './realtime/stream'
import { ToastProvider } from './toast/ToastProvider'
import { AdminPage } from './pages/AdminPage'
import { DashboardPage } from './pages/DashboardPage'
import { FlightsPage } from './pages/FlightsPage'
import { LookupPage } from './pages/LookupPage'

type Tab = 'flights' | 'lookup' | 'dashboard' | 'admin'

const TABS: Array<[Tab, string]> = [
  ['flights', 'Vuelos'],
  ['lookup', 'Mi reserva'],
  ['dashboard', 'Dashboard'],
  ['admin', 'Admin'],
]

function Header({ tab, onTab }: { tab: Tab; onTab: (tab: Tab) => void }) {
  const connected = useConnected()
  return (
    <header>
      <h1>✈ Reservas en Tiempo Real</h1>
      <nav>
        {TABS.map(([id, label]) => (
          <button
            type="button"
            key={id}
            className={`tab${tab === id ? ' active' : ''}`}
            onClick={() => onTab(id)}
          >
            {label}
          </button>
        ))}
      </nav>
      <span className={`conn ${connected ? 'on' : 'off'}`} data-testid="connection">
        {connected ? '● en vivo' : '● reconectando…'}
      </span>
    </header>
  )
}

export default function App() {
  const [tab, setTab] = useState<Tab>('flights')

  // Every page stays mounted (only hidden), so switching tabs keeps the open flight and
  // the seat I am holding, and the pages keep receiving live events.
  return (
    <ToastProvider>
      <StreamProvider>
        <Header tab={tab} onTab={setTab} />
        <main>
          <div hidden={tab !== 'flights'}>
            <FlightsPage />
          </div>
          <div hidden={tab !== 'lookup'}>
            <LookupPage />
          </div>
          <div hidden={tab !== 'dashboard'}>
            <DashboardPage />
          </div>
          <div hidden={tab !== 'admin'}>
            <AdminPage />
          </div>
        </main>
      </StreamProvider>
    </ToastProvider>
  )
}
