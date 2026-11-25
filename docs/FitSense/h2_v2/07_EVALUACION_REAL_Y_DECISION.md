# 07 · Evaluación con gpt-5-mini real y decisión

**Orden:** «ORDEN DE EJECUCIÓN — EVALUACIÓN DEFINITIVA H2-v2 CON IA REAL».
- **Opción A**, con un tope de USD 7,50 compartido entre las dos etapas.
- **Autorización:** implementar el arnés y ejecutar pruebas reales. **NO MERGE.**

## Conclusión: **RECHAZADA**

PR-2.0 **no cumple los criterios fijados de antemano** (`05` §4):
- falla el primario P1;
- falla el bloqueante K1.

Por eso **se mantiene PR-1.7** (`fitsense.ai.generacion.prompt-version=PR-1.7`, el valor por omisión) y **no se activa PR-2.0**.

**La etapa 2 no se ejecutó.** La opción A la condiciona a que la etapa 1 pase los criterios («**solo si** pasa los criterios», `05` §3). No pasó, así que quedan **USD 4,3832 sin gastar**.

PR-2.0 sí reduce mucho lo que se propuso corregir:

| Medida | PR-1.7 (A) | PR-2.0 (B) |
|---|---:|---:|
| Casos con rechazo V15 de la mitad o V26 | 6 | 2 |
| Rechazos V26 | 7 | 0 |
| Rechazos V15 de la mitad | 7 | 3 |
| Válidas a la primera | 16 | 18 |

Pero **introduce otra regresión**: en dos perfiles GAIN_MUSCLE de 4 días, el modelo **recorta las dosis** en vez de quitar colocaciones, lo que provoca rechazos V24 y V25 que PR-1.7 no tenía. Ni la mejora ni el empeoramiento son estadísticamente significativos con 25 parejas.

## 1. Comprobaciones previas (antes de la primera predicción)

