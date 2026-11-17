# Compatibilidad con Flutter y el recorrido de Kapi

**El contrato REST no cambia con #72.** Las rutas, códigos, campos y estados son los mismos, y
`PlanGenerationOperationResource` es idéntico al de #71. No se ha modificado MobileApp.

## 1. Contrato (el de #71, sin cambios)

| Método y ruta | Respuesta |
|---|---|
| `POST /api/v1/users/me/plan/generation` | **202** operación nueva `PENDING`. **200** si ya había una activa (la misma) o el plan ya existe (`COMPLETED` con `planId`). **422** sin perfil o no elegible. |
| `GET /api/v1/users/me/plan/generation/{operationId}` | **200** estado. **404** si no existe o es de otro usuario. **400** si el id no es válido. |
| `GET /api/v1/users/me/plan/generation/latest` | **200** última operación. **404** si nunca pidió una. |

Cuerpo:

- `operationId`, `status` (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` o `NO_FACTIBLE`) y `terminal`;
- `weekStartDate`, `planId`, `reasonCode`, `message` y `retryable`;
- `createdAt`, `startedAt`, `finishedAt` y `pollAfterSeconds` (3).

No expone porcentajes, tiempos estimados, `lease_owner`, ids de Replicate ni el libro.

### Cambia cuándo se emiten algunos estados, no cuáles

| Aspecto | #71 | #72 |
|---|---|---|
| `FAILED / TIEMPO_EXCEDIDO` en `PROCESSING` | 600 s desde la ejecución actual | 1.800 s desde la **primera** ejecución |
| `FAILED / EJECUCION_INTERRUMPIDA` | Tras 2 ejecuciones | Tras 3 |
| Reintento tras un `FAILED` reintentable | Volvía a pagar todo | Adopta lo pagado (24 h) |
| Espera máxima hasta un estado terminal | 30 min en cola + 10 min por ejecución (el límite se reiniciaba al reencolar) | ≤ 60 min (30 en cola + 30 desde la primera ejecución). Lo típico sigue siendo unos 75–350 s. |

## 2. Verificación contra el cliente real (MobileApp#2, solo lectura)

He leído el código de la PR `Fitsense-Real/MobileApp#2` (cabeza `f26c7ce`) sin modificarlo. Archivos:
`lib/features/welcome/domain/generation_operation.dart`, `data/plan_generation_repository.dart` y
`presentation/first_plan_generation_controller.dart`.

| La app debe poder… | Cómo lo hace el cliente | ¿Compatible con #72? |
|---|---|---|
| Crear o recuperar una operación | Guarda `operationId` (`welcome_store`). Al empezar hace `GET /{id}`; si recibe 404 o no tiene id, `POST`, que devuelve la activa o la `COMPLETED`. | Sí, mismas respuestas |
| Consultar su estado | `GET /{id}` cada `pollAfterSeconds` (3 s) | Sí |
| Recuperarla tras cerrar la app | `operationId` persistido y `GET /{id}`. El servidor sigue generando sin consultas. | Sí (E1/E2 en `appCerradaYReabierta`) |
| Mostrar el resultado | `COMPLETED` con `planId` | Sí. `planId` solo tras `persist` |
| Distinguir un error técnico de NO_FACTIBLE | Estados distintos (`failed` / `noFactible`) y textos distintos en `first_plan_result_page.dart` | Sí |
| Seguir esperando mientras se genera | `isWaiting` mientras no sea terminal; un estado desconocido cuenta como no terminal | Sí. **No se ha añadido ningún estado nuevo.** |
| No iniciar otra rutina al perder la conexión | Un error de red o un 5xx solo reprograma el `GET` con espera creciente (3 → 20 s); no llama a `POST` | Sí. Además el servidor devuelve la misma operación ante un `POST` repetido (R08). |
| Reintentar tras un fallo | `retry()` hace `POST`, que crea una operación nueva solo si la anterior es terminal | Sí. Ahora el reintento adopta lo pagado. |

La app no depende de ningún `reasonCode` concreto ni de tiempos: solo usa `status`, `planId`, `message`,
`retryable` y `pollAfterSeconds`. El cliente HTTP tiene 20 s de límite, y las rutas asíncronas responden en ms.

### Recorrido de Kapi

- El recorrido sigue igual.
- Si termina antes que la rutina, la app sigue en «preparando tu rutina» mientras el estado sea `PENDING` o
  `PROCESSING`.
- Cerrar y reabrir no reinicia nada.
- Una generación lenta pero válida (600–1.113 s) ya no termina en error a los 10 min.

## 3. Lo que no se ha hecho

- No se ha ejecutado la app Flutter en esta sesión; la verificación es contra su código y contra el contrato, con
  `MockMvc`.
- No se han modificado las PR del Mobile: no hay ninguna incompatibilidad comprobada.
