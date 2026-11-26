# 04 · Protocolo de la evaluación con gpt-5-mini real y presupuesto

> **No ejecutado. No hay autorización de gasto.** Los USD 4,3832 que quedaron sin usar de la evaluación de H2-v2 son saldo de reserva, no un permiso: el libro de esa evaluación queda cerrado. Esta evaluación necesita una autorización nueva y abriría un libro propio.

## 1. Comparación

| | A · control | B · tratamiento |
|---|---|---|
| Prompt | **PR-1.7** (vigente) | **PR-2.1** (`-Dfitsense.h2v2.version-b=PR-2.1`) |
| Igual en los dos | Los **mismos 26 perfiles**: los 24 congelados de #69 y G25–G26.<br>Catálogo `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1` y `PLAN-REV-2.0`.<br>gpt-5-mini directo (`medium`, `low`, 16.000).<br>El mismo contexto por caso (semana del 23/11/2026), validadores V1–V27 sin cambios | Igual |
| Orden | Alterno: impar A→B, par B→A | |
| Brazos nuevos | **Los dos se vuelven a pedir.** No se reutilizan las salidas de PR-1.7 del 25/11: el `predict_time` depende de la carga de Replicate ese día (K2), y la pareja debe ser contemporánea | |

**Referencia histórica,** sin entrar en la decisión: los resultados de PR-2.0 del 25/11 (`h2_v2/07`), para ver si PR-2.1 conserva su mejora en V15/V26.

## 2. Etapas

| Etapa | Arnés | Predicciones | Cota |
|---|---|---|---|
| **1 · Primera propuesta pareada** (26 casos) | `H2v2PrimeraPropuestaIT`, modo `REAL`, `version-b=PR-2.1`. **Listo** | 52 | **USD 3,1311**: A 1,5504 + B 1,5807 (`evidencia/prompts_y_cotas_26_casos_PR17_PR21.csv`) |
| **2 · Pipeline completo pareado** (13 casos: G25, G26, G05, G06, G02, G18, G14, G04, G16, G12, G08, G20, G10) | `H2v2PipelineCompletoIT`, modo `REAL`, `version-b=PR-2.1`. **Listo** y probado con Replicate simulado. **Solo si la etapa 1 pasa los criterios** | Hasta 3 por brazo y caso; **≈1,49 (A) y ≈1,36 (B)** esperadas, con la validez a la primera medida el 25/11 | Esperada **USD 2,02**. Máxima USD 4,26 con reintentos del mismo tamaño, y unos USD 4,33 con reintentos de unos 12 KB más |

**Duración:** etapa 1, unos 70–80 min (52 × ≈85 s); etapa 2, unos 40–60 min.

## 3. Criterios fijados de antemano

**Históricos** (`h2_v2/05` §4), **sin cambios**:

| # | Criterio | Tipo |
|---|---|---|
| P1 | Casos con algún rechazo V15 de la mitad o V26 en la primera propuesta: B < A **y** B ≤ 1 | Primario |
| P2 | Válidas al primer intento: B ≥ A | Primario |
| K1 | Ninguna otra categoría (V1, V4, V23, V24, V25…) sube más de 1 caso en B | Bloqueante |
| K2 | Mediana de `predict_time`: B ≤ A × 1,10 | Bloqueante |
| K3 | Etapa 2: 0 publicados inválidos (revalidación); respaldos + sin plan en B ≤ A; predicciones B ≤ A × 1,10 | Bloqueante |
| K4 | Ningún validador ni política cambia (diff de la rama) | Bloqueante |

**Nuevos**, justificados por lo observado con PR-2.0:

| # | Criterio | Tipo | Por qué |
|---|---|---|---|
| **K5** | Casos con algún rechazo **V24 o V25** en la primera propuesta: **B ≤ A**, sin margen | Bloqueante | Es la regresión concreta de PR-2.0 (V24 de 2 a 3 casos y V25 de 0 a 2). K1 tolera +1 por categoría; para la regresión que se corrige no se tolera ninguna subida |
| **K6** | Errores del proveedor (cancelaciones o tiempo agotado) en B ≤ A + 1 | Bloqueante | PR-2.0 tuvo la única cancelación (G21 B, 177 s). Un prompt más largo no puede empeorar la disponibilidad |
| S1 | B frente a PR-2.0 histórico: casos con V15 de la mitad o V26 ≤ 2 | Secundario, informativo | Comprueba que PR-2.1 conserva la mejora de PR-2.0. No decide, porque la fecha es distinta |

