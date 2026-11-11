# 05 · Experimentos de mejora de prompts (MEJ, MEJ ronda 2, CONF-IA y confirmación de pr-1.8a)

**Pregunta común:** ¿se puede mejorar la generación o la adaptación **cambiando solo lo que recibe el modelo**, sin reglas, validadores ni funciones nuevas?

**Respuesta final:**
- **No hay ninguna mejora demostrada que justifique cambiar los prompts vigentes.**
- Se mantienen **PR-1.7** (generación) y **V2.1** (adaptación).

## 0. Cómo leer este documento

- **Versiones del modelo.** El endpoint `openai/gpt-5-structured` cambió dos veces de versión durante las campañas. Las comparaciones válidas son **dentro de la misma versión**; nunca se mezclan versiones en una misma prueba.

| Versión | Vigente desde | Campañas |
|---|---|---|
| `9f4cd9ec…` | antes del 06/11/2026 | GRA-R1 y GRA-R2. BASE la solicitó fijada, pero el proveedor la devuelve como `hidden` (no verificable) |
| `045f8102…` | 06/11/2026 18:46 UTC | MEJ, MEJ ronda 2, CONF-IA (C1–C3, A1, D1–D5, I1) |
| `81cb6179…` | 10/11/2026 18:29 UTC | Confirmación final de pr-1.8a (A2 y A3) |

- **Indicador principal de generación:** planes **válidos a la primera** (primera propuesta que pasa V1–V27), sobre **19 perfiles factibles** (O01–O20 sin O11). O11 se bloquea antes de llamar al modelo.
  - Los informes de GRA dan 13/20 y 14/20 porque cuentan O11-F35. Sobre los 19 factibles son **12/19 y 13/19**, que es lo que usan MEJ y BASE.
- **Ningún plan publicado tiene infracciones en ninguna campaña.** Lo que no pasa V1–V27 no se publica: se reintenta y, si se agotan los intentos, se publica el plan determinista. Los hallazgos (V4, V15, V23, V24, V26…) cuentan propuestas **rechazadas**.
- **Estadística:** McNemar exacta por pares perfil × ronda, Wilcoxon por perfil e intervalos de Wilson al 95 %. En la ronda 2 el umbral confirmatorio era α = 0,025 (dos variantes).
- **Perfiles sintéticos.** Nada de esto mide la adherencia ni la satisfacción de usuarios reales.

## 1. Cronología

| Fecha | Campaña | Versión del modelo | Variantes | Resultado | Decisión |
|---|---|---|---|---|---|
| 06/11 | GRA-R1/R2 (referencia) | `9f4cd9ec…` | PR-1.7 + V2.1 | Ver `02` y `03` | Línea de referencia |
| 07/11–08/11 | **MEJ** | `045f8102…` | PR-1.8; V2.2 | PR-1.8: 15/19 frente a 12/19 (p = 0,45), con 13 V24 en O18. V2.2: sin diferencia (12/12 en ambas) | Mantener las líneas de V15/V26 y retirar la de dosis. V2.2: corrección de coherencia sin efecto medible. Nada activado |
| 09/11 | **MEJ ronda 2** | `045f8102…` | pr-1.8a, pr-1.8b, pr-1.8c; v2.2a, v2.4 | Confirmatoria: 21/38, 27/38 y 28/38 (p = 0,11 y 0,039, ninguna < 0,025). Adaptación: 35/36, 36/36 y 36/36 | Ninguna candidata cumple todos los criterios. La auditoría encuentra H-A1/H-A2 |
| 10/11 | **CONF-IA** | `045f8102…` | pr-1.8c y pr-1.8a frente a PR-1.7; v2.2a frente a V2.1 | pr-1.8c NO CUMPLE; pr-1.8a FAVORABLE NO CONCLUYENTE; v2.2a NO PEOR, SIN VENTAJA DEMOSTRADA | Mantener PR-1.7 y V2.1. pr-1.8a, a confirmar |
| 10/11 | **Confirmación de pr-1.8a** | `81cb6179…` | pr-1.8a frente a PR-1.7 | A2: 13/18 frente a 13/18 (p = 1,0); A3 incompleta | **Decisión B: mantener PR-1.7** |

