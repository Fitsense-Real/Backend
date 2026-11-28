# 12 · Fase 5: auditoría de calidad deportiva de REV2

**Análisis completo:** `anexo_fase5/fase5_calidad_analisis.md`. Es una auditoría independiente de solo lectura sobre `d4556b1`; sus scripts y CSV están en `anexo_fase5/`. Las cifras «antes» son de ese análisis y las «después» del barrido REV2 sobre `e2e8152`.

**Regla:** nada se rellena para alcanzar un objetivo. Todas las cifras de dosis, mínimos, techos e impacto siguen siendo **[HP][PRO]**.

## 1. Correcciones técnicas (commit `e2e8152`; generación inicial HIST sin cambio)

| # | Hallazgo (demostrado) | Corrección | Antes → después |
|---|---|---|---|
| C1 | **Artefacto de medida.** FULL_BODY no admite `upper arms` (tabla 20.2), pero `CalidadDosis` exigía series directas de brazos. Eso marcaba DOSIS_LIMITADA en todos los planes de 1–3 días; en 487 era el único motivo | Un grupo que ninguna sesión admite se informa como `grupos_no_evaluables_por_division`. No cuenta como dosis limitada y no cambia ningún plan | DOSIS_LIMITADA **2.385 → 1.898** (DOSIS_OBJETIVO 1.391 → 1.878) |
| C2 | **Dosis invisible.** `DOSIS_LIMITADA` solo vivía en la traza interna | `GET /users/me/plan/limitations` añade `DOSE / DOSIS_LIMITADA`, con los grupos y el aeróbico por debajo de lo orientativo, sin prometer resultados (`PlanLimitationsDosisTest`) | NO CUMPLE → **CORREGIDO** |
| C3 | **Regresión de selección en LOSE_WEIGHT.** El relleno por dosis podía cubrir la pierna dos veces con rodilla y ninguna con cadera | Dentro del grupo, primero el slot 4C que la semana aún no tiene (solo en modo dosis) | LW sin cadera **448 → 117**; sin rodilla **184 → 94** (HIST: 57 y 42) |
| C4 | **Impacto por nombre.** «cuerda» marcaba como MODERADO 23 ejercicios de polea con cuerda | Solo la comba («saltar la cuerda») es impacto (`ImpactoPolicyTest.cuerdaDePolea`) | Elimina los 190 planes de LW principiante y los 45 de GF principiante con impacto «no BAJO» falso |

**Lo que queda de C3:**
- 106 de los 148 planes de LW con un patrón de pierna ausente son de **1 día**: la dosis mínima de pierna (4 series/semana [HP][PRO]) cabe en un solo ejercicio.
- En 63 planes no hay series de pierna.
- Todos están marcados DOSIS_LIMITADA y **visibles para el usuario** con C2.
- Subir la dosis de pierna en LW es una decisión de dosis: **P13, pendiente**.

## 2. Clasificación de DOSIS_LIMITADA (anexo §5.1, sobre los 2.385 de `d4556b1`)

**Por gravedad**, con los mínimos que el anexo deriva de `02` ([HP][PRO]):
- (a) por debajo del objetivo pero sobre el mínimo: **303**;
- (b) por debajo del mínimo: **2.082**.

Fuera de GAIN_MUSCLE, el objetivo del código (4 series) coincide con el mínimo de la evidencia, así que (a) solo aparece en GAIN_MUSCLE y en el aeróbico de LW.

**Por causa dominante:**

| Causa | Planes |
|---|---|
| Catálogo | 620. Todos en casa sin material: el único ejercicio de hombro es 973, un gancho de boxeo |
| Días | 604 |
| Tiempo | 568 |
| Artefacto de brazos | 487 (resuelto con C1) |
| Reglas | 106 |

**Corrección a `04` §2 y §4:** decía «casi siempre catálogo o días». El tiempo y el artefacto pesan casi lo mismo.

**Proporción con limitación real de calidad:** tras C1 quedan 1.898. De ellos, el anexo estima que la parte por debajo del mínimo depende sobre todo de:
- el catálogo de casa sin material (hombro, espalda);
- 1–3 días.

**Estado:** COMPROBADO (medida). La interpretación deportiva sigue PENDIENTE DE VALIDACIÓN.

## 3. Sesiones cortas (anexo §5.2)

**Las 1.063 planificaciones con una sesión < 50 %:**
- En el 96 % o más hay 3–5 ejercicios y 6–12 series.
- 1.009 cubren todos los patrones disponibles.
- **COMPROBADO** en volumen y número de ejercicios.

