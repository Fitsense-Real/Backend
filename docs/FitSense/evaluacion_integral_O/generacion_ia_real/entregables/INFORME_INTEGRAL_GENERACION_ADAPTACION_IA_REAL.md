# Informe integral · Generación inicial y adaptación con Replicate REAL (GRA-R1 y GRA-R2)

| Elemento | Valor |
|---|---|
| Orden | «FitSense: generación inicial y adaptación completa con Replicate real» (06/11/2026) |
| Código de producción | Versión A `f1c6c02`, **sin cambios**: V46, PLAN-REV-2.0, P15-C apagado, aviso E2-a |
| Generación inicial con IA | Flujo **existente** del backend (`PlanGenerationPipeline` + `ReplicateTrainingPlanGenerator`, prompt PR-1.7). Hasta 3 propuestas, validadas con el mismo `PlanDraftValidator` (V1–V27, incluidas V24 y V25) que el motor de reglas. El respaldo por reglas solo actúa si ninguna es válida, y queda identificado como `RULE_ENGINE`. Ver `../00_ARQUITECTURA_Y_DISENO.md` |
| Adaptación con IA | Prompt V2.1 + validadores + V8C, como en R_O y R′_O |
| Modelo | Replicate `openai/gpt-5-structured`, `gpt-5-mini`, razonamiento `medium`. Versión `9f4cd9ec…` **antes de cada bloque y después del último**, sin cambios (`../evidencia/versiones.txt`) |
| Congelación | `b60e5ea`, **antes** de la primera llamada pagada (`../01_CONGELACION_Y_PRESUPUESTO.md`) |
| Corridas | GRA-R1 y GRA-R2, independientes, en bases efímeras limpias, del 06/11/2026 de 05:13 a 06:44 UTC. **Ningún plan, respuesta ni adaptación se reutilizó** de R_O o R′_O |
| Coste | **76 predicciones, USD 1,5368**. Global acumulado: 96 de 168 predicciones y USD 1,84 de 8,40 |
| Oráculo | v1.4 (v1.3 + R-2: origen real de la generación) |
| Respuestas | **Todas de Replicate real.** Ninguna simulada ni precargada. 0 respaldos |
| Adherencia | **Simulada por guion** (O01–O20 congelados). Los resultados no se atribuyen a personas |

**Qué se distingue en todo el informe:**
- **[GEN-IA]** generación inicial con IA real;
- **[ADA-IA]** adaptación con IA real;
- **[DET]** lógica determinista del backend (decisiones, adherencia, progresión, validación);
- **[SIM]** adherencia simulada;
- **[RR]** pendiente de validación deportiva.

## 1. Resultado principal

| | GRA-R1 | GRA-R2 |
|---|---|---|
| **[GEN-IA] Planes iniciales publicados desde la IA** | **20/20 factibles** (19 de la cohorte + O11-F35) | **20/20** |
| [GEN-IA] Válidos al primer intento | 13/20 | 14/20 |
| [GEN-IA] Propuestas rechazadas por el backend (todas resueltas en un reintento) | 10 | 6 |
| [GEN-IA] Planes por respaldo determinista | **0** | **0** |
| O11 (30 min) | `FUERZA_SIN_TIEMPO`, **sin llamada a la IA** (inviabilidad esperada) | Igual |
| **[ADA-IA] Adaptaciones publicadas desde la IA** | **10/10** | **9/10** (1 SIN_EFECTO evitable) |
| [ADA-IA] Propuestas rechazadas por los validadores | 0 | 0 |
| [ADA-IA] Pertinencia (ACC/ALT/CONS-J/INC) | 11/0/1/0 | 9/1/2/0 |
| **[DET] Decisión del backend = decisión recalculada** | **112/112 ciclos** | **124/124** |
| **Diez aspectos** (20 perfiles; aplicables) | **150/150 = 100 %**, 0 FALLO, 44 NA, 6 RR | **148/148 = 100 %**, 0 FALLO, 45 NA, 7 RR |
| O11-F35 (aparte) | Plan de la IA válido. Aspectos AP o NA | Igual |

**No se encontró ningún defecto del sistema y no se ha modificado nada después de ver resultados** (`INCIDENCIAS_Y_CORRECCIONES.md`).

## 2. Tabla 1 · Perfiles

