# 02 · Protocolo K20 (congelable)

**Estado:** se congela en el commit que fija el SHA de la §8. Tras ese commit no cambian el código, los perfiles, los guiones, los criterios, el prompt ni el oráculo. Si hay que corregir algo, la versión nueva se registra y la evaluación se repite **completa**; no se reemplaza nada.

## 1. Escenarios

- **U01–U20 del experimento histórico, sin cambios.** El CSV `entrada/03_PERFILES_U01_U20.csv` es byte a byte el blob `95c2839` de `lab/experimento-kora-20usuarios`. El arnés exige su SHA-256 `584e2204…`.
- Son 4 objetivos × 5 patrones:
  - E1: H/H/H;
  - E2: H/H/P40;
  - E3: H/H/O, con FATIGUE;
  - E4: H/O/O, con TOO_DIFFICULT y LACK_OF_MOTIVATION;
  - E5: H/N/N.
- **Regla P40 histórica:** cada ejercicio se registra al 40 % de su dosis total, redondeado HALF_UP. **No se cambian los porcentajes de U07 ni de U12.**
- **Semanas:**
  - S1 2026-11-02, semana medida con el patrón del perfil;
  - S2 2026-11-09, semana de continuidad, **toda hecha**;
  - S3 2026-11-16, publicada al cerrar S2.
- **Semillas:**
  - del motor, `userId × 1.000.003 + epochDay(S1)`;
  - base vacía y altas en el orden U01…U20, con `userId` = 2…21 registrados.
- Los casos independientes I01–I16 del protocolo anterior **no** forman parte de esta fase.

## 2. Recorrido de cada usuario

El arnés `EvaluacionK20IT` usa los servicios reales.

1. Alta y perfil.
2. Generación inicial del plan S1 con el motor de reglas. En R también: el plan inicial es **el mismo** que en D.
3. Registro de S1 según el patrón.
4. Ciclo S1→S2: cierre, métricas, decisión, adaptación, validación y publicación.
5. **Idempotencia:** el ciclo se repite y debe devolver el mismo plan, sin filas V32 nuevas.
6. Recuperación de S2 **por la API HTTP** (`GET /plan/{id}` y `/plan/current`, con token).
7. Registro completo de S2.
8. Ciclo S2→S3.
9. Consulta de `GET /plan/adaptations`.

Después se exportan todas las tablas de la base efímera a `tablas/`.

## 3. Condiciones

| Condición | Qué es | Estado |
|---|---|---|
| **D** | IA desactivada: núcleo determinista y respaldo | Autorizada |
| **D′** | D repetida: debe ser idéntica (huella normalizada) | Autorizada |
| S (control) | IA **simulada**: camino real de la IA, con la respuesta escrita por el laboratorio (intento 1 inválido a propósito, intento 2 la mejor candidata válida), sin red. **No es un resultado de IA**: comprueba el recorrido y el rechazo | Preparación y control |
| **R** | Replicate real, mismos perfiles, registros y plan inicial | **Pendiente de autorización** |
| **R′** | R repetida (variabilidad) | **Pendiente de autorización** |
| G (opcional) | Generación inicial con IA, 20 gemelos, bloque separado | Pendiente de decisión |

## 4. Resultado 1: recorrido correcto (objetivo 20/20)

Un usuario es **PASS** si el oráculo independiente (`03`) no encuentra ningún incumplimiento en las fases aplicables.

| Fase | Qué se comprueba |
|---|---|
| A · Generación | • El perfil almacenado coincide con el congelado.<br>• Hay un único plan vigente.<br>• Número de sesiones, días disponibles, semana, un entrenamiento por día.<br>• V5, ejercicio no repetido, V1 activo, V6 nivel, equipamiento (grupos AND, opciones OR), V7 tipo, V21 descanso, V20 piso en segundos, V4 techo y V17 coherencia |
| B · Registro | • Lo almacenado es exactamente lo registrado: H = dosis total; P40 = round(0,4 × total).<br>• Omisión con su motivo.<br>• «Sin registro» distinto de una omisión |
| C · Adherencia | Aw, Aobs y C recalculados: diferencia ≤ 0,01 pp |
| D · Decisión | La política sobre los valores **reales**: dolor primero, cobertura, ≥ 80, < 50 o 50–80 (la peor). Sesiones señaladas e intervenciones |
| E · Adaptación | • Las no señaladas, idénticas (ejercicios, dosis, descanso, fecha + 7 y minutos).<br>• En las señaladas no sube ninguna dosis, no se añade nada, el trabajo no sube y ningún sustituto es más difícil.<br>• Origen identificable.<br>• Ninguna INC de origen determinista |
| F · Publicación | • Plan válido, un solo vigente, `resulting_plan_id` correcto, linaje 3/3 (salvo P-03) e idempotencia.<br>• Texto coherente con lo ocurrido (T1) |
| G · Continuidad | • API 200 y sesiones correctas; S2 registrable.<br>• Métricas y decisión de S2.<br>• S3 válido y copia exacta con NO_CHANGE.<br>• Sin reincorporación indebida; exclusiones persistentes |

Una conservación es **CONS-J** solo si:
- la política conserva sin preguntar (CAUSA_NO_REGISTRADA);
- la IA no estaba (RESPALDO de D);
- o la sonda demostró que no había ninguna modificación simple válida.

Si había una y no se aplicó, es **CONS-POL**: la política lo permite, pero cuenta como **fallo de efectividad de la IA**. Esto no se convierte en PASS por el texto justificativo.

