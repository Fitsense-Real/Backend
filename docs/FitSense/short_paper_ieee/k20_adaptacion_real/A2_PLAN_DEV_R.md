# A2 · Plan de DEV-R: diagnóstico con Replicate real, V2 frente a V2.1 en U01–U20

**Estado: PLAN APROBADO, NO EJECUTADO. Antes de la primera predicción hace falta tu confirmación final del gasto.**
- «Todavía no autorizo realizar llamadas pagadas.»
- DEV-R es **desarrollo**, no evaluación: su resultado sirve para elegir el prompt antes de congelar y **no** se presenta como resultado científico.
- **No se ejecuta nada sobre J01–J16.**

## 1. Versión sobre la que se ejecutaría

| | |
|---|---|
| Rama | `eval/final-ieee-k20-v2`, la candidata con 4D v3.4 integrada |
| SHA | El HEAD con CI verde en [#41](https://github.com/Fitsense-Real/Backend/pull/41) en el momento de autorizar. Se anota en `configuracion.json` de cada corrida |
| Cohorte | U01–U20 (perfiles históricos, SHA `584e2204…`). Sus perfiles GM son de 60 min, así que 4D v3.4 no cambia su recorrido: la huella D es idéntica (`11` §4D v3.4) |
| Modelo | `gpt-5-mini` sobre `openai/gpt-5-structured`, esfuerzo `medium`, hasta 3 intentos por ciclo, `prefer-wait=25` |
| Prompts | Corrida DEV-R-V2 con `-Dfitsense.evaluacion.k20.prompt=v2` y corrida DEV-R-V2.1 con `-Dfitsense.evaluacion.k20.prompt=v2.1`. Mismo SHA, misma semilla, mismo orden de usuarios |

## 2. Coste y control del consumo (comprobado el 04–05/11/2026, sin ninguna predicción nueva)

| Comprobación | Resultado | Cómo |
|---|---|---|
| **Acceso a Replicate** | **Sí**: `GET /v1/account` = **200** con la credencial que inyecta el proxy de la sesión (el token no es visible). `api.replicate.com` es accesible; `replicate.com` (la web) no lo es | Consulta gratuita |
| **Modelo** | `openai/gpt-5-structured` (oficial), versión `9f4cd9ec…` (21/01/2026), variante `gpt-5-mini`. Admite `max_output_tokens`, que el backend **no** envía | `GET /v1/models/openai/gpt-5-structured` (público) |
| **Forma de facturar** | **Por tokens**, no por predicción. Cada predicción devuelve `metrics.token_input_count` y `metrics.token_output_count` | Historial de la cuenta (`GET /v1/predictions`, gratuito) |
| **Tarifa** | **0,25 USD por millón de tokens de entrada y 2,00 USD por millón de salida** para `gpt-5-mini` en Replicate. **No verificada en la página oficial de Replicate** (no accesible desde la sesión); la dan de forma coincidente varios agregadores de precios[^tarifa]. Es la misma que usaron las campañas anteriores | Búsqueda web |
| **Consumo real observado** (100 predicciones del 03/11/2026, `gpt-5-mini`, esfuerzo `medium`) | Entrada: 8.437–68.186 tokens. Salida: 741–5.260. Peor caso: **≈ 0,0275 USD**. Típico de adaptación (≈ 17.000–23.000 de entrada): ≈ 0,010–0,016 USD | Métricas de esas predicciones |

[^tarifa]: futureagi.com/llm-cost-calculator/replicate/openai-gpt-5-mini · cloudprice.net/models/replicate/openai/gpt-5-mini · getmaxim.ai/bifrost/llm-cost-calculator/provider/replicate/model/gpt-5-mini · llmreference.com/model/gpt-5-mini/replicate. Todos son terceros.

**0,05 USD no se trata como un precio fijo.** Es una **reserva de peor caso** por predicción: casi el doble del peor caso observado.

### Mecanismo de control (código de prueba, `EvaluacionK20IT`; `PresupuestoReplicateK20Test`)

1. **Antes de cada predicción** se exigen tres cosas:
   - número de predicciones ≤ `max-llamadas` (24 por corrida);
   - número × 0,05 ≤ `max-usd` (1,20 por corrida);
   - **gasto real medido + 0,05 ≤ `max-usd`**.

   Si no se cumplen, no se llama y se registra `PRESUPUESTO_AGOTADO_SIN_LLAMADA`.
2. **Después de cada predicción** se leen sus métricas (`GET /v1/predictions/{id}`, gratuito) y se suma su **coste real por tokens**.
3. **Si una predicción cuesta más que la reserva**, o sus métricas no se pueden leer, se carga la reserva y **no se hace ninguna llamada más** en esa corrida.
4. **Salida:** cada corrida escribe `predicciones_metricas.csv` (tokens, segundos, USD y acumulado por predicción) y `usd_medido` en `99_resumen.txt`.

**Verificado sin gasto:**
- prueba unitaria del coste y del identificador;
- lectura real de las métricas de una predicción existente: 68.186 / 3.672 tokens = 0,0244 USD.

**Garantía y su límite:**
- Mientras cada predicción cueste ≤ 0,05 USD, el gasto de DEV-R **no puede pasar de 2,40 USD**.
- El backend no limita los tokens de salida. Si una predicción anómala costara más, se detecta en esa misma predicción y se paran las llamadas. El exceso queda acotado por esa única predicción: unos 0,27 USD en el peor caso teórico, con salida máxima de `gpt-5-mini`.
- Para una cota dura en el proveedor puedes fijar un **límite de gasto en el panel de Replicate**. No lo puedo configurar desde aquí.

### Gasto esperado

| Corrida | Predicciones | Tope | Esperado |
|---|---:|---:|---:|
| DEV-R-V2 | ≤ 24 | 1,20 USD | ≈ 0,10–0,40 USD |
| DEV-R-V2.1 | ≤ 24 | 1,20 USD | ≈ 0,10–0,40 USD |
| **Total** | **≤ 48** | **2,40 USD** | **≈ 0,20–0,80 USD** |

## 3. Comandos (no ejecutar sin autorización expresa)

```
SHA=$(git rev-parse HEAD)
for P in v2 v2.1; do
  sh ./mvnw -o -B -ntp verify -Pintegration -DskipUTs=true -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false \
    -Dit.test=EvaluacionK20IT -Dfitsense.evaluacion.k20=true -Dfitsense.evaluacion.k20.modo=REPLICATE \
    -Dfitsense.evaluacion.k20.autorizacion-replicate=SI -Dfitsense.evaluacion.k20.credencial=proxy \
    -Dfitsense.evaluacion.k20.tarifa-entrada-usd-millon=0.25 -Dfitsense.evaluacion.k20.tarifa-salida-usd-millon=2.00 \
    -Dfitsense.evaluacion.k20.prompt=$P \
    -Dfitsense.evaluacion.k20.max-llamadas=24 -Dfitsense.evaluacion.k20.max-usd=1.20 \
    -Dfitsense.evaluacion.k20.salida=docs/FitSense/short_paper_ieee/k20_adaptacion_real/evidencia/desarrollo/DEV_R_$P \
    -Dfitsense.evaluacion.k20.commit=$SHA
  python3 -I docs/FitSense/short_paper_ieee/k20_adaptacion_real/herramientas/oraculo_k20.py \
    docs/FitSense/short_paper_ieee/k20_adaptacion_real/evidencia/desarrollo/DEV_R_$P \
    docs/FitSense/short_paper_ieee/evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv \
    docs/FitSense/short_paper_ieee/evaluacion_final_k20/entrada/04_ORACULO_TEORICO_HISTORICO.csv
done
```

**Requisitos del entorno:**
- credencial: la del proxy de la sesión, con `-Dfitsense.evaluacion.k20.credencial=proxy` (comprobada: `GET /v1/account` = 200);
- tarifa explícita en el comando, para que quede registrada en `00_configuracion.txt`.

**Reglas de ejecución:**
- Las corridas se ejecutan una vez cada una.
- Una corrida que falle por causas técnicas se conserva y se repite completa **una sola vez**, con registro.

## 4. Comparación V2 frente a V2.1 (dimensiones aprobadas)

| Dimensión | Fuente | Medida |
|---|---|---|
| **Adaptaciones efectivas publicadas** | `tablas/` y oráculo | Sesiones con origen IA, validadas, con cambio real y publicadas, sobre las **factibles** de la sonda (10 en U). U03 S3 y U04 S3 son CONS-J y no cuentan |
| **Pertinencia funcional** | Oráculo v2 | **ACC y ALT por separado**, más INC, CONS-J y CONS-POL, por causa. Una alternativa lateral (ALT) no se presenta como una simplificación demostrada |
| **Cambio mínimo ante desmotivación** | Oráculo (B-4) | Ejercicios cambiados por sesión (más de 2 = INC) |
| **Reducción de dificultad** | Catálogo y oráculo | Diferencia de dificultad de cada sustituto; ante «demasiado difícil», ninguno más difícil |
| **Conservación del músculo objetivo** | Oráculo v2 | Proporción de sustituciones con el mismo músculo objetivo |
| **Propuestas rechazadas** | `intentos_ia.csv` | Por validador y por contrato |
| **Reintentos** | `intentos_ia.csv`, `trazas_ia.jsonl` | Intentos por ciclo y respaldos |
| **Seguridad** | Oráculo (S1–S7) | 0 planes inválidos publicados, 0 sesiones no señaladas alteradas, 0 aumentos de dosis o de dificultad, 0 incoherencias de texto |
| **Coste y latencia** | `predicciones_metricas.csv` | Tokens, USD medidos, latencia mediana y p90 |

**Regla de decisión** (fijada antes de ejecutar):
1. **Seguridad** con 0 incumplimientos en ambas. Si una no la cumple, queda descartada.
2. Se prioriza el número de adaptaciones **correctas y pertinentes**, es decir efectivas y ACC o ALT; ACC se considera por encima de ALT. **No se elige por el número de cambios.**
3. Después cuentan el cambio mínimo ante desmotivación, la reducción de dificultad y la conservación del músculo.
4. Los rechazos, reintentos, coste y latencia solo desempatan.
5. **Si hay empate real en efectividad y pertinencia, se mantiene V2.**
6. La recomendación final se te presenta; la elección es tuya.

## 5. Lo que sigue después de DEV-R (también sin autorizar)

1. Congelar SHA, prompt, oráculo v2, criterios y cohortes U y J (`07`).
2. D y D′ sobre U y J.
3. R y R′ con Replicate sobre U01–U20 + J01–J16: 72 + 72 = 144 predicciones, USD 7,20 de tope.