Los 20 perfiles congelados están en `TABLA1_PERFILES.csv`, sin cambios:
- objetivos 5/5/5/5;
- niveles 7 principiante, 7 intermedio y 6 avanzado;
- 9 gimnasio, 6 casa sin equipo y 5 casa con mancuernas y bandas;
- 1–7 días, 30–75 min, restricciones y edades de 62 y 67 años.

O11-F35 es O11 con 35 minutos, informado aparte.

## 3. Tabla 2 · [GEN-IA] Generación inicial con Replicate

| | GRA-R1 | GRA-R2 |
|---|---:|---:|
| Perfiles con llamada (factibles) | 20 | 20 |
| Llamadas de generación | 30 | 26 |
| Válidas al primer intento | 13 | 14 |
| Necesitaron 2 intentos / 3 intentos | 4 / 3 | 6 / 0 |
| Propuestas rechazadas | 10 | 6 |
| Motivos (código del validador) | V15×6, V26×6, V23×2, V24×1, V4×1 | V26×5, V15×4, V13×1, V18×1 |
| Errores del proveedor | 0 | 0 |
| **Publicados desde la IA / por respaldo / sin plan** | **20 / 0 / 1 (O11)** | **20 / 0 / 1 (O11)** |
| Restricciones (A2) | 20/20 | 20/20 |
| Validez técnica del plan (A1) | 14 AP + 6 RR (catálogo) | 13 AP + 7 RR |
| Estado de dosis del plan inicial | 17 DOSIS_LIMITADA, 3 DOSIS_OBJETIVO | 17 DOSIS_LIMITADA, 3 DOSIS_OBJETIVO |
| Latencia mediana (máx.) por llamada | 75,5 s (145,2) | 66,6 s (148,0) |
| Coste de generación | USD 0,7031 | USD 0,5659 |

**Lectura de los motivos de rechazo:**
- V15: demasiados ejercicios de un mismo grupo en una sesión.
- V26: techo semanal de series por grupo.
- V23: los cuatro patrones 4C.
- V24: dosis por capacidad o clase.
- V4: duración.
- V13 / V18: estructura y cardio.

El backend rechazó cada propuesta con el motivo exacto y la reenvió para corregirla. **Ninguna propuesta inválida se publicó, y V24 no se relajó.**

**Generación con IA frente a determinista** (mismos 19 perfiles; R_O usó reglas):

| | Determinista (R_O) | IA (GRA-R1) | IA (GRA-R2) |
|---|---|---|---|
| Sesiones | 64 | 64 | 64 |
| Ejercicios por sesión (mediana, rango) | 5,5 (3–8) | 5 (3–6) | 5 (3–6) |
| Minutos declarados (mediana) | 33 | 30 | 30 |
| DOSIS_OBJETIVO / DOSIS_LIMITADA | 10 / 9 | 3 / 16 | 3 / 16 |
| Planes con observación de catálogo (A1 RR) | 14 | 6 | 7 |

**Lectura:**
- La IA genera planes válidos, algo más cortos y **más conservadores en dosis semanal**: DOSIS_LIMITADA en 16 de 19. El backend lo avisa en `/plan/limitations`.
- Usa menos ejercicios con observaciones de catálogo.

## 4. Tabla 3 · [SIM]+[DET] Ejecución y adherencia

| | GRA-R1 | GRA-R2 |
|---|---|---|
| Sesiones S1–S4 (19 perfiles) | 256: 239 completas, 6 parciales, 10 omitidas, 1 sin registro | Igual (mismos guiones) |
| Adherencias recalculadas frente a las guardadas | Iguales en todas las semanas | Iguales |
| Decisiones C1–C4 | 63 NO_CHANGE, 11 ADAPT_LOCALIZED, 2 ADAPT_EXPANDED | Igual |
| **Decisión del backend = decisión del oráculo** | **112/112** (incluidos horizontes) | **124/124** |
| V32 C1–C4 | 63 SIN_ORDEN, 10 APLICADA, 3 SIN_EFECTO | 63 SIN_ORDEN, 9 APLICADA, 4 SIN_EFECTO |

Las señales (fatiga, dolor, falta de tiempo, dificultad, desmotivación y causa no registrada) y las decisiones las detecta y toma el backend sobre los planes **nuevos** de la IA. Coinciden con las de las evaluaciones anteriores porque los guiones son los mismos. El detalle semanal por perfil está en `RESULTADOS_ADHERENCIA.csv`.

