# Pruebas con Replicate real · informe

**Fecha:** 27/10/2026 · **Modelo:** `openai/gpt-5-structured`, variante `gpt-5-mini`, razonamiento `medium` · máximo de **3 intentos** por adaptación y por generación (el del sistema, sin cambios).
**Base:** PostgreSQL efímera (Testcontainers) con participantes sintéticos. **Neon sin tocar.** Progresión encendida **solo** en el contexto aislado de la segunda campaña; en producción sigue apagada.
**Arnés:** `CampanaReplicateRealIT` (opt-in: `-Dfitsense.replicate.real=true`). Evidencia completa (propuesta original de cada llamada, rechazos por regla, plan antes y después, evidencia V32, latencias y métricas): `evidencia/replicate_real/{adaptacion,progresion}/`.

> **Estado (28/10/2026):** los cinco problemas de §4 están **corregidos y verificados** con Replicate real (§8–§12). El hallazgo posterior de la herencia de D8 (§11.1) está **aprobado, corregido y verificado** (§13). No queda nada pendiente de decisión. Las secciones §1–§7 describen la campaña **anterior** a las correcciones y se conservan como línea base.

## 1. Resumen

| | Campaña 1 · adaptación (configuración de producción) | Campaña 2 · progresión encendida (aislada) |
|---|---:|---:|
| Perfiles (los más restrictivos de cada objetivo) | 12 | 12 |
| Generaciones iniciales | 12 (11 IA, 1 reglas) | 12 (11 IA, 1 reglas) |
| Ciclos semanales | 97 | 120 |
| Ciclos con IA | 66 | 24 |
| Llamadas reales | 132 | 71 |
| Semanas publicadas | 93 de 97 | 120 de 120 |
| Violaciones de las comprobaciones independientes | **16 ciclos con dosis subidas por la IA** (+2 de semanas sin evidencia, ver §4.2) | **0** |
| Errores del proveedor | 0 | 2 (transitorios, bien gestionados) |

**Consumo total de la sesión** (consulta a Replicate, incluidos los pilotos): **218 predicciones**, 10,19 M tokens de entrada y 0,93 M de salida, 8 033 s de cómputo. **Coste estimado ≈ US$ 4,41** con la tarifa pública de GPT-5 mini (0,25 / 2,00 US$ por millón de tokens de entrada / salida); la cifra oficial es la del panel de Replicate, que no expone el precio por API.

**Conclusión:** con respuestas reales, el núcleo funciona de extremo a extremo en los cuatro objetivos: la IA genera rutinas iniciales válidas (V24+V25) y propone adaptaciones que, en su mayoría, se aceptan en 1–2 intentos. Los validadores rechazan lo que deben (sustituciones de otro patrón, ejercicios añadidos, cambios de carga sin permiso, dosis que V24 no admite), el respaldo funciona y los ciclos deterministas siguen sin llamar a la IA. **Pero la IA real encontró tres defectos que la simulada no podía encontrar** (§4). El más grave: en la configuración de producción, la regla «la IA nunca sube dosis» no se aplica.

## 2. Cómo se probó

- **Perfiles:** por objetivo, PRINCIPIANTE/30 min/casa, INTERMEDIO/45/gimnasio y AVANZADO/60/gimnasio. Mujer, 3 días (L-X-V).
- **Generación inicial:** la de producción, con la IA real (el motor de reglas solo como respaldo). Se revalida después con el validador completo (V24 y V25).
- **Campaña 1, guion de 9 semanas** (cada fila es lo que pasa en la semana medida):

  | Semana | Lo que hace la participante | Intervención esperada |
  |---|---|---|
  | s2 | omite el viernes por fatiga | `REVISAR_CARGA_Y_RECUPERACION` |
  | s3, s5, s10 | todo hecho | copia (`SIN_ORDEN`, 0 llamadas) |
  | s4 | omite el viernes por falta de tiempo | `ACORTAR_O_REORGANIZAR` |
  | s6 | viernes parcial: un ejercicio con 20 kg prescritos y anotados; el resto, dejado por fatiga | `REVISAR_CARGA` con carga |
  | s7 | omite el miércoles por demasiado difícil | `VARIANTE_MAS_SENCILLA_O_CARGA` |
  | s8 | omite miércoles y viernes por falta de motivación | `INVESTIGAR_CAMBIO_MINIMO` (dos sesiones) |
  | s9 | omite el viernes por dolor | `SEGURIDAD_ANTE_DOLOR` |

- **Campaña 2:** el guion de 10 semanas de la evaluación limpia (fatiga en la s5, falta de tiempo en la s7, el resto hecho), con la progresión encendida.
- **Comprobaciones independientes de los validadores**, en cada ciclo:
  - la IA no sube ninguna dosis publicada en una sesión señalada (series, repeticiones, segundos, carga);
  - las sesiones **no señaladas** salen idénticas (salvo un paso de progresión o una sustitución por seguridad declarados);
  - `ai_calls` de V32 coincide con las llamadas contadas, y nunca hay más de 3;
  - un ciclo determinista hace 0 llamadas;
  - cada paso cumple el 5 % (progresión), el 10 % (recuperación) y el techo de 1,10 × base;
  - la consulta de linaje V31/V32 no encuentra violaciones.
