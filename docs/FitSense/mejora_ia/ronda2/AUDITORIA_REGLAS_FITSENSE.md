# Auditoría de reglas de FitSense (MEJ ronda 2, fase 1)

**Alcance:** generación inicial (cuatro objetivos), validadores V1–V27, adaptación con V8C, progresión, recuperación y reincorporación, y los prompts PR-1.7, PR-1.8, V2.1 y V2.2.

**Modo:** gratuita y de solo lectura sobre el commit `42d9dc1`, con la política `PLAN-REV-2.0`. No se pagó ninguna predicción.

**Método:**
- Dos auditorías independientes en paralelo, una de generación y otra de adaptación. Sus informes completos, con fragmentos de código, están en `evidencia/AUDITORIA_GENERACION_detalle.md` y `evidencia/AUDITORIA_ADAPTACION_detalle.md`.
- Cada hallazgo que condiciona el experimento se comprobó de nuevo contra el código. La columna «Comprobado» lo indica.
- Regresión completa (unitarias e integración). Resultado en §8.

**Qué no se hizo, por orden expresa:** no se corrigió ninguna regla ni validador, ni se cambió política, catálogo o estructura. Los defectos se documentan aparte (§7).

## 1. Respuesta corta

| Pregunta | Respuesta |
|---|---|
| ¿El backend es correcto? | **Sí en lo que decide la publicación.** Solo se publica lo que pasa V1–V27 (generación) o V8C + V1–V27 (adaptación). V24 no se relaja nunca en la generación inicial. Progresión, recuperación y reincorporación cumplen sus límites. Hay **dos huecos de V8C** en adaptación (H-A1, H-A2): aceptan algo que la política no autoriza. Se documentan, y el experimento los vigila con una comprobación independiente |
| ¿Dónde está el problema de la IA? | **Sobre todo en lo que recibe Replicate**, no en cómo lo interpreta. PR-1.7 no da los números de V15 (`max_per_body_part` llega siempre `null` con REV2), ni los de V26 ni V27. Da dos límites de duración distintos (115 % y 100 %) y reglas genéricas de dosis que chocan con las dosis exactas de V24 en IS, LW y GF. La interpretación (O10, O18) es secundaria |
| ¿Se puede pasar a la fase pagada? | **Sí.** Ningún hallazgo invalida la medición: los validadores reales siguen siendo la autoridad, y los dos huecos de V8C se vigilan por identidad en `analisis_mej_r2.py`, con el mismo criterio que el C10 de BASE. Ver §9 |

## 2. Generación inicial por objetivo

Común a los cuatro objetivos:
- **División:** depende de los días, no del objetivo.
- **Frecuencia:** `min(días, 4 BEGINNER / 6 resto)`.
- **Minutos:** 30–75.
- **V4 al 100 %:** segundos estimados ≤ M×60.
- **Sin piso V20.**
- **V26:** techos de 6/10 series por grupo y sesión; 12/20/24 por semana.
- **V27:** una sesión PULL lleva tracción si la hay.
- **V15:** al menos 2 grupos prescribibles, y la mitad cuando hay ≥ 3 grupos y ≥ 4 ejercicios de trabajo.
- **Restricciones** (equipamiento, lugar, seguridad, bloqueos, dolor): se aplican en el conjunto elegible y las comprueba V1.