## 5. Tabla 4 · [ADA-IA] Adaptación con Replicate

| | GRA-R1 | GRA-R2 |
|---|---:|---:|
| Ciclos con orden de IA | 10 | 10 |
| Aceptadas al primer intento | 10 | 9 |
| Rechazadas por los validadores | 0 | 0 |
| SIN_EFECTO propuesto por la IA | 0 | 1 (O18, **evitable**) |
| Respaldo determinista | 0 | 0 |
| **Publicadas desde la IA** | **10/10** | **9/10** |
| Pertinencia ACC / ALT / CONS-J / INC | 11 / 0 / 1 / 0 | 9 / 1 / 2 / 0 |
| Latencia mediana (máx.) | 31,9 s (44,3) | 28,7 s (47,7) |
| Coste de adaptación | USD 0,1367 | USD 0,1312 |

**Lo que cumplen todas las adaptaciones publicadas** (oráculo, sobre el plan publicado):
- Sin más trabajo ni dificultad, y sustitutos del mismo patrón y grupo (fuera del dolor).
- Ante dolor, la regla aprobada de su rama, y ningún ejercicio de la sesión con dolor se mantiene (O12).
- El ejercicio excluido por dolor no vuelve: O06 (755) y O20 (709 / 1156).
- Las sesiones no señaladas quedan idénticas.
- Techo y prohibidos respetados.
- Se publican solo propuestas juzgadas publicables (R-1).
- **Linaje:** la adaptación parte del plan generado en la misma corrida. A9 aprobado en 19/19.

**Mantenimiento** (O02, O10 y O16): conservaciones sin IA, idénticas.

## 6. Tabla 5 · [DET] Progresión, recuperación y reincorporación

| | GRA-R1 | GRA-R2 |
|---|---|---|
| PROGRESION | 26 | 30 |
| RECUPERACION | 3: O03 (632), O14 (253), O17 (299) | 3: O03 (548), O14 (253), O17 (1315) |
| REINCORPORACION | 1: **O18**, 502 retirado en C1 y reincorporado en C14 | 0 |
| Techo 1,10 y ≤ 1 paso por ciclo; ninguna PROGRESION sin dos semanas al 100 % | Sí | Sí |

**Detalle:**
- **O03:** no hubo recuperación bloqueada; las dos corridas recuperaron en C3 dentro del 10 %.
- **Reincorporación en GRA-R2:** no aplica. En O18 la IA no retiró (SIN_EFECTO evitable) y en O19 redujo dosis en lugar de retirar. **No se forzó ninguna retirada.**

## 7. Tabla 6 · Diez aspectos

| Aspecto (AP/FA/NA/RR) | GRA-R1 | GRA-R2 |
|---|---|---|
| A1 Generación inicial | 14/0/0/6 | 13/0/0/7 |
| A2 Personalización y restricciones | 20/0/0/0 | 20/0/0/0 |
| A3 Registro | 19/0/1/0 | 19/0/1/0 |
| A4 Adherencia | 19/0/1/0 | 19/0/1/0 |
| A5 Adaptación con IA | 10/0/10/0 | 10/0/10/0 |
| A6 Mantenimiento | 19/0/1/0 | 19/0/1/0 |
| A7 Progresión y recuperación | 8/0/12/0 | 8/0/12/0 |
| A8 Reincorporación | 3/0/17/0 | 2/0/18/0 |
| A9 Continuidad | 19/0/1/0 | 19/0/1/0 |
| A10 Seguridad e integridad | 19/0/1/0 | 19/0/1/0 |
| **Total** | **150 / 0 / 44 / 6 → 100 %** | **148 / 0 / 45 / 7 → 100 %** |

**Verificaciones explícitas de la orden §13:**
- **La generación provino de Replicate:** `generation_source = AI` en 20/20 planes por corrida. El oráculo (R-2) comprobó una propuesta válida para cada uno.
- **El respaldo no cuenta como IA:** no hubo ninguno.
- **Sin ejercicios ni dosis fuera del contrato:** V1 (`available_exercises`) y V24 en el validador, más A1, A2 y A10 en el oráculo.
- **La adaptación usa el plan de la misma corrida:** linaje, A9.
- **El modo experimental apagado reproduce la generación original:** huellas S_O `7eb13353` y D_O `d46b3f50`, y las 5 REG idénticas.

