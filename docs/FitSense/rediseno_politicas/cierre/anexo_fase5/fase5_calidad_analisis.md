# Fase 5 · Calidad deportiva de los planes de PLAN-REV-2.0

**Alcance:**
- Análisis de solo lectura del worktree `/home/user/wt_aud`, rama `investigacion/rediseno-politicas`, commit `d4556b1`.
- No se ha modificado ningún fichero del repositorio. No se ha usado red, base de datos, Replicate ni WebSearch.
- Todas las fuentes bibliográficas que se citan son las de `02_EVIDENCIA_DOSIS.md` (F*, N*), con sus mismas etiquetas: [SP] salud pública, [ER] evidencia de entrenamiento, [HP] heurística de producto, [PRO] pendiente de validación profesional.

**Cambio de HEAD durante el análisis:**
- Mientras se hacía este trabajo, otra sesión añadió en el worktree los commits `f95e1f7` (17:55 UTC) y `44c8261` (17:59 UTC), y dejó sin seguimiento `cierre/` y `cohorte_M/`. Esta auditoría no los creó.
- `git diff d4556b1 44c8261` no toca los barridos, los tar de K20, las migraciones, `auditoria_catalogo/`, los documentos 00–08, `CalidadDosis`, `DosisSemanalPolicy`, `PlanningPolicyTrace` ni los recursos REST. Las cifras valen para los dos commits.
- El único cambio que roza §5.1.5: `PlanLimitationsService` añade en `44c8261` dos avisos nuevos, FREQUENCY (`S_MAX`, tope de sesiones por nivel) y TIME (`FUERZA_SIN_TIEMPO`). **Sigue sin haber aviso de dosis.** La conclusión de §5.1.5 se mantiene en los dos commits.

**Datos usados:**
- `evidencia/barridos/REV2_final` y `HIST_final`: `barrido_generacion.csv` y `planes.csv.gz`.
- Catálogo, J03 y L14: tablas de `evidencia/k20/REV2_D_J.tar.gz` y `REV2_D_L.tar.gz`, extraídas en el scratchpad. El catálogo es idéntico en los dos tar, salvo `created_at`.
- Migraciones Flyway V9–V44 y las matrices de revisión visual de `docs/FitSense/auditoria_catalogo/`.

**Reproducción:**
- Ejecutar `bash /tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts/reproducir.sh` (unos 12 s).
- Los scripts `s0`…`s16` están en `.../aud3/scripts/`, las salidas de texto en `.../aud3/out_sN.txt` y los CSV de apoyo en `.../aud3/csv/`.

**Estados que se usan:**
- **COMPROBADO:** medido y reproducible.
- **NO CUMPLE:** contradice un criterio explícito del propio proyecto (02/04/08) o del código.
- **PENDIENTE DE VALIDACIÓN:** el dato está medido, pero el juicio requiere a un profesional o una revisión visual del GIF.
- **NO EVALUADO:** no se puede determinar con lo disponible.

## 0. Validez de la reconstrucción · COMPROBADO

Los scripts `s0_verificar.py` y `s4_causa_mecanica.py` reconstruyen tres cosas con el catálogo exportado y `planes.csv.gz`:
- la clase de ejercicio de `CapabilityPrescriptionPolicy.claseDe`;
- las series directas por grupo y los minutos aeróbicos de `CalidadDosis` y `BarridoGeneracionInicialIT`;
- el estimador de duración (`SessionDurationEstimator`: 300 s de calentamiento + trabajo con 3 s por repetición + descansos + 60 s por transición).

Resultados de la comprobación:

| Comprobación | REV2 | HIST |
|---|---|---|
| `series_grupo_max`, `aerobico_trabajo_min`, `grupos_bajo_objetivo` y `estado_dosis` iguales a los del CSV | 3.776/3.776 | 3.641/3.641 |
| `seg_min_sesion` y `seg_max_sesion` iguales a los del CSV (`s4`) | 3.776/3.776 | — |
| Impacto (ImpactoPolicy reproducida sobre `name_es`) igual a `alto_impacto` y `moderado_impacto` (`s13`) | 3.776/3.776 | — |

Sobre las cifras de partida:
- `barrido_generacion.csv` tiene 2.389 filas `DOSIS_LIMITADA`, porque incluye los 4 perfiles sin plan (L1). Entre los 3.776 planes válidos, las filas `DOSIS_LIMITADA` son **2.385**, la cifra de 04/08.
- Hay **1.063** planes con alguna sesión `CORTA_RAZONABLE` y **3.207** sesiones cortas.

---

## 5.1 Los 2.385 planes `DOSIS_LIMITADA`

### 5.1.1 Mínimo razonable usado (definición del auditor a partir de 02; los umbrales son [HP][PRO])

| Componente | Objetivo del código (`DosisSemanalPolicy`) | Mínimo ESTRICTO (clasificación principal) | Mínimo LAXO (sensibilidad) | Base en 02 |
|---|---|---|---|---|
| GM: series directas por grupo grande y semana | B 6 · I 10 · A 12 | B 4 · I 6 · A 8 | igual | Tabla §1, «mínimo eficaz» [ER/HP] |
| No-GM (IS, LW, GF): series por grupo grande | 4 | 4 | IS-B/I 3; IS-A 4; LW/GF 2 | 4: F24 [ER]. 3: tabla §1 de IS, «1×3 días». 2: F3, ≥2 días × 1 serie [SP] |
| Aeróbico LW (min de trabajo/semana) | B 45 · I/A 150 | B 45 · I/A 75 | I/A 45 | 75 = equivalente vigoroso de 150 moderados (F3 [SP]), solo si la intensidad es vigorosa, lo que **no está medido**. 45 = arranque del principiante [HP] |
| Aeróbico GF | B 30 · I/A 60 | igual que el objetivo | I/A 30 | Sin mínimo inferior publicado: el objetivo [HP] hace de mínimo |

**Consecuencia estructural:**
- En los objetivos que no son GM, el objetivo del código **ya es** el mínimo de la evidencia (4 series). Por eso, todo déficit por grupo en IS, LW o GF es «bajo el mínimo» por construcción.
- La clase (a) solo puede aparecer en GM (entre el mínimo eficaz y el objetivo) y en el aeróbico de LW-I/A (entre 75 y 150).

### 5.1.2 Resultado global (`s2_dosis_limitada.py`, `s5_resumen_5_1.py`) · COMPROBADO

| Clasificación | Planes |
|---|---|
| (a) por debajo del objetivo pero ≥ mínimo ESTRICTO | **303** (12,7 %) |
| (b) algún componente por debajo del mínimo ESTRICTO | **2.082** (87,3 %) |
| (a) / (b) con el mínimo LAXO | 377 / 2.008 |
| Si no se cuenta `upper arms` cuando ninguna sesión del plan admite brazos (artefacto, §5.1.3) | sin déficit 487 · (a) 407 · (b) 1.491 |

