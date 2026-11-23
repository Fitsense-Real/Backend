# 03 · Generaciones de más de 30 s y la pantalla de Kora

## 1. Tiempos medidos con gpt-5-mini y `reasoning_effort=medium` (pruebas reales, `migracion_gpt5_mini/04`)

| | Mediana | Máximo |
|---|---|---|
| Por llamada de generación | 81,1 s | 122,9 s |
| Generación completa por perfil, con reintentos y respaldo | 81,7 s | 292,7 s |
| Por llamada de adaptación (en el cierre semanal, no en la app) | 29,1 s | 42,8 s |

**Una generación inicial con IA casi nunca termina en 30 s.**

## 2. Límites del backend (`application.properties`, sin cambios en esta tarea)

| Límite | Valor | Efecto |
|---|---|---|
| `fitsense.ai.timeout-seconds` | 150 | Sondeo de **una** predicción. Al vencer, se cancela (R-2) y cuenta como intento fallido |
| `prefer-wait-seconds` / `read-timeout-seconds` | 60 / 90 | POST a Replicate |
| `AI_ATTEMPTS` | 3 | Después, el motor de reglas |
| `fitsense.plan-generation.max-processing-seconds` | 1.800 | Vida máxima de una **operación asíncrona** desde su primera ejecución (V59) |
| `max-queued-seconds` / `max-executions` / `lease-seconds` | 1.800 / 3 / 90 | Cola, reanudaciones y propiedad del worker |
| `fitsense.ai.generate.total-budget-seconds` | 0 (desactivado) | Presupuesto total de la fase de IA |

- **Peor caso de una operación:** 3 × ~331 s + 120 s ≈ 1.113 s, dentro de los 1.800 s.
- **Ninguno de estos límites tiene que ver con 30 s.** No se tocaron. Aumentarlos no hace falta: el máximo medido es de 292,7 s.

## 3. ¿Qué API usa la app?

`Fitsense-Real/MobileApp`, leído sin modificarlo.