| | GAIN_MUSCLE | INCREASE_STRENGTH | LOSE_WEIGHT | GENERAL_FITNESS |
|---|---|---|---|---|
| Capacidades | HYPERTROPHY (régimen 4D) | STRENGTH; secundarias HYPERTROPHY y POWER (no se prescribe) | CARDIO; secundaria STRENGTH | STRENGTH, ME, CARDIO, MOBILITY; secundarias HYPERTROPHY y BALANCE |
| Dosis | **Fija por clase:** estructural 3×10/90, zona pequeña 3×13, relleno 2×10 o 2×13. Serie adicional siempre rechazada con REV2 | Dosis exacta de capacidad: STRENGTH 3×6/150 (BEGINNER 2×8/120), HYPERTROPHY 3×10/90; series hasta `max(series,3)` | CARDIO 3×120 s/60 (BEGINNER 3×90); STRENGTH estructural 3×6/150 | Tabla de capacidades; ME admite isométricos |
| Tope de colocaciones/día | 6–9 según minutos + presupuesto 4D del 85 % | 8 | 8 | 8 |
| V23 (slots 4C) | Sí, solo a la IA | Sí, solo a la IA; V25 añade STRENGTH por slot | **No** | Sí, solo a la IA |
| V25 | HYPERTROPHY semanal | STRENGTH semanal + slots con STRENGTH | CARDIO semanal **y en cada sesión** donde sea factible | Cada principal factible al menos una vez en la semana |
| Volumen orientativo (`CalidadDosis`, solo traza y aviso DOSIS_LIMITADA) | 6/10/12 series por grupo grande (mitad en zonas pequeñas) | 4 por grupo grande | 4 + 45/150 min aeróbicos | 4 + 30/60 min aeróbicos |
| Qué recibe Replicate | `dosis_por_clase`, `clase_dosis`, `slot`, `slots_exigibles`, `max_colocaciones_dia`, `minutos_utiles_dia`, costes por clase | `capacidades` (dosis codificadas), `slot`, `slots_exigibles`, `reglaDeFuerza` | `capacidades`, «dos bloques de CARDIO por sesión» | `capacidades`, `slot`, `slots_exigibles` (sin texto que los explique) |

### Restricciones que existen solo en el prompt (no se crean validadores)

| Restricción | Dónde | Qué exige el backend |
|---|---|---|
| «Cada principal en CADA sesión» (`reglaPrioritaria`, `comprobacionFinal`) | PR-1.7, para IS, LW y GF | Semanal, salvo el CARDIO de LW, que es por sesión. Contradice además a `BLOQUE_CAPACIDADES` («al menos una vez en la semana») |
| «Dos bloques de CARDIO por sesión» (LW) | PR-1.7 | Un bloque (V25). El segundo es preferencia del motor de reglas |
| «Referencia: 4 por sesión» (IS) | PR-1.7 | No se verifica |
| Regla 4: «no superar session_minutes + 15 %» | PR-1.7 y FIX-PR-1.3 (regla 11) | V4 al 100 % en segundos. El mismo prompt da después la cifra correcta |
| Regla 16 («parte de target_reps/target_sets; no pongas la misma prescripción a todos»), regla 14 («menos series, más descanso») y principios 2, 3, 5 y 6 | PR-1.7 | En IS, LW y GF, V24 exige **exactamente** una dosis codificada; `target_sets` = 4 en IS choca con el máximo de 3 |
| Regla 13 de adaptación («la carga sube como mucho un 10 %») | FIX-PR-1.3 | V8C rechaza cualquier subida de carga |
| «El descanso no se cambia nunca» | FIX-PR-1.3 | V8C no lo comprueba (H-A2) |
| Orden «empieza por estructurales», RIR, ejercicio prioritario al inicio | PR-1.7 | No se verifican (observación de calidad) |

## 3. Validadores V1–V27

**Orden de ejecución:** V1/V6/V7/V2/V3/V12/V13 → V15 → V23 → V4/V5/V7/V9/V10 → V8 → V11 → V16/V17 → V18 → V22 → V19 → V20/V21 → V24 (GM o capacidades) → V26 → V27 → V25. Se devuelven todos los problemas a la vez.

