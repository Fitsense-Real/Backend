# 04 · Protocolo de la evaluación con gpt-5-mini real y presupuesto (sin ejecutar)

> **No ejecutado. No hay autorización de gasto.** Esta evaluación necesita una autorización nueva y abriría un **libro propio** de H2-v4. Los saldos que quedaron sin usar en H2-v2 (USD 4,3832) y H2-v3 (USD 4,4673) **no se usan**: sus libros quedan cerrados.

## 1. Comparación

| | A · control | B · tratamiento |
|---|---|---|
| Prompt | **PR-1.7** (vigente) | **PR-2.2** (`-Dfitsense.h2v2.version-b=PR-2.2`) |
| Igual en los dos brazos | Los **mismos 26 perfiles**: los 24 congelados de #69 y G25–G26.<br>Catálogo `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1` y `PLAN-REV-2.0`.<br>`openai/gpt-5-mini` directo, `medium`, `low` y 16.000 tokens.<br>El mismo contexto por caso (semana del 23/11/2026), validadores V1–V27 | Igual |
| Orden | Alterno: impar A→B, par B→A | |
| Salidas | **Las dos se piden de nuevo.** No se reutiliza ninguna salida anterior | |

**Referencias históricas,** que no deciden: PR-2.0 del 25/11 y PR-2.1 del 26/11.

## 2. Etapas

| Etapa | Arnés | Predicciones | Cota |
|---|---|---|---|
| **1 · Primera propuesta pareada** (26 casos) | `H2v2PrimeraPropuestaIT`, modo `REAL`, `version-b=PR-2.2`. **Listo** | 52 | **USD 3,1154**: A 1,5504 + B 1,5650 (`evidencia/prompts_y_cotas_26_casos_PR17_PR22.csv`) |
| **2 · Pipeline completo pareado** (13 casos: G25, G26, G05, G06, G02, G18, G14, G04, G16, G12, G08, G20, G10) | `H2v2PipelineCompletoIT`, modo `REAL`, `version-b=PR-2.2`. **Listo** y probado con Replicate simulado. **Solo si la etapa 1 pasa** | Hasta 3 por brazo y caso | **Esperada ≈ USD 1,79**, con la validez a la primera medida el 26/11 (A 18/26, B 23/26 con PR-2.1). Máxima USD 4,24 con reintentos del mismo tamaño, y unos USD 4,47 con reintentos de unos 12 KB más |

**Lo que se mide:**
- **en las dos etapas:**
  - validez a la primera;
  - rechazos V15 (de la mitad y del mínimo de grupos), V23, V24, V25, V26 y otras reglas;
  - `predict_time` y tiempo en el backend;
  - errores y cancelaciones;
  - consumo reservado.
- **en la etapa 2, además:**
  - reintentos;
  - publicaciones desde la IA y respaldos de `RULE_ENGINE`, que nunca cuentan como éxito de la IA;
  - tiempo hasta un plan válido;
  - revalidación de lo publicado.

## 3. Criterios, fijados antes de ejecutar

**Los de H2-v3, sin cambios** (`h2_v3/04` §3), programados en `analizar_h2v4.py`:

| # | Criterio | Tipo |
|---|---|---|
| P1 | Casos con algún rechazo V15 de la mitad o V26 en la primera propuesta: B < A **y** B ≤ 1 | Primario |
| P2 | Válidas al primer intento: B ≥ A | Primario |
| K1 | Ninguna otra categoría sube más de 1 caso en B | Bloqueante |
| **K2** | **Mediana de `predict_time`: B ≤ A × 1,10**, como mediana de cada brazo, igual que en H2-v3 | Bloqueante |
| K3 | Etapa 2: 0 publicados inválidos; respaldos + sin plan en B ≤ A; predicciones B ≤ A × 1,10 | Bloqueante |
| K4 | Ningún validador ni política cambia | Bloqueante |
| K5 | Casos con V24 o V25: B ≤ A | Bloqueante |
| K6 | Errores del proveedor: B ≤ A + 1 | Bloqueante |

**Decisión:**
- **APROBADA** si se cumplen K1–K6 y P1, y P2 no empeora;
- **RECHAZADA** si falla cualquier bloqueante o primario;
- **INCONCLUSA** si A tiene menos de 3 casos con V15 de la mitad o V26, o si el presupuesto deja la comparación incompleta.