## 2. MEJ (primera ronda)

| Elemento | Valor |
|---|---|
| Fechas | Corridas pagadas del 07/11/2026 23:49 UTC al 08/11/2026 01:22 UTC. Protocolo congelado en `e851de7` y resultados en `42d9dc1` |
| Rama | `experimento/mejora-ia`, sobre `generation` tras la PR #42. Commits ejecutados: `e851de7` (A1, A2, G1), `c7b4b9d` (A0, C, G1C, G1R) y `ca4ef1f` (CC) |
| Versión | `045f8102…` antes y después de cada bloque (`mej_pago/versiones.txt` en el tar) |
| Perfiles | O01–O20 (19 factibles), con 10 ciclos y 12 sesiones de adaptación |
| Diseño | Una corrida por condición: C (PR-1.7) frente a G1 (PR-1.8); A0 (V2.1) frente a A1 y A2 (V2.2). La réplica G2 se sustituyó por los controles A0 y C (enmienda E-1) |
| Predicciones y coste | **102 predicciones, USD 2,00** (tope de 120 y USD 3,00) |
| Fuentes | `docs/FitSense/mejora_ia/PROTOCOLO_MEJ.md`, `RESULTADOS_MEJ.md` y `metricas_mej.json` (rama `experimento/mejora-ia`) |

### PR-1.8 (generación)
- **Problema:**
  - V26: los techos semanales no estaban en PR-1.7;
  - V15: `max_per_body_part` llegaba como `null` con REV2;
  - dosis baja: el objetivo de `CalidadDosis` no llegaba al prompt.
- **Cambio:** un bloque «COHERENCIA CON LOS VALIDADORES» con la mitad de V15, los techos de V26 y una dosis orientativa. Todo lo demás es idéntico a PR-1.7.

| Métrica (19 factibles) | MEJ-C (PR-1.7) | MEJ-G1 (PR-1.8) |
|---|---|---|
| Válidas a la primera | 12/19 | **15/19** (15/18 sin el fallo del proveedor) |
| Propuestas / rechazadas | 29 / 10 | 22 / **3** |
| Hallazgos V15 / V26 / V23 / V24 | 12 / 8 / 3 / 0 | 2 / 3 / 1 / **13** |
| Publicados desde la IA / respaldo | 19 / 0 | 18 / 1 (fallo del proveedor en O08) |
| DOSIS_LIMITADA | 17/19 | 15/19 |

- **Pareado:** 5 perfiles válidos solo con PR-1.8 y 2 solo con PR-1.7; McNemar **p = 0,45**.
- **Mejoró:** V15 + V26, de 20 a 5 hallazgos.
- **Empeoró:** O18 (GAIN_MUSCLE) dio **13 hallazgos V24 en una sola propuesta**: el modelo subió series fijadas por la dosis por clase. Se resolvió en el reintento. La dosis apenas cambió.
- **Decisión:** las líneas de V15 y V26 merecían seguir midiéndose; la línea de dosis, no. **Nada se activó.**

### V2.2 (adaptación)
- **Problema:**
  - el SIN_EFECTO evitable de O18 en GRA-R2: la factibilidad decía «OMÍTELA» aunque V8C permitía retirar un relleno;
  - no había instrucción para un dolor sin localizar (O12).
- **Cambio:** la factibilidad de ACORTAR lista los rellenos retirables, y se añade una línea de dolor (primero el mismo patrón y grupo).
- **Resultado:**
  - A0 (V2.1) 12/12 aplicadas; A1 y A2 (V2.2) 12/12 cada una;
  - pertinencia ACC 10, CONS-J 1, ALT 1 en las tres;
  - 0 INC y 0 cambios no autorizados.
  - O12 sigue en ALT en todas las condiciones.