| Validador | Resultado | Comprobado |
|---|---|---|
| **V15** | El validador es correcto: cuenta sobre los ejercicios reales y coincide con el motor de reglas. **`max_per_body_part` = null es un problema del prompt y del snapshot:** se deriva de `min_exercises_per_session`, que sale del piso V20 (= 0 con REV2), así que llega siempre null. PR-1.7 remite a ese campo nulo y PR-1.8 lo sustituye por un texto correcto | Sí (`PlanInputSnapshot:681-686`, `PlanDraftValidator:1540`) |
| **V23** | GM, IS y GF sí; LW no (`exigeSlotsResistidos`). Solo bloquea a la IA (`ENFORCE_AI_ONLY`) y mira el patrón, no la dosis. En GF ningún texto del prompt explica los slots (H-G5). GF intermedio o avanzado con 1 sesión de 30–35 min: V23 + V25 + V4 no caben (2139–2265 s frente a 1800 s, cálculo a mano). La IA se rechaza siempre y publica el motor de reglas. **Ningún perfil de la cohorte O está en ese caso** (O08 es GF de 30 min, pero con 7 días) | Cohorte comprobada |
| **V24** | **No se relaja en la generación inicial para ningún objetivo:** el validador de 3 argumentos pasa `ConservationReport.vacio()`, así que no hay ninguna `DosisAutorizada`. P-A1, progresión, reducción y herencia solo las emite V8C sobre una semana derivada. En GM la serie adicional se rechaza siempre con REV2. **Ningún prompt puede subir las series fijadas por una clase sin que V24 lo rechace.** Observación H-G8: fuera de GM, V24 acepta hasta 3 series también en principiantes, cuando su justificación (llegar al mínimo de V20) ya no existe en REV2. Es coherente con `series_max`, que viaja al modelo | Sí (`PlanDraftValidator:128-130, 599-633`) |
| **V25** | Correcto por objetivo (tabla §2). La factibilidad no tiene en cuenta el tiempo (ver V23 en GF) | — |
| **V26** | Correcto. Los techos son iguales para todos los objetivos y solo cuentan ESTRUCTURAL y RESISTIDO, sin complementos. **Relación con V15:** en GM BEGINNER (3×10 fijo, techo 6) caben como máximo 2 estructurales del mismo grupo por sesión, así que V26 es más estricto que la mitad de V15. **Con V24:** no se contradicen hasta hacer imposible un plan, porque REV2 no tiene piso de duración y quitar colocaciones siempre está permitido. H-G16 (menor): se relaja siempre que haya semana previa con datos, no solo en semanas derivadas, lo que incluye P-03 | — |
| **V27** | Correcto. No se comunica al modelo en ninguna versión del prompt (H-G4) | — |
| **Duración** | V4 correcto. V17 es redundante: el normalizador sustituye la duración declarada por la estimada antes de validar (H-G14). V20 está desactivado con REV2 | — |
| **Frecuencia y estructura** | V2, V3, V12, V13 y V18 correctos. El enfoque que declara la IA no tiene por qué coincidir con `suggested_split` (observación) | — |
| **Restricciones** | V1 y V6 sobre el conjunto elegible; V24 rechaza los elegibles que no se ofrecieron al modelo | — |
| **Repetidos** | Ningún validador rechaza el mismo `exercise_id` dos veces en una sesión. El motor de reglas lo evita. Es una observación: no se crea ninguna regla | — |

**Redundancias:** V25-HYPERTROPHY con V24 en GM; V17 con el normalizador. **Contradicciones que hacen imposible un plan:** ninguna en la cohorte. **Rechazos que faltan:** ninguno en la generación. En adaptación, ver H-A1 y H-A2.

## 4. Adaptación

**Flujo:** ejecución → motivo por sesión (el dolor manda) → evidencia (cobertura ≥ 50 %) → decisión (primero el dolor; localizada o ampliada; NO_CHANGE e INSUFFICIENT_DATA conservan) → intervención por causa → prompt → V8C + V1–V27 → hasta 3 intentos → respaldo. Solo se publica lo que pasa ambas validaciones.