NO APLICA y REQUIERE REVISIÓN **no** cuentan como aprobados.

## 8. Tabla 7 · Reproducibilidad GRA-R1 frente a GRA-R2

| | Resultado |
|---|---|
| [GEN-IA] Mismo origen (IA) | 20/20 |
| [GEN-IA] Mismo número de sesiones | 20/20 (las fija el backend) |
| [GEN-IA] Plan idéntico | **0/20** |
| [GEN-IA] Similitud de ejercicios (Jaccard), mediana (rango) | **0,36** (0,00–0,65) |
| [ADA-IA] Misma intervención | 10/10 (la decide el backend) |
| [ADA-IA] Misma clasificación del resultado | 9/10 (difiere O18) |
| [ADA-IA] Mismo cambio exacto | **0/10** |

**Lectura:**
- La parte determinista se reproduce exactamente: sesiones, decisiones e intervenciones.
- La IA produce planes y adaptaciones **distintos en cada corrida**, todos válidos.
- La variabilidad es del modelo, no del sistema.

## 9. Tabla 8 · Rendimiento y costes

| Corrida | Etapa | Predicciones | Tokens entrada / salida | Latencia mediana / máx. | USD |
|---|---|---:|---|---|---:|
| GRA-R1 | Generación | 30 | 1.048.225 / 220.508 | 75,5 / 145,2 s | 0,7031 |
| GRA-R1 | Adaptación | 10 | 334.742 / 26.491 | 31,9 / 44,3 s | 0,1367 |
| GRA-R2 | Generación | 26 | 893.012 / 171.309 | 66,6 / 148,0 s | 0,5659 |
| GRA-R2 | Adaptación | 10 | 334.492 / 23.778 | 28,7 / 47,7 s | 0,1312 |
| **Total GRA** | | **76** | | | **1,5368** |

## 10. Resultados por perfil

Abajo van las tablas por perfil. Además:
- el **libro `EVALUACION_20_PERFILES_IA_REAL.xlsx`** tiene una hoja por usuario: perfil, plan inicial con ejercicios y dosis de cada corrida, semanas, adaptaciones, pasos y resultado de los 10 aspectos;
- `SOLICITUDES_Y_RESPUESTAS_REPLICATE.csv` reúne las 76 llamadas con su prompt (fichero y huella) y la respuesta real completa;
- `PREDICCIONES_Y_COSTOS_REPLICATE.csv` reúne identificadores, tokens, latencia y coste.

### Tabla 2 (por perfil)