- **Decisión:** la corrección de ACORTAR es coherente pero sin efecto medible; la línea de dolor no cambia nada. Ningún valor por defecto cambia.

### Incidencias
- **E-1:** comparar con GRA mezclaría versiones del modelo. Por eso solo se compara MEJ con MEJ.
- **I-MEJ-1:**
  - en G1, O03 superó los 150 s y el bloque se detuvo; se completó con G1C (O04–O10, O12–O14) y G1R (reintento de O03);
  - en C, O18 c1 terminó con estado desconocido; se completó con CC (O19, O20);
  - O08 en G1 terminó `failed` y recibió el respaldo.

## 3. MEJ ronda 2

| Elemento | Valor |
|---|---|
| Fechas | Protocolo congelado el 09/11/2026 (`a57abd2`). Corridas del 09/11 06:03 UTC a 22:08 UTC. Cierre `0878f85` |
| Commit ejecutado | `1aac853` (las 40 configuraciones de corrida) |
| Versión | `045f8102…` en los 59 registros de `versiones.txt`. Las 31 filas sin versión en el CSV son la corrida reconstruida `MEJR2_G17_R2_REC` (I-R2-9) |
| Perfiles | 19 factibles y O11 aparte; O10 como caso diagnóstico. 12 sesiones de adaptación por corrida |
| Rondas | Exploratoria **R1**: 135 predicciones, USD 3,0632. Confirmatoria **R2** (orden inverso) + **R3** (orden directo): 227 predicciones, USD 4,8419 |
| Total | **362 predicciones, USD 7,9051** |
| Fuentes | `docs/FitSense/mejora_ia/ronda2/`: `PROTOCOLO_MEJ_RONDA_2.md`, `RECOMENDACION_FINAL.md`, `AUDITORIA_REGLAS_FITSENSE.md`, `INCIDENCIAS_MEJ_R2.md` y CSV (rama `experimento/mejora-ia`) |

### Variantes de generación

| Variante | Problema | Cambio respecto a PR-1.7 |
|---|---|---|
| **pr-1.8a** | V15 y V26, sin la línea de dosis que causó el V24 de MEJ | Solo las líneas de V15 y V26 de PR-1.8 |
| **pr-1.8b** | Además, la dosis | pr-1.8a + dosis orientativa, indicando que la dosis de cada ejercicio no se toca |
| **pr-1.8c** | Aprovechamiento del tiempo y sesiones con menos de 3 ejercicios (O10) | pr-1.8a + estructura del objetivo, coste en segundos de cada dosis y tiempo útil de la sesión |

### Resultados de generación (válidos a la primera, R1 / R2 / R3)

| | Válidos | Rechazadas | V15 | V26 | V24 | V23 |
|---|---|---|---|---|---|---|
| PR-1.7 | 10 / 10 / 11 | 12 / 12 / 11 | 13 / 8 / 12 | 8 / 4 / 10 | 0 / 1 / 1 | 4 / 3 / 3 |
| pr-1.8a | 14 / 15 / 12 | 4 / 4 / 11 | 2 / 2 / 5 | 0 / 2 / 5 | 0 / 0 / 1 | 1 / 2 / 4 |
| pr-1.8c | 15 / 14 / 14 | 4 / 7 / 5 | 5 / 4 / 5 | 2 / 3 / 2 | 0 / 2 / 1 | 0 / 1 / 0 |
| pr-1.8b (solo R1) | 14 | 6 | 4 | 3 | **3** | 2 |

- **pr-1.8b se excluyó tras R1** por 3 V24 frente a 0 del control.
- **Confirmatoria (R2 + R3, 38 pares):**
  - PR-1.7 21/38 (55,3 %), pr-1.8a 27/38 (71,1 %), pr-1.8c 28/38 (73,7 %); los intervalos de Wilson se solapan;
  - McNemar: pr-1.8a **p = 0,11**, pr-1.8c **p = 0,039**. Con α = 0,025, **ninguna demuestra la mejora**.
  - Con R1 incluida, pr-1.8c sí pasa el umbral (p = 0,004), pero R1 sirvió para elegir las variantes, así que ese agregado está sesgado y no se usa como prueba.
