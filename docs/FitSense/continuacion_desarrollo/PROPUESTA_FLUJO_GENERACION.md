# Propuesta · flujo de generación sin problemas de espera

Esta propuesta es solo diseño y contrato. No implementa nada, no cambia Flutter ni la generación.

## Problema

Ver `DIAGNOSTICO_TIEMPOS_GENERACION.md`: la operación dura de 60 a 350 s y la app espera 30 s.

## Alternativas comparadas

| Alternativa | Resuelve la espera | Cambia lo evaluado | Coste | Riesgo |
|---|---|---|---|---|
| **0. Actual** (síncrono) | No | — | — | Errores visibles; reintentos 409 |
| **1. Subir el timeout de la app** a ~360 s | Parcialmente: la app «espera» minutos con la pantalla bloqueada; cerrar la app sigue perdiendo la respuesta | No | Bajo (Flutter) | Mala experiencia; un proxy o el sistema operativo pueden cortar antes |
| **2. Respaldo inmediato y sustitución posterior** (D del caso 39) | Sí (~0,3 s) | **Sí.** El participante ve primero el plan del motor de reglas, que ignora `goalText`, y luego uno distinto. La sustitución choca con sesiones empezadas, la adherencia y la progresión | Alto | Sustituir un plan visto o empezado |
| **3. Generación asíncrona con estado persistente** (recomendada) | **Sí.** `202` inmediato y consulta del estado; la app puede cerrarse y recuperar el resultado | **No.** El pipeline, la IA, los validadores, el respaldo y la persistencia son los mismos; solo cambia cómo se espera | Medio: backend + app | Concurrencia y limpieza de operaciones huérfanas (abajo) |
| 4. Generación por día en paralelo | Parcialmente | Sí (rediseño completo) | Muy alto | Reglas semanales; la orden no autoriza implementarla |

**Recomendación: 3.** Es la única que resuelve la espera sin cambiar qué se genera.

## Contrato API propuesto

```
POST /api/v1/users/me/plan/generation-requests
  Cabecera opcional: Idempotency-Key: <uuid generado por la app>
  → 202 Accepted
    Location: /api/v1/users/me/plan/generation-requests/{id}
    { "id": "…", "estado": "PENDIENTE", "creada_en": "…", "consultar_en_segundos": 5 }
  → 200 OK si ya existe una operación activa o terminada con la misma Idempotency-Key (la misma respuesta)
  → 409 si ya hay un plan para esa semana (como hoy, antes de encolar)
  → 429 por frecuencia (LimiteDeGeneraciones)

GET /api/v1/users/me/plan/generation-requests/{id}
  → 200 { "id", "estado", "plan_id"?, "origen"? (IA|RESPALDO), "codigo_no_factible"?, "mensaje"?,
          "creada_en", "actualizada_en", "intentos" }
  → 404 si no es del participante

GET /api/v1/users/me/plan/generation-requests/activa
  → 200 la operación PENDIENTE o PROCESANDO del participante; 204 si no hay
```

**Estados:** `PENDIENTE` → `PROCESANDO` → `COMPLETADA` | `FALLIDA` | `NO_FACTIBLE`.

| Estado | Significado |
|---|---|
| `COMPLETADA` | Trae `plan_id` y `origen` (la IA o el respaldo validado) |
| `NO_FACTIBLE` | Reutiliza los códigos estructurados que ya existen para el NO_FACTIBLE |
| `FALLIDA` | Error técnico sin plan. Es raro, porque el respaldo cubre las caídas del proveedor |

El `POST /users/me/plan/generate` síncrono se **conserva** mientras la app migra, por compatibilidad.

## Diseño

| Aspecto | Propuesta |
|---|---|
| **Persistencia** | Tabla nueva `plan_generation_requests`, migración V58: `id` uuid, `user_id`, `week_start_date`, `estado`, `idempotency_key` (única por usuario), `plan_id` NULL, `origen`, `codigo_no_factible`, `mensaje`, `intentos`, `creada_en`, `actualizada_en`, `instancia` (quién la procesa) |
| **Ejecución** | Un ejecutor del backend (`@Async` con un pool acotado, o una tarea que reclama filas `PENDIENTE` con `FOR UPDATE SKIP LOCKED`). Ejecuta **el mismo** `GeneracionInicialService.generar` (prepare → generate → persist, con el bloqueo R-5) |
| **Idempotencia** | `Idempotency-Key` por usuario: el mismo valor devuelve la misma operación. Sin cabecera, una operación activa del mismo usuario y semana se devuelve en lugar de crear otra |
| **Reintentos seguros** | La app reintenta el `GET` sin límite (es idempotente) y el `POST` con la misma clave. El backend no reintenta la operación entera: sus reintentos con la IA siguen dentro del pipeline, como hoy |
| **Errores** | Cualquier excepción del pipeline pasa a `FALLIDA` con un mensaje neutral, y se registra. Las excepciones de dominio ya mapeadas (plan existente, perfil) se resuelven antes de encolar (`409`, `400`) |
| **App cerrada** | La operación sigue en el backend. Al reabrir, la app consulta `…/activa` o el plan vigente |
| **Reinicio del backend** | Las filas `PROCESANDO` sin latido durante más de N minutos (> 331 s, el peor caso por intento) se marcan `FALLIDA` o se reencolan. El bloqueo R-5 ya se suelta al cerrarse la conexión |
| **Seguridad** | Solo el propietario consulta su operación, con el mismo JWT. Sin datos sensibles en el estado. Límite de frecuencia como hoy |
| **Observabilidad** | `GENERACION_TIEMPOS` y `REPLICATE_TIMING` sin cambios, más el id de la operación |

## Compatibilidad con Flutter

Se documenta aquí, pero no se implementa en esta fase:

1. Llamar a `POST …/generation-requests` con una `Idempotency-Key` guardada en local.
2. Mostrar «Estamos preparando tu plan».
3. Consultar `GET …/{id}` cada 5 s, con espera creciente hasta 15 s.
4. Al recibir `COMPLETADA`, cargar el plan.
5. Al recibir `NO_FACTIBLE` o `FALLIDA`, mostrar el mensaje.
6. Al reabrir la app, `GET …/activa`.

El `receiveTimeout` de 30 s deja de afectar.

## Pruebas previstas (antes de tocar el móvil)

1. **Contrato:** `POST` → 202 con `Location`; `GET` → estados; 404 de otro usuario.
2. **Idempotencia:** dos `POST` con la misma clave → una sola operación y una sola generación (IA simulada que cuenta llamadas).
3. **Concurrencia:** dos `POST` sin clave a la vez → una operación; el ciclo semanal y la operación no coinciden (bloqueo R-5).
4. **Recuperación:** una operación `PROCESANDO` huérfana pasa a `FALLIDA` y se puede pedir otra.
5. **Equivalencia:** con la IA simulada, el plan de la ruta asíncrona es idéntico al de la síncrona para la misma entrada (misma instantánea, mismos validadores).
6. **NO_FACTIBLE:** los códigos estructurados llegan al estado.
7. **Regresión completa** con el endpoint síncrono intacto.

## Resultado

Diseño cerrado y listo para aprobar.

## Pendientes

- Tu aprobación del diseño.
- Implementarlo en una rama independiente: migración V58, servicio, controlador y pruebas. Sin fusionar automáticamente.
- Adaptar la app Flutter, que es una fase aparte.
