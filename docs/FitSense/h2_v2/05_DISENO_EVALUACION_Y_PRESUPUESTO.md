# 05 · Diseño de la evaluación con gpt-5-mini real y presupuesto

> **Sin ejecutar.** El presupuesto anterior (USD 8) está prácticamente agotado. No se hizo ninguna predicción pagada en este trabajo y **H2 no se declara resuelta**. Lo siguiente es el diseño y su consumo estimado, para autorizar o no un presupuesto nuevo.

## 1. Comparación

| | A · control | B · tratamiento |
|---|---|---|
| Prompt | **PR-1.7** (vigente) | **PR-2.0** (`fitsense.ai.generacion.prompt-version=PR-2.0`) |
| Idéntico en los dos brazos | Perfiles sintéticos (los 26 de la evaluación anterior: los 24 congelados de #69 y G25–G26 de 5–6 días), catálogo `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1`, `PLAN-REV-2.0`, gpt-5-mini directo (`medium`, `low`, 16.000), el mismo contexto preparado una vez por caso, y los validadores V1–V27 sin cambios | Igual |
| Orden | Alterno: caso impar A→B, par B→A | |

Los cuatro objetivos están representados: 6 casos por objetivo, más los dos de 5–6 días de GAIN_MUSCLE. Hay escenarios de 3, 4, 5 y 6 días, en casa y en gimnasio, con y sin restricciones.

## 2. Dos etapas

| Etapa | Qué mide | Arnés | Predicciones | Cota |
|---|---|---|---|---|
| **1 · Primera propuesta pareada** | Válidas al primer intento; rechazos V15 (de la mitad y de cobertura), V26 y del resto por código; tamaño de la salida; `predict_time` | `H2v2PrimeraPropuestaIT`, modo `REAL`. **Listo** (la ejecución sin coste en modo `PROMPTS` ya está hecha) | 26 casos × 2 = **52** | **USD 3,12** (13 casos: 26 predicciones, USD 1,41) |
| **2 · Pipeline completo pareado** | Lo anterior y también: planes publicados desde la IA, reintentos, respaldo por reglas, NO_FACTIBLE, predicciones totales, tiempo total hasta publicar, y validación independiente de lo publicado (0 inválidos) | **Por construir** sobre el mismo patrón: un participante por brazo con perfil idéntico y el pipeline real (`PlanGenerationOperationService`). Antes de gastar, un ensayo con Replicate simulado | Hasta 3 por brazo y caso. **Esperadas ≈ 1,41 por brazo**, con la tasa de validez a la primera medida en PR-1.7 (9/13) | 26 casos: esperada **USD 4,40**, máxima **USD 9,36**. 13 casos: esperada **USD 1,99**, máxima **USD 4,23** |

**Cotas y supuestos:**
- **Cálculo:** las cotas salen de las peticiones reales construidas sin coste (`evidencia/prompts_y_cotas_26_casos_PR17_PR20.csv`), con la fórmula de `REPLICATE_CONSUMO`: 1 token por byte de entrada a USD 0,25/M, y 16.000 tokens de salida a USD 2,00/M.
- **Rango por predicción:** USD 0,0405–0,0897 (media 0,0596 en A y 0,0602 en B).
- **Lo que no informa Replicate:** gpt-5-mini no devuelve tokens. El gasto real es menor que la cota, porque la salida visible es de unos 6.000 caracteres más el razonamiento, pero solo se ve en el panel de facturación. **Presupuestar por la cota.**
- **Corte por presupuesto:** antes de cada pareja (etapa 1) o de cada caso (etapa 2), el arnés calcula la cota exacta de lo que va a pedir. Si no cabe en el tope, no empieza.
- **Duración estimada:**
  - etapa 1: 52 predicciones × unos 80–95 s (la mediana de `predict_time` anterior) ≈ **75–80 min**;
  - etapa 2 con 26 casos: unas 73 predicciones ≈ **1,7–2 h**.

## 3. Opciones de presupuesto

| Opción | Qué incluye | Tope a autorizar | Qué permite concluir |
|---|---|---|---|
| **A (recomendada)** | Etapa 1 completa (26 casos) y, **solo si** pasa los criterios, la etapa 2 con 13 casos: los de LOWER/UPPER y 5–6 días que fallaron antes (G25, G26, G05, G06, G02, G18, G14, G04, G16, G12, G08, G20, G10) | **USD 7,50** (3,20 + 4,30) | Dirección y seguridad en la primera propuesta con 26 parejas. Medida completa de reintentos, respaldo y publicación en los casos difíciles |
| B | Solo la etapa 1 con 26 casos | USD 3,20 | Primera propuesta. Sin medir reintentos, respaldo ni publicación (C3, C4) |
| C (mínima) | Solo la etapa 1 con los 13 casos anteriores | USD 1,45 | Comparable con la evaluación de PR-1.9. Potencia muy baja |

## 4. Criterios fijados de antemano (B = PR-2.0, A = PR-1.7)

| # | Criterio | Tipo |
|---|---|---|
| P1 | Casos con algún rechazo **V15 de la mitad o V26** en la primera propuesta: B < A, y B como mucho 1 caso | Primario |
| P2 | Válidas al primer intento: B ≥ A | Primario |
| K1 | Ninguna otra categoría (V1, V4, V23, V24, V25…) sube más de 1 caso en B | **Bloqueante** |
| K2 | Mediana de `predict_time`: B ≤ A × 1,10. PR-1.9 lo incumplió | **Bloqueante** |
| K3 | Etapa 2: 0 planes publicados inválidos según un validador independiente; respaldos + NO_FACTIBLE en B ≤ A; predicciones totales en B ≤ A × 1,10 | **Bloqueante** |
| K4 | Ningún validador ni política cambia (diff de la rama) | **Bloqueante** |

**Decisión:**
- **Favorable** si se cumplen K1–K4 y P1, y P2 no empeora. En ese caso se propone activar PR-2.0, siempre con autorización expresa.
- **Desfavorable** si falla cualquier criterio bloqueante: se mantiene PR-1.7 y se analiza caso a caso.
- **Inconclusa** si A tiene menos de 3 casos con V15 de la mitad o V26. No se activa.

**Estadística:** McNemar exacto pareado sobre las parejas discordantes. Con 26 parejas solo una diferencia grande y en una sola dirección es significativa. Lo esperable es una evaluación **de dirección y de seguridad**, no una prueba de superioridad.

## 5. Cómo ejecutarlo (cuando se autorice)

Modos del arnés:
- `PROMPTS`: sin Replicate, para reconstruir peticiones y cotas.
- `VALIDAR`: sin coste. Vuelve a validar salidas ya pagadas, por ejemplo tras una interrupción.
- `REAL`: con coste, exige `autorizado=SI` y `tope-usd`.

```bash
# Etapa 1, con coste: exige autorizado=SI y tope-usd; el token va en el entorno, nunca en el repositorio.
mvn -o verify -Pintegration -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=H2v2PrimeraPropuestaIT -Dfailsafe.failIfNoSpecifiedTests=false \
  -Dfitsense.h2v2.modo=REAL -Dfitsense.h2v2.autorizado=SI -Dfitsense.h2v2.tope-usd=3.20 \
  -Dfitsense.h2v2.salida=<carpeta de resultados>
```

Las salidas JSON, los problemas por caso y `primera_propuesta.csv` quedan en la carpeta de resultados. Si hay una interrupción, el modo `VALIDAR` reconstruye los veredictos sin repetir predicciones.