- **Criterios para declarar candidata:**
  - pr-1.8a falla el criterio 3 (un respaldo atribuible: O12 en R3, 3 × V23);
  - pr-1.8c falla el criterio 2 (3 V24 frente a 2 del control).
- **Dosis y tiempo:** ninguna diferencia demostrable (DOSIS_LIMITADA entre el 69 y el 89 %; Wilcoxon p = 0,21–0,89).

### Variantes de adaptación

| Variante | Problema | Cambio | Aplicadas (R1 + R2 + R3) |
|---|---|---|---|
| V2.1 (control) | — | — | **35/36** (1 SIN_EFECTO evitable: O18 en R2) |
| **v2.2a** | Coherencia de ACORTAR, incluido el tope M residual (H-A3) | V2.1 + retirada de un relleno marcado `[cabe]` solo si respeta el tope M; sin la línea de dolor de V2.2 | **36/36** |
| **v2.4** | Comprensión del caso | v2.2a + un «RESUMEN DEL CASO» por sesión | **36/36** (un INC del oráculo en O12 R1 que, por identidad, es un artefacto) |

- En las tres: **0 INC por identidad y 0 cambios no autorizados en 108 sesiones**. McNemar 1:0, p = 1.
- **Decisión:** ninguna mejora demostrada justifica cambiar V2.1. v2.2a sería la opción si se busca coherencia; v2.4 se descarta porque no aporta nada sobre v2.2a.

### Auditoría de reglas
- Detectó **H-A1** (V8C aceptaba retiradas no autorizadas) y **H-A2** (V8C aceptaba cambios solo del descanso).
- Ninguna propuesta real los explotó: se vigilaron por identidad (`RETIRADA_NO_AUTORIZADA`, `DESCANSO_CAMBIADO`) con 0 casos.
- Se corrigieron después, fuera de la campaña. Ver `06_CORRECCIONES_V8C.md`.
- Otros hallazgos (H-A3, H-G4, H-G5, H-G8, H-G9, H-G14…) quedan documentados en `AUDITORIA_REGLAS_FITSENSE.md` §7 y **no se actuó sobre ellos**.

### Incidencias principales
- **Timeouts:** I-R2-3, I-R2-4, I-R2-10, I-R2-12.
- **502 del proveedor:** I-R2-5, I-R2-6.
- **Predicciones recuperadas:** I-R2-7, I-R2-8, I-R2-11.
- **G17 R2 detenido por el límite de tiempo del entorno** y reconstruido sin métricas de dosis ni tiempo (I-R2-9).
- **Error del instrumento** en el cruce entre bases, corregido (I-R2-13).
- **Antes de la ronda:** 3 pruebas de integración dependientes de la fecha (I-R2-1), resueltas más tarde con la PR #43.

## 4. CONF-IA (confirmación)

| Elemento | Valor |
|---|---|
| Fecha | 10/11/2026 |
| Rama y código | `experimento/mejora-ia`, código evaluado `ab04e67`: V8C corregido + pruebas de fecha |
| Congelación | `957a08d`, con huellas, antes de la primera llamada pagada |
| Versión | `045f8102…` |
| Bloques | Generación C1–C3 (PR-1.7 frente a pr-1.8c) y A1 (pr-1.8a, pareada con C1). Adaptación D1–D5 (V2.1 frente a v2.2a). Flujo conjunto I1 |
| Predicciones y coste | **336 predicciones, USD 7,0950**: generación 4,93, adaptación 1,38 y flujo conjunto 0,78 |
| Fuentes | `docs/FitSense/mejora_ia/confirmacion/`: `RECOMENDACION_FINAL_ADOPCION.md`, `RESULTADOS_VALIDACION_INTEGRAL.md`, `INCIDENCIAS_CONF_IA.md` y CSV (rama `experimento/mejora-ia`) |