| ID | GRA-R1: origen · llamadas · rechazos | GRA-R2: origen · llamadas · rechazos | Sesiones | Min. declarados R1 / R2 | Dosis R1 / R2 | A1 R1 / R2 | A2 R1 / R2 |
|---|---|---|---|---|---|---|---|
| O01 | IA · 1 · - | IA · 1 · - | 3 | 28/30/28 / 28/30/28 | DOSIS_OBJETIVO / DOSIS_OBJETIVO | APROBADO / APROBADO | APROBADO / APROBADO |
| O02 | IA · 1 · - | IA · 1 · - | 2 | 27/27 / 27/27 | DOSIS_LIMITADA / DOSIS_LIMITADA | REQUIERE REVISION / REQUIERE REVISION | APROBADO / APROBADO |
| O03 | IA · 3 · V15×2; V24×1; V26×1 | IA · 2 · V15×1; V26×1 | 4 | 39/33/41/25 / 39/36/39/36 | DOSIS_LIMITADA / DOSIS_OBJETIVO | APROBADO / APROBADO | APROBADO / APROBADO |
| O04 | IA · 1 · - | IA · 1 · - | 5 | 34/36/37/36/37 / 31/33/33/31/33 | DOSIS_LIMITADA / DOSIS_LIMITADA | APROBADO / REQUIERE REVISION | APROBADO / APROBADO |
| O05 | IA · 1 · - | IA · 1 · - | 4 | 26/23/26/23 / 24/23/22/23 | DOSIS_LIMITADA / DOSIS_LIMITADA | REQUIERE REVISION / REQUIERE REVISION | APROBADO / APROBADO |
| O06 | IA · 2 · V4×1 | IA · 1 · - | 3 | 29/29/29 / 29/29/29 | DOSIS_LIMITADA / DOSIS_LIMITADA | APROBADO / APROBADO | APROBADO / APROBADO |
| O07 | IA · 3 · V15×2; V26×2 | IA · 2 · V15×1; V26×1 | 4 | 26/19/22/19 / 26/27/26/27 | DOSIS_OBJETIVO / DOSIS_LIMITADA | APROBADO / APROBADO | APROBADO / APROBADO |
| O08 | IA · 1 · - | IA · 1 · - | 4 | 24/25/24/25 / 21/21/21/21 | DOSIS_LIMITADA / DOSIS_LIMITADA | REQUIERE REVISION / REQUIERE REVISION | APROBADO / APROBADO |
| O09 | IA · 1 · - | IA · 1 · - | 6 | 33/31/31/33/33/31 / 36/36/36/36/36/36 | DOSIS_LIMITADA / DOSIS_LIMITADA | APROBADO / APROBADO | APROBADO / APROBADO |
| O10 | IA · 1 · - | IA · 1 · - | 1 | 29 / 29 | DOSIS_LIMITADA / DOSIS_LIMITADA | APROBADO / APROBADO | APROBADO / APROBADO |
| O11 | sin plan · 0 · - | sin plan · 0 · - | - | - / - | - / - | APROBADO / APROBADO | APROBADO / APROBADO |
| O12 | IA · 2 · V23×1 | IA · 1 · - | 2 | 36/38 / 38/36 | DOSIS_LIMITADA / DOSIS_LIMITADA | APROBADO / APROBADO | APROBADO / APROBADO |
| O13 | IA · 3 · V15×1; V26×2 | IA · 2 · V15×1; V26×1 | 3 | 26/26/26 / 23/22/22 | DOSIS_LIMITADA / DOSIS_LIMITADA | REQUIERE REVISION / REQUIERE REVISION | APROBADO / APROBADO |
| O14 | IA · 1 · - | IA · 2 · V13×1; V18×1 | 4 | 30/30/30/30 / 27/27/27/27 | DOSIS_LIMITADA / DOSIS_LIMITADA | REQUIERE REVISION / REQUIERE REVISION | APROBADO / APROBADO |
| O15 | IA · 1 · - | IA · 1 · - | 3 | 30/28/30 / 30/30/30 | DOSIS_LIMITADA / DOSIS_LIMITADA | APROBADO / APROBADO | APROBADO / APROBADO |
| O16 | IA · 2 · V15×1; V26×1 | IA · 2 · V15×1; V26×1 | 4 | 41/36/39/36 / 39/36/39/36 | DOSIS_OBJETIVO / DOSIS_OBJETIVO | APROBADO / APROBADO | APROBADO / APROBADO |
| O17 | IA · 2 · V23×1 | IA · 1 · - | 3 | 25/24/25 / 28/28/28 | DOSIS_LIMITADA / DOSIS_LIMITADA | APROBADO / APROBADO | APROBADO / APROBADO |
| O18 | IA · 1 · - | IA · 1 · - | 3 | 24/24/24 / 30/30/30 | DOSIS_LIMITADA / DOSIS_LIMITADA | APROBADO / APROBADO | APROBADO / APROBADO |
| O19 | IA · 1 · - | IA · 1 · - | 3 | 43/43/43 / 36/36/36 | DOSIS_LIMITADA / DOSIS_LIMITADA | REQUIERE REVISION / REQUIERE REVISION | APROBADO / APROBADO |
| O20 | IA · 1 · - | IA · 2 · V26×1 | 3 | 41/41/39 / 39/39/41 | DOSIS_LIMITADA / DOSIS_LIMITADA | APROBADO / APROBADO | APROBADO / APROBADO |
| O11F35 | IA · 1 · - | IA · 1 · - | 1 | 32 / 32 | DOSIS_LIMITADA / DOSIS_LIMITADA | APROBADO / APROBADO | APROBADO / APROBADO |

### Tabla 4 (por ciclo con orden de IA)

