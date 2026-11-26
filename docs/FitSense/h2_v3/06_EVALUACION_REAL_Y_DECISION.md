# 06 · Evaluación con gpt-5-mini real de PR-2.1 y decisión

**Orden:** «ORDEN DE EJECUCIÓN — EVALUACIÓN REAL DE H2-v3 (PR-2.1)».
- **Opción A**, con un tope total de USD 7,60.
- **Autorización:** ejecutar las pruebas reales y documentar. **NO MERGE.**

## Conclusión: **RECHAZADA**

PR-2.1 **incumple el criterio bloqueante K2** (latencia).

| Brazo | Mediana de `predict_time` |
|---|---:|
| PR-1.7 (A) | 54,4 s |
| PR-2.1 (B) | **61,0 s** |
| Máximo permitido (A × 1,10) | 59,8 s |

La regla fijada de antemano (`04` §3) dice: «RECHAZADA si falla cualquier bloqueante». Por eso:
- **se mantiene PR-1.7** como versión vigente (el valor por omisión);
- PR-2.1 queda desactivada;
- **la etapa 2 no se ejecutó**: estaba condicionada a aprobar la etapa 1;
- quedan **USD 4,4673 sin gastar**.

**Todos los demás criterios se cumplen**, y en validez la mejora es clara:

| | PR-1.7 (A) | PR-2.1 (B) |
|---|---:|---:|
| Válidas al primer intento | 18 de 26 | **23 de 26** |
| Casos con V15 de la mitad o V26 | 4 | **1** |
| Rechazos V26 | 5 | **0** |
| Casos con V24 o V25 (la regresión de PR-2.0) | 1 | **0** |
| Casos con otras reglas (V23, V24) | 3 | **0** |
| Cancelaciones y errores del proveedor | 0 | 0 |

Ninguna diferencia de validez es estadísticamente significativa con 26 parejas (McNemar: p = 0,25 y p = 0,18). **H2 no se declara resuelta.**

## 1. Verificaciones antes de gastar (orden §2)

| # | Verificación | Resultado |
|---|---|---|
| 1 | PR #86 | Abierta, `mergeable_state = clean`, `verify` en verde en `3fbda299` |
| 2 | gpt-5-mini directo, O20 y V65 | `origin/generation` es ancestro de la rama |
| 3 | PR-1.7 por omisión | `fitsense.ai.generacion.prompt-version=${…:PR-1.7}` |
| 4 | PR-2.1 solo por configuración explícita; la adaptación no la lee | Ningún archivo de adaptación lee la propiedad, que tiene la suya propia (`fitsense.ai.adaptation.prompt-version`) |
| 5 | PR-1.7 y PR-2.0 iguales a los históricos | Modo `PROMPTS`: **52/52 idénticos** en caracteres y cota |
| 6 | Perfiles, catálogo, `PLAN-REV-2.0`, validadores y contexto congelados | Los mismos 26 casos y la misma semana (23/11/2026). El diff de `src/main` no toca validadores ni políticas (K4) |
| 7 | Criterios programados antes de ejecutar | `analizar_h2v3.py`: P1, P2, K1–K6 y S1, en el commit `4dcd3773` y antes de la primera predicción (`5d29e302`) |
| 8 | Arneses sin coste | Etapa 1 en modo `PROMPTS`; etapa 2 en modo `SIMULADO` con PR-2.1 |
| 9 | Presupuesto y reanudación | Libro propio con su tope en la cabecera (`# tope_total_usd=7.60`), que no se puede cambiar (prueba unitaria). La reanudación ya se había probado en la evaluación de H2-v2 |
| 10 | Credenciales | El proxy del entorno pone el token. El repositorio solo contiene el marcador `proxy` y ningún `r8_…` |

**Corrección antes de gastar.** La cota documentada de PR-2.1 (USD 3,1311) era de antes de los dos últimos ajustes de la regla 22. Se regeneró a **USD 3,1327** y se fijó en el commit `5d29e302`, antes de la primera predicción.

