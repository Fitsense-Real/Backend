# Restauración mínima de una sesión adaptada (V35, caso 11)

| | |
|---|---|
| Fecha | 27/10/2026 |
| Diseño | `DISENO_ADAPTACION_CONTEXTUAL.md` v0.3 cerrada, §3, §6.1 y caso 11 |
| Alcance autorizado | Solo las tres piezas: restauración, migración V35 y V8C. **Sin** pregunta de falta de tiempo, Flutter ni el resto de ADAPT-2 |
| Estado | Implementada y probada en local. **Apagada por defecto** (`fitsense.adaptation.restoration.enabled=false`); **no activada en Neon** |

## Qué hace

Cuando el ciclo no tiene orden de adaptar (`NO_CHANGE` o `INSUFFICIENT_DATA`), puede devolver **una** sesión adaptada a su prescripción anterior. Solo lo hace si la persona completó la versión adaptada.

1. **Elegibilidad** (`RestauracionResolver`). Para cada sesión de la semana medida se recorre el linaje V31 hacia atrás, clasificando cada paso con su intento V32:
   - **Las copias se atraviesan.**
   - **El primer paso que no es copia decide.** Solo si es una adaptación `APLICADA` con una intervención restaurable, la referencia es el origen de ese paso. Las restaurables son: fatiga (`REVISAR_CARGA_Y_RECUPERACION`), motivación (`INVESTIGAR_CAMBIO_MINIMO`) y horario (`REPROGRAMAR_DIAS`).
   - **Cualquier otro paso no restaura nada:** otra intervención, «sin efecto», una sustitución por seguridad, una restauración previa, un paso sin intento o ambiguo, o la generación inicial.
2. **Evidencia positiva.** La sesión adaptada tiene que estar **completada** en la semana medida (estado de sesión `COMPLETED`, el umbral ya aprobado del 80 %) y sin señales incompatibles declaradas en ella:

   | Intervención restaurable | Señales que impiden restaurar |
   |---|---|
   | Fatiga | Fatiga o dolor |
   | Motivación | Falta de motivación o dolor |
   | Horario | Dolor |

   Tampoco se restaura si consta dolor en alguna copia intermedia. **No se usa el RPE** (D5).
3. **Sin información, se mantiene.** El ajuste sigue copiándose y la restauración queda pendiente para el ciclo siguiente.
4. **Una por ciclo.** Se elige la adaptación más antigua y, a igualdad, la fecha más temprana. Con una orden de adaptar no se restaura nada: el productor lo rechaza.
5. **La referencia, exacta.** Mismos ejercicios, dosis, enfoque y minutos, puestos en su **día de la semana** dentro de la semana nueva.
6. **Revalidación completa.** La semana con la restauración pasa por el productor real: sustitución por seguridad, V8C y V1–V25 con el perfil y el catálogo vigentes.
   - V8C solo acepta la sesión si es **idéntica** a su referencia, sin sustituciones; en ese caso la anota en `restauradas`, no en `conservadas`.
   - Si no pasa, **no se restaura**: se publica la copia de siempre y el motivo queda en `conservation_report.restauraciones_bloqueadas`.
7. **Sin IA.** No hay llamada a la IA: el resultado es `SIN_ORDEN`, con origen `CONSERVED`.

## Cambios

| Pieza | Cambio |
|---|---|
| `V35__restauracion_de_sesiones.sql` | Columna `plan_adaptation_attempts.restored_sessions BIGINT[] NOT NULL DEFAULT '{}'`, más restricciones (abajo). `fn_intento_coherente` ampliada: `restored_sessions` debe pertenecer al plan de origen. **Solo agrega:** las filas existentes quedan con `'{}'` |
| `RestauracionResolver` (nuevo) | Elegibilidad, evidencia y referencia en la semana nueva |
| `AdaptedWeekCommandService` | En el camino sin orden: intenta la restauración elegible; si no pasa, publica la copia y anota el motivo. Detrás del interruptor |
| `AdaptedWeekProducer` | Sobrecarga de `produce` con las restauraciones: solo en continuidad; rechaza restaurar con una orden; el respaldo no las recibe |
| `ConservationValidator` (V8C) | Una sesión declarada restaurada se compara con su **referencia**: identidad exacta, segura y sin sustitución; como máximo una por ciclo |
| `ConservationReport` | `restauradas` y `restauraciones_bloqueadas` |
| `AdaptationAttempt`, `AdaptationEvidenceMapper`, `PlanAdaptationAttempt` | `restoredSessions` hasta la columna |
| `application.properties` | `fitsense.adaptation.restoration.enabled=${FITSENSE_RESTORATION_ENABLED:false}` |
| `consultas/linaje_v31_v32.sql` | Reconoce el paso `RESTAURADA`. Lee `restored_sessions` con `to_jsonb(fila)`, así que funciona igual con o sin V35 |

**Sin cambios:** el cálculo de adherencia, los umbrales, la generación inicial, el decisor, D8 (un paso restaurado corta su herencia, que es lo correcto) y V24/V25.

### Invariantes de V35 (en la base)