### Generación

| Comparación | PR-1.7 | Variante | Prueba principal | Otros criterios | Veredicto |
|---|---|---|---|---|---|
| pr-1.8c, C1–C3 (57 pares) | 34/57 | 41/57 | Wilcoxon por perfil p = 0,29 (8 mejor, 4 peor, 7 igual) | **V24 2 frente a 0** (O13 y O18 en C3, GAIN_MUSCLE); 14 rechazos V4 en LOSE_WEIGHT | **NO CUMPLE** |
| pr-1.8a, A1 frente a C1 (19 pares) | 12/19 | 16/19 | p = 0,28 (7 mejor, 3 peor, 9 igual) | V24 0/0; 3 rechazos V4 en LOSE_WEIGHT | **FAVORABLE NO CONCLUYENTE** (una sola ronda por presupuesto) |

**Por objetivo** (válidos a la primera):

| Objetivo | PR-1.7 (3 rondas) | pr-1.8c (3 rondas) | pr-1.8a (1 ronda) |
|---|---|---|---|
| GAIN_MUSCLE | 12/15 | 13/15 (V24 2) | 5/5 |
| GENERAL_FITNESS | 5/15 | 9/15 | 3/5 |
| INCREASE_STRENGTH | 3/12 | 9/12 | 4/4 |
| LOSE_WEIGHT | **14/15** | **10/15** (14 rechazos V4) | 4/5 (3 rechazos V4) |

**Análisis secundario** (informativo, no decide):
- pr-1.8c: 69/95 frente a 55/95, p = 0,078, V24 5 frente a 2;
- pr-1.8a: 43/57 frente a 33/57, p = 0,103, V24 1 frente a 2.

### Adaptación (D1–D5, con V8C corregido en las dos)
- **V2.1 58/60 aplicadas, frente a 60/60 de v2.2a.**
  - Las 2 de V2.1 son el mismo caso: O18 (quitar un relleno al acortar), SIN_EFECTO en D1 y D2.
  - McNemar 2 a 0, **p = 0,50**.
- 0 cambios no autorizados y 0 INC por identidad en las dos.
- El INC del oráculo en O12 (D1, v2.2a) es un artefacto posicional: por identidad la sesión no se endurece y el trabajo baja de 1686 a 740.
- V8C rechazó una propuesta (D3, O19, v2.2a) por una regla de ACORTAR que ya existía. El reintento se aplicó.
- **Veredicto:** NO PEOR, SIN VENTAJA DEMOSTRADA.

### Flujo conjunto I1 (PR-1.7 + v2.2a + V8C corregido)
- 12/19 válidos a la primera, **12/12 adaptaciones aplicadas** y 0 cambios no autorizados.
- **148/148** aspectos aplicables aprobados por el oráculo.
- Se usó PR-1.7 porque ninguna variante de generación «cumplía» (I-C-12).

### Incidencias del proveedor
- 5 timeouts.
- 3 errores de E/S en el POST, recuperados de la lista de la cuenta y validados sin repetir la llamada.
- 1 predicción `failed`.

## 5. Confirmación final de pr-1.8a

| Elemento | Valor |
|---|---|
| Fecha | 10/11/2026 |
| Rama | `experimento/confirmacion-pr18a-final`, creada desde `generation`; pr-1.8a trasladada sin cambios desde la ronda 2 en `f95b218`, apagada por defecto. Protocolo congelado en `5f48dd5` |
| Versión | `81cb6179…`, la misma en todos los bloques |
| Comprobación previa | Regresión 1334/0/1 (unitarias) y 593/0/44 (integración). Prompts byte a byte idénticos a los de la ronda 2 |
| Rondas | **A2** (decide; 18 perfiles, O09 excluido tras 3 timeouts de pr-1.8a) y **A3** (incompleta; no entra en la decisión) |
| Predicciones y coste | **99 predicciones, USD 2,6115**. Bloque de USD 10: gastado 9,7065, quedan 0,2935 |
| Fuentes | `docs/FitSense/mejora_ia/confirmacion_pr18a/`: `RECOMENDACION_FINAL_PR18A.md`, `ANALISIS_V4_V24.md` y `RESULTADOS_POR_OBJETIVO.md` (rama `experimento/confirmacion-pr18a-final`) |