## 2. Etapa 1: primera propuesta pareada

**Condiciones:**
- 26 perfiles, **52 predicciones nuevas** (ninguna salida antigua reutilizada);
- `openai/gpt-5-mini` directo, `medium`, `low` y 16.000 tokens;
- orden alterno (impar A→B, par B→A);
- sin reintentos.

**Resultado de la corrida:** 52 respuestas `succeeded`, 0 cancelaciones, 0 errores y ningún corte por presupuesto.

**Datos:** en `evaluacion_real/etapa1/`:
- `primera_propuesta.csv`, `registro.csv` y `predict_time.csv`;
- `salidas/`, con la salida y los problemas de cada brazo;
- `analisis.md`.

**Revalidación sin coste:** el modo `VALIDAR` sobre las 52 salidas da **52/52 veredictos idénticos** (`revalidacion_sin_coste.csv`).

### 2.1 Perfil por perfil

En las columnas de V15 se dan los rechazos de la regla de la mitad / de cobertura de grupos. El tiempo es el `predict_time` de Replicate.

| Caso | Objetivo · nivel · lugar · días | A (PR-1.7) | B (PR-2.1) | A V15 | B V15 | A V26 | B V26 | A otros | B otros | A s | B s |
|---|---|---|---|---|---|---:|---:|---|---|---:|---:|
| G01 | GM · BEG · GYM · 3 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 34,4 | 59,2 |
| G02 | GM · BEG · HOME · 4 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 44,3 | 40,8 |
| G03 | GM · INT · GYM · 4 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 57,0 | 65,6 |
| G04 | GM · INT · HOME · 3 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 37,4 | 45,5 |
| G05 | GM · INT · GYM · 5 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 54,7 | 62,7 |
| G06 | GM · BEG · GYM · 4 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 40,0 | 52,5 |
| G07 | LW · BEG · GYM · 3 | rechazada | **VÁLIDA** | 0/0 | 0/0 | 0 | 0 | V24 | — | 29,8 | 45,6 |
| G08 | LW · BEG · HOME · 4 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 52,1 | 42,3 |
| G09 | LW · INT · GYM · 4 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 39,3 | 36,3 |
| G10 | LW · INT · HOME · 3 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 39,5 | 32,5 |
| G11 | LW · INT · GYM · 5 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 62,9 | 58,2 |
| G12 | LW · BEG · GYM · 4 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 48,6 | 45,7 |
| G13 | IS · BEG · GYM · 3 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 54,1 | 43,4 |
| G14 | IS · BEG · HOME · 4 | rechazada | **VÁLIDA** | 2/0 | 0/0 | 0 | 0 | — | — | 49,3 | 51,0 |
| G15 | IS · INT · GYM · 4 | rechazada | **VÁLIDA** | 1/0 | 0/0 | 0 | 0 | — | — | 61,2 | 117,5 |
| G16 | IS · INT · HOME · 3 | rechazada | **VÁLIDA** | 0/0 | 0/0 | 1 | 0 | — | — | 62,0 | 68,1 |
| G17 | IS · INT · GYM · 5 | rechazada | rechazada | 1/0 | 2/0 | 4 | 0 | — | — | 75,0 | 78,7 |
| G18 | IS · BEG · GYM · 4 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 52,9 | 100,5 |
| G19 | GF · BEG · GYM · 3 | rechazada | **VÁLIDA** | 0/0 | 0/0 | 0 | 0 | V23 | — | 56,4 | 78,7 |
| G20 | GF · BEG · HOME · 4 | rechazada | **VÁLIDA** | 0/1 | 0/0 | 0 | 0 | — | — | 70,5 | 77,8 |
| G21 | GF · INT · GYM · 4 | VÁLIDA | **rechazada** | 0/0 | 0/1 | 0 | 0 | — | — | 95,6 | 102,2 |
| G22 | GF · INT · HOME · 3 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 84,3 | 79,9 |
| G23 | GF · INT · GYM · 5 | rechazada | **VÁLIDA** | 0/2 | 0/0 | 0 | 0 | V23 | — | 75,1 | 81,5 |
| G24 | GF · BEG · GYM · 4 | VÁLIDA | **rechazada** | 0/0 | 0/1 | 0 | 0 | — | — | 69,0 | 64,4 |
| G25 | GM · INT · GYM · 6 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 60,2 | 55,7 |
| G26 | GM · INT · HOME · 5 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 48,1 | 66,2 |