- **Parada automática:** 5 fallos seguidos del proveedor o más de 3 llamadas en un ciclo. No se activó.

## 3. Resultados por objetivo

### 3.1 Campaña 1 · adaptación (configuración de producción)

| Objetivo | Escenarios con IA (inicial + ciclos) | Llamadas reales | Inicial IA / reglas | Adaptación válida (APLICADA + SIN_EFECTO) | Inválida → RESPALDO / NO_FACTIBLE / sin plan por error | Ciclos con reintento | Rechazos V8C | V24 | V25 | Otros rechazos | Ciclos con dosis subidas |
|---|---:|---:|---|---:|---|---:|---:|---:|---:|---|---:|
| LOSE_WEIGHT | 20 | 33 | 3 / 0 | 15 | 1 / 1 / 0 | 9 | 4 | 0 | 0 | V11×2, V17×11, V20×5 | 7 |
| GAIN_MUSCLE | 20 | 29 | 2 / 1 | 16 | 0 / 1 / 0 | 4 | 2 | 13 | 0 | V11×3, V17×2, V20×4 | 0 |
| INCREASE_STRENGTH | 21 | 36 | 3 / 0 | 18 | 0 / 0 / 0 | 14 | 2 | 0 | 0 | V11×3, V17×17, V20×6 | 6 |
| GENERAL_FITNESS | 17 | 34 | 3 / 0 | 12 | 1 / 0 / **2** | 10 | 1 | 2 | 0 | V11×2, V17×14, V20×4, V23×4 | 3 |

Los rechazos cuentan los intentos fallidos que se reintentaron (los de V24 y V23 de la fila incluyen los de la generación inicial). «Sin plan por error»: semanas que pasaron los validadores y fallaron al guardarse (§4.2).

**Por intervención** (66 ciclos con IA):

| Intervención | Ciclos | APLICADA | SIN_EFECTO | RESPALDO | NO_FACTIBLE | Llamadas |
|---|---:|---:|---:|---:|---:|---:|
| `REVISAR_CARGA_Y_RECUPERACION` (fatiga y carga) | 23 | 20 | 3 | 0 | 0 | 34 |
| `ACORTAR_O_REORGANIZAR` | 11 | 9 | 2 | 0 | 0 | 17 |
| `VARIANTE_MAS_SENCILLA_O_CARGA` | 11 | 9 | 0 | 2 | 0 | 21 |
| `INVESTIGAR_CAMBIO_MINIMO` (dos sesiones) | 12 | 9 | 1 | 0 | 2 | 28 |
| `SEGURIDAD_ANTE_DOLOR` | 8 | 8 | 0 | 0 | 0 | 11 |

Intentos por ciclo con IA: 29 aceptados al primer intento, 27 al segundo y 10 necesitaron el tercero. Latencia: media de 40 s por llamada, máxima de 103 s.

### 3.2 Campaña 2 · progresión encendida (aislada)

| Objetivo | Escenarios con IA | Llamadas reales | Inicial IA / reglas | Adaptación válida | RESPALDO | Ciclos deterministas (llamadas) | Pasos: progresión / recuperación / reincorporación / vuelta atrás | Rechazos V8C | V24 | V25 | Otros | Violaciones |
|---|---:|---:|---|---:|---:|---|---|---:|---:|---:|---|---:|
| LOSE_WEIGHT | 9 | 20 | 3 / 0 | 6 | 0 | 24 (0) | 13 / 2 / 0 / 0 | 2 | 28 | 2 | V17×6 | 0 |
| GAIN_MUSCLE | 9 | 14 | 3 / 0 | 6 | 0 | 24 (0) | 15 / 0 / 0 / 0 | 0 | 16 | 0 | V20×3 | 0 |
| INCREASE_STRENGTH | 9 | 17 | 3 / 0 | 3 | 3 | 24 (0) | 13 / 2 / 0 / 0 | 0 | 16 | 0 | V17×3, V20×2 | 0 |
| GENERAL_FITNESS | 9 | 20 | 2 / 1 | 2 | 4 | 24 (0) | 13 / 2 / 0 / 0 | 7 | 14 | 0 | V17×8, V23×1 | 0 |

- **La IA no controla la progresión:** los 54 pasos de progresión y los 6 de recuperación los dio el backend en ciclos `SIN_ORDEN`, con **0 llamadas en los 96 ciclos deterministas**. Ninguno superó el 5 %, el 10 % ni el techo.
- **Con el interruptor encendido, la IA no pudo subir ninguna dosis:** 0 violaciones. V24 por capacidades rechazó 74 intentos que no respetaban la dosis autorizada, y V25 rechazó 2.
- **Reincorporación:** ninguna en 10 semanas, como en la simulación. La regla 1(a) (solo `PROGRESION` posterior al último acortamiento) no se pudo ejercitar en positivo con la IA real en este horizonte; la cubren `ReincorporacionTrasProgresionTest` y `CopiaSinIaIT`.
- **Errores del proveedor (2):**
  - un POST cortado (error de E/S): la adaptación siguió por el respaldo;
  - una generación inicial que superó los 150 s: se publicó por el motor de reglas.

## 4. Problemas encontrados (investigados; ninguna regla cambiada)

### 4.1 GRAVE · En producción, la IA puede subir dosis en una sesión señalada — **corregido (§8, C1)**

