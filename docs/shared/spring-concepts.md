# Mapa de conceptos de Spring en este proyecto

Para estudiar: cada concepto con el archivo exacto donde verlo y la pregunta que conviene poder responder.

| Concepto | Dónde verlo | Pregunta de entrevista |
|---|---|---|
| `@SpringBootApplication`, auto-configuración | `ReservationsApplication` | ¿Qué hace por dentro esa anotación? (`@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`) |
| Inyección de dependencias por constructor | Cualquier `@Service` (`SeatBookingService`) | ¿Por qué constructor y no `@Autowired` en el campo? (inmutabilidad, pruebas sin Spring, dependencias obligatorias visibles) |
| Estereotipos (`@Service`, `@Component`, `@RestController`) | `application/`, `adapter/` | ¿Hay diferencia funcional entre ellos? (casi ninguna, es semántica; `@RestController` = `@Controller` + `@ResponseBody`) |
| `@Bean` y `@Configuration` | `Clock clock()` en `ReservationsApplication` | ¿Cuándo usar `@Bean` y no `@Component`? (clases que no son tuyas, o construcción personalizada) |
| `@ConfigurationProperties` | `ReservationProperties` | ¿Ventaja sobre `@Value`? (tipado, agrupado, validable) |
| Spring Data JPA, consultas derivadas | `SeatRepository.countByFlightIdAndStatus` | ¿Cómo convierte el nombre del método en SQL? |
| `@Query` con JPQL y `@Modifying` | `SeatRepository.tryHold` | ¿Por qué `clearAutomatically`? (el contexto de persistencia queda desfasado tras un UPDATE masivo) |
| `@Transactional` | `SeatBookingService` | ¿Qué provoca un rollback? (excepción no controlada, por defecto solo `RuntimeException`) ¿Por qué falla una llamada interna a un método `@Transactional` de la misma clase? (proxy) |
| Eventos de aplicación | `ApplicationEventPublisher` en los servicios | ¿Diferencia entre `@EventListener` y `@TransactionalEventListener`? |
| `@TransactionalEventListener(AFTER_COMMIT)` | `RealtimeEventPublisher` | ¿Qué se evita con `AFTER_COMMIT`? (anunciar cambios que luego se revierten) |
| `@Scheduled` + `@EnableScheduling` | `HoldExpirationJob` | ¿`fixedDelay` o `fixedRate`? (fixedDelay espera a que termine la ejecución anterior) |
| Bean Validation | `ReservationRequest` + `@Valid` en el controlador | ¿Dónde se valida y dónde se dejan las reglas de negocio? |
| `@RestControllerAdvice` y `ProblemDetail` | `ApiExceptionHandler` | ¿Por qué centralizar el manejo de errores? |
| Server-Sent Events | `SseHub`, `StreamController` | ¿SSE o WebSocket? ¿Qué pasa con las conexiones al escalar? |
| `ApplicationRunner` | `DataSeeder` | ¿Cuándo se ejecuta respecto del arranque? (tras crear el contexto) |
| Propiedades y perfiles | `application.properties` | ¿Cómo sobrescribir una propiedad sin editar el archivo? (argumento `--clave=valor`, variable de entorno) |
| Entidades JPA | `Flight`, `Seat`, `Reservation` | ¿Por qué un constructor `protected` vacío? (lo exige JPA) |
| Restricciones de base de datos | `@UniqueConstraint` en `Reservation` | ¿Por qué no basta con validar en el código? (la base de datos es la última defensa) |

## Ejercicios sugeridos (en orden de dificultad)

1. Cambia `reservations.hold-duration` a `20s` y observa el vencimiento desde dos pestañas.
2. Añade un campo `cabinClass` a `Seat` y exponlo en `SeatResponse`.
3. Cambia H2 por PostgreSQL con Docker Compose (ver decisiones §7).
4. Escribe la prueba de concurrencia de decisiones §15 con `ExecutorService` y `CountDownLatch`.
5. Reemplaza el stream global por uno por vuelo (decisiones §5).
6. Sustituye `FakePaymentGateway` por un segundo adaptador y elige uno con un perfil de Spring.