### 5.1.3 Hallazgo principal: artefacto de medida «brazos en FULL_BODY» · NO CUMPLE (coherencia interna)

**Las dos piezas que chocan:**
- `WorkoutFocus.FULL_BODY` (tabla 20.2) admite `chest, back, upper legs, shoulders, waist`. **No admite `upper arms`.**
- `CalidadDosis` y `DosisSemanalPolicy.GRUPOS_GRANDES` sí exigen series directas de `upper arms` (4 en no-GM y 6/10/12 en GM).
- Las series indirectas no cuentan. 02 §1 dice que deberían contar 0,5 (F8).

**Efecto (`s3_brazos_fullbody.py`):**
- **Los 1.616 planes de 1–3 días** (todos FULL_BODY) salen `DOSIS_LIMITADA` sin excepción.
- En **487** de ellos, el brazo es el **único** déficit: IS 323, GM 106, GF 36, LW 22.
- Si se contaran a 0,5 las series de los empujes y tracciones multiarticulares, 1.278 de los 1.616 llegarían al objetivo de brazos.

**Lectura:**
- No es una falta de dosis según la evidencia: los brazos trabajan de forma indirecta en los presses y los remos.
- Es una contradicción entre el criterio de medida y la regla de enfoque: el déficit de brazos aparece en los 1.616 planes de 1–3 días y es el único motivo de la etiqueta en 487 (30 %).
- **Decisión pendiente** (no se propone código): excluir `upper arms` cuando ningún enfoque lo admite, o contar las indirectas a 0,5 como dice 02.

### 5.1.4 Causa dominante · COMPROBADO (atribución inferida, no prueba causal)

**Método** (`s2`, `s4`, `s5`): para cada componente en déficit (grupo o aeróbico) se combinan dos lecturas.

(1) **Contrafactual dentro de la propia rejilla.** ¿Se cumple el componente en el mismo perfil con:
- más minutos (tiempo);
- más días (días);
- un equipamiento más completo (equipamiento)?

Si no se cumple en ninguna combinación del mismo objetivo, nivel y equipo, el déficit es estructural.

(2) **Mecanismo**, mirando las sesiones que podrían alojar el componente:
- *regla_enfoque:* ninguna sesión admite el grupo;
- *catálogo:* hay ≤ 1 ejercicio elegible del grupo;
- *techo V26 por sesión*;
- *tope de colocaciones:* 6 en GM (V24, `MvpDosePolicy`) y 8 en el resto;
- *tiempo:* no cabe un ejercicio más sin pasar de M;
- *otras reglas:* había hueco y lo impidió V15, V18, V22 o el reparto de la dosis.

La causa del plan es la del componente más alejado de su objetivo.

**Por plan (2.385):**

| Causa dominante | Planes | Qué es |
|---|---|---|
| Solo brazos en FULL_BODY | **487** | Regla de enfoque y criterio de medida (§5.1.3) |
| Catálogo con el equipamiento declarado | **620** | **Todos en CASA_PESO_CORPORAL.** `shoulders` solo tiene **1** ejercicio elegible, 973 «left hook. boxing» (§5.3). El contrafactual lo resuelve con más equipamiento |
| Días | **604** | Con más sesiones se reparte la dosis. El mecanismo dominante es el tope de colocaciones por sesión |
| Tiempo | **568** | V4 (≤ 100 % de M): no cabe otro bloque o ejercicio |
| Reglas (V26 por sesión, combinaciones no monótonas, V15/V18) | **106** | Por ejemplo, GM-A con 1 día: 12 series/grupo > techo de 10 por sesión (20 componentes) |

**Por componente (5.472 componentes; `s4`):**

| Mecanismo | Componentes |
|---|---|
| regla_enfoque | 1.616 |
| tiempo | 1.491 |
| tope de colocaciones | 1.250 |
| catálogo | 728 |
| otras reglas | 330 |
| tiempo o tope | 37 |
| techo V26 | 20 |

**Desglose por objetivo, nivel, equipamiento y días** (`csv/5_1_resumen_por_dimension.csv`; «b sin artefacto» = (b) contando como si no existiera el déficit de brazos en FULL_BODY):

| Dimensión | Válidos | DOSIS_LIMITADA | (a) | (b) | b sin artefacto | brazos-FB | días | tiempo | catálogo | reglas |
|---|---|---|---|---|---|---|---|---|---|---|
| GAIN_MUSCLE | 945 | 700 (74 %) | 181 | 519 | 309 | 106 | 76 | 228 | 283 | 7 |
| INCREASE_STRENGTH | 941 | 407 (43 %) | 0 | 407 | 84 | 323 | 0 | 41 | 43 | 0 |
| LOSE_WEIGHT | 945 | 777 (82 %) | 122 | 655 | 633 | 22 | 204 | 172 | 287 | 92 |
| GENERAL_FITNESS | 945 | 501 (53 %) | 0 | 501 | 465 | 36 | 324 | 127 | 7 | 7 |
| BEGINNER | 1.260 | 704 (56 %) | 73 | 631 | 361 | 249 | 145 | 100 | 207 | 3 |
| INTERMEDIATE | 1.258 | 828 (66 %) | 128 | 700 | 523 | 124 | 225 | 222 | 206 | 51 |
| ADVANCED | 1.258 | 853 (68 %) | 102 | 751 | 607 | 114 | 234 | 246 | 207 | 52 |
| GIMNASIO | 1.258 | 726 (58 %) | 108 | 618 | 394 | 185 | 237 | 245 | 0 | 59 |
| CASA_MANCUERNAS_BANDAS | 1.258 | 724 (58 %) | 96 | 628 | 400 | 191 | 252 | 245 | 0 | 36 |
| CASA_PESO_CORPORAL | 1.260 | 935 (74 %) | 99 | 836 | 697 | 111 | 115 | 78 | 620 | 11 |
| 1 día | 536 | 536 (100 %) | 0 | 536 | 444 | 81 | 262 | 49 | 115 | 29 |
| 2 días | 540 | 540 (100 %) | 0 | 540 | 341 | 154 | 171 | 104 | 94 | 17 |
| 3 días | 540 | 540 (100 %) | 0 | 540 | 240 | 252 | 104 | 88 | 74 | 22 |
| 4 días | 540 | 246 (46 %) | 78 | 168 | 168 | 0 | 38 | 108 | 84 | 16 |
| 5 días | 540 | 208 (39 %) | 98 | 110 | 110 | 0 | 29 | 73 | 84 | 22 |
| 6 días | 540 | 160 (30 %) | 65 | 95 | 95 | 0 | 0 | 76 | 84 | 0 |
| 7 días | 540 | 155 (29 %) | 62 | 93 | 93 | 0 | 0 | 70 | 85 | 0 |

