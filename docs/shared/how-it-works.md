# Cómo funciona (guía rápida)

Este documento responde tres preguntas básicas. El detalle y las alternativas están en [decisions.md](decisions.md); el contrato exacto de la API, en [openapi.yaml](openapi.yaml).

## 1. ¿De dónde sale la información?

De una base de datos **H2 en memoria**, dentro del mismo proceso de Spring Boot.

- Al arrancar, `DataSeeder` crea 5 vuelos ficticios, cada uno con 48 asientos (8 filas × letras A–F):

| Vuelo | Ruta | Salida | Precio (COP) |
|---|---|---|---|
| AV101 | BOG → MDE | mañana 07:30 | 189.000 |
| AV103 | BOG → MDE | mañana 15:10 | 210.000 |
| AV202 | MDE → BOG | mañana 09:00 | 175.000 |
| AV310 | BOG → CTG | pasado mañana 06:45 | 260.000 |
| AV900 | BOG → MIA | pasado mañana 11:20 | 1.450.000 |

- Las fechas se calculan a partir del día en que se arranca la app, así que siempre hay vuelos futuros.
- **Al reiniciar, todo vuelve a este estado inicial.** Las reservas hechas se pierden.
- Para conservar datos hay que cambiar a PostgreSQL (ver decisions §7). El código de reservas no cambia, porque usa SQL estándar.
- Consola de la base de datos: el proyecto incluye el módulo `h2console`, pero no está configurado ni habilitado en `application.properties`.

## 2. ¿Cómo se controla el estado de cada asiento?

Cada asiento tiene un estado, un posible dueño y una hora de vencimiento:

| Campo | Significado |
|---|---|
| `status` | `AVAILABLE` (libre), `HELD` (bloqueado temporalmente), `BOOKED` (ocupado, permanente) |
| `heldBy` | `clientId` de quien lo bloqueó. **Nunca se envía a otros clientes** |
| `heldUntil` | Momento en que vence el bloqueo (UTC) |

Ciclo de vida:

```
AVAILABLE ──hold──► HELD ──confirm (dueño, a tiempo, pago ok)──► BOOKED
    ▲                │
    └── release / vence el tiempo
```

Reglas:

- **Bloquear (`hold`)**: funciona si el asiento está libre, si el bloqueo anterior ya venció, o si el mismo cliente lo vuelve a pedir (reinicia el reloj). Si no, responde `409`.
- **Quién gana una carrera**: cada cambio es un único `UPDATE` condicional. La base de datos informa cuántas filas cambió: 1 = ganaste, 0 = perdiste. No hay "leer, comprobar, escribir".
- **Duración del bloqueo**: 5 minutos por defecto (`reservations.hold-duration`).
- **Vencimiento**: un bloqueo vencido se trata como libre en la propia consulta. Además, un job corre cada segundo para liberarlo y avisar a las pantallas.
- **Confirmar**: solo el dueño del bloqueo, antes de que venza. Si no, `409`.
- **Último asiento**: al reservarse, el vuelo pasa a `SOLD_OUT` automáticamente.
- **Vuelo cancelado o agotado**: no admite nuevos bloqueos ni confirmaciones. No libera bloqueos ya existentes.
- **Identidad**: el navegador genera un `clientId` aleatorio por pestaña. No hay login.

## 3. ¿Cómo se paga?

El pago es **simulado**. No se cobra dinero ni se contacta a ningún proveedor.

- `POST /api/flights/{id}/reservations` recibe nombre, email y número de tarjeta (16 dígitos).
- Cualquier tarjeta válida se acepta, **excepto `4000000000000002`, que siempre se rechaza** (para probar el fallo).
- El cobro lo hace `FakePaymentGateway`, que implementa la interfaz `PaymentGateway`. Para usar un proveedor real se escribe otro adaptador sin tocar la lógica de reserva.
- Reclamar el asiento, cobrar y guardar la reserva ocurren en **una sola transacción**:

| Resultado | Qué pasa |
|---|---|
| Pago aceptado | Asiento `BOOKED`, se crea la reserva con código `RSV-XXXXXXXX`, se emite el evento a todos |
| Pago rechazado (`402`) | Se revierte todo: el asiento sigue `HELD` por el cliente y **no se emite ningún evento** |
| Bloqueo vencido o ajeno (`409`) | No se cobra |

- El número de tarjeta se valida y se descarta: **no se guarda ni se escribe en logs**.

## 4. ¿Cómo se enteran los otros clientes?

1. Cada navegador mantiene abierta una conexión `GET /api/stream` (Server-Sent Events).
2. Cuando una transacción se confirma, el servidor envía:
   - `seat`: un asiento cambió (bloqueado, liberado, vencido o reservado).
   - `stats`: nuevos conteos del vuelo (alimenta el dashboard).
   - `flight`: el vuelo cambió de estado (retrasado, cancelado, agotado).
3. Si la conexión se cae, el navegador reconecta y vuelve a pedir el estado completo. Los eventos perdidos no se reenvían.

## Dónde mirar en el código

| Tema | Archivo |
|---|---|
| Transiciones de estado del asiento (los `UPDATE`) | `domain/SeatRepository.java` |
| Reglas de bloquear, liberar, confirmar y vencer | `application/SeatBookingService.java` |
| Pago simulado | `adapter/out/payment/FakePaymentGateway.java` |
| Datos de ejemplo | `config/DataSeeder.java` |
| Eventos en vivo | `adapter/out/realtime/` |
