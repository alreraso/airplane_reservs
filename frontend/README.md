# Frontend: React + Vite + TypeScript

Interfaz de las reservas en tiempo real. Habla con el backend por REST (`/api/...`) y recibe cambios en vivo por Server-Sent Events (`/api/stream`).

Solo usa `react` y `react-dom`. No hay librería de componentes, de estado ni de rutas (razones y alternativas en [decisions.md §13](../docs/shared/decisions.md#13-frontend)).

## Requisitos

- Node 20 o superior (`node -v`).
- El backend corriendo en `http://localhost:8080` (ver [../backend/README.md](../backend/README.md)).

## Ejecutar

### Modo desarrollo (recarga en caliente)

Dos terminales:

```bash
# 1. Backend
cd backend && ./mvnw spring-boot:run

# 2. Frontend
cd frontend && npm install && npm run dev
```

Abre <http://localhost:5173>. Vite reenvía las peticiones `/api`, incluido el stream de eventos, al backend, así que no hace falta configurar CORS.

### Modo "un solo proceso" (lo que se entrega)

```bash
cd frontend && npm install && npm run build   # genera frontend/dist
cd ../backend && ./mvnw spring-boot:run
```

Abre <http://localhost:8080>. Spring sirve `frontend/dist`. Si construyes el frontend con el backend ya arrancado, **reinícialo**.

## Scripts

| Comando | Qué hace |
|---|---|
| `npm run dev` | Servidor de desarrollo en `:5173` con proxy a `:8080` |
| `npm run build` | Comprueba tipos (`tsc`) y genera `dist/` |
| `npm run lint` | Análisis estático con oxlint |
| `npm run preview` | Sirve `dist/` localmente (sin proxy al backend) |

## Pantallas

| Pestaña | Para qué |
|---|---|
| **Vuelos** | Buscar por origen, destino y fecha. Abrir un vuelo, elegir un asiento (se bloquea unos minutos), pagar y recibir el código de reserva |
| **Mi reserva** | Pegar el código `RSV-…` para ver de nuevo el boleto (vuelo, asiento, pasajero, total). Si el vuelo se cancela, el aviso aparece solo |
| **Dashboard** | Ocupación de todos los vuelos: libres, bloqueados y ocupados, en vivo |
| **Admin** | Cambiar el estado de un vuelo (programado, retrasado, cancelado) y ver el registro de eventos. Sin autenticación a propósito |

Cada pestaña del navegador es un cliente distinto (el identificador se guarda en `sessionStorage`). Para ver el tiempo real, abre dos pestañas.

Tras pagar, el boleto trae un botón **Copiar** para el código. Pegado en "Mi reserva" recupera la reserva incluso desde otro navegador.

Pago ficticio: cualquier tarjeta de 16 dígitos sirve, salvo `4000000000000002`, que siempre se rechaza.

## Estructura

```
src/
  api/        Cliente HTTP y tipos del contrato (reflejan docs/shared/openapi.yaml)
  realtime/   UNA conexión SSE compartida + hook useStreamEvent
  hooks/      useLiveFlights, useFlightLive, useSeatMap, useCountdown
  components/ SeatMap, CheckoutForm, Ticket, CopyButton, StatsCards, StatusBadge...
  pages/      FlightsPage, FlightDetail, LookupPage, DashboardPage, AdminPage
  toast/      Avisos en pantalla
```

## Cómo fluye el tiempo real

1. Al abrir la app, `StreamProvider` abre un único `EventSource('/api/stream')`.
2. Cada hook carga su estado por REST y se suscribe a los eventos que le importan (`seat`, `stats`, `flight`).
3. Un evento actualiza solo lo que cambió: un asiento, un contador, el estado de un vuelo.
4. Si la conexión se cae y vuelve, los hooks **recargan por REST**, porque el servidor no reenvía lo perdido. El indicador "en vivo" de la cabecera refleja la conexión.

## Notas

- Los tipos de `src/api/types.ts` se mantienen a mano. Si cambia el contrato, empieza por ahí: el compilador señalará lo demás.
- El frontend nunca ve quién tiene bloqueado un asiento (el servidor no lo expone). Recuerda localmente el que bloqueó él, en `heldByMe`.
- No hay pruebas automáticas del frontend en el repositorio. El comportamiento se verificó con un navegador real y dos clientes; ver [decisions.md §13 y §15](../docs/shared/decisions.md#15-pruebas-pendiente).