GM = GAIN_MUSCLE, LW = LOSE_WEIGHT, IS = INCREASE_STRENGTH, GF = GENERAL_FITNESS. Los 52 ids de predicción están en `etapa1/analisis.md` y `primera_propuesta.csv`.

### 2.2 Comparación (26 parejas completas)

| Métrica | A · PR-1.7 | B · PR-2.1 |
|---|---:|---:|
| Válidas al primer intento | 18 | **23** |
| Casos con V15 de la mitad o V26 | 4 | **1** |
| Casos con V15 de la mitad | 3 | 1 |
| Casos con V15, cualquiera (mitad o mínimo de grupos) | 5 | 3 |
| Casos con V26 | 2 | **0** |
| Rechazos V15 de la mitad | 4 | 2 |
| Rechazos V15, todos | 7 | 4 |
| Rechazos V26 | 5 | **0** |
| Casos con V24 o V25 | 1 | **0** |
| Casos por otras categorías | V23: 2, V24: 1 | **ninguno** |
| Cancelaciones y errores del proveedor | 0 | 0 |
| **Mediana de `predict_time`** | **54,4 s** | **61,0 s** |
| Mediana del tiempo en el backend | 56,0 s | 61,7 s |
| Mediana de caracteres de salida | 4.789 | 4.385 |
| Cota reservada | USD 1,5504 | USD 1,5822 |

### 2.3 Criterios fijados de antemano

| # | Criterio | Medido | Resultado |
|---|---|---|---|
| — | Potencia: A con al menos 3 casos de V15 de la mitad o V26 | 4 | Hay potencia: no es inconclusa |
| **P1** | B < A **y** B ≤ 1 casos con V15 de la mitad o V26 | 1 frente a 4 | **CUMPLE** |
| **P2** | Válidas a la primera: B ≥ A | 23 ≥ 18 | **CUMPLE** |
| K1 | Ninguna otra categoría sube más de 1 caso | V23 −2, V24 −1 | CUMPLE |
| **K2** | Mediana de `predict_time`: B ≤ A × 1,10 | 61,0 > 59,8 | **NO CUMPLE** |
| K3 | Etapa 2 | — | No se ejecutó (§3) |
| K4 | Ningún validador ni política cambia | Diff de `src/main`: solo el prompt, la instantánea y cómo viaja la versión | CUMPLE |
| K5 | Casos con V24 o V25: B ≤ A | 0 ≤ 1 | CUMPLE |
| K6 | Errores del proveedor: B ≤ A + 1 | 0 ≤ 1 | CUMPLE |
| S1 (informativo) | B frente a PR-2.0 histórico en V15 de la mitad o V26 | 1 frente a 2 | Conserva y mejora la corrección de PR-2.0 |

**Estadística** (McNemar exacta, informativa):

| Comparación | Discordantes solo en A | Discordantes solo en B | p |
|---|---|---|---:|
| V15 de la mitad o V26 | 3 (G14, G15, G16) | 0 | 0,25 |
| Válida a la primera | 2 (G21, G24) | 7 (G07, G14, G15, G16, G19, G20, G23) | 0,18 |

### 2.4 Latencia: qué dice el dato