### Resultado de A2

| Criterio | PR-1.7 | pr-1.8a | ¿Cumple? |
|---|---|---|---|
| 1. Válidos a la primera (p < 0,05) | 13/18 | 13/18 | **No**: 2 mejor, 2 peor, 14 igual; p = 1,0 |
| 2. V24 ≤ control | 0 | **1** (O04, GENERAL_FITNESS) | **No** |
| 3. Estructurales ≤ control | 2 | **3** (V23) | **No** |
| 4. Ningún plan publicado con violaciones | 0 | 0 | Sí |
| 5. Dosis y tiempo | — | % de M p = 0,32 | Sí (GAIN_MUSCLE baja de 57,7 a 52,5 %) |
| 6. LOSE_WEIGHT ≥ control | 5/5 | **4/5** | **No**: 3 rechazos V4 en O06 |
| 7. Sesiones INSUFICIENTE ≤ control | 1 | 0 | Sí |

**Por objetivo:**
- INCREASE_STRENGTH: 1/4 → **3/4** (menos V15 y V26).
- GENERAL_FITNESS: 2/4 → **1/4**.
- LOSE_WEIGHT: 5/5 → **4/5**. En O06, la primera propuesta tenía una sesión de 2148 s con un techo de 2100 s, y el plan publicado tiene 18 min de trabajo aeróbico frente a 36.
- GAIN_MUSCLE: 5/5 en ambas.

**Fiabilidad (A2 + A3):**
- timeouts: 0 con PR-1.7 frente a 5 con pr-1.8a (O09 cuatro veces de cuatro, O03 una);
- latencia mediana: 78,8 s frente a 88,2 s.

**A3:** quedó incompleta (se agotó el saldo). Solo como sensibilidad: 5/9 frente a 6/9 en los perfiles comunes, sin efecto sobre la decisión.

**Decisión B del protocolo: mantener PR-1.7.** La ventaja de A1 (16/19 frente a 12/19) se obtuvo con otra versión del modelo y no se reprodujo con la actual. pr-1.8a sigue apagada en su rama experimental.

## 6. Ficha por variante

| Variante | Problema que atacaba | Versión(es) del modelo | Qué mejoró | Qué empeoró | Decisión |
|---|---|---|---|---|---|
| **PR-1.7** (vigente) | — | `9f4cd9ec…`, `045f8102…`, `81cb6179…` | Control. 0 V4 en LOSE_WEIGHT en todas las campañas; sin timeouts en A2/A3 | Más rechazos V15/V26 en INCREASE_STRENGTH y V23 en GENERAL_FITNESS | **Se mantiene** |
| PR-1.8 | V15, V26 y dosis baja | `045f8102…` | V15 + V26 de 20 a 5 | 13 V24 en O18 (línea de dosis) | No adoptada; sustituida por pr-1.8a |
| pr-1.8a | V15 y V26 sin la línea de dosis | `045f8102…` (R2, A1); `81cb6179…` (A2, A3) | Fuerza; validez con `045f8102…` (no concluyente) | V4 en LOSE_WEIGHT (A1, A2), V24 y V23 en A2, timeouts y latencia | **No adoptada** (decisión B) |
| pr-1.8b | Además, la dosis | `045f8102…` (R1) | — | 3 V24 frente a 0 | Excluida tras R1 |
| pr-1.8c | Tiempo y sesiones cortas | `045f8102…` (R2, C1–C3) | V23 en GENERAL_FITNESS; fuerza | V24 en GAIN_MUSCLE; 14 V4 en LOSE_WEIGHT; latencia | **No adoptada** (NO CUMPLE) |
| **V2.1** (vigente) | — | `9f4cd9ec…`, `045f8102…` | Control. 0 cambios no autorizados | SIN_EFECTO evitable en O18 (GRA-R2, MEJ R2, CONF-IA D1/D2) | **Se mantiene** |
| V2.2 | O18 y dolor sin localizar | `045f8102…` (MEJ) | Coherencia de ACORTAR | Nada medible | No adoptada; base de v2.2a |
| v2.2a | Coherencia de ACORTAR (tope M) | `045f8102…` (R2, CONF-IA) | Resuelve O18 (36/36 y 60/60) | Un rechazo de V8C con reintento aplicado (D3, O19) | **No adoptada** (sin ventaja demostrada) |
| v2.4 | Comprensión del caso | `045f8102…` (R2) | Igual que v2.2a | Un timeout; no aporta nada | Descartada |

