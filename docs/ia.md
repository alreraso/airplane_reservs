# Uso de Inteligencia Artificial

## 1. Herramientas utilizadas

| Herramienta | Modelo | Uso |
|---|---|---|
| Kiro (agente dentro del IDE) | Selección automática del modelo (no se fija una versión concreta) | Generación del backend, frontend y documentación; ejecución de comandos para verificar |

## 2. Prompts y casos de uso

Instrucciones clave dadas al agente:

1. *"La idea es resolver el kata del PDF para entender mejor Java Spring Boot y algunas estructuras de arquitectura."*
2. *"Tú lo haces todo, pero enseñándome: por qué usamos cierta estructura o arquitectura, y si usamos algo porque es un proyecto pequeño, dejar en un archivo la opción para el futuro."*

Casos de uso concretos:

- **Lectura del enunciado**: el PDF no contenía texto seleccionable (solo imágenes). El agente lo convirtió a imágenes de cada página para leerlo.
- **Diseño**: propuesta de alternativas con costos antes de escribir código (hexagonal ligera, SSE vs WebSocket, estrategias de bloqueo), documentadas en [decisions.md](shared/decisions.md).
- **Generación de código**: entidades, repositorios, servicios, controladores y el frontend en React + Vite + TypeScript.
- **Verificación en navegador real** (Playwright, dos clientes a la vez): bloqueo visible en el otro cliente, expiración, pago rechazado, boleto, carrera por un asiento, dashboard y cancelación de un vuelo.
- **Verificación con ejecución real**, no solo compilación: 30 peticiones concurrentes sobre un asiento (1 ganó, 29 recibieron 409), expiración del bloqueo, rollback por pago rechazado, validación, y recepción de eventos por el stream SSE.
- **Documentación**: arquitectura con diagramas Mermaid, mapa de conceptos de Spring.

## 3. Refactorización y criterio propio

Casos reales de esta sesión donde hubo que revisar o corregir el trabajo de la IA:

- **Versión de Spring Boot**: Spring Initializr entregó Spring Boot 4.1.2, más nuevo que el conocimiento previo del modelo (por ejemplo, el starter web ahora es `spring-boot-starter-webmvc`). En lugar de asumir nombres de Boot 3, se leyó el `pom.xml` generado antes de escribir código.
- **Limpieza**: un nombre de clase totalmente calificado quedó dentro de `SeatBookingService` y se reemplazó por un `import`.
- **Orden de arranque**: al probar, `/` respondía 404 porque `index.html` se creó con la aplicación ya iniciada (Spring resuelve la página de bienvenida al arrancar). Se confirmó reiniciando; no era un error de configuración.

> **Completar por quien entrega:** añade aquí tus propias correcciones. Ejemplos de lo que un evaluador espera: una decisión de la IA que cambiaste (y por qué), código que descartaste, o un riesgo que detectaste al revisar (por ejemplo, el `clientId` anónimo o el endpoint de estado abierto, ver decisions §10).

## 4. Impacto

Lo que no se puede afirmar con honestidad desde aquí es el tiempo ahorrado.

> **Completar por quien entrega:** estimación propia de tiempo ahorrado y áreas donde la IA aportó más valor.

Observaciones objetivas:

- Mayor valor percibido: boilerplate (DTOs, controladores, frontend), documentación estructurada y la comparación de alternativas.
- Menor valor sin supervisión: decisiones de concurrencia y de límites transaccionales, que deben entenderse y poder defenderse en la sustentación, no solo aceptarse.
- La IA no sustituye entender el *porqué*: por eso [decisions.md](shared/decisions.md) y [spring-concepts.md](shared/spring-concepts.md) existen.
