import { useState, type FormEvent } from 'react'
import type { PassengerData } from '../api/types'
import { mmss } from '../format'

interface Props {
  seatNumber: string
  secondsLeft: number
  busy: boolean
  onSubmit: (data: PassengerData) => void
  onRelease: () => void
}

export function CheckoutForm({ seatNumber, secondsLeft, busy, onSubmit, onRelease }: Props) {
  const [data, setData] = useState<PassengerData>({
    passengerName: '',
    email: '',
    cardNumber: '4111111111111111',
  })

  const field = (name: keyof PassengerData) => ({
    value: data[name],
    onChange: (e: { target: { value: string } }) => setData({ ...data, [name]: e.target.value }),
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    onSubmit(data)
  }

  return (
    <form className="checkout" onSubmit={submit}>
      <h3>Asiento {seatNumber}</h3>
      <p className="countdown">
        Tiempo restante: <b data-testid="countdown">{mmss(secondsLeft)}</b>
      </p>
      <label>
        Nombre
        <input name="passengerName" required {...field('passengerName')} />
      </label>
      <label>
        Email
        <input name="email" type="email" required {...field('email')} />
      </label>
      <label>
        Tarjeta (16 dígitos, ficticia)
        <input name="cardNumber" pattern="\d{16}" required {...field('cardNumber')} />
      </label>
      <small>Usa 4000000000000002 para simular un pago rechazado.</small>
      <div className="row">
        <button type="submit" disabled={busy}>
          Confirmar y pagar
        </button>
        <button type="button" className="ghost" disabled={busy} onClick={onRelease}>
          Liberar asiento
        </button>
      </div>
    </form>
  )
}
