# 06 · Evaluación real de PR-2.2 (opción C) y decisión

**Orden:** «ORDEN DE EJECUCIÓN — EVALUACIÓN REAL H2-v4 (PR-2.2)», opción C, tope **USD 10,80**. **NO MERGE. NO DESPLIEGUE.**
**Fecha:** 26/11/2026, 19:04–21:12 UTC. `openai/gpt-5-mini` directo, `medium`, `low`, 16.000 tokens; brazos PR-1.7 (A) y PR-2.2 (B).
**Reglas de análisis:** fijadas, programadas y subidas **antes** de la primera llamada pagada (`04` §7, commit `4d1daddd`). No se cambiaron después.

## Resultado en una línea

**RECHAZADA en la etapa 1.** Fallan:
- **P1** (primario): 3 perfiles de B con V15 de la mitad, frente a un máximo de 1;
- **K2** (bloqueante): mediana de `predict_time` de 75,9 s en B, frente a un límite de 72,9 s.

PR-2.2 **mejora mucho la validez a la primera:** 45 de 51 frente a 29, con 12 perfiles mejor y 1 peor (p = 0,0034). También **elimina V24, V25 y V26.**

Pero no cumple los dos criterios. Por la regla previa, **la etapa 2 no se ejecutó.** **H2 no queda resuelta.**

## Las seis preguntas

| # | Pregunta | Respuesta |
|---|---|---|
| 1 | ¿Aumenta los planes válidos a la primera? | **Sí.** 45 frente a 29 en las 51 parejas completas (A: 30 de 52 contando G08 r2). Por perfil: 12 mejoran y 1 empeora (G24); signos exacta p = 0,0034. Las dos réplicas, por separado: 23 frente a 14 y 22 frente a 15 |
| 2 | ¿Reduce V15 y V26? | **Reduce, pero no lo bastante.** V26: de 6 perfiles (10 predicciones) a **0**. V15 de la mitad o V26: de 8 perfiles a 3 (G14, G17, G18), con un solo rechazo cada uno; McNemar p = 0,0625. **P1 exige ≤ 1: no se cumple** |
| 3 | ¿Mantiene las correcciones de V23, V24 y V25? | **Sí.** V24 o V25: de 3 perfiles a 0 (K5). V23: de 4 a 1 (G19 r2) |
| 4 | ¿Corrige G17, G21 y G24? | **No del todo: cada uno falla en una de sus dos réplicas.**<br>- G17: r1 con V15 de la mitad (LOWER_BODY), r2 válida.<br>- G21: r1 válida, r2 con V15 del mínimo de grupos.<br>- G24: r1 con V15 de grupos, r2 válida. **A tenía G24 válida en las dos** |
| 5 | ¿Cumple K2? | **No.** Mediana de B 75,9 s frente a A 66,3 s × 1,10 = 72,9 s. Pareada: B − A = +8,0 s; B / A = 1,131; B más lento en 32 de 51 parejas y en 19 de 26 perfiles (signos p = 0,029) |
| 6 | ¿Reduce reintentos y el respaldo `RULE_ENGINE`? | **No medido:** la etapa 2 no se ejecutó, por la regla. Como indicio, que no decide: B necesitaría reintento en 6/51 primeras propuestas (más 1 error), y A en 22/52 |

## 1. Las 104 predicciones

| | Réplica 1 (G01→G26) | Réplica 2 (G26→G01, brazos invertidos) | Total |
|---|---|---|---|
| Pedidas | 52 | 52 | **104** |
| Con respuesta | 52 | 51 | **103** |
| Error del proveedor | 0 | 1 (G08, brazo B) | 1 |
| Sin ejecutar por presupuesto | 0 | 0 | 0 |
| Reservado | USD 3,1154 | USD 3,1154 | **USD 6,2308** |

**Revalidación sin coste** (modo `VALIDAR`, mismo validador y contexto) sobre las 103 salidas guardadas:
- **103/103 coinciden** con lo registrado en la corrida: estado, validez, V15 y otros (`etapa1/revalidacion_sin_coste.csv`);
- ninguna salida usa ejercicios fuera de `available_exercises` (`etapa1/metricas_por_prediccion.csv`).