| # | Comprobación | Resultado |
|---|---|---|
| 1 | PR #85: CI y conflictos | `verify` en verde en `f1d83e13`; `mergeable_state = clean` |
| 2 | La rama contiene gpt-5-mini directo, O20 y V65 | `origin/generation` (`98276a93`, con #83 y #84) es ancestro de la rama |
| 3 | Parámetros | `openai/gpt-5-mini` directo, `reasoning_effort=medium`, `verbosity=low` y `max_completion_tokens=16000`. Los dos arneses lo comprueban al empezar |
| 4 | PR-2.0 apagado por omisión | `fitsense.ai.generacion.prompt-version=${…:PR-1.7}`; la etapa 2 lo comprueba (`promptBuilder.version() == PR-1.7`) |
| 5 | Versiones de prompt | `PlanPromptBuilder.VERSION = PR-1.7` y `VERSION_H2V2 = PR-2.0`. La instantánea lleva `prompt_version` |
| 6 | Perfiles, catálogo, validadores y criterios congelados | El modo `PROMPTS`, sin coste, reconstruye las **52 peticiones idénticas** (caracteres y cota) a `evidencia/prompts_y_cotas_26_casos_PR17_PR20.csv`. K4: el diff de `src/main` frente a `generation` no toca ningún validador, política ni migración (`evaluacion_real/k4_diff_src_main.txt`) |
| 7 | Resultados históricos de H2 | `docs/FitSense/cierre_gpt5_mini_h2/` (PR-1.7 frente a PR-1.9, 13 parejas) y `evidencia/revalidacion_26_salidas_reales.csv` |
| 8 | Control del presupuesto | `LibroPresupuestoH2v2` (§3). Con pruebas unitarias, y probado con Replicate simulado y un tope pequeño |

## 2. Etapa 1: primera propuesta pareada (26 casos, 52 predicciones)

**Condiciones:**
- los mismos casos, perfiles, catálogo, política (`PLAN-REV-2.0`), modelo y validadores (V1–V27) que la evaluación preparada;
- el mismo contexto en los dos brazos;
- orden alterno: casos impares A→B, pares B→A;
- sin reintentos.

**Arnés:** `H2v2PrimeraPropuestaIT`, modo `REAL`, con un tope de etapa de USD 3,20.

**Resultado de la corrida:**
- **52 llamadas**;
- **51 respuestas**;
- **1 error del proveedor:** G21 B quedó cancelada en Replicate tras 177 s, por encima del límite de espera de 150 s;
- ningún corte por presupuesto.

Datos: `evaluacion_real/etapa1/primera_propuesta.csv`, `predict_time.csv`, `registro.csv` y `salidas/`, con la salida y los problemas de cada brazo. Análisis: `etapa1/analisis.md`, generado por `analizar_h2v2.py`.

### 2.1 Perfil por perfil

En las columnas de V15 se dan los rechazos de la regla de la mitad / de cobertura. `predict_time` sale de `GET /v1/predictions/{id}`, sin coste.

| Caso | Objetivo · nivel · lugar · días | A (PR-1.7) | B (PR-2.0) | A V15 | B V15 | A V26 | B V26 | A otros | B otros | A s | B s |
|---|---|---|---|---|---|---:|---:|---|---|---:|---:|
| G01 | GM · BEG · GYM · 3 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 61,1 | 77,9 |
| G02 | GM · BEG · HOME · 4 | rechazada | rechazada | 2/0 | 0/0 | 3 | 0 | — | V24×16, V25 | 53,6 | 74,5 |
| G03 | GM · INT · GYM · 4 | VÁLIDA | **rechazada** | 0/0 | 0/0 | 0 | 0 | — | **V24×24, V25** | 66,7 | 91,3 |
| G04 | GM · INT · HOME · 3 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 60,5 | 75,0 |
| G05 | GM · INT · GYM · 5 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 89,2 | 69,5 |
| G06 | GM · BEG · GYM · 4 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 59,7 | 75,9 |
| G07 | LW · BEG · GYM · 3 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 68,1 | 73,4 |
| G08 | LW · BEG · HOME · 4 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 77,4 | 82,0 |
| G09 | LW · INT · GYM · 4 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 96,0 | 69,6 |
| G10 | LW · INT · HOME · 3 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 74,9 | 86,4 |
| G11 | LW · INT · GYM · 5 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 128,7 | 105,8 |
| G12 | LW · BEG · GYM · 4 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 101,4 | 103,3 |
| G13 | IS · BEG · GYM · 3 | rechazada | **VÁLIDA** | 0/0 | 0/0 | 0 | 0 | V24×3 | — | 98,3 | 79,0 |
| G14 | IS · BEG · HOME · 4 | rechazada | **VÁLIDA** | 1/0 | 0/0 | 0 | 0 | — | — | 89,3 | 87,8 |
| G15 | IS · INT · GYM · 4 | rechazada | rechazada | 2/0 | 2/0 | 3 | 0 | — | — | 98,6 | 122,1 |
| G16 | IS · INT · HOME · 3 | rechazada | rechazada | 0/0 | 0/0 | 1 | 0 | — | V24×3 | 88,8 | 65,6 |
| G17 | IS · INT · GYM · 5 | rechazada | **VÁLIDA** | 1/0 | 0/0 | 0 | 0 | — | — | 139,3 | 166,0 |
| G18 | IS · BEG · GYM · 4 | VÁLIDA | **rechazada** | 0/0 | 1/0 | 0 | 0 | — | — | 82,8 | 74,8 |
| G19 | GF · BEG · GYM · 3 | VÁLIDA | **rechazada** | 0/0 | 0/0 | 0 | 0 | — | V23 | 74,6 | 140,0 |
| G20 | GF · BEG · HOME · 4 | rechazada | **VÁLIDA** | 0/0 | 0/0 | 0 | 0 | V23 | — | 95,2 | 127,4 |
| G21 | GF · INT · GYM · 4 | VÁLIDA | **sin respuesta** (cancelada a los 177 s) | 0/0 | — | 0 | — | — | — | 124,7 | — |
| G22 | GF · INT · HOME · 3 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 63,0 | 99,8 |
| G23 | GF · INT · GYM · 5 | rechazada | rechazada | 1/1 | 0/1 | 0 | 0 | — | — | 88,9 | 134,1 |
| G24 | GF · BEG · GYM · 4 | rechazada | **VÁLIDA** | 0/0 | 0/0 | 0 | 0 | V24×1 | — | 91,8 | 136,7 |
| G25 | GM · INT · GYM · 6 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 52,3 | 123,0 |
| G26 | GM · INT · HOME · 5 | VÁLIDA | VÁLIDA | 0/0 | 0/0 | 0 | 0 | — | — | 52,5 | 56,7 |

GM = GAIN_MUSCLE, LW = LOSE_WEIGHT, IS = INCREASE_STRENGTH, GF = GENERAL_FITNESS. Los ids de predicción de los 52 brazos están en `primera_propuesta.csv` y en `etapa1/analisis.md`.

**G21 queda fuera de la comparación pareada.** No se repitió, porque no puede cambiar el veredicto: P1 ya falla con las otras 25 parejas, y no se repiten llamadas innecesarias. Su reserva cuenta igual. Contarla como un fallo de B no cambia P2 (18 ≥ 17) y solo empeoraría la latencia de B.

### 2.2 Comparación (25 parejas completas)

| Métrica | A · PR-1.7 | B · PR-2.0 |
|---|---:|---:|
| Válidas a la primera | 16 | **18** |
| Casos con V15 de la mitad o V26 | 6 | **2** |
| Casos con V15 de la mitad | 5 | 2 |
| Casos con V15 (cualquiera) | 5 | 3 |
| Casos con V26 | 3 | **0** |
| Rechazos V15 de la mitad | 7 | 3 |
| Rechazos V15 (todos) | 8 | 4 |
| Rechazos V26 | 7 | **0** |
| Casos por otras categorías | V23: 1, V24: 2 | V23: 1, **V24: 3, V25: 2** |
| Mediana de `predict_time` | 82,8 s | 86,4 s |
| Mediana del tiempo en el backend | 84,2 s | 87,8 s |
| Mediana de caracteres de salida | 4.773 | 4.768 |
| Cota reservada | USD 1,4615 | USD 1,4767 |

**Parejas discordantes (McNemar exacta):**
- **V15 de la mitad o V26:** solo en A, 5 casos (G02, G14, G16, G17, G23); solo en B, 1 (G18); p = 0,22.
- **Válida a la primera:** solo A, 3; solo B, 5; p = 0,73.

### 2.3 Criterios fijados de antemano

| # | Criterio | Medido | Resultado |
|---|---|---|---|
| — | Potencia: A con al menos 3 casos de V15 de la mitad o V26 | 6 | Hay potencia: no es inconclusa por construcción |
| **P1** (primario) | B < A **y** B ≤ 1 casos con V15 de la mitad o V26 | 2 frente a 6 | **NO CUMPLE.** B < A sí, pero B = 2 > 1 (G15 y G18) |
| P2 (primario) | Válidas a la primera: B ≥ A | 18 ≥ 16 | CUMPLE |
| **K1** (bloqueante) | Ninguna otra categoría sube más de 1 caso | V23 +0, V24 +1, **V25 +2** | **NO CUMPLE** |
| K2 (bloqueante) | Mediana de `predict_time`: B ≤ A × 1,10 | 86,4 ≤ 91,1 | CUMPLE |
| K3 (bloqueante) | Etapa 2 | — | No se ejecutó (§4) |
| K4 (bloqueante) | Ningún validador ni política cambia | Diff de `src/main` | CUMPLE |

**Regla previa:** «Desfavorable si falla cualquier criterio bloqueante». Falla K1, y además P1, así que el resultado es **desfavorable**. Se mantiene PR-1.7.

### 2.4 Ejemplos concretos

**Donde PR-2.0 mejora:**
- **G02** (GM · principiante · casa · 4 días). PR-1.7 pone 3 de 4 ejercicios de piernas en dos sesiones: «dedica 3 de 4 ejercicios a upper legs» y «suma 9 series directas de upper legs y el techo por sesión es 6». PR-2.0 no tiene ningún rechazo V15 ni V26. Sigue rechazada por otra causa (V24/V25, abajo).
- **G14 y G17** (INCREASE_STRENGTH). PR-1.7 dedica 3 de 4 ejercicios a upper legs en la sesión LOWER_BODY. Con PR-2.0 las dos son **válidas a la primera**.
- **G15** (INCREASE_STRENGTH · intermedio · gimnasio · 4 días). Los 3 rechazos V26 de PR-1.7 desaparecen, pero la regla de la mitad sigue (2 y 2).
- **G13, G20 y G24.** PR-1.7 falla V24 o V23; PR-2.0 es válida. Estos casos no tienen relación directa con el contrato V15/V26.

**Donde PR-2.0 empeora:**
- **G03** (GM · intermedio · gimnasio · 4 días). Con PR-1.7 es válida. Con PR-2.0 hay **24 rechazos V24 y 1 V25**:
  - el modelo pone 3×8 o 2×10 con 60 s de descanso, cuando la dosis exigida es 3×10 con 90 s (HYPERTROPHY);
  - V25 da HYPERTROPHY por omitida, porque ninguna colocación lleva su dosis;
  - es decir, **para no pasarse del techo de series recorta la dosis** en vez de quitar una colocación.

  G02 repite el patrón (16 rechazos V24 y 1 V25).
- **G18** (INCREASE_STRENGTH · principiante · 4 días). Con PR-1.7 es válida; con PR-2.0 hay un rechazo V15 de la mitad: «dedica 3 de 4 ejercicios a upper legs».
- **G19** (GENERAL_FITNESS · principiante · 3 días). Con PR-1.7 es válida; con PR-2.0 la semana no cubre LOWER_HIP (V23).
- **G21.** La única predicción sin respuesta de toda la etapa es de PR-2.0: cancelada a los 177 s.

**Lectura.** El contrato V15/V26 funciona en lo que comunica:
- V26 pasa de 7 rechazos a 0;
- V15 de la mitad pasa de 7 a 3.

Pero el modelo resuelve la tensión entre los techos de V26 y las dosis de V24/4D **bajando dosis**. Eso es justo lo que V24 prohíbe, y su propio mensaje lo dice («si no cabe en el día, quita una colocación en vez de recortar la dosis»). La instrucción es factible: la rutina del motor de reglas cumple todas las cuotas en 26 de 26 perfiles (`04`). Lo que falla es cómo la interpreta el modelo, no un contrato imposible.

### 2.5 Seguridad (O20) y factibilidad

- **O20.** Los 26 perfiles congelados **no tienen exclusiones por dolor**, así que la etapa 1 no ejercita O20.
  - Lo cubre la prueba sin coste `H2v2PipelineSimuladoIT`: con PR-2.0, el ejercicio con dolor no se ofrece y una propuesta que lo trae se rechaza.
  - No se añadieron perfiles: la orden prohíbe alterarlos.
- **Otras reglas.** Ninguna propuesta de ningún brazo tuvo rechazos fuera de V15, V23, V24, V25 y V26.
- **Factibilidad.** Ningún caso fue inviable para la IA en la comprobación previa, ni con A ni con B.

## 3. Presupuesto

**Reglas aplicadas (`LibroPresupuestoH2v2`):**
- un libro único para las dos etapas (`evaluacion_real/libro_presupuesto.csv`), que solo crece;
- antes de **cada** predicción se escribe en disco su reserva: 1 token por byte de entrada más 256, y 16.000 tokens de salida, a USD 0,25 y 2,00 por millón;
- la reserva no se devuelve aunque la predicción falle o se cancele;
- si no cabe, la llamada no se hace;
- en la etapa 1, además, solo se empieza una pareja si caben sus dos brazos;
- una corrida reanudada no repite lo ya recibido.

| | Reservado (cota) |
|---|---:|
| Etapa 1: 52 reservas, 52 predicciones creadas (51 con respuesta y 1 cancelada) | **USD 3,1168** |
| Etapa 2: no ejecutada | USD 0 |
| **Total reservado** | **USD 3,1168 de 7,50** |
| Sin gastar | USD 4,3832 |

**Coste real facturado:** **no verificable desde aquí**. Replicate no devuelve los tokens de gpt-5-mini, ni de salida ni de razonamiento, y el coste solo aparece en su panel de facturación. La reserva es una **cota superior**: la salida visible fue de unos 4.800 caracteres de mediana, muy por debajo de los 16.000 tokens reservados, pero el razonamiento no se ve.

## 4. Etapa 2: arnés listo, no ejecutada

**Por qué no se ejecutó.** El diseño aprobado (opción A, `05` §3) prevé la etapa 2 «**solo si** pasa los criterios» de la etapa 1. Con K1 y P1 fallidos, ejecutarla gastaría hasta USD 4,23 en medir reintentos y respaldo de una versión que ya no puede aprobarse: K3 es bloqueante, pero no compensa otro bloqueante fallido. No se ejecutó para no cambiar la regla después de ver los resultados.

**El arnés está construido y probado sin coste** (`H2v2PipelineCompletoIT`):
- **Camino de producción:** `PlanGenerationOperationService.request → claimNext → execute`, con hasta 3 intentos de IA, reintento corregido, respaldo del motor de reglas y publicación.
- **Lo que registra:**
  - procedencia real (`AI` o `RULE_ENGINE`; un respaldo nunca cuenta como IA);
  - predicciones y problemas de cada intento;
  - tiempo total;
  - revalidación de lo publicado con el validador y el mismo contexto;
  - ejercicios fuera de `available_exercises`.
- **Presupuesto:** antes de cada caso comprueba el peor caso (3 predicciones por brazo); antes de cada predicción reserva su cota exacta y verifica que es la petición enviada.
- **Ensayo con Replicate simulado** (`evaluacion_real/etapa2_simulado/`), en dos pasadas:
  - `ensayo/`, 3 casos con los dos prompts:
    - la salida válida se publica como `AI` / `gpt-5-mini` en 1 predicción;
    - la inválida da 3 rechazos y respaldo `RULE_ENGINE` con modelo nulo;
    - cada petición lleva la versión de su brazo;
    - hay una reserva y un resultado por predicción;
    - lo publicado se revalida como válido.
  - `corte_presupuesto/`, con un tope de USD 0,75: hace el primer caso y deja los demás como `NO_EJECUTADO_PRESUPUESTO`, sin superar nunca el tope.

**Si se quiere medir igualmente**, se puede lanzar sin tocar código: `-Dfitsense.h2v2.e2.modo=REAL -Dfitsense.h2v2.autorizado=SI -Dfitsense.h2v2.tope-usd=4.38`. El libro impide pasar del total de USD 7,50.

## 5. Regresión y CI

| Ejecución | Resultado |
|---|---|
| `mvn verify -Pintegration` completo, local, con los arneses nuevos | Unitarias: **1.635, 0 fallos**, 1 omitida. Integración: **919, 0 fallos**, 74 omitidas: las opt-in, entre ellas los dos arneses pagados. **BUILD SUCCESS** (28 min 18 s) |
| CI de GitHub (`verify`) | En verde en los puntos de control `7103a819` y `2726dea3`; ver la cabeza de la PR |

## 6. Recomendación

1. **No activar PR-2.0.** Se mantiene PR-1.7, que es el valor por omisión, y no hay que tocar nada en Railway.
2. **Mantener la PR #85 abierta y sin fusionar**, como propuesta rechazada en su forma actual.

   No se fusiona como código desactivado sin una decisión expresa. Si se decide conservar el arnés y la evidencia, se pueden separar en otra PR.
3. **Si se quiere otra iteración (PR-2.1):** hay que tratar la causa observada.
   - Qué debería hacer: que el prompt, al hablar del techo de V26, diga explícitamente que **la dosis de 4D/V24 no se recorta nunca** y que se quita una colocación. Hoy eso solo llega en el reintento, con el mensaje de V24.
   - Qué exige: una evaluación nueva con su propio presupuesto, porque los criterios no se reajustan sobre estos datos.
4. **El tamaño de la muestra limita la conclusión.** La mejora en V15/V26 (5 a 1 discordantes, p = 0,22) no es estadísticamente significativa. El empeoramiento en V24/V25 se concentra en 2 perfiles.
