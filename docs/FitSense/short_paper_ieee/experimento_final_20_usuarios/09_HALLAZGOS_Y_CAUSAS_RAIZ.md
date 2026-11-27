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


---

# Hallazgos de la etapa C (Replicate real)

| # | Hallazgo | Categoría | Acción |
|---|---|---|---|
| H8 | **Intento 1:** el espía del laboratorio lanzaba `ClassCastException` (`String.valueOf(char[])`) antes de llamar. Hubo 0 predicciones | **A** | Corregido en el laboratorio (`c63bad2`). Intento conservado |
| H9 | **Intento 2:** el proxy de esta sesión corta a los 30 s (502). Con la espera síncrona por defecto de 60 s se perdieron 7 de 12 respuestas que Replicate sí completó y cobra. Después, el disyuntor dejó de llamar | **E**, por el entorno. No es un defecto de la lógica | Anexo 2: `prefer-wait-seconds=25`, una propiedad existente. **Propuesta P3:** valorar 25–30 s como valor por defecto para tolerar intermediarios con límite de 30 s. Sería un cambio de configuración de producción y queda pendiente de tu decisión |
| H10 | **Sustituciones de la IA en VARIANTE_MAS_SENCILLA_O_CARGA** (U04 S2: 7 ejercicios; U09 S2: 6), comprobadas en el catálogo:<br>• **las 13 conservan el patrón de movimiento y el músculo objetivo**;<br>• la dificultad baja de 2 a 1 en 8 y es igual en 5 (ninguna sube);<br>• la dosis no cambia (3×10 o 3×6, con los mismos descansos).<br>Dos observaciones:<br>• **(a)** el ejercicio 580 se llama en español «Curl inclinado con mancuerna», pero el catálogo lo clasifica como HORIZONTAL_PUSH y pectorales, así que su sustitución por «Press de banca con banda» es coherente. **El nombre parece mal traducido**;<br>• **(b)** V8C comprueba el patrón **por conjunto** (cada añadido tiene un patrón de entre los retirados) y la dificultad máxima del perfil (V6), pero **no** que cada variante sea menos difícil que la que sustituye. En estos datos no hizo falta: ninguna subió | **Observación.** Ningún defecto demostrado: ninguna regla vigente se incumplió | Ninguna. El catálogo y los validadores no se tocan (prohibido). **P4** (pendiente de tu decisión): revisar el nombre del 580 y valorar exigir «dificultad ≤ la del sustituido» en VARIANTE. La adecuación clínica sigue necesitando revisión profesional |
| H11 | 4 de 8 propuestas aceptadas no cambiaron nada efectivo (`SIN_EFECTO`). En 3 de ellas, el intento anterior acortaba la sesión por debajo de V20 | Comportamiento válido (**G**). El sistema conserva antes de publicar algo inválido | Ninguna |
| H12 | Las 5 propuestas de adaptación rechazadas lo fueron por **V20** (duración mínima del 70 %). El modelo tiende a acortar ante fatiga, dificultad o falta de motivación | Comportamiento válido (**G**). Los validadores protegen | Ninguna. Es dato para el artículo |

**Defectos del backend demostrados en la etapa C: 0.**
