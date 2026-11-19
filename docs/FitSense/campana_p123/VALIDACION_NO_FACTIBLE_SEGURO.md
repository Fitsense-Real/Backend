# Validación de la regeneración segura (P-03)

## 1. En la campaña

| Usuario y semana | Dolor | IA (simulada) | Camino de P-03 | Publicado | Validación |
|---|---|---|---|---|---|
| N06 S4 → S5 | 129, identificado | 3 respuestas que no cumplen el contrato | **Nivel 1:** SUST-SEG-1, 129 → 78 (assisted triceps dip), misma dosis 3×6, mismo día (30/11), enfoque y 43 min | Sí, plan con V32 `RESPALDO` y API `REGENERACION_SEGURA_SUSTITUCION` | VALIDA_CON_AUTORIZACIONES; frecuencia 4/4; 129 ausente |
| N08 S2 → S3 | 791, identificado | Sustitución válida (124, mismo patrón y dosis) | No hizo falta: la adaptación fue APLICADA | Sí, ADAPTACION_APLICADA | VALIDA |

- **Nivel 2 (regeneración con reglas):** no fue necesario en la campaña (`regeneraciones.csv` vacío): el nivel 1 resolvió.
- **NO_FACTIBLE:** 0 semanas. Ninguna de las 100 semanas quedó sin plan.

## 2. Pruebas automáticas (CI)

| Requisito | Prueba | Resultado |
|---|---|---|
| Dolor con sustituto elegible | `AdaptacionCompletaIT.dolorSinIa` | Nivel 1: 7 sustituciones; misma frecuencia y fechas |
| Dolor sin sustituto, con regeneración determinista válida | `PoliticasP123IT.sinSustitutoRegeneraYEsIdempotente` | V32 NO_FACTIBLE y, en el mismo ciclo, un plan `RULE_ENGINE`. `safe_regeneration_attempts` PUBLICADO; API `REGENERACION_SEGURA_REGLAS` |
| IA apagada | `dolorSinIa` | Nivel 1, 0 llamadas |
| IA con respuesta inválida simulada | `iaInvalidaSustitucionDeterminista` | 3 llamadas (ni una más) y después el nivel 1 |
| Regeneración determinista inviable | `regeneracionInviable` | NO_FACTIBLE con el motivo (V15) en la tabla y en la API; **ningún plan publicado** |
| Doble ejecución del ciclo | `sinSustitutoRegeneraYEsIdempotente` | Mismo plan: 1 plan por semana, 1 fila V32 y 1 fila P-03 |
| Interrupción y reintento | `interrupcionYReintento` | La caída al registrar la traza deshace también el plan (una transacción). El reintento publica **una** vez |
| Sesiones ya ejecutadas | `sinSustituto…`, `dolorIdentificado…` | Historial de ejecución idéntico antes y después |
| Restricciones múltiples y ejercicio ya excluido | `restriccionesMultiplesYDolorAnterior` | Se suman las exclusiones; ninguna bandera prohibida en lo publicado |
| Sin cambios de frecuencia | En todas | Mismas sesiones (V2) |
| Sin publicación insegura | En todas, más el guardia de trabajo | Lo excluido nunca aparece. Ante dolor, el nivel 1 no puede subir repeticiones ni segundos (`masTrabajoQueLaMedida`) |
| La IA añade trabajo ante dolor | `CorreccionesReplicate*IT.c5DolorAnadeMas` | La propuesta con más trabajo se rechaza 3 veces y nunca se publica. El nivel 1 publica con el mismo número de ejercicios y sin subir el volumen |
| D8 y dolor | `HerenciaD8IT.dolorNoAprovechaD8` | El nivel 1 no pasa V20 en el viernes acortado; el nivel 2 publica con reglas sin lo que dolió |

## 2 bis. Auditoría del nivel 2 (cierre del PR #16)

**Defecto confirmado y corregido:** con dosis previamente reducidas (fatiga) o tras un ACORTAR, la regeneración con dosis base subía el trabajo de la semana medida:
- LOSE_WEIGHT: 102 → 108 repeticiones;
- D8: 450 → 486.

Ahora, si la regeneración sube las repeticiones o los segundos totales, el nivel 2 no publica: queda NO_FACTIBLE con el motivo y se conserva P-02. Detalle en `AUDITORIA_P03_NIVEL2.md`.

| Prueba nueva | Resultado |
|---|---|
| `ProgresionIT.p03Nivel2TrasReduccion` (3 objetivos) | Nunca se publica más trabajo; LOSE_WEIGHT queda NO_FACTIBLE |
| `ProgresionIT.p03Nivel2TrasProgresion` | Sin paso nuevo, sin V31 falso, sin el excluido, idempotente |
| `HerenciaD8IT.dolorNoAprovechaD8` | NO_FACTIBLE («sube el trabajo»); exclusión activa |

## 3. Garantías

- **P-02 se mantiene activo en P-03:**
  - la regeneración prepara con el conjunto elegible sin lo excluido;
  - después, una comprobación explícita rechaza cualquier borrador que lo traiga.
- **Sin pérdida de historial:**
  - el V32 NO_FACTIBLE sigue constando;
  - la traza P-03 lo enlaza (`adaptation_attempt_id`).
- **V31/V32 coherentes:**
  - el nivel 1 publica por `publishAdapted` (linaje completo y sustituciones en el informe);
  - el nivel 2 publica una generación nueva (sin V31 hacia la semana medida), enlazada al V32 que la originó;
  - la consulta de linaje (`linaje_v31_v32.sql`) no encuentra violaciones (`LinajeIntegridadIT` incluye ahora un caso de P-03).
- **Dosis:**
  - nivel 1: la de SUST-SEG-1, sin subir el trabajo de la sesión del dolor;
  - nivel 2: las dosis base de 4D, revalidadas por V24, y **nunca más trabajo total que la semana medida**.