**Decisión:**
- **APROBADA** si se cumplen K1–K6 y P1, y P2 no empeora. En ese caso se propone activar PR-2.1, siempre con autorización expresa.
- **RECHAZADA** si falla cualquier bloqueante.
- **INCONCLUSA** si A tiene menos de 3 casos con V15 de la mitad o V26, o si el presupuesto deja la comparación incompleta.

**Estadística:** McNemar exacto sobre las parejas discordantes. Con 26 parejas solo se ven diferencias grandes: es una evaluación de **dirección y seguridad**.

## 4. Control del presupuesto

Es el mismo mecanismo de la evaluación de H2-v2, ya probado:
- **Libro propio**, por ejemplo `docs/FitSense/h2_v3/evaluacion_real/libro_presupuesto.csv` con `-Dfitsense.h2v2.libro=…`, que solo crece. El tope total se fija en `LibroPresupuestoH2v2.TOPE_TOTAL_USD`, que hoy vale 7,50; si se autoriza otro tope, se cambia esa constante en el commit de la evaluación.
- **Reserva antes de cada predicción**, con su cota exacta. No se devuelve aunque la predicción falle o se cancele.
- **Si no cabe, no se llama.**
- **Parejas completas** en la etapa 1 y el peor caso comprobado antes de cada caso en la etapa 2.
- **Reanudación sin repetir.**
- **Reservado frente a facturado:** el coste facturado solo se ve en el panel de Replicate, porque gpt-5-mini no devuelve tokens.

## 5. Presupuesto

| Opción | Qué incluye | Tope a autorizar | Qué permite concluir |
|---|---|---|---|
| **A (recomendada)** | Etapa 1 completa (26 parejas) y, **solo si** pasa los criterios, la etapa 2 con 13 casos | **USD 7,60**: 3,20 + 4,40 | La misma base que la evaluación de PR-2.0, comparable con ella, más la medida de reintentos, respaldo y publicación |
| B | Solo la etapa 1 | USD 3,20 | Primera propuesta. No mide reintentos, respaldo ni publicación (K3) |
| C (más potencia) | Etapa 1 con **dos réplicas** por perfil (52 parejas) y la etapa 2 condicionada | USD 10,70: 6,30 + 4,40 | Duplica las parejas discordantes posibles. Sigue siendo pequeña para diferencias moderadas |

## 6. Cómo ejecutarlo (cuando se autorice)

```bash
# Etapa 1 (con coste; el token lo pone el entorno, nunca el repositorio)
mvn -o verify -Pintegration -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=H2v2PrimeraPropuestaIT -Dfailsafe.failIfNoSpecifiedTests=false \
  -Dfitsense.h2v2.modo=REAL -Dfitsense.h2v2.autorizado=SI -Dfitsense.h2v2.version-b=PR-2.1 \
  -Dfitsense.h2v2.tope-usd=3.20 -Dfitsense.h2v2.libro=docs/FitSense/h2_v3/evaluacion_real/libro_presupuesto.csv \
  -Dfitsense.h2v2.salida=docs/FitSense/h2_v3/evaluacion_real/etapa1
# Analisis (sin coste): predict_time por GET y criterios
docs/FitSense/h2_v2/evaluacion_real/obtener_predict_time.sh <ids> > .../predict_time.csv
python3 -I docs/FitSense/h2_v3/analizar_h2v3.py etapa1 .../primera_propuesta.csv .../predict_time.csv
# Etapa 2 (solo si la etapa 1 pasa): -Dit.test=H2v2PipelineCompletoIT -Dfitsense.h2v2.e2.modo=REAL ... -Dfitsense.h2v2.tope-usd=4.40
```

**Los criterios ya están programados**, antes de cualquier predicción: `docs/FitSense/h2_v3/analizar_h2v3.py` calcula P1, P2, K1, K2, K3, K5, K6 y S1.

**Comprobado sobre los datos de PR-2.0, sin coste:** da el mismo veredicto que su evaluación (DESFAVORABLE: P1 y K1), y K5 también lo habría rechazado (3 casos con V24 o V25 frente a 2).
