# Backend: API de reservas de vuelos

Spring Boot 4.1 · Java 17 · Maven (wrapper incluido) · H2 en memoria.
El mismo proceso expone la API REST, el stream de eventos en tiempo real (SSE) y sirve el frontend React ya construido (`../frontend/dist`).

Contrato formal (OpenAPI): [../docs/shared/openapi.yaml](../docs/shared/openapi.yaml). Explicación de cómo funciona: [../docs/shared/how-it-works.md](../docs/shared/how-it-works.md).

## 1. Cómo ejecutarlo

**Requisito:** Java 17 o superior (`java -version`). Maven no hace falta: lo descarga `mvnw`.

Ejecuta **desde la carpeta `backend/`** (el frontend se busca en `../frontend/dist`, ruta relativa al directorio de trabajo):

```bash
cd backend
./mvnw spring-boot:run
```

La API funciona sola. Para ver también la interfaz en `http://localhost:8080/`, construye antes el frontend (requiere Node 20 o superior):

```bash
cd frontend && npm install && npm run build
```

O usa el servidor de desarrollo de Vite, que reenvía `/api` a este backend (ver [../frontend/README.md](../frontend/README.md)).

Disponible en `http://localhost:8080`. La primera ejecución descarga dependencias y tarda más.

| Qué | Dónde |
|---|---|
| API | `http://localhost:8080/api/...` |
| Frontend (tras `npm run build`) | `http://localhost:8080/` |
| Stream de eventos | `http://localhost:8080/api/stream` |

### Como jar

```bash
./mvnw -DskipTests package
java -jar target/reservations-0.0.1-SNAPSHOT.jar
```

### Cambiar la configuración sin editar archivos

Cualquier propiedad se sobrescribe con un argumento:

```bash
# con Maven
./mvnw spring-boot:run -Dspring-boot.run.arguments="--reservations.hold-duration=20s --server.port=9090"

# con el jar
java -jar target/reservations-0.0.1-SNAPSHOT.jar --reservations.hold-duration=20s --server.port=9090
```

| Propiedad | Por defecto | Efecto |
|---|---|---|
| `reservations.hold-duration` | `5m` | Cuánto dura el bloqueo temporal de un asiento. Formatos: `20s`, `5m` |
| `reservations.expiration-check-ms` | `1000` | Cada cuántos milisegundos el job libera bloqueos vencidos |
| `server.port` | `8080` | Puerto HTTP |

### Datos