| Intervención | Sustituir | Retirar | Dosis | Estado |
|---|---|---|---|---|
| ACORTAR (falta de tiempo) | No | **1 relleno** sin slot, con el resto intacto | Bajar | Correcto. Tope M correcto en V8C |
| VARIANTE_MAS_SENCILLA (dificultad) | Mismo patrón (multiconjunto), sin subir impacto | **No autorizado, pero V8C lo acepta (H-A1)** | Bajar | **Hueco** |
| SEGURIDAD_ANTE_DOLOR | Cualquier patrón, añadidos ≤ retirados, volumen 18.1 que no sube | Sí | Bajar; la carga no | Correcto técnicamente; pendiente de revisión profesional |
| REVISAR_CARGA (fatiga) | No | No | Bajar; en GM solo P-A1 (3×10 → 2×10) | Correcto. Fuera de GM la reducción no tiene cota propia (observación) |
| INVESTIGAR (motivación) | P-A2: 1 lateral estricta | No | Bajar | Correcto. Varias bajadas aceptadas (observación) |
| REPROGRAMAR | No | No | No | Solo el día |

**Casos pedidos:**
- **O18 (ACORTAR, GM):** retirar un relleno está permitido (`permiteQuitarEjercicio`, y V8C exige relleno, que esté en el catálogo y que sea uno solo, sin otros cambios). V2 y V2.1 lo contradecían: «No sustituyas ni retires», «D: ninguna», «OMITELA». V2.2 y v2.2a lo ofrecen con los mismos criterios que V8C. Queda una incoherencia residual, el tope M (H-A3); la ronda 2 la corrige **solo en v2.2a y v2.4**, y V2.2 conserva su texto.
- **O12 (dolor sin ejercicio identificado):**
  - **Técnicamente permitido hoy:** se excluyen todos los ejercicios de la sesión, sin caducidad. Se pueden reemplazar por elegibles de cualquier patrón, en igual o menor número y sin subir el volumen 18.1; la dificultad, solo dentro de V6.
  - **Pendiente de revisión profesional (declarado en el código):** la exclusión de todos es «provisional»; el límite de dificultad está apagado; la retirada localizada está apagada; y la vía IA (volumen 18.1) y P-03 (repeticiones y segundos por separado) miden «no más trabajo» de forma distinta.
  - No se añade ninguna restricción.
- **O04:** el oráculo empareja por posición cuando las dos listas tienen la misma longitud. Antes `[119, 520, 1054, 1309, 555]`, después `[119, 128, 315, 1054, 1309]`. Los cambios reales son 520→128 y 555→315, todos ELBOW_FLEXION y de menor dificultad, y V8C los acepta. Los tres «sustituto de otro patrón» son un **artefacto del instrumento**. No se modifica el oráculo.

## 5. Progresión, recuperación y reincorporación

| Regla | Estado |
|---|---|
| Dos semanas al 100 % real antes de progresar (`UMBRAL_PROGRESION = 100`) | Correcto |
| Aumento gradual: +1 repetición, +1 serie solo con las repeticiones al máximo, paso ≤ 5 % | Correcto |
| Techo acumulado 1,10 × V_base del linaje | Correcto |
| Como mucho un paso por ciclo (`pasoEstrecho`, un solo `PasoProgresion`) | Correcto |
| Recuperación ≤ 10 %, hacia la referencia sin superarla | Correcto |
| Reincorporación solo tras una retirada ACORTAR aplicada, nunca la retirada por dolor | Correcto |
| Progresión antes de reincorporar (`pasosTrasRetirada == 0 → mantener`) | Correcto |
| Sin restauración automática: la exclusión por dolor no caduca y NO_CHANGE no progresa | Correcto |
| **Las autorizaciones de adaptación y progresión no relajan V24 en la generación inicial** | **Correcto** |

No se modifica ninguna política.

## 6. Prompts: qué recibe Replicate

| Contenido | PR-1.7 | PR-1.8 (MEJ) | V2.1 | V2.2 (MEJ) |
|---|---|---|---|---|
| Datos (snapshot) | Completo | Igual | Semana medida + snapshot | Igual |
| V15 (la mitad) | **No** (campo null) | Sí | — | — |
| V26 (techos por grupo) | **No** | Sí | — | — |
| V27 | No | No | — | — |
| Dosis orientativa de `CalidadDosis` | No | Sí, ambigua: en GM empujó a subir series (1 propuesta, 13 V24) | — | — |
| Duración | 115 % (regla 4) **y** M×60 (DURACIÓN CALCULADA) | Igual | Regla 11 (+15 %) y techo M×60 | Igual |
| Costes de dosis | Solo GM (por clase) | Solo GM | — | — |
| Factibilidad A–F por sesión | — | — | Sí; en GM+ACORTAR «ninguna opción, omítela» | Ofrece retirar un relleno (sin el tope M) |
| Pertinencia por causa | — | — | Sí | Sí + línea de dolor (no cambia nada medible) |