## 7. Por qué se mantienen PR-1.7 y V2.1

**PR-1.7:**
- Ninguna variante cumplió los criterios fijados de antemano en una ronda confirmatoria.
- La mejor (pr-1.8a) **no reprodujo** su ventaja con la versión actual del modelo (13/18 frente a 13/18).
- Las variantes que añaden el bloque «COHERENCIA CON LOS VALIDADORES» **trasladan errores**: mejoran fuerza, pero aparecen rechazos V4 en pérdida de peso en dos versiones del modelo, y más timeouts.
- PR-1.7 nunca tuvo un V4 en LOSE_WEIGHT.

**V2.1, aunque v2.2a fue favorable en O18:**
- La diferencia es **un solo caso** (O18, quitar un relleno al acortar), con McNemar p = 0,50 en CONF-IA y p = 1 en la ronda 2.
- Con V2.1, ese caso termina en SIN_EFECTO: **no se publica ningún cambio y la sesión se conserva**. No es un riesgo para el usuario, sino una adaptación que no llega a hacerse.
- V2.1 no produjo ningún cambio no autorizado en ninguna campaña.
- Adoptar v2.2a sería, como mucho, una mejora de **coherencia** en un caso concreto, no una mejora estadística. Se decidió no cambiar el prompt vigente sin ventaja demostrada.

## 8. Evidencia e integridad

| Campaña | Evidencia | Comprobación |
|---|---|---|
| MEJ | `docs/FitSense/mejora_ia/evidencia/MEJ_corridas_y_oraculos.tar.gz` (908 entradas) | `SHA256SUMS.txt` 5/5 OK; el tar se lee sin errores |
| MEJ ronda 2 | `docs/FitSense/mejora_ia/ronda2/EVIDENCIA_MEJ_RONDA_2.zip` (2978 archivos, 18 MB) | **No hay `SHA256SUMS.txt`.** Huellas comprobadas a mano: `correr_MEJ_R2.sh` (71d46f86…), `correr_MEJ_R2_compleccion.sh` (18bc96f5…) y `analisis_mej_r2.py` (c328532b…) coinciden con el protocolo y las incidencias. El zip se abre sin errores |
| CONF-IA | `docs/FitSense/mejora_ia/confirmacion/EVIDENCIA_CONFIRMATORIA.zip` (2353 archivos, 15 MB) | `SHA256SUMS_CONGELACION.txt`: 11/11 entradas verificables OK (3 sobre blobs `ab04e67:<ruta>`) y 1 registro informativo de la versión. El ZIP se abre sin errores |
| pr-1.8a | `docs/FitSense/mejora_ia/confirmacion_pr18a/EVIDENCIA_PR18A.zip` (1090 archivos, 7,6 MB) | `SHA256SUMS_CONGELACION_PR18A.txt`: 10/11 archivos OK; difiere `correr_PR18A.sh`, modificado y documentado en I-P-2. El ZIP se abre sin errores |