Base de datos H2 **en memoria**. Al arrancar se cargan 5 vuelos de ejemplo con 48 asientos cada uno. **Todo se reinicia al apagar la app.** Los vuelos y sus ids (1 a 5) están en [how-it-works.md](../docs/shared/how-it-works.md#1-de-dónde-sale-la-información).

### Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `Port 8080 was already in use` | Otro proceso usa el puerto. Cierra el otro o usa `--server.port=9090` |
| `/` devuelve 404 | No existe `frontend/dist` (ejecuta `npm run build` y **reinicia** el backend, porque detecta la página de inicio al arrancar), o ejecutaste desde otra carpeta (hazlo desde `backend/`) |
| `UnsupportedClassVersionError` | Tu Java es anterior a 17 |

## 2. Conceptos que necesitas para usar la API

- **`clientId`**: no hay login. Tu cliente inventa un identificador aleatorio (por ejemplo, un UUID) y lo envía al bloquear, liberar y confirmar. Solo quien bloqueó un asiento puede confirmarlo.
- **Bloqueo temporal**: bloquear un asiento lo reserva por un tiempo (5 min por defecto). Si no confirmas antes de `heldUntil`, se libera solo.
- **Estados del asiento**: `AVAILABLE`, `HELD`, `BOOKED`.
- **Estados del vuelo**: `SCHEDULED`, `DELAYED`, `CANCELLED`, `SOLD_OUT`. Solo los dos primeros admiten reservas.
- **Fechas**: `departureTime` y `arrivalTime` son hora local sin zona (`2026-11-19T07:30:00`). `heldUntil` y `createdAt` son UTC (`...Z`).
- **Moneda**: pesos colombianos (COP).

## 3. Peticiones

Todos los ejemplos usan `curl` contra `localhost:8080`. Los cuerpos y respuestas son JSON.

### Resumen

| # | Método y ruta | Para qué | Éxito |
|---|---|---|---|
| 1 | `GET /api/flights` | Buscar vuelos | 200 |
| 2 | `GET /api/flights/{flightId}` | Ver un vuelo | 200 |
| 3 | `GET /api/flights/{flightId}/seats` | Mapa de asientos | 200 |
| 4 | `GET /api/flights/{flightId}/stats` | Métricas de ocupación | 200 |
| 5 | `POST /api/flights/{flightId}/seats/{seatNumber}/hold` | Bloquear un asiento | 200 |
| 6 | `DELETE /api/flights/{flightId}/seats/{seatNumber}/hold` | Liberar un asiento propio | 204 |
| 7 | `POST /api/flights/{flightId}/reservations` | Confirmar y pagar | 201 |
| 8 | `PATCH /api/flights/{flightId}/status` | Cambiar estado del vuelo (simulación) | 200 |
| 9 | `GET /api/stream` | Eventos en vivo (SSE) | 200 |
| 10 | `GET /api/reservations/{code}` | Recuperar una reserva por su código | 200 |

---

### 1. Buscar vuelos

`GET /api/flights`

| Parámetro (query) | Obligatorio | Descripción |
|---|---|---|
| `origin` | No | Código de aeropuerto de 3 letras (`BOG`). No distingue mayúsculas |
| `destination` | No | Código de 3 letras |
| `date` | No | Fecha de salida `yyyy-MM-dd`. Sin fecha: todos los vuelos desde hoy |

```bash
curl "localhost:8080/api/flights?origin=BOG&destination=MDE"
```

```json
[
  {
    "id": 1, "flightNumber": "AV101", "origin": "BOG", "destination": "MDE",
    "departureTime": "2026-11-19T07:30:00", "arrivalTime": "2026-11-19T08:25:00",
    "price": 189000.00, "status": "SCHEDULED", "availableSeats": 48
  }
]
```

Sin resultados devuelve `[]` con 200. Una fecha inválida devuelve 400.

### 2. Ver un vuelo

`GET /api/flights/{flightId}` devuelve el mismo objeto de arriba. `404` si no existe.

```bash
curl localhost:8080/api/flights/1
```

### 3. Mapa de asientos

`GET /api/flights/{flightId}/seats?clientId=...`

`clientId` es opcional. Si lo envías, tus propios asientos bloqueados vienen con `heldByMe: true`.

```bash
curl "localhost:8080/api/flights/1/seats?clientId=ana-123"
```

```json
[
  { "seatNumber": "1A", "status": "AVAILABLE", "heldUntil": null, "heldByMe": false },
  { "seatNumber": "1B", "status": "HELD", "heldUntil": "2026-11-18T20:13:41.417Z", "heldByMe": true },
  { "seatNumber": "1C", "status": "BOOKED", "heldUntil": null, "heldByMe": false }
]
```

Los asientos van en orden: fila 1 (A–F), fila 2, etc. `heldUntil` solo tiene valor mientras está `HELD`.

### 4. Métricas de ocupación

`GET /api/flights/{flightId}/stats`

```bash
curl localhost:8080/api/flights/1/stats
```

```json
{ "flightId": 1, "total": 48, "available": 45, "held": 2, "booked": 1 }
```

`available + held + booked = total`.

### 5. Bloquear un asiento

`POST /api/flights/{flightId}/seats/{seatNumber}/hold`

| Campo (body) | Obligatorio | Descripción |
|---|---|---|
| `clientId` | Sí | Tu identificador. No puede estar vacío |

```bash
curl -X POST localhost:8080/api/flights/1/seats/3C/hold \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"ana-123"}'
```

```json
{ "seatNumber": "3C", "heldUntil": "2026-11-18T20:13:41.417Z" }
```

Funciona si el asiento está libre, si su bloqueo anterior venció, o si ya es tuyo (el reloj se reinicia). Emite un evento `seat` a todos los clientes conectados.

| Error | Cuándo |
|---|---|
| 400 | Falta el cuerpo o `clientId` vacío |
| 404 | El vuelo o el asiento no existe |
| 409 | Otro cliente lo tiene bloqueado, ya está reservado, o el vuelo está `CANCELLED` / `SOLD_OUT` |

### 6. Liberar un asiento

`DELETE /api/flights/{flightId}/seats/{seatNumber}/hold?clientId=...`

`clientId` es **obligatorio** en la query.

```bash
curl -X DELETE "localhost:8080/api/flights/1/seats/3C/hold?clientId=ana-123"
```

Responde `204` sin cuerpo. Es idempotente: si el asiento no es tuyo o no está bloqueado, también responde `204` y no cambia nada. Falta de `clientId`: `400`.

### 7. Confirmar y pagar

`POST /api/flights/{flightId}/reservations`

| Campo (body) | Obligatorio | Regla |
|---|---|---|
| `clientId` | Sí | El mismo que usaste para bloquear |
| `seatNumber` | Sí | El asiento que bloqueaste |
| `passengerName` | Sí | No vacío |
| `email` | Sí | Formato de correo válido |
| `cardNumber` | Sí | 16 dígitos. Ficticio |

```bash
curl -X POST localhost:8080/api/flights/1/reservations \
  -H 'Content-Type: application/json' \
  -d '{
    "clientId": "ana-123",
    "seatNumber": "3C",
    "passengerName": "Ana Gómez",
    "email": "ana@example.com",
    "cardNumber": "4111111111111111"
  }'
```

```json
{
  "code": "RSV-6367CA45", "flightId": 1, "seatNumber": "3C",
  "passengerName": "Ana Gómez", "amount": 189000.00,
  "createdAt": "2026-11-18T19:13:13.491Z"
}
```

`code` es la referencia del boleto. El asiento queda `BOOKED` de forma permanente. Si era el último libre, el vuelo pasa a `SOLD_OUT`.

**Pago simulado:** cualquier tarjeta de 16 dígitos se acepta, salvo `4000000000000002`, que siempre se rechaza.

| Error | Cuándo |
|---|---|
| 400 | Cuerpo incompleto o inválido (correo mal formado, tarjeta que no tiene 16 dígitos) |
| 402 | Pago rechazado. El bloqueo sigue vigente hasta `heldUntil`, puedes reintentar |
| 404 | El vuelo no existe |
| 409 | No tienes un bloqueo activo en ese asiento (venció, es de otro, o nunca lo bloqueaste) o el vuelo no admite reservas |

### 8. Cambiar el estado de un vuelo (simulación)

`PATCH /api/flights/{flightId}/status`

Endpoint **sin autenticación a propósito**, para simular retrasos o cancelaciones. Emite un evento `flight`.

```bash
curl -X PATCH localhost:8080/api/flights/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"DELAYED"}'
```

Valores permitidos: `SCHEDULED`, `DELAYED`, `CANCELLED`, `SOLD_OUT`. Devuelve el vuelo actualizado. Un valor desconocido o ausente da `400`; un vuelo inexistente, `404`.
Un vuelo `CANCELLED` o `SOLD_OUT` rechaza nuevos bloqueos y confirmaciones, pero **no libera** los bloqueos que ya existían.

### 9. Eventos en tiempo real

`GET /api/stream`: conexión abierta de tipo Server-Sent Events.

```bash
curl -N localhost:8080/api/stream
```

```
event:hello
data:connected

event:seat
data:{"flightId":3,"seatNumber":"1A","status":"HELD","heldUntil":"2026-11-18T20:13:41.417Z"}

event:stats
data:{"flightId":3,"total":48,"available":47,"held":1,"booked":0}

event:flight
data:{"flightId":2,"status":"CANCELLED"}
```

| Evento | Cuándo | Contenido |
|---|---|---|
| `hello` | Al conectar | Texto `connected` |
| `seat` | Un asiento se bloquea, libera, vence o reserva | `flightId`, `seatNumber`, `status`, `heldUntil` |
| `stats` | Después de cada `seat` | Los mismos campos que el endpoint de métricas |
| `flight` | El vuelo cambia de estado (incluido `SOLD_OUT` automático) | `flightId`, `status` |

- Es **un solo stream para todos los vuelos**: cada evento trae `flightId` y tú filtras.
- El servidor envía un comentario `: ping` cada 15 s para detectar conexiones muertas.
- Los eventos se emiten **solo después de confirmar el cambio en la base de datos**. Un pago rechazado no genera ninguno.
- Si te desconectas, **los eventos perdidos no se reenvían**: al reconectar, vuelve a pedir el estado con los endpoints 3 y 4.

En el navegador:

```js
const source = new EventSource('/api/stream');
source.addEventListener('seat', (e) => console.log(JSON.parse(e.data)));
```

### 10. Recuperar una reserva por su código

`GET /api/reservations/{code}`

Sirve para volver a ver el boleto después de cerrar la pantalla de confirmación.

```bash
curl localhost:8080/api/reservations/RSV-6367CA45
```

```json
{
  "code": "RSV-6367CA45",
  "passengerName": "Ana Gómez",
  "maskedEmail": "a**@example.com",
  "seatNumber": "3C",
  "amount": 189000.00,
  "createdAt": "2026-11-18T19:13:13.491Z",
  "flight": {
    "id": 1, "flightNumber": "AV101", "origin": "BOG", "destination": "MDE",
    "departureTime": "2026-11-19T07:30:00", "arrivalTime": "2026-11-19T08:25:00",
    "status": "SCHEDULED"
  }
}
```

- El código **no distingue mayúsculas** y se ignoran los espacios alrededor, porque suele pegarse.
- El **correo sale enmascarado**. Los datos de la tarjeta no se guardan, así que no existen para devolver.
- `flight.status` es el estado **actual**: si el vuelo se cancela después de comprar, aquí se ve.
- `404` si ningún código coincide.
- **El código es la única credencial** (no hay login). Son 8 caracteres hexadecimales y no hay límite de intentos, así que un atacante podría probar códigos. Es aceptable para un kata; ver [decisions.md §10](../docs/shared/decisions.md#10-identidad-del-cliente).

## 4. Formato de errores

Todos los errores usan el mismo formato (RFC 9457):

```json
{
  "title": "Conflict",
  "status": 409,
  "detail": "Seat 3C is not available",
  "instance": "/api/flights/1/seats/3C/hold"
}
```

| Código | Significado |
|---|---|
| 400 | Datos inválidos: cuerpo ausente o mal formado, validación fallida, parámetro obligatorio ausente, fecha o enum inválido. Hoy `detail` es un mensaje general (no lista campo por campo) |
| 402 | Pago rechazado |
| 404 | Vuelo, asiento o reserva inexistente |
| 409 | Conflicto: asiento no disponible, bloqueo inexistente o vencido, vuelo no reservable |

## 5. Flujo completo de ejemplo

```bash
H='Content-Type: application/json'
B=localhost:8080/api/flights

curl "$B?origin=BOG&destination=MDE"                                   # 1. elegir vuelo (id 1)
curl "$B/1/seats?clientId=ana-123"                                    # 2. ver asientos
curl -X POST $B/1/seats/3C/hold -H "$H" -d '{"clientId":"ana-123"}'   # 3. bloquear
curl -X POST $B/1/reservations -H "$H" -d '{
  "clientId":"ana-123","seatNumber":"3C","passengerName":"Ana Gómez",
  "email":"ana@example.com","cardNumber":"4111111111111111"}'         # 4. pagar
curl $B/1/stats                                                       # 5. ver ocupación
```

En otra terminal, `curl -N localhost:8080/api/stream` mostrará los eventos de cada paso.

## 6. Estructura del código

```
src/main/java/com/airline/reservations/
  domain/        Entidades (Flight, Seat, Reservation) y repositorios
  application/   Servicios (reglas), eventos, excepciones, puertos
  adapter/in/web/        Controladores REST, DTOs, manejo de errores
  adapter/out/           Pago simulado y envío de eventos SSE
  config/        Datos de ejemplo (DataSeeder)
src/main/resources/application.properties
```

Para saber por qué está organizado así y qué alternativas había: [../docs/architecture.md](../docs/architecture.md) y [../docs/shared/decisions.md](../docs/shared/decisions.md).
