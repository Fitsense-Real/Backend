# 04 · Perfiles sintéticos: resultados y comparación

## 1. Protocolo

- **Perfiles:** los 20 de la **cohorte O**. Es la cohorte de la línea base comparable más reciente con IA real, que incluye generación y adaptación (GRA-R1 y GRA-R2, `evaluacion_integral_O/generacion_ia_real`).
  - El arnés comprueba el sha256 de `O_PERFILES.csv` (`9b996825…`).
  - **No se modificaron** los perfiles, las reglas, el oráculo (v1.4) ni los criterios.
- **Arnés:** `EvaluacionK20IT`, en modo integral. Se ejecutan cuatro semanas medidas por perfil, más el horizonte de su escenario.
- **Bloques:**
  - G: O01–O14, con la progresión apagada;
  - P: O15–O20, con la progresión encendida.
- **Política y adaptación:** `PLAN-REV-2.0` y V2.1, como en GRA.
- **Comandos:** `evidencia/correr_G5M.sh`, pasos `SMOKE`, `G`, `GC`, `P`, `MG`, `MG2` y `MP`.
- **Análisis:** `evidencia/analizar_G5M.py` y el oráculo `oraculo_integral_O_v1_4.py`. El oráculo reproduce **byte a byte** el resultado archivado de GRA-R1 (`REPRODUCE_OK`).

| Serie | Modelo / endpoint | Esfuerzo | Corridas | Perfiles |
|---|---|---|---|---|
| GRA-R1, GRA-R2 (línea base, 06/11/2026) | `gpt-5-mini` vía `openai/gpt-5-structured` | medium | `GRA_R1_G`+`_P` y `GRA_R2_G`+`_P` | 20 |
| **G5M low** | `openai/gpt-5-mini` directo | **low** (inicial de la orden) | `G5M_G` (solo O01–O08: el resto lo cortó el bloqueo I-2), `G5M_G_COMPLEMENTO` (O09–O14), `G5M_P` | 20 |
| **G5M medium** | `openai/gpt-5-mini` directo | **medium** (ajuste por resultados) | `G5M_MEDIUM_G` (O03, O07, O08, O09, O12), `G5M_MEDIUM_G2` (los demás de G), `G5M_MEDIUM_P` | 20 |

O11 no llama a la IA en ninguna serie: el bloqueo previo `FUERZA_SIN_TIEMPO` lo detiene, a propósito. Por eso los porcentajes se calculan sobre 19 perfiles.

## 2. Resultados

| Métrica | GRA-R1 | GRA-R2 | **G5M low** | **G5M medium** |
|---|---|---|---|---|
| Propuestas válidas a la primera | 12/19 | 13/19 | **5/19** | **11/19** |
| Perfiles válidos tras 1, 2 o 3 intentos | 12 / 4 / 3 | 13 / 6 / 0 | 5 / 6 / 2 | 11 / 2 / 5 |
| Propuestas rechazadas por V1–V27 | 10 | 6 | **28** | 15 |
| Motivos de rechazo | V15 ×6, V26 ×6, V23 ×2, V24, V4 | V26 ×5, V15 ×4, V13, V18 | **V24 ×15**, V15 ×12, V20 ×4, V23 ×4, V26 ×3, V25 | V15 ×10, V23 ×5, V26 ×4, V24, V4 |
| **Planes publicados desde la IA** | 19/19 | 19/19 | **13/19** | **18/19** |
| Planes por respaldo determinista (`RULE_ENGINE`) | 0 | 0 | **6** | **1** |
| Llamadas de generación | 29 | 25 | 41 | 33 |
| Errores del proveedor (generación y adaptación) | 0 | 0 | 0 | 0 |
| Respuestas no interpretables (forma del JSON) | 0 | 0 | 0 | 0 |
| Adaptaciones con IA: llamadas → resultado | 10 → 10 APLICADA (AI) | 10 → 9 AI + 1 SIN_EFECTO | 10 → 10 APLICADA (AI) | 9 → 8 AI + 1 SIN_EFECTO |
| Latencia mediana (máx.) por llamada de generación | 75,5 s (145,2) | 66,6 s (148,0) | **29,0 s (47,9)** | 81,1 s (122,9) |
| Latencia mediana (máx.) de la generación por perfil, con intentos | 81,7 s (350,1) | 75,7 s (246,0) | 55,7 s (131,8) | 81,7 s (292,7) |
| Latencia mediana (máx.) por llamada de adaptación | 28,0 s (44,3) | 28,6 s (47,7) | **11,8 s (18,3)** | 29,1 s (42,8) |
| Oráculo v1.4: APROBADO / (APROBADO + FALLO) | 150/150 (100 %) | 148/148 (100 %) | 90/91 en `G5M_G` y 87/87 en `G5M_G_COMPLEMENTO`+`G5M_P`. Ver la nota ¹ | **145/146 (99,3 %)** ² |
| Oráculo: A2 (personalización y restricciones) | 20/20 | 20/20 | Todos los evaluados | 20/20 |
| Oráculo: A10 (seguridad e integridad) | 19/19 | 19/19 | Todos los evaluados | 19/19 |