**Salidas informativas añadidas** (no deciden):
- **S1:** B frente a PR-2.0 (2 casos) y PR-2.1 (1 caso) históricos;
- **latencia pareada:** mediana de B − A y de B / A por pareja, y número de parejas con B más lento;
- **etapa 2:** tiempo hasta un plan válido por brazo.

**Aplicado a los datos de PR-2.1** (sin coste), `analizar_h2v4.py` reproduce su veredicto: RECHAZADA por K2.

**Sobre K2: no se propone cambiarlo.** Los datos muestran que es ruidoso entre días:
- PR-2.0 lo cumplió con +15,4 s de mediana pareada;
- PR-2.1 lo incumplió con +6,3 s.

Una alternativa, como medir el tiempo hasta un plan válido o usar la mediana del cociente pareado, sería un **cambio de protocolo motivado por estos resultados**. Si se quisiera, tendría que **decidirse y congelarse antes** de la evaluación de PR-2.2, nunca para reinterpretar la de PR-2.1. La forma de reducir el ruido **sin cambiar el criterio** es la opción C: más parejas.

## 4. Control del presupuesto

- **Libro nuevo:** `docs/FitSense/h2_v4/evaluacion_real/libro_presupuesto.csv`, abierto con su tope (`-Dfitsense.h2v2.tope-total-usd`). El tope queda en la cabecera y no se puede cambiar; la prueba `LibroPresupuestoH2v2Test` lo comprueba.
- **Reserva antes de cada predicción,** con su cota exacta. No se devuelve aunque falle o se cancele.
- **Si una reserva no cabe, no se llama.** En la etapa 1, una pareja solo empieza si caben sus dos brazos. En la etapa 2, se comprueba el peor caso antes de cada caso.
- **Reanudación sin repetir:** se usan el registro y las salidas guardadas.
- **Reservado frente a facturado:** el coste facturado solo se ve en el panel de Replicate.

## 5. Presupuesto

| Opción | Qué incluye | Tope a autorizar | Qué permite concluir |
|---|---|---|---|
| **A (recomendada)** | Etapa 1, 26 parejas, con tope de etapa de USD 3,20 y, **solo si** pasa, la etapa 2 con 13 casos y tope de USD 4,50 | **USD 7,70** | La misma base que PR-2.0 y PR-2.1, comparable con ellas, más reintentos, respaldo y tiempo hasta un plan |
| B | Solo la etapa 1 | USD 3,20 | Primera propuesta. Sin K3 ni el tiempo hasta un plan |
| C (K2 más robusto) | Etapa 1 con **dos réplicas** por perfil (52 parejas, tope USD 6,30) y la etapa 2 condicionada (USD 4,50) | USD 10,80 | Duplica las parejas para la validez y para la latencia. Necesita una extensión pequeña y sin coste del arnés (índice de réplica en el registro), que se haría y probaría antes de gastar |

## 6. Cómo ejecutarlo (cuando se autorice)

```bash
# Etapa 1 (con coste; el token lo pone el entorno, nunca el repositorio)
mvn -o verify -Pintegration -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=H2v2PrimeraPropuestaIT -Dfailsafe.failIfNoSpecifiedTests=false \
  -Dfitsense.h2v2.modo=REAL -Dfitsense.h2v2.autorizado=SI -Dfitsense.h2v2.version-b=PR-2.2 \
  -Dfitsense.h2v2.tope-usd=3.20 -Dfitsense.h2v2.tope-total-usd=<tope autorizado> \
  -Dfitsense.h2v2.libro=docs/FitSense/h2_v4/evaluacion_real/libro_presupuesto.csv \
  -Dfitsense.h2v2.salida=docs/FitSense/h2_v4/evaluacion_real/etapa1
# Análisis (sin coste)
docs/FitSense/h2_v2/evaluacion_real/obtener_predict_time.sh <ids> > .../predict_time.csv
python3 -I docs/FitSense/h2_v4/analizar_h2v4.py etapa1 .../primera_propuesta.csv .../predict_time.csv
# Etapa 2 (solo si la etapa 1 pasa)
#   -Dit.test=H2v2PipelineCompletoIT -Dfitsense.h2v2.e2.modo=REAL -Dfitsense.h2v2.version-b=PR-2.2 -Dfitsense.h2v2.tope-usd=4.50 ...
```