- **K2 (el criterio fijado):** compara la mediana de cada brazo. 61,0 / 54,4 = **1,121**, por encima del 1,10 permitido.
- **Lectura pareada** (informativa, **no cambia el veredicto**):
  - B es más lento en 16 de 26 parejas;
  - la diferencia B − A tiene mediana +6,3 s y media +7,6 s;
  - la mediana del cociente por pareja es 1,078.
- **Por orden de ejecución:**
  - cuando A va primero, las medianas son A 57,0 s y B 62,7 s;
  - cuando B va primero, A 49,3 s y B 52,5 s.
  - B es más lento en los dos órdenes, así que no es un efecto del orden.
- **Contexto del día:** las latencias fueron mucho menores que el 25/11 (mediana de 87,1 s en la evaluación de PR-2.0), es decir, Replicate estaba menos cargado. El criterio es relativo y cada pareja se pidió seguida, así que la comparación es válida.
- **Causas posibles,** no aisladas: el prompt tiene unos 2.400 caracteres más, y la jerarquía y la comprobación final probablemente piden más razonamiento al modelo. La salida visible fue algo más corta (4.385 frente a 4.789 caracteres).
- **Tiempo hasta un plan válido** (estimación informativa, **no es un criterio**):
  - con las tasas de validez observadas y suponiendo la misma tasa en los reintentos, el número esperado de predicciones por generación sería ≈1,40 con A y ≈1,13 con B;
  - eso da ≈76 s con A y ≈69 s con B hasta un plan válido de la IA;
  - medir esto era el propósito de la etapa 2, que no se ejecutó por la regla previa.

## 3. Etapa 2: no se ejecutó

La orden (§6) y el protocolo (`04` §2) solo la permiten con la etapa 1 aprobada, y K2 es bloqueante. **No se hizo ninguna predicción de la etapa 2**, para no gastar presupuesto midiendo una versión que ya no se puede aprobar.

El arnés sigue listo y probado (`evidencia/etapa2_simulado_PR21/`).

## 4. Presupuesto

**Libro de H2-v3:** `evaluacion_real/libro_presupuesto.csv`, con el tope en la cabecera (`# tope_total_usd=7.60`). Es independiente del de H2-v2 y no usa su saldo.

| | Reservado (cota) |
|---|---:|
| Etapa 1: 52 reservas y 52 predicciones creadas, todas `succeeded` | **USD 3,1327** |
| Etapa 2: no ejecutada | USD 0 |
| **Total reservado** | **USD 3,1327 de 7,60** |
| Sin gastar | USD 4,4673 |

- **Reglas aplicadas:** cada reserva se apuntó **antes** de crear su predicción. No hubo devoluciones, repeticiones ni cortes.
- **Coste real facturado:** **no verificable desde aquí**. Replicate no devuelve los tokens de gpt-5-mini, ni de salida ni de razonamiento, y el coste solo aparece en su panel de facturación. La reserva es una cota superior.
- **Predicciones fallidas o canceladas:** ninguna.

## 5. Mejoras y regresiones

**Casos que mejoran con PR-2.1** (rechazados con A, válidos con B):
- **G14, G15** (IS, 4 días): A pone 3 de 4 ejercicios de piernas en LOWER_BODY (V15); B reparte.
- **G16** (IS, casa, 3 días): A pasa el techo de V26; B no.
- **G07** (LW, principiante): A no lleva la dosis de V24; B sí.
- **G19 y G23** (GF): A deja la semana sin un slot (V23) o con menos grupos de los exigidos en una sesión (V15, mínimo de grupos); B cumple.
- **G20** (GF, casa): A, mínimo de grupos de V15; B válida.
- **G17** sigue rechazado en los dos brazos, pero B elimina los 4 rechazos V26 de A y le quedan 2 de la mitad de V15 (3 de 5 en pecho en PUSH y 3 de 4 en piernas en LOWER_BODY).

