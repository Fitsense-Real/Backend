# Pruebas con Replicate real · informe

**Fecha:** 27/10/2026 · **Modelo:** `openai/gpt-5-structured`, variante `gpt-5-mini`, razonamiento `medium` · máximo de **3 intentos** por adaptación y por generación (el del sistema, sin cambios).
**Base:** PostgreSQL efímera (Testcontainers) con participantes sintéticos. **Neon sin tocar.** Progresión encendida **solo** en el contexto aislado de la segunda campaña; en producción sigue apagada.
**Arnés:** `CampanaReplicateRealIT` (opt-in: `-Dfitsense.replicate.real=true`). Evidencia completa (propuesta original de cada llamada, rechazos por regla, plan antes y después, evidencia V32, latencias y métricas): `evidencia/replicate_real/{adaptacion,progresion}/`.

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

### 4.1 GRAVE · En producción, la IA puede subir dosis en una sesión señalada

- **Qué pasó:** en la campaña 1, **16 ciclos publicaron dosis más altas** que las de la semana medida, todos como `APLICADA`:
  - 8 en `REVISAR_CARGA_Y_RECUPERACION`, es decir, **después de una omisión por fatiga**;
  - 4 en `INVESTIGAR_CAMBIO_MINIMO`;
  - 3 en `VARIANTE_MAS_SENCILLA_O_CARGA`;
  - 1 en `ACORTAR`.
- **Ejemplos:** `3x6 → 4x6` en dos ejercicios (fuerza, intermedio); `2x6 → 3x6` en siete ejercicios de una sesión; `3x6/150 s → 2x8/180 s` (sube repeticiones aunque baja series); `2x12 → 4x12`.
- **Causa:** la regla existe (`ConservationValidator.revisarDosisDeLaIa`: «La IA no puede subir dosis: solo el backend progresa»), pero **solo se ejecuta con `autorizarDosis`**. Ese indicador vale `progresion || validaCapacidades`, y en producción los dos interruptores están apagados. Se diseñó junto con las dosis autorizadas para V24, y la comprobación de subida quedó dentro del mismo `if`.
- **Por qué no salió antes:** la IA simulada nunca subía dosis. En GAIN_MUSCLE no ocurre porque su V24 (4D) fija la dosis exacta. En la campaña 2 (interruptor encendido), 0 casos.
- **Propuesta (pendiente de tu autorización, porque cambia el comportamiento publicado):** ejecutar siempre la comprobación de subida y dejar solo la emisión de dosis autorizadas para V24 detrás de `autorizarDosis`. Es aplicar una regla ya aprobada, no relajarla. Efecto esperado: esos 16 ciclos pasarían a reintento y, si la IA insiste, a `RESPALDO`.

### 4.2 V9 no comprueba el mínimo de series en ejercicios de duración

- **Qué pasó:** en 2 perfiles de GENERAL_FITNESS la IA propuso un ejercicio de duración con **1 serie** (`1150: 1x30 s`; `1325: 1x120 s`). Pasó todos los validadores y **la base lo rechazó al guardar** (`ck_pwe_min_sets`: series ≥ 2 si no son nulas). El ciclo terminó con excepción: **semana sin plan y sin evidencia V32**, después de 2–3 llamadas pagadas.
- **Causa:** `PlanDraftValidator.validatePrescription` aplica el mínimo de series (V9) solo a `SETS_REPS`; para `DURATION` solo comprueba los segundos (V10).
- **Propuesta:** aplicar el mínimo de series también a los ejercicios de duración cuando declaran series, para que el validador diga lo que ya exige la base y el reintento de la IA pueda corregirlo. Pendiente de autorización.

### 4.3 V11 deja sin plan una semana con carga sugerida y sesión omitida

- **Qué pasó:** 2 perfiles acabaron en `NO_FACTIBLE` (semana sin plan). El ejercicio tenía 20 kg sugeridos y la participante **omitió esa sesión** (motivación), así que no anotó peso. V11 exige entonces quitar la carga.
  - La IA la mantuvo en los 3 intentos.
  - El **respaldo determinista también la mantiene** (conserva la sesión tal cual), así que falla igual.
- **Matiz:** la carga de partida la puso el arnés (como en `PendientesProgresionIT`), pero la situación es real: una persona que anota peso y después falta a esa sesión.
- **Propuesta:** que el respaldo conserve la sesión **sin la carga sugerida** cuando V11 lo exige. Pendiente de autorización.

### 4.4 Observaciones sin regla vigente que las prohíba

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