| Restricción | Regla |
|---|---|
| `ck_paa_restauradas_unicas` | Sin repetidos |
| `ck_paa_restauradas_disjuntas` | Disjunto de `targeted`, `applied`, `no_effect` y `conserved` |
| `ck_paa_una_restauracion` | A lo sumo una por intento |
| `ck_paa_restauracion_sin_cambio_de_ia` | Solo con `SIN_ORDEN` o `SIN_EFECTO`; nunca con `APLICADA` ni `RESPALDO` |
| `ck_paa_restauracion_publicada` | Solo si hay plan publicado |
| `fn_intento_coherente` | Pertenece al plan de origen |

V33 (`APLICADA` ⇔ `applied_sessions` no vacío) no cambia: una restauración nunca entra en `applied_sessions`. `AdaptationAttempt` comprueba las mismas reglas antes de llegar a la base.

## Pruebas

**`RestauracionIT`** (5 pruebas, ciclo de producción, PostgreSQL 16 real, IA de corrección simulada). El participante es de `GENERAL_FITNESS`, porque en `GAIN_MUSCLE` 4D impone la dosis exacta y un ajuste por fatiga no se puede publicar. En la semana de la fatiga, la IA simulada baja 2 repeticiones de un ejercicio.

| Caso | Resultado |
|---|---|
| **Caso 11:** fatiga adaptada (`APLICADA`); viernes completado sin fatiga → `NO_CHANGE` | Se publica `SIN_ORDEN`, `CONSERVED`, `ai_calls = 0`, **ninguna llamada** a la IA. `restored_sessions` = el viernes; lunes y miércoles en `conserved`, idénticos. El viernes queda **exactamente** como la referencia (ejercicios, dosis, minutos, enfoque, mismo día de la semana). V31: las tres derivan de la semana medida. Una sola versión vigente. Historial y adherencia ya calculada intactos. **Multisemana:** la semana siguiente copia la restaurada sin volver a restaurarla. Linaje: 0 violaciones |
| Sin información del viernes adaptado | No se restaura y el ajuste se mantiene. Tras una copia intermedia completada, se restaura la referencia **de la semana 1** |
| Completado, pero con fatiga declarada en un ejercicio | No se restaura |
| Restricción nueva: un ejercicio de la referencia queda bloqueado | No se restaura; el motivo («V1: el ejercicio 473 no está en available_exercises») consta en `restauraciones_bloqueadas`. Ningún ejercicio bloqueado se publica |
| Una modificación por ciclo: el viernes cumple para restaurarse y el lunes se omite por falta de tiempo (`ADAPT_LOCALIZED`) | Solo se adapta el lunes; `restored_sessions` vacío. El ciclo siguiente (`NO_CHANGE`) restaura el viernes |

**`RestauracionV8CTest`** (5 pruebas, dominio puro, con el productor, V8C y la validación estricta reales):
- una restauración idéntica se publica y va en `restauradas`, no en `conservadas` ni en `aplicadas`;
- una restauración distinta de la referencia, o en otro día, la rechaza V8C;
- dos restauraciones en el mismo ciclo se rechazan;
- con una orden de adaptar no se acepta ninguna.

**Contraprueba.** Con el interruptor apagado:
- el caso 11 falla (no se restaura: `restored_sessions` queda vacío);
- el escenario del ejercicio bloqueado da `NO_FACTIBLE` igual que con la restauración encendida.

**Regresión completa:** 699 pruebas unitarias y 161 de integración, sin fallos. Hay 2 omitidas: A01, que solo corre bajo demanda, y una que ya se omitía. Incluye P2-D, los cuatro objetivos, D8 y `LinajeIntegridadIT`. La restauración está apagada en todas las pruebas salvo `RestauracionIT`.

**Base de pruebas externa de P2-D.** Las pruebas heredadas (`InvariantesV33IT`, `PublicacionAtomicaIT`, `DisparadorDiferidoIT`, …) corren con Flyway apagado y validación de esquema, así que su base necesita V35 aplicada a mano. En local (`fs-p2d`) se aplicó; sin ella, esas 7 pruebas fallan al arrancar con «missing column [restored_sessions]». La base «FitSenseTests» de Neon necesitaría lo mismo antes de volver a usarla.

## Observación: ejercicio bloqueado a mitad del plan (comportamiento previo)

En el escenario de la restricción nueva, la semana **tampoco** se publica sin la restauración (`NO_FACTIBLE` por V1): la semana adaptada también contiene el ejercicio que la persona bloqueó, y P2-D no sustituye un ejercicio bloqueado por la persona, solo los excluidos por seguridad (SUST-SEG-1). Con la restauración apagada pasa exactamente lo mismo, así que **no lo introduce esta implementación**. No se corrigió: queda como hallazgo para decidir.

## Antes de activar en Neon

1. **Desplegar V35.** V34 es todavía una migración **propuesta** (`docs/FitSense/migraciones_propuestas/`). Si V35 se aplica antes, V34 tendrá que renumerarse (por ejemplo, a V36) o habrá que habilitar `outOfOrder` en Flyway. Hay que decidirlo antes del despliegue.
2. **Confirmar la base y el esquema** con la comprobación de cordura (`SELECT count(*) FROM weekly_training_plans;`) y volver a ejecutar `linaje_v31_v32.sql` en solo lectura.
3. **Encender** `FITSENSE_RESTORATION_ENABLED=true` solo con autorización.