- **Qué pasó:** en la campaña 1, **16 ciclos publicaron dosis más altas** que las de la semana medida, todos como `APLICADA`:
  - 8 en `REVISAR_CARGA_Y_RECUPERACION`, es decir, **después de una omisión por fatiga**;
  - 4 en `INVESTIGAR_CAMBIO_MINIMO`;
  - 3 en `VARIANTE_MAS_SENCILLA_O_CARGA`;
  - 1 en `ACORTAR`.
- **Ejemplos:** `3x6 → 4x6` en dos ejercicios (fuerza, intermedio); `2x6 → 3x6` en siete ejercicios de una sesión; `3x6/150 s → 2x8/180 s` (sube repeticiones aunque baja series); `2x12 → 4x12`.
- **Causa:** la regla existe (`ConservationValidator.revisarDosisDeLaIa`: «La IA no puede subir dosis: solo el backend progresa»), pero **solo se ejecuta con `autorizarDosis`**. Ese indicador vale `progresion || validaCapacidades`, y en producción los dos interruptores están apagados. Se diseñó junto con las dosis autorizadas para V24, y la comprobación de subida quedó dentro del mismo `if`.
- **Por qué no salió antes:** la IA simulada nunca subía dosis. En GAIN_MUSCLE no ocurre porque su V24 (4D) fija la dosis exacta. En la campaña 2 (interruptor encendido), 0 casos.
- **Propuesta (pendiente de tu autorización, porque cambia el comportamiento publicado):** ejecutar siempre la comprobación de subida y dejar solo la emisión de dosis autorizadas para V24 detrás de `autorizarDosis`. Es aplicar una regla ya aprobada, no relajarla. Efecto esperado: esos 16 ciclos pasarían a reintento y, si la IA insiste, a `RESPALDO`.

### 4.2 V9 no comprueba el mínimo de series en ejercicios de duración — **corregido (§8, C2)**

- **Qué pasó:** en 2 perfiles de GENERAL_FITNESS la IA propuso un ejercicio de duración con **1 serie** (`1150: 1x30 s`; `1325: 1x120 s`). Pasó todos los validadores y **la base lo rechazó al guardar** (`ck_pwe_min_sets`: series ≥ 2 si no son nulas). El ciclo terminó con excepción: **semana sin plan y sin evidencia V32**, después de 2–3 llamadas pagadas.
- **Causa:** `PlanDraftValidator.validatePrescription` aplica el mínimo de series (V9) solo a `SETS_REPS`; para `DURATION` solo comprueba los segundos (V10).
- **Propuesta:** aplicar el mínimo de series también a los ejercicios de duración cuando declaran series, para que el validador diga lo que ya exige la base y el reintento de la IA pueda corregirlo. Pendiente de autorización.

### 4.3 V11 deja sin plan una semana con carga sugerida y sesión omitida — **corregido (§8, C3)**

- **Qué pasó:** 2 perfiles acabaron en `NO_FACTIBLE` (semana sin plan). El ejercicio tenía 20 kg sugeridos y la participante **omitió esa sesión** (motivación), así que no anotó peso. V11 exige entonces quitar la carga.
  - La IA la mantuvo en los 3 intentos.
  - El **respaldo determinista también la mantiene** (conserva la sesión tal cual), así que falla igual.
- **Matiz:** la carga de partida la puso el arnés (como en `PendientesProgresionIT`), pero la situación es real: una persona que anota peso y después falta a esa sesión.
- **Propuesta:** que el respaldo conserve la sesión **sin la carga sugerida** cuando V11 lo exige. Pendiente de autorización.

### 4.4 Observaciones sin regla vigente que las prohíba — **ambas convertidas en regla (§8, C4 y C5)**

- **`APLICADA` solo por minutos (3 ciclos):** la IA no cambió ejercicios, dosis ni carga, solo la duración declarada (p. ej. 51 → 41 min), y `cambio()` lo cuenta como adaptación efectiva. Infla ligeramente la métrica de adaptaciones aplicadas. **Decisión tuya:** ¿un cambio solo de minutos debe ser `SIN_EFECTO`?
- **Ante dolor se pueden añadir más ejercicios de los que se retiran.** En el piloto: 3 retirados y 4 añadidos (un curl de bíceps nuevo); 0 casos en la campaña.
  - V8C no limita los añadidos con `SEGURIDAD_ANTE_DOLOR`: retorna antes de comprobarlos. Con la dificultad sí rechaza «añadir trabajo nuevo».
  - En otro caso de dolor la sesión pasó de 44 a 55 min.
  - **Decisión tuya:** ¿limitar los añadidos a como mucho los retirados?
- **El proxy de este entorno corta las peticiones a los 30 s.** Replicate retenía el POST hasta 60 s (`Prefer: wait`), así que la petición se cortaba mientras **la predicción seguía corriendo y cobrándose sin que nadie la leyera**. Ocurrió en el primer piloto: 3 predicciones huérfanas, ≈ US$ 0,04.
  - **Corrección:** `fitsense.ai.prefer-wait-seconds`, con 60 por omisión (producción no cambia). La campaña usó 20; el sondeo que ya existía recoge el resultado.
  - **Recomendación:** comprobar en el despliegue que ningún proxy ni balanceador corte antes de 60 s, o bajar el valor.