**Por objetivo y nivel** ((a) / (b) estricto):

| Objetivo | B | I | A |
|---|---|---|---|
| GM | 73/145 | 67/161 | 41/213 |
| IS | 0/135 | 0/136 | 0/136 |
| LW | 0/203 | 61/226 | 61/226 |
| GF | 0/148 | 0/177 | 0/176 |

En IS, 323 de los 407 planes lo son solo por el artefacto de brazos.

**Componentes que más fallan** (`out_s2.txt`, `out_s4.txt`):
- **LW aeróbico.** 650 planes, 450 bajo el mínimo estricto. LW-I/A llega a 150 min solo en 84 de 630 planes (42 por nivel, con 5–7 días); la mediana es 72. Mecanismo: tiempo 479 y tope 131.
- **GF aeróbico.** 465 planes; mecanismo: tope 239 y tiempo 203.
- **`shoulders`.** En GM, 563 planes; en LW, 489. En casa sin material es catálogo; en el resto, tope o tiempo.
- **`upper arms`.** 1.616 planes en FULL_BODY (regla de enfoque).

**Discrepancia con la documentación:**
- 04 §1 dice que `DOSIS_LIMITADA` se debe «casi siempre al catálogo o al número de días».
- Medido: catálogo 26 %, días 25 %, tiempo 24 %, artefacto de brazos 20 % y reglas 4 %.
- El tiempo y el artefacto de medida pesan tanto como el catálogo.

### 5.1.5 ¿Puede el usuario conocer la limitación? · NO CUMPLE para la dosis; COMPROBADO solo para la tracción

**Lo que se escribe:** `PlanningPolicyTrace.de` (solo en las políticas sin piso V20) añade `dosis = CalidadDosis.evaluar(...)` a la traza, con:
- `estado`;
- `grupos_bajo_objetivo`;
- `aerobico_trabajo_minutos` y `aerobico_objetivo_minutos`;
- la `clase` de cada sesión.

**Dónde queda:**
- Se guarda en la columna JSONB `weekly_training_plans.planning_policy_trace` (V44; `WeeklyTrainingPlanCommandServiceImpl.java:567`).
- `WeeklyTrainingPlan.getPlanningPolicyTrace()` no tiene ningún lector en `src/main`.

**Recursos REST del plan:**
- `GET /users/me/plan/current`, `GET /users/me/plan/{planId}` y `POST /users/me/plan/generate` devuelven `WeeklyTrainingPlanResource`, con `PlannedWorkoutResource` y `PlannedExerciseResource`.
- Ninguno incluye la traza, el estado de dosis, la clase de sesión ni las series o minutos frente al objetivo.
- El `rationale` del motor de reglas es un texto fijo («Plan generado por el motor de reglas según tu objetivo, nivel, equipamiento y restricciones») y solo se persiste con una adaptación.

**`GET /users/me/plan/limitations`** (`PlanLimitationsService` y `PlanLimitationsResource`):
- Solo informa de **slots obligatorios sin ejercicios** (SLOT) y de **capacidades principales NO_FACTIBLE** (CAPABILITY), medidos sobre el conjunto elegible **antes** de generar.
- No usa `CalidadDosis`.
- En casa con peso corporal (J03, L14) sí avisaría de `UPPER_PULL`: «no hay ejercicios de tracción… Tu plan trabaja empuje, piernas y core». Eso cubre la recomendación de 02 §7 [HP], y es lo único que se comprueba como visible.
- En `44c8261`, posterior al commit auditado, el servicio añade además FREQUENCY y TIME. Ninguno de los dos informa de la dosis.

**Conclusión:**
- `DOSIS_LIMITADA`, `grupos_bajo_objetivo`, el aeróbico por debajo de 45/150 y las sesiones `CORTA_RAZONABLE` **solo llegan a la traza interna** (base de datos y exportaciones de evidencia). **No llegan a la API.**
- 02 §1 exige que, si los días o los minutos no permiten el mínimo eficaz, el plan «lo indica (“dosis por debajo de lo recomendado para tu objetivo”)». **No está implementado.**
- No se encontró en `src/main` ningún texto que prometa pérdida de peso (búsqueda de «perder, pérdida de peso, quemar, adelgaz»). Esto es COMPROBADO solo para el backend; el frontend no se evaluó.

---

## 5.2 Las 1.063 planificaciones con alguna sesión < 50 % de M

`s6_sesiones_cortas.py`, `s14_criterios_cortas.py`, `s7_casos_k20.py`. CSV: `5_2_planes_cortos.csv`, `5_2_sesiones_cortas.csv`, `5_2_caso_J03.csv`, `5_2_caso_L14.csv`.

### 5.2.1 Panorama · COMPROBADO

**Planes con alguna sesión corta, sobre los válidos:**

| Objetivo | B | I | A |
|---|---|---|---|
| IS | 249 (79 %) | 148 (47 %) | 148 (47 %) |
| GM | 159 (50 %) | 99 (31 %) | 95 (30 %) |
| LW | 102 (32 %) | 0 | 0 |
| GF | 63 (20 %) | 0 | 0 |

- **Por días:** 1 → 18, 2 → 82, 3 → 104, 4 → 211, 5 → 218, 6 → 210, 7 → 220.
- **Por equipamiento:** casa con peso corporal 379, casa con mancuernas 349 y gimnasio 335.
- **Sesión más corta:** 1.134 s (25,2 % de M).
- **IS-B:** 672 de sus 810 sesiones cortas duran < 20 min; la mediana es 1.152 s (32,5 % de M).

**Criterios de 02 §2 [HP] sobre las sesiones cortas:**

| Objetivo · nivel | Sesiones | 3–5 ejercicios de trabajo | ≥ 3 multiarticulares | 6–12 series efectivas | FULL_BODY con los patrones exigidos (3 en 20–25 min, 4 en 30–35) |
|---|---|---|---|---|---|
| IS-B | 810 | 775 | 494 (61 %) | 795 | 152/196 |
| IS-I | 524 | 524 | 343 (65 %) | 518 | 4/58 |
| IS-A | 526 | 526 | 346 (66 %) | 520 | 4/58 |
| GM-B | 378 | 335 | 104 | 270 | 19/62 |
| GM-I | 324 | 239 | 157 | 94 (mediana 13) | — |
| GM-A | 284 | 223 | 129 | 89 (mediana 13) | — |
| LW-B | 203 | 200 | 9 | 200 | — |
| GF-B | 158 | 120 | 26 | 110 | 2/3 |

**Semana de los 1.063 planes:**