**El error de G08 r2 B:**
- El backend no pudo leer la respuesta del POST: `content type [application/octet-stream]`, sin id.
- Un listado gratuito de Replicate muestra que la predicción **sí se creó y terminó**: `g40bmfgq8nrzr0d236ktnedbfm`, `succeeded`, 55,0 s. Se cuenta como **probablemente facturada**.
- Por la regla previa, cuenta en K6 y no se repitió.
- Recuperada por GET y validada **solo como sensibilidad**, resulta **válida**. El veredicto no cambia: K2 75,2 frente a 72,9 s, y P1 3 frente a 8 (`etapa1/sensibilidad_G08_r2_B/`).

## 2. Tabla por perfil, brazo y réplica

`✓` válida, `✗` rechazada. `V15m` es V15 de la mitad y `V15g` el mínimo de grupos; ×n es el número de rechazos. Se da el `predict_time`.

| Perfil | A r1 | A r2 | B r1 | B r2 |
|---|---|---|---|---|
| G01 | ✓ 49 s | ✓ 44 s | ✓ 49 s | ✓ 71 s |
| G02 | ✗ V15m×2 V26×3 · 60 s | ✗ V15m×1 V26×2 · 57 s | ✓ 68 s | ✓ 78 s |
| G03 | ✓ 57 s | ✓ 52 s | ✓ 58 s | ✓ 58 s |
| G04 | ✓ 48 s | ✓ 36 s | ✓ 64 s | ✓ 45 s |
| G05 | ✗ V23×1 · 69 s | ✓ 60 s | ✓ 61 s | ✓ 69 s |
| G06 | ✗ V15g×1 V26×2 · 45 s | ✗ V15m×1 V26×2 · 50 s | ✓ 77 s | ✓ 71 s |
| G07 | ✗ V24×6 · 46 s | ✓ 40 s | ✓ 44 s | ✓ 44 s |
| G08 | ✓ 59 s | ✓ 63 s | ✓ 56 s | error proveedor |
| G09 | ✓ 63 s | ✓ 60 s | ✓ 61 s | ✓ 44 s |
| G10 | ✓ 41 s | ✓ 45 s | ✓ 51 s | ✓ 51 s |
| G11 | ✓ 71 s | ✓ 71 s | ✓ 76 s | ✓ 62 s |
| G12 | ✓ 77 s | ✓ 61 s | ✓ 55 s | ✓ 53 s |
| G13 | ✓ 61 s | ✓ 63 s | ✓ 85 s | ✓ 51 s |
| G14 | ✗ V15m×2 V26×3 · 84 s | ✗ V15m×2 V26×3 · 60 s | ✓ 75 s | ✗ V15m×1 · 76 s |
| G15 | ✗ V15m×2 · 71 s | ✗ V15m×3 V26×4 · 68 s | ✓ 97 s | ✓ 110 s |
| G16 | ✓ 73 s | ✓ 66 s | ✓ 83 s | ✓ 62 s |
| G17 | ✗ V24×1 · 59 s | ✗ V15m×2 V26×5 · 89 s | ✗ V15m×1 · 104 s | ✓ 73 s |
| G18 | ✗ V15m×2 V26×3 · 68 s | ✗ V15m×4 V26×3 · 95 s | ✗ V15m×1 · 76 s | ✓ 78 s |
| G19 | ✗ V23×1 · 59 s | ✗ V15m×1 · 70 s | ✓ 82 s | ✗ V15g×1 V23×1 · 68 s |
| G20 | ✗ V15g×1 V23×1 · 72 s | ✗ V15g×1 V23×1 · 68 s | ✓ 103 s | ✓ 93 s |
| G21 | ✗ V15g×1 · 85 s | ✗ V15m×1 · 94 s | ✓ 110 s | ✗ V15g×1 · 109 s |
| G22 | ✗ V23×1 V24×3 · 97 s | ✗ V23×1 V24×3 · 79 s | ✓ 80 s | ✓ 92 s |
| G23 | ✓ 101 s | ✓ 111 s | ✓ 141 s | ✓ 80 s |
| G24 | ✓ 81 s | ✓ 120 s | ✗ V15g×1 · 90 s | ✓ 114 s |
| G25 | ✓ 80 s | ✓ 68 s | ✓ 109 s | ✓ 100 s |
| G26 | ✓ 66 s | ✓ 112 s | ✓ 82 s | ✓ 86 s |

Detalle completo por predicción: `etapa1/analisis.md` y `etapa1/primera_propuesta.csv`, con id, estado, tiempos, cota y rechazos.

## 3–6. Validez, V15/V26, V23/V24/V25 y casos especiales