## 5. Ejemplos

### 5.1 Cambios aceptados

| Caso | Antes (semana medida) | Publicado |
|---|---|---|
| Fatiga, LOSE_WEIGHT intermedio | `1176:3x120s, 1054:3x120s, …, 1225:3x10r` (51 min) | `1176:3x90s, 1054:3x90s, …, 1225:2x10r` (41 min) |
| Falta de tiempo, GAIN_MUSCLE | `1096:3x10, 307:3x10, 1263:3x10, 1105:2x13` (24 min) | Retira el relleno `1105`; el resto intacto (18 min) |
| Dificultad, GAIN_MUSCLE principiante | `1099, 144, 704, 143` | `933, 1076, 101, 117`: mismo patrón, mismo músculo y dificultad 1 en los cuatro |
| Dolor, GAIN_MUSCLE intermedio | `547, 998, 553, 739, 1103, 519` | Seis sustitutos elegibles con la dosis 4D exacta; ninguno de los anteriores |
| Fatiga con descanso, GENERAL_FITNESS | descansos 90 s | descansos 120–180 s, mismas series y repeticiones |

### 5.2 Rechazos (y lo que hizo la IA en el reintento)

| Regla | Propuesta rechazada | Reintento |
|---|---|---|
| V8C | Sustituir un empuje horizontal por una sentadilla en «demasiado difícil» (otro patrón) | Corrigió al mismo patrón |
| V8C | Añadir `1097` y `1154` sin retirar nada | Los quitó |
| V8C | Bajar la carga 18 → 16,5 kg en `INVESTIGAR_CAMBIO_MINIMO`, que no autoriza tocarla | Mantuvo la carga |
| V8C | Añadir un ejercicio en `REVISAR_CARGA`, que no permite sustituir | Lo quitó |
| V24 | GAIN_MUSCLE con 75 s de descanso cuando 4D exige 3x10 / 90 s | Ajustó a 90 s |
| V24 | Un ejercicio de estabilidad por duración en hipertrofia | Lo cambió por uno resistido |
| V23 | Semana inicial sin tracción superior | Añadió el patrón |
| V20 / V17 | Sesión por debajo del 70 % del tiempo, o minutos declarados que no cuadran | Corrigió (los rechazos más frecuentes: 44 de V17) |
| V11 | Mantener 20 kg sin peso anotado | **No lo corrigió en 3 intentos** (§4.3) |

## 6. IA simulada frente a IA real

| Aspecto | Simulada (`EvaluacionLimpiaProgresionIT`, 72 perfiles) | Real |
|---|---|---|
| Qué propone | Solo lo que permite cada intervención (−2 repeticiones o −1 serie; retirar el último relleno) | Cambios más amplios: toca descansos y duraciones, reduce varias dosis a la vez, sustituye sesiones enteras, añade ejercicios, cambia cargas |
| Intentos | Siempre válida al primero | 44 % al primero, 41 % al segundo, 15 % al tercero |
| Rechazos | Casi ninguno | 101 en la campaña 1 (88 en adaptación y 13 en generación); los más frecuentes, V17 (minutos declarados) y V20 (duración mínima) |
| Subidas de dosis | Nunca | 16 ciclos con la configuración de producción (§4.1) |
| Semanas sin plan | 0 de 720 | 4 de 97 (§4.2 y §4.3) |
| Llamadas en ciclos deterministas | 0 | 0 (las dos campañas) |
| Progresión (backend) | 312 / 14 / 0 en 720 semanas | 54 / 6 / 0 en 120 semanas, todas dentro de los límites |
| Latencia | Instantánea | Media 40 s, máxima 173 s; 1 tiempo agotado |

La simulación sirvió para el diseño y la regresión, pero **no podía encontrar los defectos de §4**: todos nacen de propuestas que la IA simulada nunca hacía.

## 7. Recomendaciones y decisiones pendientes

1. **Autorizar §4.1** (la comprobación de subida, siempre activa). Es la más importante: hoy, con la configuración de producción, la IA puede subir dosis justo después de una omisión por fatiga.
2. **Autorizar §4.2** (mínimo de series en ejercicios de duración) y **§4.3** (respaldo sin carga sugerida cuando V11 la prohíbe). Las dos evitan semanas sin plan.
3. **Decidir §4.4**: si un cambio solo de minutos es `SIN_EFECTO`, y si ante dolor se limitan los añadidos.
4. Después, **repetir la campaña 1** con los mismos perfiles para comprobar las correcciones (≈ US$ 3).
5. Mantener el aviso del proxy en el despliegue (`prefer-wait-seconds`).

No se ha cambiado ninguna regla ni ningún validador. V24, V25 y V31–V34 siguen intactos, sin migraciones nuevas. La progresión sigue apagada en producción y no se ha desplegado nada.

**Límite de esta evidencia:** demuestra que FitSense genera y adapta rutinas con respuestas reales de la IA dentro de sus reglas (y dónde no). No demuestra que mejore la adherencia de personas reales; eso requiere la evaluación con participantes.

---

# Segunda parte · correcciones y campaña de verificación

## 8. Las cinco correcciones

Cada una se reprodujo **antes** de corregirla: la prueba falla con el código anterior y pasa con el nuevo. La comprobación se hizo dos veces: en las pruebas unitarias y en `CorreccionesReplicateApagadoIT`, ejecutado contra el código de producción anterior.

