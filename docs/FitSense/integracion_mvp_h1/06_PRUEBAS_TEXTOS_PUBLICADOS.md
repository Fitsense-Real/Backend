# 06 · Pruebas de los textos publicados (PR B)

**Código:** `cc2bf7b`, sobre el PR A `983e216`.
**Entorno:** PostgreSQL efímero, IA simulada, sin Replicate.

## 1. Pruebas nuevas o ampliadas

| Prueba | Qué demuestra | Resultado |
|---|---|---|
| `DescripcionAdaptacionTest` (11) | Cada clase de cambio con su texto exacto: U04 (bajadas), R01 (laterales), **dificultad mayor**, **mixto (baja + sube)**, **sin comparación**, **retirada sobrante**, R03 (dosis), no aplicada, conservada sin alternativa, dolor (sin comparar la dificultad aunque suba) y sin sesiones señaladas | 11/11 |
| `SustitucionDificultadMayorTest` (4) | V8C acepta 580 (1) → 56 (2) en VARIANTE con las dos reglas y ante dolor; SUST-SEG-1 elige un sustituto de dificultad 3 antes que uno de dificultad 1 | 4/4 |
| `MensajeDeIntervencionTest` (15) | `message_shown` en intención para cada salida de la orden y para los 9 tipos de intervención: sin palabras de resultado, ≤ 500 caracteres y sin porcentaje cuando el dato no es fiable | 15/15 |
| `MensajeIntencionDecisionTest` (2) | `reason` técnico intacto y `message` con la intención; `UserIntervention` guarda la intención; sin texto propio, se comporta como antes | 2/2 |
| `SinAjustePorcentualTest` (4, existente) | `message_shown` no nulo y ≤ 500; la intervención se construye | 4/4 |
| `ConservacionPatronH1IT.justificacionVerificable` (ampliada) | En el plan **publicado**, `adjustment_reason`: no lleva el motivo técnico; dice «registrada como de menor dificultad» si baja, «registrado como de mayor dificultad» si sube y «con la misma dificultad registrada» si es igual | pasa |
| `AdaptacionCompletaIT.cicloCompleto` (ampliada, 18 combinaciones de objetivo y escenario) | Con el ciclo real (ejecutar, medir, decidir, adaptar y publicar): `message_shown` no promete y dice «Revisaremos» (o «mantenemos» si no hay orden); `adjustment_reason` describe el resultado («Retiramos N ejercicio» o «no aplicamos cambios esta semana») y no lleva el motivo técnico | 22/22 |

## 2. Lo que los textos no cambian

Comprobado en la misma IT y en la regresión:

| Requisito (fase 4.3) | Cómo se comprueba | Resultado |
|---|---|---|
| No cambia la adherencia | REPLAY `metricas.csv` idéntico | ✅ |
| No cambia la decisión | `AdaptacionCompletaIT` (decisión, estado y sesiones aplicadas, sin cambios en sus aserciones); REPLAY `historial.csv` y `v32.csv` idénticos | ✅ |
| No cambia la propuesta de Replicate | El `reason` que va a planning y al snapshot es el mismo (`MensajeIntencionDecisionTest`); `WeeklyCycleService.toAdjustment` no lee `message` | ✅ |
| No cambia el plan publicado | REPLAY `planes.csv` idéntico; `AdaptacionCompletaIT` §3 | ✅ |
| No cambia la dosis | REPLAY `PASOS.csv` idéntico; pruebas de dosis de V8C (40) | ✅ |
| No cambian los validadores | El diff de `src/main` del PR B no toca ningún validador (`evidencia/diff_B_src_main.diff`) | ✅ |
| Solo cambia la descripción | Diff de `src/main`: `DescripcionAdaptacion` (nueva), `MensajeDeIntervencion` (nueva), 1 llamada en `publishAdapted` y 1 en `UserInterventionCommandServiceImpl` | ✅ |

## 3. Regresión completa del PR B

| Bloque | Ejecutadas | Superadas | Fallidas | Errores | Omitidas |
|---|---:|---:|---:|---:|---:|
| Unitarias | **1.118** (1.086 + 32) | 1.118 | **0** | **0** | 0 |
| Integración | **495** (494 + 1: `justificacionVerificable`) | 459 | **0** | **0** | 36 (*opt-in*, **las mismas** que en el PR A) |
| 960 perfiles + REPLAY | 3 | 2 | **0** | **0** | 1 (inspección) |

- **960 perfiles:** `planes.csv` `d109a3da…a859` y `colocaciones.csv` `d227848b…265ef`, **idénticos** a la referencia.
- **REPLAY:** pasa. Solo cambian 2 marcas de tiempo de `exclusiones_dolor.csv`; se descartó y no se sobrescribió nada (`evidencia/replay_B_diferencias.diff`).

La REPLAY no compara textos. La comprobación de los textos está en las IT de §1.