Unidad: perfiles con la categoría en **alguna** réplica (`04` §7).

| Categoría | A · PR-1.7 | B · PR-2.2 |
|---|---:|---:|
| Válidas a la primera (parejas completas) | 29 | **45** |
| V15 de la mitad o V26 | 8 (G02, G06, G14, G15, G17, G18, G19, G21) | **3** (G14, G17, G18) |
| — de ellos, con V26 | 6 (10 predicciones) | **0** |
| V24 o V25 (K5) | 3 (G07, G17, G22) | **0** |
| V23 | 4 (G05, G19, G20, G22) | 1 (G19) |
| V15 del mínimo de grupos (informativo; no entra en K1, igual que en H2-v3) | 3 (G06, G20, G21) | 3 (G19, G21, G24) |

**Casos de la orden:**

| Caso | Qué se mira | Resultado B (PR-2.2) |
|---|---|---|
| **G02, G03** | Sin V24/V25 | **Sí**, en las dos réplicas. G02 pasa de 2 rechazos en A a 2 válidas en B. G03, válida en los dos brazos |
| **G15, G18** | Reparto | **G15:** 2/2 válidas (A 0/2).<br>**G18:** r1 con V15 de la mitad (`upper legs` 3 de 4 en LOWER_BODY), r2 válida (A 0/2) |
| **G19** | LOWER_HIP (V23) | r1 válida; r2 con V23 y V15 de grupos (A 0/2) |
| **G17** | La mitad | r1 con V15 de la mitad (LOWER_BODY, `upper legs` 3 de 4), r2 válida |
| **G21, G24** | Grupos en LOWER_BODY (los complementos no cuentan) | **G21:** r1 válida, r2 con `upper legs` en las 3 de trabajo.<br>**G24:** r1 con `upper legs` en las 2 de trabajo, r2 válida |

**Dosis, reparto, cobertura y duración** (`etapa1/metricas_por_prediccion.csv`, sin coste):
- Mediana de series de trabajo por semana: A 36, B 30. Ejercicios por semana: A 18, B 16.
- Patrones cubiertos: mediana 8 en los dos brazos.
- Duración prevista máxima por sesión: 60 min en los dos.
- Ninguna dosis fuera de lo autorizado: 0 rechazos V24 y V25 en B.

## 7–8. Latencia y variabilidad

| | A · PR-1.7 | B · PR-2.2 |
|---|---:|---:|
| Mediana de `predict_time`, todas las respuestas (K2) | 66,3 s (n = 52) | **75,9 s** (n = 51) |
| Réplica 1 / réplica 2 | 67,3 / 66,4 s | 76,7 / 71,1 s |
| Brazo pedido primero / segundo | 63,2 / 68,1 s | 72,1 / 77,5 s |
| Mediana por perfil: rango (IQR) | 42,0–106,0 s (57,7–76,2) | 44,3–110,7 s (57,6–91,1) |
| Diferencia entre las dos réplicas de un perfil (mediana) | 7,7 s | 10,4 s |
| Tiempo en el backend: mediana, máximo y número > 90 s | 68,1 s · 121,7 s · 8 | 76,6 s · 142,1 s · 13 |
| Entrada (mediana de caracteres) | 100.969 | 103.032 |

**Lectura:**
- B es más lento en **las dos réplicas y en los dos órdenes**, así que no es un efecto del orden ni de una franja de carga.
- La entrada de B es solo unos 2.000 caracteres mayor. Con la pendiente de `01` §2 (≤ 1,72 s por 10.000 caracteres), eso explica **menos de 0,4 s** de los +8 s.
- El exceso viene del trabajo del modelo con las instrucciones de PR-2.2, no del tamaño del texto. Replicate no da tokens, así que no se puede separar el razonamiento.
- El día fue más lento en general: A dio 66,3 s, frente a 54,4 s el 26/11 por la mañana. K2 compara dentro del mismo día, así que eso no lo invalida.

## 9. Criterios (P1, P2, K1–K6)