| | Problema | Corrección | Dónde | Reproducción con el código anterior |
|---|---|---|---|---|
| C1 | La IA subía dosis con los interruptores apagados | La comprobación de subida corre **siempre**; solo la emisión de dosis autorizadas para V24 sigue detrás de `autorizarDosis`. Además, una sesión con una subida rechazada ya no figura como aplicada en el informe (fallo menor que salió al probar el interruptor encendido) | `ConservationValidator.revisar` / `revisarDosisDeLaIa` | Los 16 pares reales se aceptaban (16/16). En el ciclo, `APLICADA` con la subida publicada |
| C2 | `DURATION` con 1 serie: excepción SQL | V9 exige ≥ 2 series también en los ejercicios por duración **que declaran series** (lo que exige `ck_pwe_min_sets`); sin series sigue siendo válido | `PlanDraftValidator.validatePrescription` | Excepción al guardar, semana sin plan y **sin evidencia V32** |
| C3 | Carga sugerida sin peso anotado: semana sin plan | La copia y el respaldo retiran a `null` **solo** las cargas que rechaza V11 (con la propia V11, `PlanDraftValidator.problemasDeCarga`) y revalidan la semana. Queda registrado en `conservation_report.correcciones_del_backend`. No se aplica cuando hay un paso de progresión, porque V8C exige la identidad; la copia sin paso que se intenta después sí lo hace | `AdaptedWeekProducer.continuidad` | `NO_FACTIBLE` tras 3 intentos |
| C4 | `APLICADA` solo por minutos | Una **señalada** que vuelve con los mismos ejercicios, series, repeticiones, segundos y descansos conserva la duración validada de la semana medida y queda `SIN_EFECTO` (anotado en `correcciones_del_backend`). Si cambia el contenido, la duración la sigue juzgando V17 contra el estimador. Las no señaladas siguen bajo V8C, que rechaza cambiarles los minutos | `AdaptedWeekProducer.minutosSegunContenido` | `APLICADA` con `applied_sessions` no vacío |
| C5 | Ante dolor se añadía trabajo | Con `SEGURIDAD_ANTE_DOLOR`: no puede haber más añadidos que retirados, y el volumen de la sesión (18.1, con el divisor de la configuración, el mismo de V8) no puede subir. El patrón sigue sin exigirse, y la elegibilidad (V1), la carga (`revisarCarga`) y el tiempo (V4/V17/V20) siguen igual. La dificultad conserva su regla propia | `ConservationValidator.revisarSustitucion`; divisor desde `StrictPlanValidation.divisorDeVolumen` | El rechazo por «añadir trabajo» no existía |

Sin migraciones (`correcciones_del_backend` es una clave nueva del JSONB de V32). V24, V25 y V31–V34 sin cambios. La progresión sigue apagada en producción.

## 9. Pruebas añadidas

| Prueba | Qué cubre |
|---|---|
| `DosisIaV8CTest` (40) | Los 16 incrementos reales, con `autorizarDosis` en false y en true; controles: bajar series o segundos sigue aceptado; las autorizaciones para V24 solo con el interruptor; una sustitución por dificultad no se compara con el ejercicio retirado |
| `SeriesMinimasV9Test` (5) | Los dos casos reales (`1x30 s`, `1x120 s`); 2 o más series válido; sin series válido; V10 aparte; SETS_REPS como siempre |
| `AdaptedWeekProducerTest` (+6) | C4: solo minutos → `SIN_EFECTO` con la duración validada (en tres intervenciones); menos series → `APLICADA`; menos repeticiones → `APLICADA`; menos descanso con permiso → `APLICADA`; sin permiso → rechazado; la copia no toca minutos |
| `DolorSinMasTrabajoV8CTest` (6) | Retira 3 y añade 4 → rechazada; 3 por 3 con más volumen → rechazada; 3 por 3 con el mismo volumen → aceptada; 3 por 2 → aceptada; un sustituto por duración medido con el divisor; la dificultad sin cambios. Sin alternativa válida → `NO_FACTIBLE` con motivo (`AdaptedWeekProducerTest`, «Fallo de IA con dolor señalado») |
| `CorreccionesReplicate{Apagado,Encendido}IT` (5 + 5) | El ciclo completo con publicación y evidencia, en los dos estados del interruptor. La IA simulada repite en los 3 intentos la propuesta defectuosa real. C1 → `RESPALDO` sin subida; C2 → rechazo V9 y publicación con evidencia; C3 → la carga se conserva con peso anotado y se retira a `null` sin él; C4 → `SIN_EFECTO` con la duración validada; C5 → `NO_FACTIBLE` trazable, sin publicar más trabajo |

**Regresión:**
- 787 unitarias y 211 de integración, 0 fallos (8 omitidas, todas opcionales).
- `GeneracionReferenciaIT`: **72/72 idénticos**.
- `EvaluacionLimpiaProgresionIT` (10 semanas, 72 perfiles): informe **idéntico byte a byte** al anterior.
- `CicloConcurrenteIT` 9/9; `CopiaSinIaIT` 5/5; `PendientesProgresionIT` 6/6; `ProgresionIT` 17/17.

## 10. Campaña de verificación con Replicate real

