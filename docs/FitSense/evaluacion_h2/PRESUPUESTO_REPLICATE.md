# Presupuesto cerrado de Replicate (fase 6)

**No se ha hecho ninguna llamada a Replicate. USD 0 gastados.** Este presupuesto necesita tu aprobación expresa.

## 1. Modelo exacto

| | |
|---|---|
| Endpoint | `https://api.replicate.com/v1/models/openai/gpt-5-structured/predictions` (`application.properties`) |
| Modelo | `gpt-5-mini` (`fitsense.ai.model`) |
| Razonamiento | `reasoning_effort=medium` |
| Versión | El modelo oficial de Replicate (`/models/...`) no fija un hash de versión. Las campañas anteriores registraron la `9f4cd9ec…`. El arnés guarda la versión servida en cada predicción |

## 2. Precio por unidad facturada

| Fuente | Tarifa | Estado |
|---|---|---|
| Agregadores independientes ([cloudprice](https://cloudprice.net/models/replicate/openai/gpt-5-mini), [futureagi](https://futureagi.com/llm-cost-calculator/replicate/openai-gpt-5-mini), [llmreference](https://www.llmreference.com/model/gpt-5-mini/replicate)) | **USD 0,25 por millón de tokens de entrada · USD 2,00 por millón de salida** | Coinciden entre sí y con las campañas anteriores |
| Página oficial de Replicate | — | **No verificada.** `replicate.com` está fuera de la red permitida de esta sesión |
| Coste real medido | USD 0,0202 por predicción (GRA: 76 predicciones, USD 1,5368); histórico 0,014–0,023 | Medido por tokens en campañas reales |
| Tokens por predicción (medidos) | Entrada p50 26.700 / p95 78.700; salida p50 7.000 / p95 10.200 | `rendimiento_generacion/01` |

Con esa tarifa, una predicción cuesta unos **USD 0,021 en la mediana y USD 0,040 en el p95**. **La reserva de peor caso es de USD 0,05 por predicción**, la misma que en las campañas anteriores.

## 3. Número exacto de casos

| Bloque | Casos | Detalle |
|---|---|---|
| **G** · generación pareada A/B | **24 parejas = 48 generaciones** | `MATRIZ_GENERACION_REAL.xlsx` |
| **AD** · adaptación real (configuración A) | **6 perfiles: 6 generaciones iniciales y 28 ciclos con IA** | 4 en modo adaptación × 6 semanas con IA + 2 en modo progresión × 2 semanas con IA (`MATRIZ_ADAPTACION_REAL.xlsx`) |
| **S** · flujo asíncrono (#71) | 4 escenarios | **Fuera de este presupuesto**: depende de #71 y de un entorno aislado (`PLAN_EJECUCION_REPLICATE.md` §4) |

## 4. Máximo de reintentos

**3 intentos por generación o adaptación**, el máximo del sistema. El arnés no reintenta un caso entero ni repite casos.

## 5. Número máximo de predicciones

| Bloque | Previstas | Máximo |
|---|---|---|
| G | ~65 (1,35 por generación, media observada en GRA) | **144** (48 × 3) |
| AD | ~44 (8 iniciales + 36 de adaptación) | **102** (6 × 3 + 28 × 3) |
| **Total G + AD** | **~109** | **246** |

## 6. Coste esperado

| | Cálculo | USD |
|---|---|---|
| G | 65 × 0,0202 | ≈ 1,31 |
| AD | 44 × 0,0202 | ≈ 0,89 |
| **Total esperado** | 109 × 0,0202 | **≈ 2,20** (rango 1,53–2,51 con 0,014–0,023 por predicción) |

## 7. Coste máximo

| Supuesto | USD |
|---|---|
| Todas las predicciones máximas (246) al p95 medido (0,040) | ≈ 9,84 |
| Todas a la reserva de peor caso (0,05) | ≈ 12,30 |
| **Con el tope propuesto** | **USD 8,00 como máximo**, pase lo que pase |

## 8. Mecanismo de interrupción

1. **Tope duro antes de cada predicción.** Lo aplican los dos arneses con `ControlPresupuestoK20.puedeLlamar`: `n × 0,05 ≤ tope` y `n ≤ max-predicciones`. Si no cabe, no se llama, la campaña se detiene y el caso se marca `CORTE_PRESUPUESTO`.
2. **Parejas completas.** El arnés pareado no empieza una pareja si no caben sus 6 predicciones de peor caso. La comparación nunca queda desequilibrada.
3. **Orden y tope por bloque:**
   - **G**, con `tope-usd=7,20` (144 × 0,05: cabe entero);
   - después **AD**, con `tope-usd = 8,00 − gasto medido de G`. Con lo previsto, unos 6,7, que dan 134 predicciones frente a las 102 que necesita.
4. **Cancelación (R-2).** Una predicción que agota el sondeo se cancela en Replicate. **Se cuenta igual en el tope.**
5. **Cinco fallos seguidos del proveedor** detienen la campaña de adaptación.
6. **Recomendado: segunda barrera fuera del código.** Fijar un límite de gasto en la cuenta de Replicate, por ejemplo USD 10.

## Decisión que necesito

| Opción | Tope | Qué cubre |
|---|---|---|
| **A (recomendada)** | **USD 8** | G completo y, previsiblemente, AD completo |
| B | USD 5 | Solo G (7,20 no cabe, así que se ejecutan 16 parejas: 96 × 0,05 = 4,80) |
| C | USD 13 | G + AD sin posibilidad de corte, aunque todas las predicciones fueran de peor caso |