| # | Regla (`04` §3 y §7) | Valor | Resultado |
|---|---|---|---|
| Potencia | A ≥ 3 perfiles con V15 de la mitad o V26 | 8 | Hay potencia |
| **P1** | Perfiles B < A **y** B ≤ 1 | 3 frente a 8 | **NO CUMPLE** |
| P2 | Válidas B ≥ A | 45 ≥ 29 | Cumple |
| K1 | Ninguna otra categoría sube más de 1 perfil | V23 −3, V24 −3 | Cumple |
| **K2** | Mediana B ≤ A × 1,10 | 75,9 > 72,9 | **NO CUMPLE** |
| K3 | Etapa 2 | No ejecutada | — |
| K4 | Ningún validador ni política cambia | Diff de `src/main` | Cumple |
| K5 | V24 o V25: B ≤ A | 0 ≤ 3 | Cumple |
| K6 | Errores del proveedor: B ≤ A + 1 | 1 ≤ 1 | Cumple |

**Veredicto de la regla previa: DESFAVORABLE en la etapa 1, y por tanto RECHAZADA** (falla un primario y un bloqueante). No es INCONCLUSA: hay potencia y no faltó ninguna predicción por presupuesto.

**Por réplica** (informativo, no decide):
- Réplica 1: P1 2 frente a 5; K2 76,7 frente a 67,3 × 1,1 = 74,0 → no.
- Réplica 2: P1 1 frente a 8; K2 71,1 frente a 66,4 × 1,1 = 73,0 → sí.

- **La réplica 1 sola falla P1 y K2.**
- **La réplica 2 sola cumpliría los dos.**

Las dos réplicas discrepan, que es justo el ruido que la opción C quería promediar. La regla fijada de antemano es la de los perfiles sobre las dos réplicas y las 103 predicciones, **no la réplica más favorable**, así que el veredicto no cambia.

## 10. Estadística pareada por perfil

Las dos réplicas de un perfil no son independientes, así que **la unidad es el perfil (26)**.
- **V15 de la mitad o V26:** 5 perfiles solo en A y 0 solo en B. McNemar exacta, p = 0,0625.
- **Validez:** `d = válidas B − válidas A` por perfil. 12 positivos, 1 negativo (G24) y 13 iguales. Signos exacta, p = 0,0034.
- **Latencia:** media por perfil de log(B / A). 19 perfiles con B más lento y 7 con B más rápido; signos p = 0,029; mediana por perfil de B / A = 1,056.

## 11–12. Pipeline, reintentos y respaldos

**La etapa 2 no se ejecutó.** La orden dice: «Si falla un criterio bloqueante o primario en la etapa 1, no ejecutar la etapa 2». Por tanto:
- K3, reintentos, respaldos `RULE_ENGINE` y tiempo hasta un plan válido **no se midieron con IA real** en esta evaluación;
- no se publicó ningún plan.

El comportamiento del pipeline con PR-2.2 está probado sin coste en `H2v4PipelineSimuladoIT`: 3 intentos y respaldo, ningún inválido publicado, O20.

## 13. Presupuesto: reservado frente a facturado

| Concepto | USD |
|---|---:|
| Tope autorizado (cabecera del libro, `# tope_total_usd=10.80`) | 10,80 |
| Tope de la etapa 1 | 6,30 |
| **Reservado en la etapa 1** (104 reservas: A 3,1009 + B 3,1299) | **6,2308** |
| Reservado en la etapa 2 | 0 (no ejecutada) |
| **Saldo sin usar, que queda cerrado** | **4,5692** |
| Facturado | No visible desde aquí: solo en el panel de Replicate |

- Las reservas son **cotas superiores**: entrada × USD 0,25/M, más 16.000 tokens × USD 2/M.
- Se crearon **104 predicciones** en Replicate (103 registradas más la de G08 r2 B), todas `succeeded`. Lo facturado debería quedar **por debajo** de lo reservado.
- **No se usó ningún saldo de H2-v2 ni de H2-v3**, cuyos libros siguen cerrados.
- El libro está en `evaluacion_real/libro_presupuesto.csv`, con una reserva antes de cada llamada y su resultado.

## 14. Casos que mejoran y que empeoran (B frente a A, por perfil)

**Mejoran (12):**

| Perfil | A | B |
|---|---|---|
| G02, G06, G15, G20, G22 | 0/2 | **2/2** |
| G05, G07 | 1/2 | **2/2** |
| G14, G17, G18, G19, G21 | 0/2 | 1/2 |

**Empeoran (1):** **G24**, de 2/2 a 1/2. La sesión LOWER_BODY pone sus 2 ejercicios de trabajo en `upper legs`.

**Iguales (13):** G01, G03, G04, G08, G09, G10, G11, G12, G13, G16, G23, G25 y G26. Todos válidos en los dos brazos (G08 B, en su única réplica con respuesta).