Los mismos 12 perfiles y el mismo guion de §2, con la configuración de producción (progresión apagada).

**Una ejecución descartada.** El primer intento se detuvo por la regla de parada: cinco fallos seguidos del proveedor. Fueron `429 Too Many Requests`: con **menos de US$ 5 de saldo**, Replicate reduce la creación de predicciones a 6 por minuto con ráfaga de 1, y la campaña lanzaba 4 perfiles a la vez. Sus resultados están contaminados por la limitación y no se usan. Se repitió con un perfil cada vez (`-Dfitsense.replicate.hilos=1`): 88 min, 138 llamadas, **0 errores del proveedor**.

### 10.1 Objetivos de la orden

| Objetivo | Antes | Después |
|---|---:|---:|
| Aumentos de dosis no autorizados publicados | 16 ciclos | **0** |
| Excepciones de persistencia por el mínimo de series | 2 | **0** (V9 rechazó 6 propuestas de 1 serie antes de guardar; la IA las corrigió o se usó el respaldo) |
| Semanas perdidas por V11 | 2 | **0** |
| `APLICADA` solo por minutos | 3 (dos campañas) | **0** |
| Ante dolor, más ejercicios o más volumen publicados | 4 de 8 adaptaciones por dolor con más volumen (+21 % a +63 %: 68→111, 126→188, 100→136, 140→170) | **0 de 4** |
| Violaciones de las comprobaciones independientes | 18 | **0** |

### 10.2 Antes y después, por objetivo

| Objetivo | Ciclos (antes → después) | Llamadas | APLICADA | SIN_EFECTO | RESPALDO | NO_FACTIBLE | Dosis subidas publicadas |
|---|---|---|---|---|---|---|---|
| LOSE_WEIGHT | 25 → 20 | 33 → 31 | 15 → 9 | 0 → 0 | 1 → 2 | 1 → 3 | 7 → **0** |
| GAIN_MUSCLE | 25 → 24 | 29 → 29 | 10 → 8 | 6 → 6 | 0 → 1 | 1 → 1 | 0 → 0 |
| INCREASE_STRENGTH | 27 → 24 | 36 → 37 | 18 → 13 | 0 → 0 | 0 → 2 | 0 → 1 | 6 → **0** |
| GENERAL_FITNESS | 20 → 22 | 34 → 41 | 12 → 9 | 0 → 1 | 1 → 3 | 0 → 3 | 3 → **0** |

Las llamadas incluyen la generación inicial. Hay menos `APLICADA` porque las adaptaciones que solo «funcionaban» subiendo dosis, añadiendo trabajo ante dolor o cambiando minutos ahora se reintentan, pasan al respaldo o quedan `SIN_EFECTO`. Es el comportamiento pedido: la métrica ya no cuenta como adaptación lo que no lo era.

**Por intervención (después):**

| Intervención | Ciclos | APLICADA | SIN_EFECTO | RESPALDO | NO_FACTIBLE | Llamadas |
|---|---:|---:|---:|---:|---:|---:|
| `REVISAR_CARGA_Y_RECUPERACION` | 24 | 15 | 4 | 5 | 0 | 41 |
| `ACORTAR_O_REORGANIZAR` | 12 | 10 | 1 | 1 | 0 | 19 |
| `VARIANTE_MAS_SENCILLA_O_CARGA` | 12 | 7 | 0 | 0 | 5 (§11.1) | 25 |
| `INVESTIGAR_CAMBIO_MINIMO` | 7 | 3 | 2 | 2 | 0 | 18 |
| `SEGURIDAD_ANTE_DOLOR` | 7 | 4 | 0 | 0 | 3 (§11.2) | 18 |

**Rechazos por regla en los ciclos (intentos que se reintentaron):** V8C 58, V17 34, V20 27, V11 7, V9 6, V24 1, V15 1, V16 1; y en la generación inicial, V24 5 y V23 1. Ejemplos de V8C nuevos, todos de la IA real:
- «la sesión 24 está señalada por dolor y su volumen sube de 70 a 128»;
- «… sube la dosis del ejercicio …»;
- «la sesión 264 está señalada por dolor y su volumen sube de 91 a 141».

La generación inicial no cambia: 11 de 12 por IA y 1 por reglas, las 12 válidas con V24+V25.

## 11. Semanas sin plan después de las correcciones (8): causas

### 11.1 D8 no heredaba la exención tras una señalada sin cambios (5 casos) — **aprobado y corregido (§13)**

En los cinco el patrón es idéntico:
1. s4: `ACORTAR` aplicado deja el viernes bajo el mínimo de V20, exento por D8.
2. s5: la copia hereda la exención.
3. s6: el viernes se señala con `REVISAR_CARGA_Y_RECUPERACION` (carga y fatiga), la IA no da una propuesta válida y el **respaldo publica la sesión idéntica**.
4. s7: la copia de ese viernes **ya no hereda** la exención, así que V20 la rechaza (p. ej. 34 min con mínimo de 42). Ni la IA ni el respaldo pueden tocar esa sesión, que no está señalada, y la semana sale `NO_FACTIBLE`.