| Aspecto | Resultado |
|---|---|
| Cobertura de slots | 1.009 cubren todos los slots con material. Los **54 que no** son todos LW-B: falta LOWER_HIP en 36 y LOWER_KNEE en 18 |
| Frecuencia | Grupos grandes entrenados ≥ 2 días en 1.006. Las excepciones son 18 planes de 1 día y 39 LW-B en casa con peso corporal sin hombro (catálogo) |
| Dosis | `DOSIS_OBJETIVO` 693 · (a) 88 · (b) **282** |
| De los 282 (b) | 190 solo por brazos en FULL_BODY · 89 por hombro en casa con peso corporal (catálogo) · 3 GF-B de 1 día |

**Estados:**

| Punto | Estado | Motivo |
|---|---|---|
| Clasificación `CORTA_RAZONABLE` | **NO CUMPLE** (frente a la definición de 02 §2) | CalidadDosis solo comprueba ≥ 3 ejercicios y ≥ 6 series. 02 §2 llama **insuficiente** a la sesión que «no llega al mínimo semanal… sin que el plan lo advierta» o que «deja sin cubrir un patrón principal en toda la semana». Con esa definición, al menos **282** planes (bajo el mínimo y sin aviso en la API) y **54** (LW-B sin un patrón de pierna) no serían razonables. Los 190 del artefacto de brazos dependen de §5.1.3 |
| Criterio de patrones por sesión en FULL_BODY de IS-I/A | **NO CUMPLE** el [HP] de 02 §2 | 108 de 116 sesiones (28–32 min, con 2–3 patrones) no llegan a los 3 o 4 patrones exigidos; la semana sí los cubre. El criterio es [HP][PRO] |
| Volumen y número de ejercicios en IS | **COMPROBADO** | Dentro de 02 §2 en ≥ 96 % de las sesiones cortas |
| Si cumplen su propósito | **PENDIENTE DE VALIDACIÓN** | Ver el detalle por objetivo en §5.4 |
| Llenado | **COMPROBADO** | No se propone rellenar: la orden P-G1 y 02 §2 («la duración de la sesión no es un criterio») lo excluyen |

### 5.2.2 INCREASE_STRENGTH principiante (~20 min) · PENDIENTE DE VALIDACIÓN

**Dosis:**
- La sesión tipo tiene 4 ejercicios a 2×8 con 120 s (la dosis de fuerza del principiante en `CapabilityPrescriptionPolicy`): 8 series efectivas y unos 1.152 s.
- Cubre en la semana todos los slots disponibles con dosis de fuerza en los 315 planes.

**Series de fuerza por patrón y semana** (`s12`): **325 de 1.155** pares slot–plan (28 %) quedan con 2 series (< 3), frente a 111 en HIST.

**Lectura frente a la evidencia:**
- La tabla §1 de 02 pone el mínimo de IS-B en 3 series por patrón principal y semana («1×3 días»).
- N5 [ER, revisión narrativa] admite < 3 series por multiarticular y semana en las 12 primeras semanas.
- Es técnicamente coherente con N5 y queda **por debajo** de la tabla de 02 en el 28 % de los patrones.
- Requiere decisión profesional (P2).

### 5.2.3 J03 · LOSE_WEIGHT principiante, casa con peso corporal, 4 días de 70 min (plan 7, S1 2026-11-02, REV2_D_J)

| Día | Enfoque | Ejercicios (series × reps o s / descanso) | Estimado | Clase (traza) |
|---|---|---|---|---|
| Lun | UPPER | 1262 2×8/120 · 775 2×8/120 · 1320 2×8/120 · 853 3×90 s · 253 3×90 s · 1054 3×90 s | 2.274 s (54 %) | COMPLETA |
| Mar | LOWER | 1073 2×8/120 · 1117 3×90 s · 1093 3×90 s | 1.368 s (33 %, unos 23 min) | CORTA_RAZONABLE |
| Jue | UPPER | 1115 2×8/120 · 775 2×8/120 · 509 2×8/120 · 853 · 1054 · 1117 (3×90 s) | 2.274 s (54 %) | COMPLETA |
| Vie | LOWER | 1263 2×8/120 · 253 · 1093 (3×90 s) | 1.368 s (33 %) | CORTA_RAZONABLE |

**Traza:** `DOSIS_LIMITADA`, `grupos_bajo_objetivo = [shoulders]`, aeróbico 45/45 min. Series: chest 4, back 4 (las 4 son del 775), upper arms 4, upper legs 4.

| Aspecto | Hallazgo | Estado |
|---|---|---|
| Cobertura | Empuje (todo el tren superior es HORIZONTAL_PUSH), rodilla (1263, una vez) y cadera (1073, una vez). Sin tracción (catálogo, L4; la API lo avisa). Sin hombro directo: el único ejercicio es 973 | COMPROBADO |
| Volumen | Pierna 4 series/semana (1 ejercicio de 2 series por sesión), igual al mínimo de LW [SP F3: 1–3 series por grupo y sesión, ≥ 2 días]. Empuje ≈ 12 series/semana (6 ejercicios de flexión o fondo) | COMPROBADO |
| Reparto | 3:1 entre empuje y pierna, en un objetivo en que la musculatura grande de la pierna es la que más interesa. Lo produce el reparto UPPER/LOWER con la misma dosis por grupo. ¿Es la división adecuada para LW-B? | PENDIENTE DE VALIDACIÓN |
| Aeróbico | 45 min de trabajo = objetivo de arranque [HP]. No se promete pérdida de peso | COMPROBADO |
| Frecuencia | Cada grupo, 2 días; ningún grupo grande en días pegados (V18) | COMPROBADO |
| Nivel | Todo es de dificultad 1, pero por la regla de palabras clave de V10, no por aprobación humana (§5.3). 1262 y 1115 exigen una flexión completa: dudoso para un principiante desentrenado | PENDIENTE DE VALIDACIÓN |
| Impacto | 1093 se clasifica MODERADO por el nombre («correr»), aunque la revisión visual asistida por IA describe «de pie elevando rodillas», sin vuelo. Según el criterio de 02 §6 («LW en casa principiante: solo BAJO»), el plan lo incumpliría si la etiqueta fuera correcta | PENDIENTE DE VALIDACIÓN |
| Carga articular | El lunes y el jueves encadenan 3 flexiones o fondos + gateo de oso + escalador (6 ejercicios en apoyo de manos): carga de muñeca y hombro | PENDIENTE DE VALIDACIÓN |
| Propósito | Cumple las cifras [HP] de su objetivo. Las sesiones LOWER de 23 min son coherentes con la dosis elegida. No se propone rellenar; la pregunta profesional es de **reparto**, no de minutos | PENDIENTE DE VALIDACIÓN |

