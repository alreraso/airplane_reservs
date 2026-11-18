# Decisiones técnicas y alternativas

Este archivo responde una pregunta por cada decisión: **¿por qué esto y no otra cosa?**
Cada entrada sigue la misma plantilla para que sirva también como guía en proyectos futuros:

- **Contexto**: qué problema obligaba a decidir.
- **Elegimos**: lo que hay hoy en el código.
- **Por qué**: la razón concreta para *este* proyecto (pequeño, una instancia, aprendizaje).
- **Alternativas**: otras opciones reales, con su costo.
- **Cuándo cambiar**: la señal que indica que la decisión ya no sirve.

> Regla general del proyecto: **elegir lo más simple que cumpla el requisito, y dejar escrito el camino de salida.**
> Un proyecto pequeño no necesita Kafka ni microservicios, pero sí debe saber cuándo los necesitaría.

Índice

1. [Estilo de arquitectura](#1-estilo-de-arquitectura)
2. [Organización de paquetes](#2-organización-de-paquetes)
3. [Evitar la sobre-reserva (concurrencia)](#3-evitar-la-sobre-reserva-concurrencia)
4. [Expiración del bloqueo temporal](#4-expiración-del-bloqueo-temporal)
5. [Tiempo real: SSE](#5-tiempo-real-sse)
6. [Eventos internos y momento de publicación](#6-eventos-internos-y-momento-de-publicación)
7. [Persistencia](#7-persistencia)
8. [Escalar más allá de una instancia](#8-escalar-más-allá-de-una-instancia)
9. [Pago como puerto](#9-pago-como-puerto)
10. [Identidad del cliente](#10-identidad-del-cliente)
11. [API: DTOs, errores y validación](#11-api-dtos-errores-y-validación)
12. [Configuración y tiempo](#12-configuración-y-tiempo)
13. [Frontend](#13-frontend)
14. [Herramientas de build y versiones](#14-herramientas-de-build-y-versiones)
15. [Pruebas (pendiente)](#15-pruebas-pendiente)
16. [Checklist reutilizable para un proyecto nuevo](#16-checklist-reutilizable-para-un-proyecto-nuevo)

---

## 1. Estilo de arquitectura

**Contexto.** El kata deja la arquitectura libre y pide defenderla.

**Elegimos.** Monolito modular con una arquitectura hexagonal ligera (puertos y adaptadores): un solo proceso desplegable, con las dependencias apuntando hacia el centro.

```
adapter.in.web  ──►  application  ──►  domain
                          │
adapter.out.*  ◄──────────┘   (la aplicación define puertos; los adaptadores los implementan)
```

**Por qué.**
- Una sola persona, un solo despliegue, un solo modelo de datos: un monolito es lo más barato de operar y depurar.
- La regla "la aplicación no conoce HTTP, SSE ni el proveedor de pagos" cuesta casi nada y se nota: `PaymentGateway` es una interfaz en `application.port` y `FakePaymentGateway` vive afuera.
- Es la mejor forma de *aprender* a separar responsabilidades sin pagar la complejidad de la distribución.

**Alternativas.**

| Opción | Gana en | Cuesta |
|---|---|---|
| Capas clásicas (controller → service → repository) | Es lo más simple y lo más conocido | Los servicios terminan acoplados a detalles de infraestructura |
| Hexagonal estricta (entidades JPA separadas del dominio, mappers) | Dominio 100% puro y testeable sin framework | Mucho código de mapeo; no se justifica con 4 entidades |
| Microservicios (vuelos, asientos, reservas, notificaciones) | Escalar y desplegar equipos por separado | Transacciones distribuidas: el bloqueo y la confirmación de un asiento dejarían de ser atómicos |
| Serverless | Cero servidores que administrar | Las conexiones SSE/WebSocket de larga duración encajan mal |

**Cuándo cambiar.** Al tener varios equipos que se pisan, o cuando búsqueda y reservas necesiten escalar de forma muy distinta. Primero se extrae un módulo, no se parte todo.

**Concesión consciente.** Las entidades JPA (`Flight`, `Seat`, `Reservation`) *son* el modelo de dominio, y los repositorios de Spring Data viven en `domain`. Una hexagonal purista los separaría. Aquí duplicar clases no aporta nada todavía.

---

## 2. Organización de paquetes

**Elegimos.** Paquetes por *capa hexagonal* (`domain`, `application`, `adapter.in`, `adapter.out`, `config`).

**Por qué.** Hace visible la regla de dependencias, que es lo que se quiere aprender.

**Alternativas.**
- **Por funcionalidad** (`flight/`, `seat/`, `reservation/`, cada uno con sus capas): escala mejor cuando hay muchos módulos y deja los límites claros ("arquitectura que grita su negocio").
- **Módulos Maven/Gradle separados** por capa: el compilador *impide* violar la regla de dependencias. Es más formal y más pesado.

**Cuándo cambiar.** Con más de ~6 entidades, pasar a paquetes por funcionalidad con capas adentro.

---

## 3. Evitar la sobre-reserva (concurrencia)

**Contexto.** Dos clientes pueden pulsar el mismo asiento en el mismo milisegundo. Es el requisito más importante del kata.

**Elegimos.** Una **única sentencia `UPDATE` condicional** por transición, y el número de filas afectadas dice quién ganó:

```sql
UPDATE seat SET status='HELD', held_by=:cliente, held_until=:hasta
WHERE flight_id=:v AND seat_number=:a
  AND (status='AVAILABLE' OR (status='HELD' AND (held_until < :ahora OR held_by=:cliente)))
```

1 fila = ganó. 0 filas = perdió (HTTP 409). No existe el patrón "leer, comprobar, escribir", así que no hay ventana de carrera. Verificado con 30 peticiones simultáneas: 1 respuesta 200 y 29 respuestas 409.

Red de seguridad extra: restricción `UNIQUE (flight_id, seat_number)` en `Reservation`. Si algún día un bug dejara pasar una doble reserva, la base de datos la rechaza.

**Por qué.**
- Es correcto en cualquier base de datos con transacciones y no necesita infraestructura extra.
- La base de datos es la única fuente de verdad, así que también es correcto con varias instancias de la app.
- Es más fácil de razonar que los locks en memoria.

**Alternativas.**

| Opción | Cómo funciona | Cuesta |
|---|---|---|
| Bloqueo optimista (`@Version`) | Lee la entidad, modifica, y falla con `OptimisticLockException` si alguien cambió la fila | Hay que capturar el error y reintentar; más código para el mismo resultado |
| Bloqueo pesimista (`SELECT ... FOR UPDATE`) | Bloquea la fila mientras se decide | Mantiene bloqueos y conexiones abiertas; riesgo de deadlocks; peor con mucha contención |
| `synchronized` / `ReentrantLock` en memoria | Un lock por asiento en la JVM | **Solo funciona con una instancia.** Se rompe al escalar |
| Redis (`SET NX PX`, Redlock) | Lock distribuido con TTL propio | Otra pieza que operar; la consistencia entre Redis y la base de datos pasa a ser un problema nuevo |
| Cola por vuelo (actor / single-writer) | Un solo hilo serializa todas las operaciones de un vuelo | Muy predecible, pero obliga a un diseño asíncrono completo |

**Cuándo cambiar.** Si la contención por *un mismo asiento* fuera enorme (venta tipo concierto con miles de usuarios por plaza), pasar a una cola de entrada o a Redis para absorber el pico antes de llegar a la base de datos.

---

## 4. Expiración del bloqueo temporal

**Elegimos.** Dos mecanismos que se complementan:

1. **Corrección (lazy)**: un bloqueo vencido se considera libre *en la propia consulta* (`held_until < :ahora`). Aunque el job falle, nunca se rechaza a un cliente por un bloqueo muerto, ni se confirma un bloqueo vencido (`held_until >= :ahora` en `tryConfirm`).
2. **Notificación (job)**: `HoldExpirationJob` corre cada segundo (`@Scheduled`), libera los vencidos y publica el evento para que las pantallas se actualicen.

**Por qué.** Separa *qué es verdad* (la base de datos, siempre) de *quién se entera* (el job). Un fallo del job retrasa una notificación, pero no corrompe datos.

**Alternativas.**
- **TTL de Redis con keyspace notifications**: expiración automática, pero las notificaciones no están garantizadas y se mezcla estado en dos sistemas.
- **Quartz / ShedLock**: necesarios si hay varias instancias y el job debe correr una sola vez. Con una instancia, `@Scheduled` basta.
- **Un temporizador por asiento** (`ScheduledExecutorService.schedule`): se pierde al reiniciar y consume memoria por cada bloqueo.

**Cuándo cambiar.** Con varias instancias: añadir ShedLock (o aceptar que el job corra en todas, porque `tryExpire` es idempotente y solo una gana por asiento).

---

## 5. Tiempo real: SSE

**Contexto.** Los clientes deben ver al instante bloqueos, liberaciones, reservas y cambios de estado del vuelo.

**Elegimos.** **Server-Sent Events** con `SseEmitter` de Spring MVC, en **un solo stream global** (`/api/stream`). Cada evento lleva `flightId` y el navegador filtra lo que le interesa. Eventos: `seat`, `stats`, `flight`.

**Por qué.**
- El flujo es **unidireccional** (servidor → navegador). Las acciones del cliente ya viajan por REST, que es más simple de validar y de testear.
- `EventSource` en el navegador reconecta solo, y funciona sobre HTTP normal (proxies, CORS, autenticación igual que el resto).
- Un stream global evita administrar suscripciones por vuelo. Con pocos clientes, el tráfico extra es irrelevante.
- Tras reconectar, el cliente vuelve a consultar el estado completo, así que perder eventos no deja la pantalla desincronizada.

**Alternativas.**

| Opción | Gana en | Cuesta |
|---|---|---|
| WebSocket + STOMP (`spring-boot-starter-websocket`) | Bidireccional, topics por vuelo (`/topic/flights/1`), muy común en entrevistas | Más configuración, manejo de reconexión y de heartbeats propio; no se necesita enviar mensajes por el socket |
| WebSocket nativo / Socket.IO | Control total o ecosistema Node | Hay que construir el protocolo de mensajes |
| Polling (cada 1–2 s) | Trivial, funciona detrás de cualquier infraestructura | Latencia, carga constante y desperdicio |
| Long polling | Más eficiente que el polling | Complejidad similar a SSE sin sus ventajas |

**Variante de SSE por vuelo.** Un endpoint `/api/flights/{id}/stream` y un hub con `Map<flightId, Set<SseEmitter>>`. Se justifica cuando hay cientos de vuelos activos y no se quiere enviar a cada cliente eventos que no le importan.

**Cuándo cambiar.** Pasar a STOMP si el cliente tuviera que *enviar* mensajes en vivo (chat con soporte, edición colaborativa) o si el producto ya usa un broker.

**Detalle importante.** `SseHub.heartbeat()` envía un comentario `ping` cada 15 s. Sirve para detectar conexiones muertas y para evitar que proxies intermedios cierren conexiones inactivas.

---

## 6. Eventos internos y momento de publicación

**Elegimos.** Los servicios publican eventos de dominio con `ApplicationEventPublisher`, y `RealtimeEventPublisher` los escucha con `@TransactionalEventListener(phase = AFTER_COMMIT)`.

**Por qué.** El caso que lo justifica está en el código: en `confirm`, el asiento se reclama y *después* se cobra. Si el pago se rechaza, la transacción hace rollback. Con un listener normal, los clientes habrían visto "Ocupado" durante un instante por algo que nunca ocurrió. Con `AFTER_COMMIT`, solo se anuncia lo que quedó confirmado. Verificado: un pago rechazado no emite ningún evento.

**Alternativas.**
- **Notificar directamente desde el servicio**: más simple, pero puede anunciar cambios que luego se revierten.
- **Patrón Outbox** (guardar el evento en una tabla dentro de la misma transacción y publicarlo después): garantiza que el evento nunca se pierda si la app cae justo tras el commit. Hoy ese riesgo es aceptable porque el cliente se resincroniza al reconectar.
- **Broker (Kafka, RabbitMQ)**: necesario para comunicar varios servicios o instancias; excesivo para una sola.

**Cuándo cambiar.** Outbox + broker cuando otro sistema (correo, facturación, analítica) deba reaccionar a las reservas de forma fiable.

---

## 7. Persistencia

**Elegimos.** H2 en memoria, Spring Data JPA, `ddl-auto=create-drop`, y datos de demostración cargados por `DataSeeder`.

**Por qué.** Cero instalación: `./mvnw spring-boot:run` y funciona. Los datos se reinician en cada arranque, perfecto para una demo.

**Alternativas.**
- **PostgreSQL** (con Docker Compose): el paso natural. El `UPDATE` condicional es SQL estándar y funciona igual. Lo que cambia: dependencia `postgresql`, URL, y reemplazar `create-drop` por migraciones.
- **Flyway o Liquibase**: control versionado del esquema. Obligatorio apenas haya datos que conservar; `ddl-auto=update` no es una opción segura en producción.
- **JDBC / jOOQ en lugar de JPA**: SQL explícito y sin "magia". Atractivo porque las partes críticas ya son SQL a mano, pero JPA da el resto del CRUD gratis.
- **Redis como almacén de bloqueos**: ver el punto 3.

**Cuándo cambiar.** En cuanto los datos deban sobrevivir a un reinicio: PostgreSQL + Flyway.

**Nota de aprendizaje.** `spring.jpa.open-in-view=false` evita mantener la conexión abierta durante el renderizado de la respuesta y obliga a pensar dónde empieza y termina cada transacción.

---

## 8. Escalar más allá de una instancia

**Estado actual.** Todo funciona con **una** instancia. El único estado en memoria es la lista de conexiones SSE en `SseHub`. La corrección de las reservas no depende de ella, porque está en la base de datos.

**Qué se rompería con dos instancias.** Un cambio hecho en la instancia A solo se enviaría a los clientes conectados a A.

**Camino de salida.**
1. Publicar los eventos en **Redis Pub/Sub** (o un broker) y que cada instancia los reenvíe a sus propios clientes SSE.
2. Poner un balanceador sin necesidad de sesión pegajosa (no hay sesión: la identidad viaja en cada petición).
3. Ejecutar el job de expiración con ShedLock, o dejarlo en todas las instancias (es idempotente).

**Alternativa.** Usar un servicio gestionado de tiempo real (Ably, Pusher, AWS AppSync) y dejar de mantener conexiones largas. Cuesta dinero y añade dependencia externa.

---

## 9. Pago como puerto

**Elegimos.** Interfaz `PaymentGateway` en `application.port`, implementada por `FakePaymentGateway` (rechaza la tarjeta `4000000000000002`).

**Por qué.** Es el ejemplo más claro de inversión de dependencias del proyecto: la lógica de reserva no sabe quién cobra. Cambiar a un proveedor real es crear un adaptador nuevo sin tocar `SeatBookingService`.

**Alternativas y advertencia.**
- Con un proveedor real, **no se debe llamar a un servicio externo dentro de una transacción de base de datos** que mantiene filas bloqueadas, porque una respuesta lenta retiene la conexión. El flujo habitual es: reservar → cobrar con una llave de idempotencia → confirmar, con un estado intermedio `PENDING_PAYMENT` y compensación si el cobro falla.
- Para este kata, mantener todo en una transacción es más simple y correcto, porque el pago es instantáneo.
- Nunca guardar ni registrar en logs el número de tarjeta. Aquí solo se valida el formato y se descarta.

---

## 10. Identidad del cliente

**Elegimos.** Un `clientId` anónimo (UUID) generado por el navegador y guardado en `sessionStorage`. Es por *pestaña*, así que dos pestañas actúan como dos clientes distintos, lo que facilita la demostración.

**Por qué.** El kata no incluye usuarios ni login. El identificador solo prueba "este bloqueo es mío". El servidor nunca lo expone a otros clientes (`SeatResponse` solo dice `heldByMe`).

**Recuperar una reserva (`GET /api/reservations/{code}`).** El código `RSV-XXXXXXXX` es la única credencial: quien lo conoce puede leer el boleto. Para limitar el daño, el correo se devuelve enmascarado y los datos de tarjeta nunca se guardan. Son 32 bits de código sin límite de intentos, así que se podrían adivinar con paciencia. En producción: código más largo y aleatorio (por ejemplo, 128 bits), límite de intentos por IP y, mejor aún, que la consulta exija además el correo completo o un usuario autenticado.

**Limitación.** Cualquiera que conozca el `clientId` de otro podría liberar o confirmar su asiento. Es aceptable para un prototipo.

**Alternativas.** Spring Security con JWT u OAuth2 y asociar el bloqueo al usuario autenticado. Es obligatorio en cualquier despliegue real, y también para proteger el endpoint de cambio de estado del vuelo (`PATCH /api/flights/{id}/status`) y la pestaña **Admin** del frontend que lo usa. Hoy son abiertos a propósito para poder simular retrasos.

---

## 11. API: DTOs, errores y validación

**Elegimos.**
- **DTOs como `record`**: el API no expone entidades JPA. Así se puede cambiar la base de datos sin romper el contrato y se evita publicar campos internos como `heldBy`.
- **`ProblemDetail`** (RFC 9457, soportado por Spring) para errores, centralizado en `ApiExceptionHandler`. Mapa de códigos: 404 no existe, 409 conflicto (asiento ocupado, bloqueo inválido, vuelo no reservable), 402 pago rechazado, 400 datos inválidos.
- **Bean Validation** (`@Valid`, `@NotBlank`, `@Email`, `@Pattern`) en el borde del sistema.
- **Excepciones de negocio sin HTTP**: viven en `application.exception`. El adaptador web decide el código de estado, no el servicio.

**Alternativas.** Un formato de error propio (`{code, message}`): válido, pero hay que documentarlo y mantenerlo. Devolver `ResponseEntity` desde cada controlador: repite lógica. Usar `Optional`/`Result` en lugar de excepciones: más explícito, más ceremonia en Java.

**Idempotencia.** `release` no falla si no hay nada que liberar. Un reintento por mala red no genera errores.

---

## 12. Configuración y tiempo

**Elegimos.**
- `@ConfigurationProperties` sobre un `record` (`ReservationProperties`) para `hold-duration` y `expiration-check-ms`. Es tipado, validable y se puede cambiar por línea de comandos: `--reservations.hold-duration=8s`.
- Un bean `Clock` inyectado en lugar de llamar a `Instant.now()` directamente.

**Por qué.** El tiempo es una dependencia. Con `Clock` inyectado, una prueba puede congelar o avanzar el reloj en vez de esperar 5 minutos reales.

**Alternativas.** `@Value("${...}")` (válido para un valor suelto, desordenado con varios). Perfiles de Spring (`application-prod.properties`) cuando existan varios entornos.

---

## 13. Frontend

**Contexto.** El kata pide un frontend en Angular o React, con un mapa de asientos que se actualice en vivo.

**Elegimos.** **React 19 + Vite 8 + TypeScript**, en `frontend/`, sin librerías de componentes ni de estado. Solo `react` y `react-dom` como dependencias de ejecución.

- **Desarrollo:** `npm run dev` levanta Vite en `:5173`, que reenvía `/api` (incluido el stream SSE) al backend. Mismo origen para el navegador, así que no hay CORS.
- **Todo en un proceso:** `npm run build` genera `frontend/dist`, y Spring lo sirve (`spring.web.resources.static-locations`). Solo se necesita Node para construirlo, no para ejecutarlo.

**Por qué.**
- Es lo que pide el kata, y Vite es el estándar actual para empezar un proyecto React sin framework de servidor.
- **TypeScript** tipa los DTOs (`src/api/types.ts`) según `openapi.yaml`. Si el contrato cambia, el compilador marca lo que hay que tocar.
- **Sin librerías extra:** `fetch` y `EventSource` son nativos. El estado cabe en hooks y `useState`, porque el servidor es la fuente de verdad y el cliente solo refleja.

**Cómo está organizado**

| Carpeta | Contenido |
|---|---|
| `src/api` | Cliente HTTP y tipos del contrato |
| `src/realtime` | **Una sola** conexión SSE para toda la app. Los componentes se suscriben con `useStreamEvent` |
| `src/hooks` | Datos con tiempo real: `useLiveFlights`, `useFlightLive`, `useSeatMap`, `useCountdown` |
| `src/components` | Piezas de presentación (mapa de asientos, formulario de pago, boleto, barras) |
| `src/pages` | Vuelos, Dashboard y Admin |

**Decisiones de diseño que importan**
- **Una conexión SSE compartida** en un `StreamProvider`. Abrir un `EventSource` por componente multiplicaría conexiones y reconexiones.
- **REST carga, SSE parcha.** Cada hook carga el estado por REST y los eventos actualizan solo lo que cambió. Al reconectar (evento interno `open`) se recarga todo, porque el servidor no reenvía lo perdido.
- **`heldByMe` es estado local.** El servidor nunca revela quién bloquea un asiento, así que el cliente recuerda el suyo al bloquearlo. Al vencer o liberarse, los eventos lo limpian.
- **Las tres páginas permanecen montadas** (solo se ocultan). Cambiar de pestaña conserva el vuelo abierto y el asiento bloqueado, y todas siguen recibiendo eventos.
- **Aviso de cambios del vuelo y de expiración:** el usuario recibe un mensaje cuando el vuelo se cancela o se retrasa, o cuando su bloqueo vence. Si el vuelo deja de ser reservable, el formulario de pago se oculta.

**Alternativas.**

| Opción | Gana en | Cuesta |
|---|---|---|
| HTML + JS sin framework (la primera versión de este proyecto) | Cero herramientas, un solo comando | No cumple el kata. El estado manual en el DOM se vuelve difícil de seguir al crecer |
| Angular | Estructura, DI, tipado, estándar corporativo | Más ceremonia y archivos para este tamaño |
| Next.js / Remix | Rutas, SSR | El servidor ya existe y no hace falta SEO |
| TanStack Query | Caché, reintentos, sincronización de datos | Una dependencia más. Sería el siguiente paso natural para reemplazar los `useEffect` de carga |
| Redux / Zustand | Estado global explícito | El estado compartido es poco y vive en el servidor |
| React Router | URLs por pantalla (`/admin`, `/flights/3`) | Hoy las 3 pestañas se resuelven con un `useState`. Conviene al querer enlaces compartibles |

**Cuándo cambiar.** Añadir **React Router** cuando se necesiten URLs enlazables, y **TanStack Query** cuando haya muchas consultas con caché o reintentos. Para varios equipos, generar los tipos desde `openapi.yaml` en lugar de mantenerlos a mano. La carpeta `shared/` del kata sirve para eso.

**Verificación.** Un navegador real (Playwright) con dos clientes comprobó: bloqueo visible en el otro cliente, expiración, pago rechazado, boleto, carrera por un asiento, dashboard y cancelación de un vuelo con aviso. No se incluyó en el repositorio (ver §15).

---

## 14. Herramientas de build y versiones

**Elegimos.** Maven con **Maven Wrapper** (`mvnw`), Java 17, Spring Boot 4.1.x (Spring Framework 7).

**Por qué.** El wrapper descarga la versión exacta de Maven: nadie debe instalar nada salvo un JDK. Java 17 es la versión mínima de Spring Boot 3 y 4 y ya estaba instalada.

**Alternativas.** Gradle (más flexible y rápido en proyectos grandes, DSL más difícil de leer); Java 21 (hilos virtuales, `spring.threads.virtual.enabled=true`, interesante para muchas conexiones SSE abiertas).

**Aviso de versión.** Spring Boot 4 reorganizó los módulos: el starter web ahora se llama `spring-boot-starter-webmvc`, y muchos tutoriales de Boot 3 mencionan nombres que ya no aplican. Si algo de un tutorial no compila, revisa primero la versión.

---

## 15. Pruebas (pendiente)

No se incluyeron pruebas automáticas todavía; la verificación se hizo con ejecución real (`curl` concurrente). Si se añaden, este es el orden de valor:

1. **Concurrencia**: N hilos intentan `hold` del mismo asiento, exactamente uno gana. Es la prueba que defiende el requisito principal del kata.
2. **Expiración**: con `Clock` controlado, un bloqueo vencido no se puede confirmar, y el job lo libera.
3. **Rollback del pago**: pago rechazado deja el asiento `HELD` y no emite eventos.
4. **Capa web**: `@WebMvcTest` para códigos de estado y validación.
5. **Repositorio**: `@DataJpaTest` para las consultas condicionales.

---

## 16. Checklist reutilizable para un proyecto nuevo

Preguntas que conviene responder y anotar *antes* de escribir código:

1. **¿Qué invariante nunca puede romperse?** (aquí: un asiento, un dueño). Decidir *quién* la garantiza: base de datos, no la memoria.
2. **¿Cuántas instancias habrá?** Si es una, no pagues la complejidad de la distribución, pero apunta qué estado en memoria habría que mover.
3. **¿Quién necesita enterarse de los cambios y con qué retraso?** Eso decide SSE, WebSocket o polling.
4. **¿Qué pasa si falla a mitad del proceso?** Define el límite de la transacción y cuándo se publican los eventos.
5. **¿Qué depende del reloj, de la red o de un tercero?** Ponlo detrás de una interfaz inyectable (`Clock`, `PaymentGateway`).
6. **¿Qué datos no deben salir por el API?** Usa DTOs desde el primer día.
7. **¿Cómo se ve un error para el cliente?** Un solo formato, un solo lugar donde se traduce.
8. **¿Cuál es la señal de que esta decisión ya no sirve?** Escríbela junto a la decisión (sección "Cuándo cambiar").