**Causa:** `ReduccionAutorizadaResolver` corta la herencia ante cualquier sesión `SIN_EFECTO` cuya intervención no sea `ACORTAR`, aunque la sesión publicada sea idéntica. Así está escrito D8 («una intervención nueva la interrumpe»). Antes de C1 casi no se veía, porque en s6 la IA «resolvía» subiendo dosis; ahora s6 acaba en respaldo, que es lo correcto, y el hueco aparece.

**Propuesta:** que una sesión `SIN_EFECTO` o conservada por el respaldo, **idéntica** a la que tenía la exención, no interrumpa la herencia. Aprobada el 27/10/2026 con condiciones; implementada y verificada en §13.

### 11.2 Dolor sin propuesta válida: `NO_FACTIBLE` por diseño (3 casos)

La IA real propuso sustitutos con mucho más volumen (70→128, 56→118, 91→141), y C5 los rechazó. En el reintento chocó con V17/V20 o con «siguen ejercicios de la sesión que dolió». Sin respuesta usable, el respaldo **no vuelve a publicar el ejercicio que dolió** y deja la semana sin plan, con el motivo en V32. Es el comportamiento seguro y trazable que pide la orden; no se publica una rutina potencialmente problemática para tener plan. En los otros 4 casos de dolor la IA encontró una sustitución válida.

## 12. Consumo

| Tramo | Predicciones | Tokens (entrada / salida) | Coste estimado |
|---|---:|---:|---:|
| Primera parte (pilotos y campañas 1 y 2) | 218 | 10,19 M / 0,93 M | US$ 4,41 |
| Verificación: ejecución limitada (descartada) | 65 | 2,17 M / 0,27 M | US$ 1,09 |
| Verificación: ejecución válida | 138 | 6,58 M / 0,59 M | US$ 2,82 |
| **Total de la sesión** (consulta a Replicate) | **422** | 18,97 M / 1,80 M | **≈ US$ 8,34** |

Tarifa pública de GPT-5 mini (0,25 / 2,00 US$ por millón); la cifra oficial está en el panel de Replicate. **El saldo de la cuenta está por debajo de US$ 5**: antes de otra campaña conviene recargarlo, o seguir con un solo hilo.

Evidencia: `evidencia/replicate_real/despues/` (cada llamada con la propuesta original, los rechazos, el plan antes y después, V32, latencia y tokens). No contiene secretos.

No se ha desplegado nada ni se ha activado la progresión.

---

# Tercera parte · herencia de D8 (aprobada el 27/10/2026)

## 13. La corrección y su verificación

### 13.1 Regla implementada

Solo en `ReduccionAutorizadaResolver` (más su prueba). El recorrido del linaje hacia atrás (V31 + V32) ya no se corta en un paso `SIN_EFECTO` de una intervención distinta de `ACORTAR` si se cumplen **todas** estas condiciones:

| Condición | Cómo se comprueba |
|---|---|
| Existe una autorización D8 válida en el origen | El recorrido tiene que llegar a un `ACORTAR_O_REORGANIZAR` aplicado en un intento `APLICADA`, como antes. Sin él, no hay exención |
| Linaje continuo desde el acortamiento | Cada paso con su único intento V32 y su sesión de origen. Si falta cualquiera, no hay exención |
| La sesión publicada es **idéntica** a la anterior | Ejercicios en el mismo orden, tipo, series, repeticiones, segundos, descanso, carga, enfoque y duración. Se ignoran identificadores, fechas y estado de ejecución |
| Si la intervención cambió la prescripción, no se hereda | Cualquier diferencia de las anteriores (incluida una carga retirada por V11) corta la herencia |
| Otra sesión que cambia no afecta | La clasificación es por sesión de origen |
| El dolor no aprovecha D8 | Una intervención con revisión de seguridad (`SEGURIDAD_ANTE_DOLOR`) nunca hereda. Además V8C sigue rechazando conservar el ejercicio que dolió, con o sin exención |

Un `RESPALDO` registra la señalada como `SIN_EFECTO` en V32, así que el mismo caso cubre los dos. Sin cambios en V20 (la exención sigue siendo solo D8), V24 ni V25, ni en la generación inicial. Sin migraciones.

**Fuera del alcance (sin cambios):** dentro de la misma semana, una sesión **señalada** que la IA devuelve idéntica no recibe la exención en V8C (solo la reciben las copias no señaladas). V20 rechaza la propuesta y el respaldo publica la misma sesión. El resultado es el mismo plan, registrado como `RESPALDO` en lugar de `SIN_EFECTO`.

### 13.2 Pruebas

| Prueba | Qué cubre | Con el código anterior |
|---|---|---|
| `ReduccionAutorizadaResolverTest` (9) | Positivas: ACORTAR → copia → fatiga con respaldo idéntico → copia (hereda); igual con `SIN_EFECTO`; otra sesión del mismo intento aplicada (hereda). Negativas: sin D8 original; cambia una serie; cambia la carga (incluida la retirada por V11) o el descanso; otro ejercicio, otro orden u otra duración; linaje roto (sin intento o sin sesión de origen); intervención por dolor | Las 3 positivas fallan; las 6 negativas pasan |
| `HerenciaD8IT` (3), ciclo completo, progresión apagada | El escenario exacto de los cinco casos: s2 `ACORTAR` (viernes bajo el mínimo), s3 copia, s4 fatiga en el viernes con la IA subiendo una serie o devolviéndolo igual (→ `RESPALDO` idéntico), s5 dificultad en el miércoles (la copia del viernes tiene que seguir exenta), s6 copia. Y el dolor: el viernes acortado que dolió, conservado tal cual, no se publica (`NO_FACTIBLE` con motivo) | Las dos positivas fallan en s5 con `NO_FACTIBLE` por V20, el mismo fallo de la campaña real |