### 5.2.4 L14 · INCREASE_STRENGTH principiante, casa con peso corporal, 4 días de 64 min (plan 40, REV2_D_L)

| Día | Enfoque | Ejercicios | Estimado |
|---|---|---|---|
| Lun | UPPER | 318 body-up 2×8/120 · 496 drop push-up 2×8/120 · 775 2×8/120 · 973 left hook boxing 2×10/90 | 1.134 s (30 %, unos 19 min) |
| Mar | LOWER | 1196 inverse leg curl 2×8/120 · 1112 pelvic tilt into bridge 2×8/120 · 294 calf raise 2×13/90 · 1185 side-to-side toe touch 2×13/90 | 1.152 s |
| Jue | UPPER | 1115 · 775 · 509 (2×8/120) · 973 2×10/90 | 1.134 s |
| Vie | LOWER | 1103 potty squat 2×8/120 · 1195 inverse leg curl 2×8/120 · 1254 standing calves 2×13/90 · 57 3/4 sit-up 2×13/90 | 1.152 s |

**Traza:** `DOSIS_OBJETIVO`, las 4 sesiones `CORTA_RAZONABLE`.

| Aspecto | Hallazgo | Estado |
|---|---|---|
| Dosis nominal | `DOSIS_OBJETIVO` gracias a dos ejercicios dudosos: back 4 (2× 775, una flexión invertida con patrón HORIZONTAL_PUSH) y shoulders 4 (2× 973, un gancho de boxeo). Sin los dudosos (`s10`), los **325** planes `DOSIS_OBJETIVO` de casa con peso corporal del barrido (los 325, sin excepción) pasarían a `DOSIS_LIMITADA` | NO CUMPLE (la etiqueta exagera la dosis) |
| Patrones de fuerza | Empuje 12 series/semana. Rodilla: 1103 (2 series) es la única sentadilla; 1195 y 1196 son curl femoral nórdico asistido (`is_isolation = true`, rodillas en el suelo), etiquetados KNEE_DOMINANT, y acreditan LOWER_KNEE. Cadera: 1112, 2 series. Sentadilla y bisagra quedan con 2 series/semana, por debajo del mínimo de 3 de la tabla §1. Sin tracción (catálogo) | NO CUMPLE la tabla de 02; PENDIENTE para N5 |
| Equilibrio | Empuje:tracción 12:0, declarado en la API (UPPER_PULL) | COMPROBADO |
| Duración | 19 min con 8 series efectivas y 2–3 ejercicios con dosis de fuerza por sesión. Dentro de 02 §2 en número y series. El martes solo tiene 1 multiarticular real (1112) | PENDIENTE DE VALIDACIÓN |
| Propósito | Para fuerza de principiante en casa sin material es una dosis mínima. La calidad la limitan el catálogo y las etiquetas, no los minutos. No se propone rellenar | PENDIENTE DE VALIDACIÓN |

---

## 5.3 Calidad de los ejercicios de J03 y del catálogo

`s8_catalogo_migraciones.py` (ficha y migraciones; `csv/5_3_fichas.csv`), `s9_uso_ejercicios.py` (uso en los barridos) y matrices `auditoria_catalogo/{fase1,revision_visual,v38}`.

**Datos generales:**
- El nombre original es `name_en`, del dataset hasaneyldrm/exercises-dataset. Los GIF son © Gym visual (V9).
- Las `instructions_en/es` vienen del dataset y **en varios casos contradicen el GIF**: no son una fuente fiable.
- **La dificultad 1** de todos estos ejercicios sale de la **regla de palabras clave de V10**: el peso corporal da 1; las palabras «one, single, jump, archer…» suman 1; «assisted, kneeling, wall…» restan 1.
- `exercise_difficulty_approvals` solo tiene 4 aprobaciones humanas (122, 776, 872 y 878, de LOWER_HIP). **Ninguna dificultad de J03 ni de L14 está aprobada por personas.**
- Las revisiones visuales previas (V36–V41 y las matrices) son **«asistidas por IA»**, con la doble revisión humana **pendiente**.

