# Validación de la memoria persistente del dolor (P-02)

## 1. En la campaña (`evidencia/exclusiones_dolor.csv`)

| Usuario | Ejercicio | Motivo | Evento original | Registro | Estado final |
|---|---|---|---|---|---|
| N06 | 129 (band close-grip push-up) | DOLOR_IDENTIFICADO | Plan 42, sesión 133, ejercicio planificado 610, 23/11/2026 (S4) | Ciclo semanal | **ACTIVA** hasta el final (S10) |
| N08 | 791 (dumbbell supported squat) | DOLOR_IDENTIFICADO | Plan 20, sesión 62, ejercicio planificado 297, 9/11/2026 (S2) | Ciclo semanal | **REACTIVADA** en S7, con evidencia de la acción explícita |

### Persistencia entre semanas

| Usuario | Antes del dolor | Semanas con plan después | ¿Reaparece? |
|---|---|---|---|
| N06 | 129 en S1–S4 | S5–S11 (7 semanas publicadas) | **No.** `DOLOR_PERSISTENTE` en cada preparación; 0 apariciones |
| N08 | 791 en S1–S2 | S3–S11 (9 semanas publicadas; desde S7 ya reactivado) | **No** |

- `historial.csv` → `publica_excluido = false` en las 139 semanas publicadas: principales, extensión y variante.
- Solo N06 y N08 recibieron exclusiones; ningún otro usuario las hereda.
- **La ausencia de nuevos reportes no levantó nada:** N06 cumplió S5–S10 sin dolor y 129 sigue ACTIVA.

### Reactivación de N08 en S7

- **Acción:** `PainMemoryService.reactivar(usuario, 791, confirmación = true, "confirmo que ya no siento molestias (simulado)")`, el mismo servicio que usa `POST /users/me/pain-exclusions/791/reactivation`.
- **Control de seguridad:** sin conflicto (791 no choca con «sin rodillas ni suspensión»), así que REACTIVADA, con `candidatoHoy = true`.
- **Evidencia guardada:** `CONFIRMACION_EXPLICITA_DEL_USUARIO user=9 ejercicio=791 via=POST … ; control de seguridad sin conflicto`.
- **S7–S11:** 791 **no** volvió. La semana se conserva o progresa y no se añade automáticamente. Reactivar no es reincorporar.

## 2. Pruebas automáticas (CI)

| Requisito | Prueba | Resultado |
|---|---|---|
| Dolor en una sesión, sin identificar | `AdaptacionCompletaIT.dolorSinIa`, `PoliticasP123IT.antesDelReinicio` | Se excluyen todos los ejercicios de la sesión (`DOLOR_SESION_SIN_IDENTIFICAR`, vía `OMISION`) |
| Dolor con ejercicio identificado | `PoliticasP123IT.dolorIdentificadoPersisteCincoSemanas` | Solo ese ejercicio (1 fila) |
| Semana siguiente y cinco semanas después | Ídem | Ausente en las 6 semanas; la fila sigue ACTIVA |
| Nueva generación | Ídem (generación a 20 semanas) | Ni en el conjunto elegible ni en el plan |
| Adaptación | `dolorSinIa`, `iaInvalidaSustitucionDeterminista` | Sustituido; no se republica |
| Progresión | `ProgresionIT.p02ProgresionSinElExcluido` | 6 semanas, con pasos; ninguno sobre el excluido |
| Reincorporación | `ProgresionIT.p02ReincorporacionDescartadaPorDolor` | Reserva DESCARTADA; nunca vuelve en 14 ciclos |
| Reinicio de la aplicación | `antesDelReinicio` y `despuesDelReinicio` (`@DirtiesContext`) | La exclusión sigue y se aplica con el contexto nuevo |
| Otro usuario no la hereda | `dolorIdentificadoPersisteCincoSemanas` | Para otro usuario el ejercicio sigue siendo candidato |
| Reactivación sin autorización | `PoliticasP123IT.reactivacion` | 401 sin token, 404 con el token de otro usuario, 422 sin confirmar |
| Reactivación explícita válida | Ídem | REACTIVADA con evidencia; vuelve al conjunto elegible |
| No se reincorpora automáticamente | Ídem | La semana siguiente no lo trae; releer la semana del dolor no lo resucita |
| Rechazada por una restricción incompatible | `reactivacionRechazadaPorSeguridad` | `REACTIVACION_RECHAZADA_POR_SEGURIDAD` (brazos por encima de la cabeza no permitidos); sigue ACTIVA |
| El reactivado sigue sujeto a los validadores | Diseño | Solo vuelve al conjunto elegible; V1–V25 y V8C como cualquier candidato |

## 2 bis. Concurrencia y orden (cierre del PR #16)

| Caso | Prueba | Resultado |
|---|---|---|
| Dos reactivaciones simultáneas del mismo ejercicio | `PoliticasP123IT.reactivacionesSimultaneas` | Una 200 y otra 404; una sola fila REACTIVADA (bloqueo de fila) |
| Dolor nuevo después de reactivar | `PoliticasP123IT.dolorNuevoTrasReactivar` | Evento nuevo, otra vez ACTIVA; la reactivación anterior sigue constando |
| Registrar dos veces la misma semana | Ídem, y `MigracionV43IT` | Sin duplicados (`ON CONFLICT DO NOTHING`), también ante carreras |
| GET sin token, sin exclusiones o con exclusiones propias | `PoliticasP123IT.reactivacion` | 401; `[]`; solo las propias |

## 3. Aislamiento por usuario

- Las consultas filtran siempre por `user_id`.
- La API solo opera sobre `/users/me`. Una reactivación sobre un ejercicio sin exclusión activa **propia** devuelve 404, sin revelar si existe la de otro.

## 4. Lo que la persona no ve todavía (Flutter)

No se tocó Flutter. El contrato está en `POLITICAS_P01_P02_P03_IMPLEMENTACION.md` §2:
- listar las exclusiones;
- reactivar con la confirmación explícita;
- explicar que reactivar no garantiza que el ejercicio aparezca.