Los estados son PASS, FAIL, NO_FACTIBLE (con su causa verificada) y NO_EVALUADO. Ni NO_FACTIBLE ni NO_EVALUADO cuentan como PASS.

## 5. Resultado 2: efectividad de la IA (criterio PREREGISTRADO, para tu aprobación)

**Población:** las sesiones señaladas para la IA de los 8 usuarios E3/E4 (U03, U04, U08, U09, U13, U14, U18, U19): 4 × 1 + 4 × 2 = **12 sesiones**.

**Factibilidad:** la sonda (`07`) enumera, en la **misma** corrida y antes del primer intento, las modificaciones simples que permite la intervención, y las juzga con los validadores reales.
- En la preparación: **10/12 factibles**.
- U03 S3 y U04 S3 son imposibles por V24/4D y V20.

**Por sesión, en cada corrida R:**

| # | Métrica |
|---|---|
| 1 | Elegible (12) |
| 2 | Factible (sonda) |
| 3 | Propuestas de Replicate (intentos) |
| 4 | Propuestas rechazadas y por qué validador (contrato, V8C, V20, V24…) |
| 5 | Cambio válido publicado por la IA (origen IA) |
| 6 | Cambio pertinente (ACC o ALT del oráculo) |
| 7 | Efectivamente publicado |
| 8 | Conservada (CONS-J o CONS-POL) |
| 9 | Resuelta por el respaldo |

Además: latencia y coste.

**Criterio de éxito (se fija ahora y no cambia):**
- **E-1 (principal).** Adaptaciones efectivas pertinentes: origen IA, publicadas, ACC o ALT, sin ningún incumplimiento S. Se cuentan sobre las sesiones factibles de R y R′ juntas.
  - Hace falta **≥ 80 %** (≥ 16 de 20, con 10 + 10).
  - Y el **límite inferior de Wilson al 95 % ≥ 50 %**.
  - Y **ninguna corrida por debajo de 7/10**.
- **E-2.** En cada corrida, **≥ 6 de los 7 usuarios con alguna sesión factible** reciben al menos una adaptación efectiva pertinente.
- **E-3 (obligatorio).** 0 incumplimientos de seguridad, 0 rutinas inválidas publicadas y los 20 recorridos PASS en R y R′.
- **Aportación frente al determinista.** Se informa EP(R) − EP(D). En D, por diseño, las 12 sesiones terminan en RESPALDO-COPIA: **0 adaptaciones efectivas**.

**Justificación:**
- La corrida histórica aplicó 4 de 8 (50 %), sin límite de dificultad y sin sonda.
- El criterio exige una mejora clara y que la estimación sea compatible con más de la mitad de éxitos incluso en su peor caso estadístico.
- No penaliza a la IA por los casos imposibles, que se informan aparte como NF.
- La variante estricta (solo ACC) se informa también, sin umbral.

Si R/R′ no cumplen el criterio, se informa así y las conclusiones se limitan. No se ajustan ni el prompt ni el criterio.

## 6. Presupuesto propuesto para R/R′ (no autorizado)

- **Llamadas por corrida:** solo llaman los 8 ciclos E3/E4, con hasta 3 intentos cada uno. La semana 2 es toda H → NO_CHANGE, sin llamada. Total: **≤ 24 predicciones por corrida**.
- **Límite por corrida:** **30 predicciones / USD 2**, con un supuesto conservador de 0,05 por predicción; observado ≈ 0,02.
- **R + R′:** 60 / USD 4, dentro de la referencia de 80 / USD 4 por corrida.
- **G (opcional):** 20 gemelos × hasta 3 intentos, ≤ 60 predicciones / USD 3, con su propio límite.
- El arnés cuenta cada predicción **antes** de hacerla y se detiene en el límite. R exige `-Dfitsense.evaluacion.k20.autorizacion-replicate=SI`.
- **Comprobación previa:** `GET /v1/account` = 200 (gratuita).

## 7. Reglas

- **Carpetas:** cada corrida escribe en una carpeta nueva y el arnés se niega a escribir en una carpeta con contenido.
- **Repeticiones:** ninguna repetición selectiva. Un fallo técnico se conserva y la corrida se repite **completa** una sola vez, registrándolo.
- **Prepararación frente a resultados:** la preparación (`evidencia/preparacion/`) **no** es un resultado del artículo. Los resultados definitivos están en `evidencia/final/<condición>`.
- **Seguridad:** un incumplimiento de seguridad en D o D′ detiene la evaluación antes de R.

## 8. Congelación

| Elemento | Valor congelado |
|---|---|
| Versión (`src/main`) | La de `7ee69c2` (#38 + #39 + P3 sobre `76e15c5`) |
| SHA de evaluación | el del commit que contiene este documento (se registra en `12` y en `00_configuracion.txt` de cada corrida) |
| Perfiles | SHA-256 `584e2204ec8eb41ef95bb61b7fe6a6f6c90164a36fe920d1097dcae250ae2307` |
| Política | PLAN-HIST-1 · `MVP-1.8` |
| Prompt | V1, `SessionFixPromptBuilder` blob `91fc686d` |
| Catálogo | 1.219 activos, huella `425fc958d26b405a00a233fe9d8a3c3b` |
| Migraciones | 46, checksum `9981294ded945dc1882bd26f2468c30f` |
| Criterios | este documento y `03` |
| Oráculo | `herramientas/oraculo_k20.py` y `test_oraculo_k20.py`, en ese commit |
| Modelo (R) | `gpt-5-mini` · `openai/gpt-5-structured` · `medium` · 3 intentos · `prefer-wait=25` |
