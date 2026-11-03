# Hallazgos de A0 y sus causas

**Fuente:** `evidencia/A0` (ejecución original sin correcciones, laboratorio `911a0d3`, backend `0974fb3`).

**Categorías de la orden:**
- A. Laboratorio.
- B. Cálculo de adherencia.
- C. Decisión.
- D. Generación o validación.
- E. Integración.
- F. Diferencia entre el artículo y el código.
- G. Válido pero no factible.

## Resumen

| Comparación con el oráculo | Coinciden | No coinciden |
|---|---:|---:|
| Generación inicial publicada | 20 | 0 |
| Aw | 18 | 2 (U07, U12) |
| Aobs | 18 | 2 (U07, U12) |
| C | 20 | 0 |
| Decisión (oráculo del artículo) | 18 | 2 (U07, U12) |
| Métricas del backend frente a mi recálculo independiente | **20 (diferencia máxima 0,00)** | 0 |

**Defectos del backend demostrados (categorías B, C, D y E): 0.**

## H1. U07 y U12: la regla P40 congelada no da el 40 % exacto con dosis enteras

| Campo | Valor |
|---|---|
| Observado | **U07:** S3 = 39,26 %; Aw = Aobs = 79,75; `ADAPT_LOCALIZED` sobre S3, causa no registrada, `SIN_EFECTO` (conservación deliberada).<br>**U12:** S3 = 38,33 %; 79,44; lo mismo |
| Oráculo | 80 → sin cambio |
| Causa raíz | Se registra 0,4 × dosis prescrita, redondeado como fija el protocolo.<br>**U07:** 4 ejercicios de 3×6 = 18 repeticiones → 7,2 → 7 (38,89 %), y 2 de 3×10 → 12 (40 %).<br>**U12:** 4 de 2×8 = 16 → 6,4 → 6 (37,5 %), y 2 de 90 s → 108 s (40 %).<br>**Con la regla uniforme del protocolo** (cada ejercicio al 40 %, redondeado), el 40,00 % exacto es inalcanzable en esas sesiones.<br>Con un **reparto desigual** sí se podía llegar a 40,00:<br>• U07: 7, 7, 8 y 8 de 18, más 11 y 11 de 30;<br>• U12: cuatro veces 6 de 16, más 121 s y 122 s de 270 s.<br>Pero entonces cada ejercicio se aleja del 40 %, y el protocolo, congelado antes de ejecutar, fijó la regla uniforme |
| ¿Contradice el backend sus reglas? | **No.** Aobs < 80 da `ADAPT_LOCALIZED`, que señala la peor sesión con registro.<br>Sin motivo de omisión → `CAUSA_NO_REGISTRADA` → no se cambia nada (`SIN_EFECTO`). El plan siguiente es la misma semana, validada |
| Categoría | **A**: la regla de entrada congelada no da P40 exacto con dosis enteras. Más **F**: el umbral es estricto (79,75 no llega a 80) |
| Acción | **Ninguna en esta orden.** Cambiar el reparto después de ver el resultado sería cambiar la entrada para mejorar el resultado, y eso está prohibido. Cambiar la prescripción o el umbral también. Se conserva el porcentaje real, y la decisión queda como propuesta P1 (`10_CORRECCIONES_APLICADAS.md`) |

## H2. Variable de decisión: Aobs en lugar de Aw

| Campo | Valor |
|---|---|
| Observado | `AdaptationOrderDecider` decide con Aobs |
| Efecto en los 20 casos | **Ninguno.**<br>• En E1–E4, Aw = Aobs (cobertura del 100 %).<br>• En E5 decide antes la regla de cobertura (C = 33,33 < 50) |
| Categoría | **F** |
| Acción | Ninguna en el código. Documentar en el artículo (`23_DISCREPANCIAS_CON_EL_SHORT_PAPER.md`) |

## H3. Sin IA, «adaptar» no cambia el plan (E3 y E4)

| Campo | Valor |
|---|---|
| Observado | 8 casos (U03, U04, U08, U09, U13, U14, U18, U19).<br>• La decisión es la correcta: `ADAPT_LOCALIZED` sobre S3, o `ADAPT_EXPANDED` sobre S2 y S3.<br>• El resultado es `RESPALDO`: con `ai_calls` = 0 se copia la semana medida, se publica y se valida |
| Causa | Diseño. Las intervenciones REVISAR_CARGA, VARIANTE e INVESTIGAR requieren IA. Sin ella solo se aplican las sustituciones de seguridad ante dolor |
| Categoría | **F** (si el artículo atribuye la adaptación al sistema sin IA). No es un defecto: **la decisión de adaptar y la adaptación aplicada son cosas distintas**, como pide la orden |
| Acción | Ninguna. Es lo que la etapa C debe medir con Replicate real |

## H4. Duración de las sesiones por debajo de los minutos declarados

| Campo | Valor |
|---|---|
| Observado | GM y FU con 60 min → 43 min (72 %). PP con 45 → 34 (76 %). AG con 45 → 32–34 (71–76 %) |
| Causa | Política `PLAN-HIST-1`: V20 exige al menos el 70 % y V4 permite hasta el 115 %. El motor llena hasta la estructura completa, no hasta el tiempo disponible. C2-D, en el PR #17 sin fusionar y sin autorizar, trata precisamente esto |
| Categoría | **F** (solo si el artículo dice que el plan ocupa el tiempo disponible). No es un defecto |
| Acción | Ninguna |

## H5. La API rotula igual «sin cambio» y «datos insuficientes»

| Campo | Valor |
|---|---|
| Observado | `resultado` = `SIN_ADAPTACION` en E1, E2 y E5. El campo `decision` sí los distingue (`NO_CHANGE` frente a `INSUFFICIENT_DATA`), y `motivos` llega vacío |
| Categoría | Observación de contrato, no un defecto: el dato está en `decision` |
| Acción | Ninguna. Es una posible mejora posterior al MVP: un rótulo propio y un texto para los datos insuficientes |

## H6. Documentación interna: «adherencia ponderada»

| Campo | Valor |
|---|---|
| Observado | El comentario de `V30__registration_coverage.sql` describe un denominador ponderado por minutos. El código (`WeeklyMetricsCalculator`) pesa cada sesión 1, y la columna se llama `weighted_adherence_pct` |
| Categoría | **F** (documental) |
| Acción | No se puede editar una migración ya aplicada (cambiaría su *checksum*). Queda documentado aquí |

## H7. Laboratorio: sesiones N tras el cierre

| Campo | Valor |
|---|---|
| Observado | Las sesiones N siguen `SCHEDULED` en la semana cerrada |
| Causa | El barrido horario está sustituido por el reloj del laboratorio. En producción quedarían `SKIPPED` sin motivo |
| Efecto | Ninguno: en los dos casos no cuentan como registro (C = 33,33 y Aobs = 100) |
| Categoría | **A** (condición del laboratorio), sin efecto en los resultados |

## Lo que se buscó y no apareció

- **Diferencias entre las métricas guardadas y el recálculo independiente:** 0 en 60 sesiones.
- **Violaciones de linaje V31/V32:** 0 en los 20 usuarios.
- **Generaciones rechazadas o NO_FACTIBLE:** 0. Las 20 se publicaron con el motor de reglas en 1 intento y 1 validación sin problemas.
- **Sesiones no señaladas que cambiaron:** 0. Ninguna sesión cambió en ningún plan siguiente.
- **Omisión declarada frente a ausencia de registro:** el backend las distingue (SKIPPED con motivo frente a sin registro).
- **Llamadas a Replicate:** 0.