| ID (código) | name_en → name_es | Grupo / objetivo · patrón · aislamiento | Datos relevantes | Uso en REV2 | Valoración | Estado |
|---|---|---|---|---|---|---|
| **1262** (0778) | spider crawl push up → «Flexión con desplazamiento de araña» | chest/pectorals · HORIZONTAL_PUSH · no | V9: upper legs/glutes; V39 lo pasa a chest/pectorals (revisión IA del GIF: «flexión de brazos; la pierna es accesoria»). Instrucciones: flexión completa con rodilla al codo. Suelo sí; impacto BAJO; dificultad 1 por V10 | 385 planes (prescripción más frecuente: 3×10/90) | Variante de flexión completa con exigencia de core y cadera: **probablemente no es de nivel 1** para un principiante desentrenado. «Desplazamiento» sugiere avance (crawl), lo que hay que confirmar en el GIF | PENDIENTE DE VALIDACIÓN (visual y profesional) |
| **775** (1772) | elbow lift - reverse push-up → «Elevación de codo y push-up invertido» | back/upper back · **HORIZONTAL_PUSH** · no | Instrucciones: «tumbado boca abajo… presiona con las palmas para levantar el tronco… aprieta la espalda alta» (incompatible con «elbow lift», que normalmente se hace boca arriba). fase1: G1 patrón-zona incoherente y G3 nombre-zona; revisión visual: grupo SIN_ERROR, sin decisión sobre el patrón | **1.468 planes** (1.056 en casa sin material); 106 apariciones en sesiones PULL | Cuenta como serie directa de espalda y como empuje. En casa sin material es la «espalda» de casi todos los planes. Patrón dudoso: o es una retracción escapular boca arriba (espalda alta, no empuje) o un *press-up* boca abajo (empuje o extensión). Adecuado para principiante en cualquiera de las dos lecturas | **NO CUMPLE** coherencia de etiquetas; PENDIENTE (visual) |
| **1320** (0815) | triceps dips floor → «Dip de tríceps en piso» | upper arms/triceps · HORIZONTAL_PUSH · no | GIF (revisión IA): «sentado en el suelo, manos detrás de la cadera». **Las instrucciones ES/EN dicen «Siéntate en el borde de una silla o banco»**; la matriz lo marcó CONFIRMADO (gravedad BAJA), pero **ninguna migración corrige el texto**, y `/exercises/{id}` expone las instrucciones. `requires_floor` corregido en V37 | 359 planes | El fondo en el suelo es asequible para un principiante (extensión de codo con extensión de hombro). El texto induce a usar una silla, en contra del criterio [SEG] de 02 §7. Patrón: más bien extensión de codo o empuje vertical que horizontal | **NO CUMPLE** (instrucciones); PENDIENTE (patrón) |
| **1263** (2368) | split squats → «Sentadillas búlgaras» | upper legs/quads · KNEE_DOMINANT · no | Instrucciones y revisión visual (SIN_ERROR): zancada estática con los dos pies en el suelo. **La traducción es errónea:** la búlgara lleva el pie trasero elevado en un banco y es más difícil | 317 planes | Adecuado para principiante, con la salvedad del equilibrio. El nombre en español puede llevar a hacer una versión más difícil y con mueble | **NO CUMPLE** (traducción); ejercicio adecuado: PENDIENTE PRO |
| **1261** (1364) | standing pelvic tilt → «Inclinación pélvica de pie» | back/**spine** · **TRUNK_FLEXION** · no | Revisión visual (A01): SIN_ERROR. Gesto de movilidad o control lumbopélvico | 342 planes (2×10/90 como hipertrofia) | No es trabajo resistido de espalda, pero **cuenta como serie directa de `back`**. **08 L5 lo describe como HORIZONTAL_PUSH: es incorrecto.** En el catálogo exportado y en V26 es TRUNK_FLEXION | **NO CUMPLE** (cuenta para la dosis); 08 L5 inexacto |
| **1308** (3231) | two toe touch (male) → «Toque de dos dedos de los pies» | back/spine · TRUNK_FLEXION · no | fase1: gravedad ALTA (S1, suelo sin bandera). V37: `requires_overhead` → true. Revisión visual: grupo **PROBABLE waist**, no aplicado | 370 planes | Flexión de tronco o movilidad; **cuenta como serie de espalda**. Mismo error de 08 L5 | **NO CUMPLE** (cuenta para la dosis); PENDIENTE (grupo) |
| **973** (2271) | left hook. boxing → «Giro izquierdo. boxeo» | shoulders/delts · TRUNK_ROTATION · no | Revisión visual (A02): SIN_ERROR. Es un **gancho de boxeo** al aire | **976 planes** (914 en casa sin material), sobre todo 2×10/90 | **Único ejercicio de hombro en casa sin material.** Con el GIF no se comprueba que sea trabajo resistido de deltoides (no hay carga externa) y aporta las 4 «series de hombro» de L14. La traducción «Giro izquierdo» no describe el gesto | **NO CUMPLE** (cuenta para la dosis); PENDIENTE PRO |
| 1115 (0661) | push-up inside leg kick → «Flexión con patada interna» | chest · HORIZONTAL_PUSH | V39: upper legs/glutes → chest (revisión IA) | 325 | Flexión completa + patada: dudoso como nivel 1 | PENDIENTE (visual y PRO) |
| 509 (2398) | close-grip push-up (on knees) → «Flexión cerrada con rodillas» | upper arms/triceps · HORIZONTAL_PUSH | Revisión visual SIN_ERROR; rodillas sí | 363 | Adecuado para principiante | COMPROBADO (técnico); PRO pendiente |
| 853 (3221) | half knee bends (male) → «**Media sentadilla**» | cardio · LOCOMOTION_CARDIO, DURATION | El GIF (revisión IA, decisión D1) muestra una **marcha con rodillas altas sin saltos**; las instrucciones describen una media sentadilla | 1.648 planes (3×90–120 s) | Como marcha es BAJO y adecuado. **El nombre y las instrucciones no corresponden al gesto** | **NO CUMPLE** (nombre e instrucciones); PENDIENTE |
| 253 (3360) | bear crawl → «Gateo de oso» | cardio · LOCOMOTION_CARDIO | `requires_kneeling = true`, aunque el GIF va con las rodillas en el aire (restricción de más) | 1.661 | Exige muñeca, hombro y espacio; BAJO | PENDIENTE PRO |
| 1054 (0630) | mountain climber → «Escalador» | cardio · LOCOMOTION_CARDIO | BAJO (sin «rápido» en el nombre); revisión visual SIN_ERROR | 1.659 | Intensidad alta, en plancha; sin vuelo | PENDIENTE PRO |
| 1073 (3013) | low glute bridge on floor → «Puente de glúteos bajo en el suelo» | upper legs/glutes · HIP_EXTENSION | V28: duplicado funcional del 872 | 449 | Adecuado para principiante | COMPROBADO (técnico) |
| 1117 (3552) | quick feet v. 2 → «Pies rápidos v. 2» | cardio (V39: antes upper legs/quads) | En revisión, revisado como cardio (D1). GIF: «apoyos rápidos alternos sin vuelo» → BAJO | 1.669 | Correr en el sitio sobre la parte delantera del pie, a la que 02 §6 asigna MODERADO por definición, y el GIF dice que no hay vuelo | PENDIENTE (visual y PRO) |
| 1093 (3638) | push to run → «Empujar para correr» | cardio | Instrucciones: flexión + escalador. Revisión visual (V38): «de pie elevando rodillas» (y `requires_floor` → false). ImpactoPolicy lo pone en **MODERADO por la palabra «correr»** | 433 planes (420 apariciones en LOWER) | Nombre, instrucciones y GIF no coinciden; la etiqueta de impacto la decide el nombre | **NO CUMPLE** (coherencia); PENDIENTE |

**Otros hallazgos del catálogo que afectan a la calidad:**

- **Grupos de espalda sin tracción** (`s9`): en REV2, las filas de plan con `back` y sin patrón de tracción son, por número de filas:

  | ID | Ejercicio | Filas |
  |---|---|---|
  | 775 | elbow lift - reverse push-up | 2.542 |
  | 1066 | lower back curl | 415 |
  | 1308 | two toe touch | 410 |
  | 1261 | standing pelvic tilt | 380 |
  | 1259 | standing archer | 267 |
  | 1146 | scapula dips | 94 |

  En casa sin material, el material de espalda son exactamente 5 ejercicios (775, 1066, 1259, 1261 y 1308), y **ninguno es una tracción ni un ejercicio escapular en prono** como los que propone 02 §7 (Y-T-W). La propuesta de 02 §7 no se puede aplicar con este catálogo.
- **Hombro en casa sin material:** solo 973 (`s3`). Es la causa del 26 % de los `DOSIS_LIMITADA` (§5.1.4) y, a la vez, la única fuente de «dosis de hombro» de los planes que sí llegan.
- **ImpactoPolicy por nombre con falsos positivos** (`s13`): 25 ejercicios activos con `high_impact = false` pasan a MODERADO por el nombre.
  - 23 son de **polea con «cuerda»** (*rope attachment*): remos, jalones y extensiones de tríceps.
  - 1 es 1093 («correr»).
  - **Los 190 planes LW-B y los 45 GF-B con algún ejercicio «no BAJO» se explican solo por estos falsos positivos.** No hay impacto real en principiantes LW o GF.
  - La cifra de 04 de 1.007 planes con impacto moderado está inflada por este efecto.
  - PENDIENTE: decidir si «cuerda» se acota a «saltar la cuerda o comba» (P10).
- **KNEE_DOMINANT con aislamiento:** 13 ejercicios activos (curl femoral y extensiones) acreditan LOWER_KNEE.
  - REV2: 194 planes IS o GF tienen el patrón de rodilla con dosis de fuerza **solo** con aislamiento (IS 44, GF 150); HIST, 208.
  - Es previa a REV2. PENDIENTE PRO: ¿debe un curl femoral cubrir el patrón «sentadilla o zancada»?
- **Instrucciones con otro material:** 318 body-up («superficie elevada, banco o paralelas») y 1196 (máquina de curl femoral), con `body weight`. En 1196, fase1 lo señala E2 para 1195; en 318 y 1196 no se ha cotejado el GIF. PENDIENTE (visual).

**Qué requiere validación visual (GIF), expresamente:**
- 1262 y 1115: ¿hay desplazamiento? ¿flexión completa?
- 775: posición boca arriba o boca abajo, y patrón.
- 1320: patrón.
- 1308: grupo.
- 973: si hay trabajo resistido.
- 853 y 1093: nombre e instrucciones frente al gesto.
- 1117: vuelo.
- 318, 1195 y 1196: material.

**Qué requiere validación profesional:**
- la dificultad de todos (V10 no es una aprobación);
- si 973, 1261 y 1308 deben contar como series directas;
- si un curl femoral acredita el patrón de rodilla;
- la carga de muñeca y hombro en LW-B;
- el reparto UPPER/LOWER en LW-B.

---

## 5.4 Calidad por objetivo

`s11_calidad_objetivo.py` (`csv/5_4_calidad_por_objetivo.csv`), `s12`, `s13`, `s15` y `s16`. Medianas de los planes válidos, HIST → REV2.

**Distinción que se mantiene en todo el apartado:**
- **Adecuación técnica:** el plan cumple las cifras [HP] de 02 y del código.
- **Validación profesional:** si esas cifras y esos ejercicios son adecuados. Esto último sigue **PENDIENTE** en todos los casos (P2).

### GAIN_MUSCLE

| Métrica | B | I | A |
|---|---|---|---|
| Series por grupo trabajado y semana (mediana) | 12 → 9 | 12 → 12 | 12 → 12 |
| Grupos dentro de la banda de 02 (6–10 / 10–16 / 12–20) | 27 → **65 %** | 31 → 46 % | 41 → 59 % |
| Grupos bajo el mínimo eficaz (4/6/8) | 4 → 4 % | 10 → 8 % | 21 → **22 %** |
| Grupos sobre la banda | 59 → 22 % (11–12, ≤ techo) | 38 → 21 % (≤ 20) | 21 → 0 % |
| Máximo por sesión y grupo | 12 → 6 | 24 → 9 | 24 → 9 |
| Grupos con ≥ 2 sesiones (planes de ≥ 2 días) | 98 → 99 % | 98 → 99 % | 98 → 99 % |

**Volumen:**
- REV2 mejora de forma clara la distribución: 0 grupos por encima del techo, frente a 446 planes con > 24 series en HIST.
- Por grupo (`s15`), `shoulders` queda en 6 / 7,5 / 8 de mediana, por debajo del objetivo de 10 y 12 en I/A: el hombro se trabaja sobre todo de forma indirecta en los presses, que no cuentan.
- `upper arms` tiene 0 series directas en los 1–3 días (FULL_BODY).
- La prescripción dominante es 3×10/90 (hipertrofia), coherente con [ER] F1.

**Recuperación:**
- Ningún grupo grande se repite en días consecutivos, salvo `upper arms` en PUSH→PULL (298 pares de sesiones; HIST, 327).
- Ahí son tríceps y luego bíceps, músculos distintos, así que no es un problema de recuperación.

**Estados:** volumen y frecuencia, **COMPROBADO** (técnico). El 22 % de los grupos de GM-A bajo el mínimo es **NO CUMPLE** frente al [HP] de la tabla; procede sobre todo de los 1–3 días (226 de los 317 grupos; tope de 6 ejercicios por sesión) y del hombro (151 de 317) (comprobación ad hoc con `comun.py`, misma lógica que `s11`). Las cifras, **PENDIENTE PRO**.

### INCREASE_STRENGTH

| Métrica | Resultado |
|---|---|
| Slots disponibles cubiertos con dosis de fuerza | 100 % de los planes en las dos políticas |
| Patrón de rodilla cubierto solo por aislamiento (curl femoral o extensión) | 44 planes REV2 |
| Series de fuerza por slot y semana | B: 28 % de los slots con < 3. I/A: mínimo 3 siempre; un 25–26 % exactamente en 3 |
| Dosis | B 2×8 / 120 s; I/A 3×6 / 150 s (`reps(5)` sube a 6 por `min_reps = 6`) |

**Lectura:**
- Los descansos de 120–150 s son coherentes con el rango habitual de 2–3 min para multiarticulares pesados [ER, F1/ACSM].
- 8 repeticiones en el principiante es una carga moderada, no «pesada»: decisión [PRO].
- Las sesiones FULL_BODY de IS-I/A llevan 2–3 patrones por sesión (§5.2.1) y la semana los cubre todos.
- Sin tracción en casa con peso corporal (catálogo; declarado en la API).
- El trabajo accesorio de brazos no existe con 1–3 días (FULL_BODY).

**Estados:**
- Cobertura de patrones y descansos: **COMPROBADO** (técnico).
- Dosis de fuerza del principiante: **PENDIENTE PRO** (28 % por debajo de la tabla de 02; dentro de N5).
- Acreditación de LOWER_KNEE por aislamiento: **PENDIENTE PRO** (regla previa).

### LOSE_WEIGHT

| Métrica | B | I/A |
|---|---|---|
| Minutos aeróbicos de trabajo por semana (mediana) | 36 → 45 (objetivo 45) | 48 → 72 (objetivo 150) |
| Planes que llegan a 150 | — | 0 → 84 de 630 (42 por nivel; 2 con 5 días y el resto con 6–7) |
| % del trabajo que es aeróbico | 75 → 86 % | 82 → 92 % |
| Días con fuerza ≥ 2 (planes de ≥ 2 días) | 100 % | 100 % |
| Series por grupo (mediana) | 4 (HIST: piernas 12) | 6 |

**Hallazgo nuevo de cobertura** (`s12`): REV2 deja la semana **sin ningún ejercicio de cadera** en 125 (B), 163 (I) y 160 (A) de 315 planes, y **sin rodilla** en 60, 64 y 60. HIST: 3 y 21–27.

**Lectura:**
- El objetivo de 4–6 series por grupo «upper legs» se cubre con un solo patrón.
- 02 §1 y F3 [SP] piden trabajar «todos los grupos principales». La pierna está trabajada, pero glúteo e isquios (bisagra o extensión de cadera) faltan en ~40–50 % de los planes.

**Impacto:**
- LW-B: 0 ejercicios ALTO; los «MODERADO» son falsos positivos de nombre (§5.3).
- LW-I/A: alto impacto en 81 y 122 planes, frente a 296–299 en HIST. Es coherente con que en el barrido `allow_high_impact = true`.
- La propuesta [SEG][PRO] de 02 §6 («LW en casa: solo BAJO por defecto») **no está implementada**: P-C2 solo prefiere el menor impacto en los complementos.

**Promesa de resultados:** el backend no la hace (§5.1.5). El aeróbico por debajo de 150 no se comunica al usuario.

**Estados:**

| Punto | Estado |
|---|---|
| Equilibrio fuerza/aeróbico | COMPROBADO (técnico); PRO pendiente |
| Aeróbico I/A < 150 en el 87 % de los planes (546/630), sin aviso | NO CUMPLE frente a 02 §1 («lo indica») |
| Cobertura de cadera y rodilla | NO CUMPLE frente a F3 en el 40–50 % (cadera) y el 20 % (rodilla) de los planes; regresión frente a HIST |
| Impacto por defecto en casa | NO EVALUADO como regla (no existe); en los datos, COMPROBADO que no hay ALTO en LW-B |

### GENERAL_FITNESS

| Métrica | B | I/A |
|---|---|---|
| Minutos aeróbicos (mediana) | 18 → 36 (objetivo 30) | 30 → 48 (objetivo 60); ninguno llega a 150 |
| Movilidad (mediana de colocaciones por semana) | 2 → 4 | 3 → 4 |
| % de trabajo aeróbico | 41 → 67 % | 46 → 63 % |
| Slots con dosis de fuerza en todos | 313/315 | 304/315 |
| Series por grupo (mediana) | 4–6 | 6–9 |
| Alto impacto (planes, I/A) | — | 281–290 → 135–166 |
| Patrón de rodilla cubierto solo por aislamiento | 150 planes (todos los niveles) | |

**Lectura:** la distribución de fuerza, aeróbico y movilidad es más equilibrada que en HIST y está dentro de las cifras [HP]. El aeróbico de 1–3 días es estructuralmente bajo (4–48 min): con 1 día, 4–6 min.

**Estados:** distribución **COMPROBADO** (técnico). Aeróbico bajo en 465 planes, **sin aviso: NO CUMPLE** frente a 02 §1. Cifras **PENDIENTE PRO**.

---

## Tabla de estados

| Punto | Estado | Cifra clave (script) |
|---|---|---|
| 5.1 Clasificación (a)/(b) | COMPROBADO; umbrales PENDIENTE PRO | (a) 303 · (b) 2.082 · laxo 377/2.008 (`s2`, `s5`) |
| 5.1 Causa dominante | COMPROBADO (inferida) | catálogo 620 · días 604 · tiempo 568 · brazos-FB 487 · reglas 106 (`s5`) |
| 5.1 Artefacto `upper arms` en FULL_BODY | NO CUMPLE (coherencia) | 1.616 planes de 1–3 días; 487 solo por eso (`s3`) |
| 5.1 La limitación de dosis llega al usuario | NO CUMPLE | Solo en `planning_policy_trace`; `/plan/limitations` cubre slots y capacidades, no la dosis |
| 5.1 Aviso de tracción en casa | COMPROBADO | `PlanLimitationsService`, UPPER_PULL |
| 5.2 Sesiones cortas: volumen y ejercicios (IS) | COMPROBADO | ≥ 96 % dentro de 3–5 ejercicios y 6–12 series (`s14`) |
| 5.2 La etiqueta `CORTA_RAZONABLE` frente a 02 §2 | NO CUMPLE | 282 bajo el mínimo y sin aviso; 54 LW-B sin patrón de pierna (`s6`, `s14`) |
| 5.2 IS-B ~20 min | PENDIENTE PRO | 28 % de los patrones con 2 series/semana (`s12`) |
| 5.2 J03 | PENDIENTE PRO (reparto 12:4 empuje:pierna, dificultad, muñeca) | `s7` |
| 5.2 L14 | NO CUMPLE (dosis nominal) / PENDIENTE PRO | `DOSIS_OBJETIVO` por 775 y 973; 325/325 en casa sin material dependen de ellos (`s10`) |
| 5.3 Etiquetas 775, 1261, 1308 y 973 | NO CUMPLE; PENDIENTE visual | Cuentan como series directas (`s9`, `s10`) |
| 5.3 Instrucciones 1320 (silla), 853 y 1093 (no coinciden con el GIF) | NO CUMPLE | matrices `revision_visual` y V38 |
| 5.3 Traducción de 1263 («búlgaras») y 853 («media sentadilla») | NO CUMPLE | V11 |
| 5.3 Dificultad 1 de 1262 y 1115 | PENDIENTE (visual y PRO) | Regla V10, sin aprobación |
| 5.3 08 L5 (1261 y 1308 «HORIZONTAL_PUSH») | Documentación inexacta | Son TRUNK_FLEXION (V26) |
| 5.4 GM | COMPROBADO (técnico); PRO pendiente; A: 22 % bajo el mínimo | `s11`, `s15` |
| 5.4 IS | COMPROBADO (patrones y descansos); PENDIENTE PRO (dosis B, aislamiento en LOWER_KNEE: 44) | `s11`, `s12`, `s16` |
| 5.4 LW | NO CUMPLE (cadera ausente en ~40–50 %; aeróbico sin aviso); sin promesa de peso (COMPROBADO, backend) | `s11`, `s12` |
| 5.4 GF | COMPROBADO (distribución); aeróbico sin aviso: NO CUMPLE | `s11` |
| 5.4 Impacto por nombre | PENDIENTE (P10) | 25 falsos positivos; explican los 235 planes B «no BAJO» (`s13`) |
| Frontend y textos de la app | NO EVALUADO | Fuera del repositorio |

**Ficheros de apoyo** (`/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/csv/`):
- `5_1_dosis_limitada_planes_final.csv` (por plan: clase, causa y mecanismo);
- `5_1_componentes_mecanica.csv` (por componente);
- `5_1_resumen_por_dimension.csv`;
- `5_2_planes_cortos.csv` y `5_2_sesiones_cortas.csv`;
- `5_2_caso_J03.csv` y `5_2_caso_L14.csv`;
- `5_3_fichas.csv`;
- `5_4_calidad_por_objetivo.csv`.