**Casos que empeoran:**
- **G21 y G24** (GENERAL_FITNESS, 4 días): con B, la sesión LOWER_BODY solo trabaja `upper legs`: «debe cubrir al menos 2 grupos distintos de: [upper legs, lower legs, waist]».
  - Es el mismo patrón de C11. En GENERAL_FITNESS, `waist` y `lower legs` tienen prioridad 0 y no se envían, y el modelo los deja fuera aunque la regla 21 diga que no están prohibidos.
  - **Regresión residual:** la jerarquía ya lo dice (22.3: «cada sesión cubre al menos `min_body_parts` grupos») y el modelo no lo sigue en estos dos casos.

**Regresiones de PR-2.0 corregidas:**
- **V24/V25 (G02, G03):** 0 casos con B, frente a 3 de PR-2.0 el 25/11.
- **V15 de G15 y G18:** válidas con B.
- **V23 de G19:** válida con B.

**Seguridad:**
- ninguna propuesta de ningún brazo usa ejercicios fuera de `available_exercises`;
- no hay rechazos de reglas distintas de V15, V23, V24 y V26;
- O20 no se ejercita, porque los perfiles congelados no tienen dolor; está cubierto con Replicate simulado (`03`).

## 6. Limitaciones

1. **Muestra de 26 parejas:** ninguna diferencia de validez es significativa (p = 0,18 y 0,25).
2. **K2 se mide sobre la primera propuesta.** No captura el ahorro de reintentos, que solo mide la etapa 2, y la etapa 2 no se ejecutó por la regla previa.
3. **Un solo día de medición.** La latencia depende de la carga de Replicate. La comparación es pareada y contemporánea, pero el margen de K2 es pequeño: 61,0 frente a 59,8 s.
4. **Coste real no verificable.**
5. **O20 no se ejercita con IA real.**

## 7. Validación técnica posterior (orden §11)

| Comprobación | Resultado |
|---|---|
| Regresión unitaria completa | **1.636, 0 fallos**, 1 omitida |
| Regresión de integración completa | **927, 0 fallos**, 74 omitidas (las opcionales, entre ellas los arneses pagados). BUILD SUCCESS (33 min 15 s) |
| Compatibilidad con O20 | `H2v3PipelineSimuladoIT`, 6/6 (el ejercicio con dolor no se ofrece y no se publica con PR-2.1), y las pruebas de O20 de la suite en verde |
| PR-1.7 por omisión | `fitsense.ai.generacion.prompt-version=${FITSENSE_AI_GENERACION_PROMPT_VERSION:PR-1.7}` |
| Libro y tope | `LibroPresupuestoH2v2Test`, 3/3 |
| CI de GitHub | `verify` en verde en `a29a66a4`; la PR no tiene conflictos |
| Resultados históricos | Sin cambios: `h2_v2/evaluacion_real/` intacto |

## 8. Recomendación

1. **Se mantiene PR-1.7** y **no se activa PR-2.1**. No hay que tocar nada en Railway.
2. **PR #86 sigue abierta y sin fusionar**, como propuesta rechazada en esta evaluación. No se recomienda el merge.
3. **Si se decide otra iteración**, hay dos frentes, cada uno con una evaluación nueva y su propia autorización:
   - **latencia:** acortar el texto añadido (la regla 22 y la comprobación final repiten en parte las reglas 15 y 21) y volver a medir K2;
   - **reparto de LOWER_BODY en GENERAL_FITNESS:** que la instantánea o la comprobación nombren los grupos que faltan para el mínimo (G21, G24).
4. **Criterio de latencia.** Si se quisiera cambiar K2, por ejemplo para medir el tiempo hasta un plan válido en lugar de la primera propuesta, sería **un cambio de protocolo decidido después de ver estos datos**. Tendría que justificarse por separado y aplicarse solo a una evaluación nueva, nunca para reinterpretar esta.