¹ **Nota sobre `G5M low`:**
- El oráculo se pasó por separado sobre `G5M_G` (14 perfiles, de los que O09–O14 quedaron afectados por el bloqueo I-2) y sobre `G5M_G_COMPLEMENTO` + `G5M_P`.
- Único FALLO: **O04, A5**. Es un DURATION sin series, que el oráculo lee como «más trabajo» aunque el trabajo baja de 696 a 344 s (`05` H-2).

² **Único FALLO de `G5M medium`: O20, A6.**
- La sesión conservada 213 cambia el ejercicio 537 por el 538 con la misma dosis.
- No lo hizo la IA. Es la sustitución determinista **`SUST-SEG-1`** del backend (P2-D, aprobada el 23/10/2026), registrada en `conservation_report.sustituciones` con `regla=SUST-SEG-1` y `motivo=dolor_registrado`.
- El oráculo v1.4 no la modela como paso registrado.
- Solo apareció en esta corrida, porque el ejercicio que dolió estaba también en una sesión conservada. No depende del modelo.

**Por objetivo:**
- **Bloque G** (O01–O14: GAIN_MUSCLE, LOSE_WEIGHT, INCREASE_STRENGTH y GENERAL_FITNESS, con varios escenarios de cumplimiento):
  - con `low`, 5 de los 6 respaldos son de G (O03, O07, O08, O09, O12);
  - con `medium`, 4 de esos 5 pasan a publicarse desde la IA.
- **Bloque P** (O15–O20, progresión y reincorporación): `medium` iguala a GRA, con 4/6 válidas a la primera y 6/6 desde la IA, frente a 2/6 y 5/6 con `low`.
- **Subconjunto de los 5 perfiles G que acabaron en respaldo con `low`:**
  - `low`: 0/5 desde la IA;
  - `medium`: 4/5 desde la IA, con V24 bajando de 6 a 1;
  - GRA: 5/5 en las dos corridas.

**Cumplimiento de restricciones:**
- Ningún plan publicado infringe V1–V27: los validadores se ejecutan antes de publicar y ninguno se relajó.
- El oráculo independiente no encuentra infracciones de personalización (A2) ni de seguridad (A10) en ninguna serie.

## 3. Tiempo y coste

**Coste medido de GRA:** tokens informados por Replicate, a 0,25 USD por millón de entrada y 2,00 USD por millón de salida.

**Coste de G5M:** gpt-5-mini directo **no informa tokens** (`01`), así que hay dos cifras:
- la **cota** que cargó el control de presupuesto: un token por byte UTF-8 de entrada, más 256, más 16.000 de salida;
- un **mínimo estimado**: tokens de entrada por caracteres con la razón de GRA (3,44 caracteres por token, mismos prompts), más la salida visible. **No incluye el razonamiento.**

| | GRA-R1 | GRA-R2 | G5M low | G5M medium |
|---|---|---|---|---|
| Llamadas (generación + adaptación) | 39 | 35 | 51 | 42 |
| Coste | **0,8218** medido | **0,6810** medido | cota 3,3241 · mínimo 0,6117 | cota 2,6717 · mínimo 0,4914 |
| Por llamada | 0,0211 | 0,0195 | cota 0,0652 · mínimo 0,0120 | cota 0,0636 · mínimo 0,0117 |

- **El gasto real de G5M está entre el mínimo y la cota.** Con `medium`, el razonamiento lo factura OpenAI como salida, a 2,00 USD por millón.
  - Si gpt-5-mini razonara como gpt-5-mini en GRA, que usaba el mismo modelo con `medium` y facturaba entre 3.700 y 11.400 tokens de salida, el coste por llamada sería **del mismo orden que en GRA** (≈ 0,02 USD).
  - **No se pudo verificar desde la sesión.** Se comprueba en el panel de Replicate, en Billing.
- **Salida visible máxima:** 7.845 caracteres, del orden de 2.300 tokens, muy por debajo de `max_completion_tokens=16.000`. Ninguna respuesta se cortó.

## 4. Conclusión y ajuste

1. **`low`, el valor inicial de la orden, no basta para la generación inicial.**
   - Recorta dosis (V24) y reparte peor los grupos (V15).
   - Resultado: 5/19 propuestas válidas a la primera y 6/19 planes por respaldo.
   - Es más del doble de rápido (29 s por llamada) y adapta bien (10/10).
2. **`medium` recupera la calidad de la línea base:** 11/19 válidas a la primera, 18/19 desde la IA y el oráculo al 99,3 %, con un tiempo similar al de GRA.
3. **Ajuste aplicado (por resultados medidos, como prevé la orden):**
   - `fitsense.ai.reasoning-effort` queda en **`medium`** por omisión;
   - `verbosity=low` y `max_completion_tokens=16000` no cambian;
   - `low` sigue disponible con `FITSENSE_AI_REASONING_EFFORT=low` para comparar.
4. **No se cambió** ninguna regla, ningún validador ni ningún prompt de negocio para favorecer al modelo.

**Límites de esta comparación:** ver `05` §4. Hay una sola repetición por configuración, y la serie `medium` se compone de tres corridas.