| Corrida | ID | Ciclo | Intervención | Resultado | Cambio publicado | Trabajo | Pertinencia |
|---|---|---|---|---|---|---|---|
| GRA-R1 | O04 | C1 | VARIANTE_MAS_SENCILLA_O_CARGA | ACEPTADA_INTENTO_1 | 47: sus 518->119;520->128 red 673.sets 3->2;1293.sets 3->2;1258.dur 30->20;555.reps 10->9 | 786->538 | ACC |
| GRA-R1 | O05 | C1 | CAUSA_NO_REGISTRADA;INVESTIGAR_CAMBIO_MINIMO | ACEPTADA_INTENTO_1 | 71: sus 1096->494ret - | 72: sus 1264->1103ret - | 74: sus -ret - | 360->360 ; 336->336 ; 336->336 | ACC;ACC;CONS-J |
| GRA-R1 | O06 | C1 | SEGURIDAD_ANTE_DOLOR | ACEPTADA_INTENTO_1 | 91: sus -ret 755 | 774->720 | ACC |
| GRA-R1 | O12 | C1 | SEGURIDAD_ANTE_DOLOR | ACEPTADA_INTENTO_1 | 182: sus -ret 755;296;253;1309;548;352 | 774->252 | ACC |
| GRA-R1 | O14 | C2 | REVISAR_CARGA_Y_RECUPERACION | ACEPTADA_INTENTO_1 | 210: red 253.sets 3->2;1117.sets 3->2 | 684->504 | ACC |
| GRA-R1 | O03 | C1 | REVISAR_CARGA_Y_RECUPERACION | ACEPTADA_INTENTO_1 | 28: red 632.sets 3->2;323.sets 3->2;75.sets 3->2 | 366->312 | ACC |
| GRA-R1 | O18 | C1 | ACORTAR_O_REORGANIZAR | ACEPTADA_INTENTO_1 | 53: sus -ret 502 | 348->270 | ACC |
| GRA-R1 | O19 | C1 | ACORTAR_O_REORGANIZAR | ACEPTADA_INTENTO_1 | 97: red 1175.sets 3->2;853.sets 3->2;1097.sets 3->2;1073.sets 3->2;1112.sets 3->2 | 882->588 | ACC |
| GRA-R1 | O20 | C1 | SEGURIDAD_ANTE_DOLOR | ACEPTADA_INTENTO_1 | 179: sus -ret 709 | 354->294 | ACC |
| GRA-R1 | O17 | C1 | REVISAR_CARGA_Y_RECUPERACION | ACEPTADA_INTENTO_1 | 38: red 299.reps 15->14;1117.sets 3->2;67.dur 30->20;294.reps 13->12 | 546->424 | ACC |
| GRA-R2 | O04 | C1 | VARIANTE_MAS_SENCILLA_O_CARGA | ACEPTADA_INTENTO_1 | 47: sus 1293->253;518->119;520->128;673->713 red 1258.dur 30->20 | 726->484 | ACC |
| GRA-R2 | O05 | C1 | CAUSA_NO_REGISTRADA;INVESTIGAR_CAMBIO_MINIMO | ACEPTADA_INTENTO_1 | 71: sus 1096->494ret - | 72: sus 1264->1263ret - | 74: sus -ret - | 330->330 ; 336->336 ; 336->336 | ACC;ACC;CONS-J |
| GRA-R2 | O06 | C1 | SEGURIDAD_ANTE_DOLOR | ACEPTADA_INTENTO_1 | 91: red 1117.sets 3->2;1054.sets 3->2 ret 755 | 774->480 | ACC |
| GRA-R2 | O12 | C1 | SEGURIDAD_ANTE_DOLOR | ACEPTADA_INTENTO_1 | 182: sus -ret 360;110;532;66;539;515 | 786->372 | ALT |
| GRA-R2 | O14 | C2 | REVISAR_CARGA_Y_RECUPERACION | ACEPTADA_INTENTO_1 | 210: red 253.dur 90->80;1054.dur 90->80 | 636->576 | ACC |
| GRA-R2 | O03 | C1 | REVISAR_CARGA_Y_RECUPERACION | ACEPTADA_INTENTO_1 | 28: red 548.sets 3->2;995.sets 3->2;739.sets 3->2 | 336->282 | ACC |
| GRA-R2 | O18 | C1 | ACORTAR_O_REORGANIZAR | SIN_EFECTO_PROPUESTO_POR_IA | 53: sus -ret - | 438->438 | CONS-J |
| GRA-R2 | O19 | C1 | ACORTAR_O_REORGANIZAR | ACEPTADA_INTENTO_1 | 133: red 853.sets 3->2;775.sets 3->2;1059.sets 3->2 | 828->672 | ACC |
| GRA-R2 | O20 | C1 | SEGURIDAD_ANTE_DOLOR | ACEPTADA_INTENTO_1 | 215: sus -ret 1156 | 384->294 | ACC |
| GRA-R2 | O17 | C1 | REVISAR_CARGA_Y_RECUPERACION | ACEPTADA_INTENTO_1 | 38: red 1315.reps 15->14;1117.sets 3->2;1258.dur 30->20 | 606->490 | ACC |