| Rama de Mobile | Cliente de generación | Espera |
|---|---|---|
| `generation` (`a7516b2`, 23/11, con la PR #17 de FCM). **Es la que tiene la pantalla de 30 s** | **Asíncrono:** `POST /api/v1/users/me/plan/generation` y `GET …/{operationId}` cada `pollAfterSeconds` (3 s), con espera creciente hasta 20 s si hay errores de red o 5xx | `generationScreenDuration = 30 s`, solo **visual** |
| `main` (`3a4e26d`, 06/11) | Síncrono: `POST /api/v1/users/me/plan/generate` con `receiveTimeout` de 5 min | Animación sin límite de 30 s |

## 4. Comportamiento con la pantalla de 30 s (rama `generation`)

Leído en `onboarding_page.dart` (`submit` y `settleGeneration`), `first_plan_generation_controller.dart` y `plan_generation_repository.dart`.

### Comprobado contra el backend en las pruebas de integración (todas en verde en la regresión)

| Pregunta de la orden | Comportamiento | Evidencia |
|---|---|---|
| ¿Puede seguir la generación después de 30 s? | **Sí.** El POST responde en milisegundos con 202 y la operación `PENDING`. El worker la ejecuta aparte durante el tiempo que necesite, hasta 1.800 s | `GeneracionAsincronaIT`, `GeneracionAsincronaResilienciaIT`: «generaciones VIVAS de 30, 90, 300, 600, 900 y 1500 s no se dan por vencidas» |
| ¿Hay solicitudes duplicadas si el usuario espera, reabre o reintenta? | **No.** Un `POST` repetido devuelve la **misma** operación activa (200), o la `COMPLETED` si el plan ya existe. Ocho solicitudes simultáneas crean una sola operación. Un reintento solo crea otra si la anterior es terminal y reintentable, y **adopta las predicciones ya pagadas** (24 h) | E6, E1/E2, E13/E15, R15/R20 |
| ¿Se conserva y se puede consultar el estado? | **Sí.** `GET /{operationId}` y `GET /latest` dan `status`, `terminal`, `planId`, `reasonCode`, `message`, `retryable` y `pollAfterSeconds`. La app guarda `operationId` (`welcome_store`) y lo recupera al reabrir | E1/E2 («la app se cierra y vuelve: latest devuelve LA MISMA y un POST no crea otra») |
| ¿Puede una respuesta tardía publicar dos planes o dejar estados inconsistentes? | **No.** Hay un solo plan por operación. Una ejecución que pierde el lease no publica (fencing). Si el proceso muere dentro de la publicación, la reanudación publica **una**. Un resultado tardío tras el máximo **no** se publica | E5, E11, E16, R16, R01/R18 |
| ¿Qué pasa si Replicate falla, agota el tiempo o responde algo inválido? | Se reintenta (hasta 3 intentos). Si no, el **respaldo** validado del motor de reglas, registrado `RULE_ENGINE`. Un 503 en el sondeo se cancela sin bucles. Un 4xx da `ERROR_ENVIO` sin predicción creada. Si el id no llega, `NO_CONFIRMADA` y no se reenvía. La operación termina `COMPLETED` con respaldo o `FAILED` reintentable; nunca queda colgada | R07, R11, R12, R14/R10, `ReplicateRespuestasInvalidasTest` |
| ¿Se reanuda sin pagar dos veces con la petición de gpt-5-mini? | **Sí.** El libro V59 y la reanudación se prueban ahora con la petición directa de producción | `ReanudacionConReplicateSimuladoIT` (12 casos), cambiada en esta tarea al endpoint `gpt-5-mini` |

### Comprobado en el código de la app (sin ejecutarla)

| Pregunta de la orden | Comportamiento |
|---|---|
| ¿La pantalla da por fallida una operación válida a los 30 s? | **No.** A los 30 s, `settleGeneration` decide según el estado:<br>• COMPLETED: continúa;<br>• error o estado terminal: lo muestra;<br>• en curso: muestra **«pendiente»** y ofrece «continuar» al recorrido de bienvenida mientras sigue consultando.<br>Un `ref.listen` sobre la generación vuelve a decidir con cada cambio. **Nunca muestra un plan que no existe** |
| ¿Duplica solicitudes al perder la red? | **No.** Un error de red o un 5xx solo reprograma el `GET` con espera creciente (3 → 20 s). No vuelve a hacer `POST` |
| ¿Trata como error un estado que no conoce? | **No.** Un estado desconocido cuenta como no terminal |

**Conclusión:** con la rama `generation` de Mobile, **las dos implementaciones son compatibles**. Una generación de 81 s, o de 292 s, termina en «pendiente» y después en COMPLETED, sin duplicados ni errores.

**Pendiente:** la app no se ha ejecutado en esta sesión. La compatibilidad se verifica contra su código y contra el contrato con MockMvc.

## 5. Riesgo con la rama `main` de Mobile (síncrona)

- Si se publicara una versión de la app basada en `main`, la llamada síncrona espera como máximo **300 s**.
- El máximo medido con gpt-5-mini `medium` es **292,7 s**: el margen es de 7 s.
- **Si la llamada se pasa de 300 s:**
  - la app cancela y muestra «La rutina no llegó en cinco minutos»;
  - el backend **termina y publica** el plan igual (el síncrono no se cancela al cortarse el cliente);
  - al reintentar **después** de que el backend publique, la app recibe 409 y lee el plan con `GET /current`, que su código ya contempla;
  - si reintenta **mientras** el backend sigue generando, recibe 409 (R-5, generación en curso), `GET /current` todavía no tiene plan y la app vuelve a mostrar el error;
  - **no hay plan duplicado**, solo una experiencia de error.
- **Recomendación:** publicar la app desde la rama `generation`, con el cliente asíncrono (D-M2 en `04_DEPENDENCIAS_MOBILE.md`).
- **No se aumentó ningún límite del backend.**
