# Comparación funcional C1 frente a C2

**Fuentes:**
- `14_MATRIZ_CALIDAD_POR_ADAPTACION.csv`, con las 12 preguntas de la fase 10:
  - las 32 sesiones adaptadas con Replicate real en C1 r1, C1 r2, C2 r1 y C2 r2 (PR #29), en las primeras filas;
  - las 22 sesiones adaptadas en la cohorte R (RC1_r1 y RC2_r1, dos semanas), añadidas al final (§4);
- `02`/`03`: sustituciones;
- `06`/`07`: continuidad y combinaciones, sin red.

**Las secciones 1–3 se escribieron antes de la fase con Replicate** y se apoyan en las corridas del PR #29. **La fase con Replicate sobre R01–R08 se ejecutó después de tu aprobación** (opción A): sus resultados están en el §4, en `12` y en `13`.

**Tres niveles, separados (fase 10):**
- **Validez técnica:** se cumplen las reglas implementadas.
- **Coherencia funcional:** la modificación es compatible con el propósito de la intervención, según el catálogo y las reglas documentadas.
- **Adecuación profesional:** **no evaluada**. Ninguna prueba de software la sustituye.

## 1. Resumen

| | C1 (r1 + r2) | C2 (r1 + r2) |
|---|---:|---:|
| Sesiones adaptadas | 12 | 20 |
| **Técnicamente válidas** | **12 (100 %)** | **20 (100 %)** |
| Coherentes según el catálogo y la regla | 7 (58 %) | 12 (60 %) |
| Funcionalmente discutibles (Q2/Q3) | 5 (42 %) | 8 (40 %) |
| …con cambio de músculo principal (sesiones; no excluyentes) | 2 | 3 (4 sustituciones) |
| …VARIANTE con alguna sustitución de dificultad igual (sesiones) | 4 | 4 |
| …VARIANTE resuelta solo bajando la dosis (sesiones) | 1 | **3** |
| Sesiones con margen 0 tras la adaptación | 2 | 5 |
| Fuera de alcance, dosis subida, descanso cambiado, bajo el mínimo, linaje | 0 | 0 |

**Lectura:**
- **C2 aplica más** (20 sesiones frente a 12) **sin peor proporción de cambios discutibles** (40 % frente a 42 %).
- **Los problemas funcionales son los mismos en las dos condiciones**, porque vienen de la regla (V8C por patrón) y del catálogo (`03`), no del prompt.
- **Dos diferencias de estilo de C2:**
  - **(a) Ante TOO_DIFFICULT, C2 baja la dosis** (una serie menos de cardio, −10 s) en lugar de sustituir: U14 S2 en r1 y U19 S2 en r1 y r2. En C1 r1 también ocurre, una vez (U19 S2). El objetivo VARIANTE de V2 dice «variantes más sencillas **y/o algo menos de dosis**», y la intervención («o ajustar la carga») lo permite. Pero **no simplifica la técnica**: es Q5.
  - **(b) C2 lleva más sesiones exactamente al mínimo V20**: 5 frente a 2 (`06`).

## 2. Las 12 preguntas, en conjunto

| # | Pregunta | C1 | C2 |
|---|---|---|---|
| 1 | ¿Aplicado? | 12 de 12 (APLICADA y publicado) | 20 de 20 |
| 2 | ¿Responde al motivo? | Fatiga e INVESTIGAR: sí (reducen dosis). VARIANTE: 5 de 6 parcial (dificultad igual o solo dosis) | Fatiga e INVESTIGAR: sí. VARIANTE: 7 de 8 parcial |
| 3 | ¿Patrón conservado? | Sí, en todas | Sí, en todas |
| 4 | ¿Músculo conservado? | No en 2 sustituciones (580 → 313 y 933 → 318) | No en 4 (580 → 129, 775 → 313 y 780 → 119 ×2) |
| 5 | ¿Dificultad? | Nunca sube | Nunca sube |
| 6 | ¿Dosis respetada? | Sí: nada sube, descanso igual | Sí |
| 7 | ¿Equipamiento? | Dentro del catálogo filtrado del perfil | Ídem |
| 8 | ¿Duración válida? | Sí, ≥ mínimo | Sí, ≥ mínimo |
| 9 | ¿Resto del entrenamiento conservado? | Sí | Sí |
| 10 | ¿Combinación válida? | Sí (las combinaciones inválidas se rechazaron antes) | Sí (0 rechazos) |
| 11 | ¿Continuidad? | Válida. 2 sesiones en margen 0: en la siguiente semana se conservan (`06`) | Válida. 5 sesiones en margen 0: ídem |
| 12 | ¿Revisión profesional? | Sí: 5 sesiones (y muestra del resto) | Sí: 8 sesiones (y muestra del resto) |

## 3. Estabilidad en escenarios nuevos (sin Replicate)

Con la cohorte R01–R08 y el guion V2 (`R_GUION_2S`):
- La **descripción de V2** fue correcta en todos los perfiles nuevos:
  - márgenes de 0, 6, 30, 60, 102, 151, 201 y 228 s;
  - minutos 30–75;
  - casa o gimnasio.

  Lo que V2 llama válido se aceptó; lo que llama «NO cabe» se rechazó por V20; y donde dice «sin solución» la sesión se conservó.
- Esto prueba que **la factibilidad es correcta** en perfiles nuevos. **No prueba** cómo responderá el modelo real a esos perfiles: eso es el §4.

## 4. Cohorte R01–R08 con Replicate real (RC1_r1 frente a RC2_r1)

**Diseño:** opción A aprobada. Una corrida por condición, dos semanas, mismos perfiles, base efímera nueva y una única diferencia, `prompt=v1` o `v2` (`09`).

**Es una sola corrida por condición: las diferencias son indicios, no estimaciones.**

### 4.1 Resultado técnico

| | C1 (RC1_r1) | C2 (RC2_r1) |
|---|---:|---:|
| Operaciones con IA (semana 1 + semana 2) | 6 + 7 = 13 | 6 + 7 = 13 |
| …**sin ninguna opción válida** según la factibilidad (R04 sem. 1, margen 6 s; R07 sem. 2, margen 3 s) | 2 | 2 |
| **Aplicadas (V32 APLICADA)**, semana 1 | **5 de 6** | **5 de 6** |
| **Aplicadas**, semana 2 | **3 de 7** | **6 de 7** |
| Aplicadas sobre las factibles (11) | 8 (73 %) | **11 (100 %)** |
| SIN_EFECTO | 5: las 2 sin opción y 3 factibles perdidas (R05, R06, R08 en la semana 2; H14) | 2: solo las 2 sin opción, omitidas por el modelo en el primer intento |
| Predicciones | 22 | **13** (exactamente 1 por operación) |
| Rechazos del validador | **9, todos V20** | **0** |
| Operaciones que necesitaron más de un intento | 8 de 13 | 0 de 13 |
| Coste | US$ 0,3295 | **US$ 0,1749** |
| Violaciones de seguridad (fuera de alcance, dosis subida, descanso, bajo el mínimo, linaje) | **0** | **0** |
| Publicaciones inválidas | 0 | 0 |
| Sesiones llevadas al mínimo V20 por la adaptación | 0 | 1 (R06 S3, 1320 → 1260 s) |
| Sesiones ya generadas en el mínimo y adaptadas solo con sustitutos de la misma dosis | R05 S2 (sem. 1) | R05 S2 (sem. 1 y 2) |

**Lectura técnica:**
- **Lo que V2 promete, se cumple en perfiles nuevos y con el modelo real:**
  - 0 rechazos;
  - una llamada por operación;
  - omite exactamente las operaciones sin solución y aplica todas las factibles.
- **C1 repite el patrón del PR #29:**
  - retira demasiado, V20 lo rechaza;
  - en la semana 2 el segundo intento se rinde aunque había margen (R08: retiró 735 s con un margen de 201 s).

### 4.2 Calidad funcional (matriz, filas RC)

| | C1 (9 sesiones adaptadas) | C2 (13 sesiones adaptadas) |
|---|---:|---:|
| Técnicamente válidas | 9 (100 %) | 13 (100 %) |
| Coherentes según el catálogo y la regla | 2 (22 %) | 4 (31 %) |
| Funcionalmente discutibles (Q2/Q3) | 7 (78 %) | 9 (69 %) |
| …con cambio de músculo principal (sesiones / sustituciones) | 6 / 9 | 5 / 6 |
| …VARIANTE con alguna sustitución de dificultad igual (sesiones) | 7 | 6 |
| …VARIANTE resuelta solo bajando la dosis (sesiones) | 0 | **3** (R03 sem. 1 y 2, R08 sem. 2) |
| Sustituciones / con dificultad menor / igual / mayor | 29 / 3 / 26 / 0 | 37 / 2 / 35 / 0 |
| Patrón conservado | 29 de 29 | 37 de 37 |
| Sesiones que en la semana 2 vuelven a sustituir lo introducido en la semana 1 | 2 (R01: 3 ejercicios; R03: 1) | 2 (R01: 6 de 6; R05: 8 de 8) |
| FATIGUE e INVESTIGAR: reducen la dosis sin sustituir | 2 de 2 | 4 de 4 |

**Lectura funcional:**
- **La proporción de adaptaciones discutibles es alta en las dos condiciones** (78 % y 69 %), más que en el PR #29 (42 % y 40 %).
- **No viene del prompt, sino de las sesiones de esta cohorte.** En las sesiones TOO_DIFFICULT de la cohorte R:
  - **60 de las 66 sustituciones parten de un ejercicio de dificultad 1**, la mínima del catálogo: no hay variante más sencilla;
  - en GM, 4D no admite reducir la dosis (R01);
  - o la sesión se generó sin margen (R05).

  Con esas restricciones, **las dos condiciones resuelven TOO_DIFFICULT con un cambio lateral de 6 a 8 ejercicios**, y lo repiten la semana siguiente (H12).
- **C2 no es más agresivo:** ninguna sustitución sube la dificultad o la dosis, y no cambió ningún descanso.
  - Aplica más porque recupera las 3 operaciones factibles que C1 perdió, no porque cambie más por operación.
  - **Diferencia de estilo de C2 (H15):** baja una repetición en lugar de sustituir (3 sesiones). Lleva una sesión de fatiga al mínimo exacto (R06).
- **Cambios de músculo:** 9 en C1 y 6 en C2. 4 de esos 15 vienen del 862 (abdominales clasificado como VERTICAL_PULL, H13): 2 en cada condición.

**Conclusión del §4:**
- **Técnicamente, V2 es mejor que V1 en perfiles nuevos:** más adaptaciones factibles aplicadas (11/11 frente a 8/11), 0 rechazos, el 59 % de las predicciones y el 53 % del coste.
- **En calidad funcional no hay mejora atribuible al prompt.** Los cambios discutibles vienen de la regla (V8C por patrón), del catálogo y de la falta de variantes más sencillas, y afectan igual a C1 y a C2.
- **Más adaptaciones aplicadas no significa mejores adaptaciones:** la ventaja de V2 es de fiabilidad y de coste, no de calidad.