### Tabla 5 (pasos por perfil)

| ID | GRA-R1: PROGRESION / RECUPERACION / REINCORPORACION | GRA-R2 |
|---|---|---|
| O03 | 0 / 1 / 0 | 0 / 1 / 0 |
| O14 | 0 / 1 / 0 | 0 / 1 / 0 |
| O15 | 3 / 0 / 0 | 3 / 0 / 0 |
| O16 | 1 / 0 / 0 | 1 / 0 / 0 |
| O17 | 1 / 1 / 0 | 1 / 1 / 0 |
| O18 | 11 / 0 / 1 | 15 / 0 / 0 |
| O19 | 4 / 0 / 0 | 4 / 0 / 0 |
| O20 | 6 / 0 / 0 | 6 / 0 / 0 |

### Tabla 6 (por perfil; R1 | R2)

| ID | A1 | A2 | A3 | A4 | A5 | A6 | A7 | A8 | A9 | A10 |
|---|---|---|---|---|---|---|---|---|---|---|
| O01 | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | NA·NA | NA·NA | AP·AP | AP·AP |
| O02 | RR·RR | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | NA·NA | NA·NA | AP·AP | AP·AP |
| O03 | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | AP·AP |
| O04 | AP·RR | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | NA·NA | AP·AP | AP·AP |
| O05 | RR·RR | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | NA·NA | AP·AP | AP·AP |
| O06 | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | AP·AP | AP·AP |
| O07 | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | NA·NA | NA·NA | AP·AP | AP·AP |
| O08 | RR·RR | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | NA·NA | NA·NA | AP·AP | AP·AP |
| O09 | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | NA·NA | NA·NA | AP·AP | AP·AP |
| O10 | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | NA·NA | NA·NA | AP·AP | AP·AP |
| O11 | AP·AP | AP·AP | NA·NA | NA·NA | NA·NA | NA·NA | NA·NA | NA·NA | NA·NA | NA·NA |
| O12 | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | NA·NA | AP·AP | AP·AP |
| O13 | RR·RR | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | NA·NA | NA·NA | AP·AP | AP·AP |
| O14 | RR·RR | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | AP·AP |
| O15 | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | AP·AP | NA·NA | AP·AP | AP·AP |
| O16 | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | AP·AP | NA·NA | AP·AP | AP·AP |
| O17 | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | AP·AP |
| O18 | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·NA | AP·AP | AP·AP |
| O19 | RR·RR | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | AP·AP |
| O20 | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP | AP·AP |
| O11F35 | AP·AP | AP·AP | AP·AP | AP·AP | NA·NA | AP·AP | NA·NA | NA·NA | AP·AP | AP·AP |

## 11. Limitaciones

- **La adherencia es simulada por guion.** La IA es real tanto en la generación como en la adaptación.
- **Muestra pequeña:** 20 generaciones y 10 adaptaciones con IA por corrida. Las proporciones son descriptivas.
- **Variabilidad del modelo:** planes distintos en cada corrida (Jaccard 0,36), un SIN_EFECTO evitable con justificación incorrecta y entre 6 y 10 propuestas de generación rechazadas, todas corregidas en el reintento.
- **Calidad deportiva no validada:**
  - 6–7 planes con observaciones del catálogo (E2, E3, E4/E7);
  - planes de la IA más conservadores en dosis (DOSIS_LIMITADA en 16 de 19);
  - la observación A-2 (patrón nuevo ante dolor sin localizar).
- **La versión del modelo no se puede fijar** sin cambiar el backend; se registró y no cambió.
- **El oráculo es del mismo laboratorio:** v1.4, con pruebas negativas y congelado antes de pagar.