**Conclusión:**
- **Generación:** hay que cambiar lo que recibe la IA. Faltan los números de V15, V26 y V27, la duración tiene dos límites y las reglas genéricas de dosis contradicen a V24.
- **Adaptación:** V2.1 y V2.2 ya describen bien lo que V8C acepta, salvo el detalle del tope M. La diferencia que queda es de interpretación: pertinencia y qué cambio elige.
- **Lo que el código y las pruebas locales ya responden:** V24 no se relaja, V15 está bien, O04 es un artefacto y O10 cumple la política. No se paga ninguna corrida para ello.

## 7. Defectos de reglas (documentados aparte, sin corregir)

| Id | Severidad | Defecto | Efecto en el experimento |
|---|---|---|---|
| H-A1 | Media | V8C acepta en VARIANTE_MAS_SENCILLA propuestas que solo retiran (`ConservationValidator:1247`, `if (d.anadidos().isEmpty()) return;`), aunque sean varias o estructurales; la política solo autoriza retirar en ACORTAR. Ninguna prueba lo cubre | Un cambio así se publicaría. **Se vigila por identidad** en todas las adaptaciones de la ronda 2 (`analisis_mej_r2.py`, `RETIRADA_NO_AUTORIZADA`), con el mismo criterio que el C10 de BASE («VARIANTE no permite sustituir ni retirar») |
| H-A2 | Media | Con permiso de dosis, V8C acepta un cambio solo del descanso. Fuera de GM, V24 por capacidades está apagada en la adaptación; el prompt lo prohíbe | Igual (`DESCANSO_CAMBIADO`) |
| H-G11 | Media (trazabilidad) | Con una variante de prompt, `input_snapshot.prompt_version` y la etiqueta de la petición dicen «PR-1.7» | La ronda 2 registra la variante en la carpeta, el `config` del arnés y el volcado del prompt |
| H-G12 | Baja | La pista de reintento de V24 cita `clase_dosis` (GM) en todos los objetivos | — |
| H-G9 | Alta (factibilidad, cálculo) | GF intermedio o avanzado con 1×30–35 min: la IA no tiene solución y publica el motor de reglas | Fuera de la cohorte |
| H-G10 | Media (condicional al catálogo) | V23, V27 y `sinTraccionReal` cuentan como «exigibles» ejercicios no prescribibles | No verificado con datos; ningún rechazo de MEJ se debió a eso |
| H-G16 / H-A6 | Baja | La relajación de V26 depende de que haya semana previa, no de la vía de generación | — |
| H-G17, H-G18 | Baja | Tope de colocaciones de GM derivado de V20; comentarios desfasados | — |

## 8. Regresión

Ver `DIAGNOSTICO_GENERACION_ADAPTACION.md` §4: recuento real y pruebas que dependen de la fecha.

## 9. Decisión

**No hay defectos que invaliden el experimento.** La medición usa los validadores reales, y los huecos H-A1/H-A2 se vigilan por identidad con `analisis_mej_r2.py`, independiente de V8C. Se continúa con la fase 2 dentro de USD 10, con variantes que solo cambian lo que recibe la IA:
- **G1 (pr-1.8a):** V15 + V26.
- **G2 (pr-1.8b):** dosis orientativa sin tocar la dosis.
- **G3 (pr-1.8c):** estructura del objetivo + coste de cada dosis + tiempo útil.
- **A1 (v2.2a):** solo la corrección de ACORTAR.
- **A2 (v2.4):** resumen del caso.

Ninguna añade reglas ni autorizaciones.