**La etiqueta `CORTA_RAZONABLE` NO CUMPLE la definición de `02` §2:**
- `02` §2 llama *insuficiente* también a la semana que queda bajo el mínimo sin avisar, o que deja un patrón principal sin cubrir.
- **Con C2 el aviso ya existe.** C3 reduce el problema de patrones.
- La etiqueta sigue midiendo solo ≥ 3 ejercicios y ≥ 6 series.
- Propuesta (P14): renombrarla o ampliar su criterio. No se cambia sin aprobación porque altera la clasificación publicada en `04`.

**IS principiante (~20 min):**
- 672 de 810 sesiones duran menos de 20 min.
- El 28 % de los patrones recibe 2 series de fuerza por semana.
- Coherente con N5, pero por debajo del mínimo de 3 de la tabla de `02`.
- **PENDIENTE DE VALIDACIÓN** profesional.

**J03:**
- Las sesiones LOWER duran ~23 min, con 1 ejercicio de pierna de 2×8.
- Reparto semanal: unas 12 series de empuje frente a 4 de pierna.
- La duración es coherente con la dosis; lo que se debe revisar es **el reparto empuje/pierna**, no los minutos.
- **PENDIENTE.**

**L14:**
- Sesiones de ~19 min.
- Su DOSIS_OBJETIVO es **nominal**: la espalda sale de 775 y el hombro de 973 (ver §4).
- La rodilla la cubren en parte curl femorales aislados.
- **PENDIENTE.**

## 4. Catálogo (anexo §5.3; no se modifica ningún ejercicio: hace falta revisión visual o profesional)

| Ejercicio | Hallazgo | Estado |
|---|---|---|
| 775 | Espalda con patrón de empuje; instrucciones que no casan con el nombre. Está en 1.468 planes | NO CUMPLE (etiqueta). Patrón PENDIENTE (visual) |
| 1261, 1308 | Flexión de tronco o movilidad, no empuje. **`08` L5 era inexacto** al llamarlos HORIZONTAL_PUSH. Cuentan como series de espalda | PENDIENTE (visual y profesional) |
| 973 | Gancho de boxeo: es el único «hombro» en casa sin material | PENDIENTE (profesional) |
| 1320 | Las instrucciones dicen «borde de una silla o banco», pero el GIF no tiene silla. La ficha `/exercises/{id}` expone ese texto | NO CUMPLE (instrucciones) |
| 1263 | «Sentadillas búlgaras» es en realidad una zancada estática; adecuada para principiante | Traducción a corregir |
| 853 | «Media sentadilla» es en realidad una marcha de rodillas altas | Traducción a corregir |
| 1093 | Nombre, instrucciones y GIF no coinciden | PENDIENTE (visual) |
| 1262, 1115 | Flexión completa con gesto añadido; el nivel 1 es dudoso para un principiante | PENDIENTE (profesional) |
| 580 | «dumbbell incline breeding» (apertura) traducido como «Curl inclinado» y clasificado como HORIZONTAL_PUSH, con dosis de fuerza 3×6 | NO CUMPLE (traducción); patrón PENDIENTE |
| 13 aislados KNEE_DOMINANT (curl femoral, extensión) | Acreditan el patrón de rodilla (sentadilla/zancada). REV2: 194 planes de IS/GF con el patrón de rodilla de fuerza cubierto **solo** por aislados (HIST: 208). `claseDe` ignora `is_isolation` | NO CUMPLE (calidad, **previo a REV2**). Propuesta P15: que el aislado no cubra el slot (cambia el catálogo y la semántica; requiere aprobación) |
| Dificultad | Toda la dificultad 1 viene de la regla de palabras clave de V10, sin aprobación humana (solo hay 4 aprobaciones) | PENDIENTE (profesional) |

**Matiz sobre la revisión visual:** las revisiones visuales anteriores fueron **asistidas por IA**; no sustituyen a una revisión profesional.

## 5. Por objetivo (anexo §5.4; adecuación técnica, sin validación profesional)

| Objetivo | Comprobado | Pendiente o no cumple |
|---|---|---|
| GAIN_MUSCLE | Grupos dentro de la banda: 65/46/59 % (HIST: 27/31/41 %); ninguno por encima del techo; frecuencia de 2 días en el 99 % | Avanzado: el 22 % de los grupos bajo el mínimo (1–3 días, hombro) |
| INCREASE_STRENGTH | El 100 % cubre los patrones con dosis de fuerza; descansos de 120/150 s | Dosis de principiante; 44 planes con la rodilla acreditada solo por aislados |
| LOSE_WEIGHT | No promete pérdida de peso | Aeróbico de intermedio/avanzado < 150 min en el 87 % de los planes: **ahora visible** (C2). Patrones de pierna: ver §1 C3 |
| GENERAL_FITNESS | Reparto equilibrado | Aeróbico bajo: ahora visible (C2) |
