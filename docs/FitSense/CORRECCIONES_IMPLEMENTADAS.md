# Correcciones implementadas (auditoría integral, 30/10/2026)

Rama `auditoria-integral`, [PR #9](https://github.com/Fitsense-Real/Backend/pull/9).
- Ningún umbral ni política aprobada cambia.
- Ningún validador (V8C, V15, V18, V20, V23, V24, V25) se relajó.
- Las migraciones nuevas, V41 y V42, **no se han aplicado en ningún entorno persistente**.

## 1. 262 y 14 ejercicios más acreditaban slots que su movimiento no hace

| | |
|---|---|
| Problema | 262 *barbell skier* (`shoulders`/`HIP_HINGE`) acreditaba LOWER_HIP sin bisagra: 11 veces en las referencias V40, en el FULL_BODY de avanzados de gimnasio. Otros 14 tenían grupo o patrón incoherentes con el movimiento predominante |
| Reproducción | Uso en `generacion_referencia_v40.txt`, revisión de los GIF asistida por IA y `CoherenciaGrupoSlotIT` (avisos de cruces) |
| Causa raíz | Datos del catálogo, no el motor: el slot sale del patrón (`MandatorySlot`) |
| Cambio | `V41__catalog_c4_c6_fase2.sql` (25 cambios de columna, con comprobación previa y final). Detalle en `AUDITORIA_INTEGRAL_LOGICA.md` §2.1 |
| Pruebas | `CatalogoV41IT`, `CoherenciaGrupoSlotIT`, `GeneracionReferenciaV40IT` (V40 congelada), `GeneracionReferenciaIT` (oficial V41), `ReferenciasHistoricasInmutablesTest` (huella de V40) |
| Commit | `e60cdee` |
| Estado | **CORREGIDO Y PROBADO**. Quedan 54, 1174, 1295, 862 y 1086 pendientes de decisión |

## 2. A01: una omisión por falta de tiempo siempre acortaba la sesión

| | |
|---|---|
| Problema | Cualquier omisión con `LACK_OF_TIME` llevaba a ACORTAR_O_REORGANIZAR y llamaba a la IA, aunque fuera un imprevisto o la persona no tuviera ningún minuto ese día. Repetido semana a semana, reducía el entrenamiento sin evidencia de que el problema fuera la duración |
| Reproducción | Siete IT existentes (`HerenciaD8IT`, `PendientesProgresionIT`, `CopiaSinIaIT`, `AdaptacionCompletaIT`, `CorreccionesReplicateBase`, `LinajeIntegridadIT`, `ProgresionIT`) daban por hecho que un salto `LACK_OF_TIME` sin más datos acorta: era el comportamiento ADAPT-1. `AdaptacionPorCausasIT.faltaDeTiempoSinEvidenciaDeRecorte` fija el nuevo |
| Causa raíz | `TipoIntervencion.deMotivo` asigna la intervención solo por el motivo. D1, D2, D6 y D7 de `DISENO_ADAPTACION_CONTEXTUAL.md` v0.3, ya aprobadas, no estaban implementadas |
| Cambio | Subcausa opcional al omitir (`lackOfTimeContext`: IMPREVISTO, SIN_TIEMPO_ESE_DIA o MENOS_TIEMPO_HABITUAL, y `availableMinutes` de 15 a 180), compatible con los clientes actuales. Nueva intervención **CONSERVAR**, que no llama a la IA. `FaltaDeTiempoPolicy` solo acorta con MENOS_TIEMPO_HABITUAL y unos minutos menores que la sesión. V8C rechaza una sesión acortada que supere esos minutos. `V42__adaptation_lack_of_time_context.sql` añade las columnas y sus restricciones, CONSERVAR en V32 y `adaptation_rule_version` (ADAPT-1 o ADAPT-2) |
| Pruebas | `AdaptacionPorCausasIT` (8 casos), `FaltaDeTiempoPolicyTest` (4). Siete IT con premisa ADAPT-1 declaran ahora la subcausa (`saltoPorFaltaDeTiempo`), con las mismas aserciones |
| Commits | `a335e48`, y `194f270` (el texto de CONSERVAR enuncia sus prohibiciones, como exige `TipoIntervencionTextoCoherenteTest`; fallo del CI) |
| Estado | **CORREGIDO Y PROBADO** |

## 3. Un ejercicio dejado por dolor no se resolvía si la adherencia era buena

| | |
|---|---|
| Problema | (a) Semana cumplida con un ejercicio dejado por dolor: NO_CHANGE, el ejercicio volvía la semana siguiente. (b) Con una adaptación localizada, se señalaba la peor sesión aunque el dolor estuviera en otra: la sesión con dolor se copiaba sin cambios y la semana acababa NO_FACTIBLE o RECHAZADA |
| Reproducción | `AdaptacionPorCausasIT.cumplidaConDolor` y `dolorYFaltaDeTiempoEnLaMismaSemana`; `ValidacionMultisemanaIT` «10 varios motivos» |
| Causa raíz | `AdaptationOrderDecider` solo miraba las sesiones problemáticas (cumplimiento menor del 80 %). §2.1 de `POLITICA_ADAPTACION_Y_PROGRESION.md` pide SEGURIDAD_ANTE_DOLOR ante dolor en cualquier ejercicio |
| Cambio | El decisor reúne las sesiones con dolor registrado. Si no adaptaría por adherencia, emite ADAPT_LOCALIZED solo con ellas como SEGURIDAD_ANTE_DOLOR. Si adapta, las incluye siempre. `SessionOutcomeIdentifier` no cambia: se respeta `completadaConDolorNoSeSenala` |
| Pruebas | `AdaptationOrderDeciderTest`: `dolorConAdherenciaBuena`, `dolorAntesQueLaPeor`, `fatigaConAdherenciaBuena` (la fatiga con adherencia buena sigue sin adaptar) |
| Commit | `a2271ab` |
| Estado | **CORREGIDO Y PROBADO** |

## 4. No había forma de saber si la sesión adaptada se hizo

| | |
|---|---|
| Problema | El diseño v0.3 §4 define el resultado por sesión adaptada, pero no existía ningún indicador |
| Cambio | `docs/FitSense/consultas/resultado_sesion_adaptada.sql`, consulta de solo lectura sobre V32, el linaje V31, `planned_workouts` y `workout_sessions`. No crea tablas |
| Prueba | `AdaptacionPorCausasIT.resultadoPorSesionAdaptada` |
| Commit | `68a1f15` |
| Estado | **CORREGIDO Y PROBADO**. No es una medida causal |

## 5. Validación multisemana (sin cambio de producción)

`ValidacionMultisemanaIT` (12 secuencias × 4 objetivos, commit `8dff41a`) no encontró defectos nuevos fuera de los puntos 2 y 3.

Encontró el pendiente del cambio de perfil. Se reproduce con `-Dfitsense.pendientes=true` y **no se corrigió** porque requiere decisión (`VALIDACION_MULTISEMANA.md` §4).