**V15 que queda en B:** siempre 1 rechazo por propuesta, en LOWER_BODY (5) o FULL_BODY (1, G19 r2).
- **La mitad:** G14 r2, G17 r1 y G18 r1, con `upper legs` en 3 de 4 ejercicios.
- **Un solo grupo de trabajo:** G21 r2 y G24 r1 en LOWER_BODY, solo `upper legs`; G19 r2 en FULL_BODY, solo `back`.

Ver los ficheros `*_problemas.txt`.

## 15. Limitaciones

1. **Latencia sin causa demostrada.** Replicate no da tokens. La hipótesis es que el modelo razona más con las reglas de PR-2.2, porque el tamaño no lo explica. El día afecta a los dos brazos por igual: K2 es una comparación dentro del día.
2. **G08 r2 B** se perdió por un fallo de lectura del cliente con `application/octet-stream`, aunque la predicción terminó y probablemente se facturó.
   - En producción, eso haría que el backend lo trate como no disponible y **reintente o caiga al respaldo pagando dos veces**.
   - Es un punto de robustez **fuera del alcance** de esta orden. No se tocó código de producción.
3. **Dos réplicas por perfil** no son muestras independientes. Por eso se usa la unidad perfil; la potencia sigue siendo limitada (McNemar p = 0,0625 con 5 frente a 0).
4. **O20 con dolor:** los 26 perfiles congelados no tienen dolor, así que **esta evaluación real no demuestra O20 con dolor**. Está cubierto por `H2v4PipelineSimuladoIT` y la suite de O20.
5. **Etapa 2 no ejecutada:** no hay datos reales de reintentos, respaldo ni tiempo hasta un plan con PR-2.2.
6. **Facturación** solo visible en el panel de Replicate.

## 16. Decisión

**PR-2.2 queda RECHAZADA por los criterios fijados de antemano (P1 y K2).** No se activa, y PR-1.7 sigue por omisión.

**H2 no se declara resuelta.**

Lo que sí muestra esta evaluación, para decidir el siguiente paso:
- PR-2.2 sube mucho la validez a la primera (+16 parejas válidas);
- elimina V24, V25 y V26 en estas 103 respuestas;
- deja un V15 residual en 3 perfiles, cada uno en una sola réplica;
- cuesta unos 8 a 10 s más de `predict_time`, un 13 % pareado.

**No se hicieron predicciones adicionales.** La PR #87 queda **abierta, sin merge, a la espera de la decisión final**.

## Regresión tras la evaluación

`mvn verify -Pintegration` completo y local, sobre el código de `32d22d88` (el código no cambió después):
- unitarias: **1.640, 0 fallos**, 1 omitida (las 4 nuevas son `PlanEjecucionH2Test`);
- integración: **935, 0 fallos**, 74 omitidas, que son las opcionales, entre ellas los arneses pagados;
- **BUILD SUCCESS** (27 min 40 s).

Incluye O20, generación inicial, reintentos, motor de reglas, adaptación, progresión, reincorporación y los contratos de PR-2.0, PR-2.1 y PR-2.2. CI de GitHub: ver los checks de la PR #87.

## Anexos (`evaluacion_real/`)

| Archivo | Contenido |
|---|---|
| `libro_presupuesto.csv` | Libro nuevo con tope 10,80: 104 reservas y sus resultados |
| `etapa1/registro_replicas.csv` | Clave, id de Replicate, ms y origen de las 103 respuestas |
| `etapa1/primera_propuesta.csv` (y `_r1`, `_r2`) | Una fila por predicción: perfil, réplica, brazo, orden, versión, entrada, cota, id, tiempo, validez y rechazos |
| `etapa1/predict_time.csv` | `predict_time` de las 103 (GET sin coste) |
| `etapa1/analisis.md` | Salida de `analizar_h2v4.py etapa1r` |
| `etapa1/revalidacion_sin_coste.csv` | Revalidación de las 103: 0 diferencias |
| `etapa1/metricas_por_prediccion.csv` y `metricas_salidas.py` | Series, dosis, reparto, patrones, duración y ejercicios fuera de lo ofrecido |
| `etapa1/salidas/` | Las 103 salidas completas y sus rechazos |
| `etapa1/sensibilidad_G08_r2_B/` | La predicción de G08 r2 B que el backend no pudo leer, recuperada y validada (informativo) |
| `etapa1/99_resumen_r*.txt` y `consumo_replicate_r*.log` | Resumen y consumo de cada corrida |