**Regresión:**
- 796 unitarias y 214 de integración, 0 fallos.
- `GeneracionReferenciaIT`: **72/72 idénticos**.
- `EvaluacionLimpiaProgresionIT`: informe **idéntico**.
- `LinajeIntegridadIT` pasa, incluido su caso F: sin la evidencia V32 del acortamiento no hay exención y V20 rechaza la copia.
- `CicloConcurrenteIT` 9/9; `CorreccionesReplicate{Apagado,Encendido}IT` 5/5 + 5/5.

### 13.3 Campaña con Replicate real (mismos 12 perfiles y guion, progresión apagada, un solo hilo)

119 min, 166 llamadas, 1 error transitorio del proveedor, **0 violaciones** de las comprobaciones independientes. Siguen en 0 las subidas de dosis, las excepciones de persistencia, las semanas perdidas por V11, las `APLICADA` solo por minutos y el trabajo añadido ante dolor.

| | Antes de D8 (§10) | Después de D8 |
|---|---:|---:|
| `NO_FACTIBLE` en la semana de dificultad (s7) | **5** | **0** |
| `NO_FACTIBLE` por dolor sin propuesta válida (s9) | 3 | 6 |
| Perfiles que completan el guion hasta s9 | 7 de 12 | 12 de 12 |
| `VARIANTE_MAS_SENCILLA_O_CARGA` | 7 APLICADA, 5 NO_FACTIBLE | 11 APLICADA, 1 RESPALDO, 0 NO_FACTIBLE |

Los `NO_FACTIBLE` por dolor suben porque **más perfiles llegan a la semana 9**: antes, 5 se quedaban en la 7. Hay 12 ciclos de dolor (antes 7): 6 `APLICADA` y 6 `NO_FACTIBLE`. En los 6 la IA no dio una sustitución válida en 3 intentos (más volumen, o V17/V20), y el respaldo no vuelve a publicar el ejercicio que dolió. Es el comportamiento seguro de §11.2.

**Por objetivo (ciclos semanales):**

| Objetivo | Ciclos (antes → después) | APLICADA | SIN_EFECTO | RESPALDO | NO_FACTIBLE (s7 / s9) |
|---|---|---|---|---|---|
| LOSE_WEIGHT | 20 → 24 | 9 → 11 | 0 → 0 | 2 → 4 | 2/1 → **0**/3 |
| GAIN_MUSCLE | 24 → 26 | 8 → 8 | 6 → 6 | 1 → 3 | 1/0 → **0**/1 |
| INCREASE_STRENGTH | 24 → 27 | 13 → 15 | 0 → 0 | 2 → 3 | 1/0 → **0**/0 |
| GENERAL_FITNESS | 22 → 25 | 9 → 11 | 1 → 1 | 3 → 4 | 1/2 → **0**/2 |

### 13.4 La excepción solo se conserva cuando corresponde

- **Los 6 perfiles con el patrón** (s4 `ACORTAR` aplicado, s6 señalada en el viernes que acabó en `RESPALDO` o `SIN_EFECTO`): en los seis, el viernes publicado en s6 es **idéntico** al de la semana anterior en ejercicios, orden, series, repeticiones o segundos, descansos, carga y minutos (p. ej. `24 min [53:2x60s/30s, 1176:2x60s/30s, 896:2x6r/90s@20kg, …]` → idéntico). En los seis s7 se publicó.
- **Los 5 perfiles en que s6 sí cambió el viernes** (`APLICADA`): el viernes nuevo quedó en el mínimo de V20 o por encima (21→23, 24→26, 30→31, 42→43, 22→24 min, con mínimos de 21, 31 y 42), y se publicó por la regla ordinaria, sin necesitar la exención. Para el caso contrario (una sesión modificada que queda bajo el mínimo) **la campaña real no dio ningún ejemplo**. Lo cubren `ReduccionAutorizadaResolverTest` (cambia la dosis, la carga, el descanso, el contenido o la duración → no hereda) y `LinajeIntegridadIT` (sin evidencia, no hay exención).
- **Dolor:** ninguna sesión con dolor se publicó apoyándose en D8 (los 6 `NO_FACTIBLE` de s9 tienen el motivo «Sin respuesta de la IA no se puede resolver el dolor…»).

### 13.5 Consumo

Esta parte: 178 predicciones, incluida una ejecución de la campaña que se perdió al reiniciarse el contenedor (≈ 12 predicciones, ≈ US$ 0,14). La ejecución válida costó ≈ US$ 3,59.

**Total de la sesión (consulta a Replicate): 600 predicciones, 27,57 M tokens de entrada y 2,61 M de salida, ≈ US$ 12,11** con la tarifa pública de GPT-5 mini. La cifra oficial está en el panel de Replicate, y el saldo seguía por debajo de US$ 5 (por eso la campaña corre con un solo hilo).

Evidencia: `evidencia/replicate_real/despues_d8/`. Progresión apagada, Neon sin cambios, sin migraciones, nada desplegado.

