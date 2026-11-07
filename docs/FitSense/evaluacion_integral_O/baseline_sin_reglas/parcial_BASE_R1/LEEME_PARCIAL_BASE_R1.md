# BASE-R1 · entregables parciales (BASE-R2 en curso)

**Estado:** cifras provisionales de una sola corrida. Los entregables finales (BASE-R1 + BASE-R2, tablas IEEE, resumen y ZIP) sustituirán a esta carpeta.

- **Evaluación:** solo lectura, con el instrumento congelado en `ba2f16e`.
- **Corrección I-1 del instrumento:**
  - **Defecto:** el validador añade, cuando hay V13 o V18, una sugerencia sin código («DIVISION: usa estas fechas…»), y el contador la contaba como infracción.
  - **Corrección:** ahora se registra como aviso y no se cuenta.
  - **Efecto en BASE-R1:** −5 hallazgos por etapa.
  - **Efecto en GRA:** −1 (O14, R2).
  - **Conservación:** el conteo original se guarda en la evidencia.

## Generación (19 perfiles factibles; O11 aparte)

| | GRA-R1, 1.ª propuesta | BASE-R1 |
|---|---|---|
| Propuestas evaluadas | 19 | 19 (+ O11) |
| Planes con ≥ 1 violación (etapa NORMALIZADO, equivalente a GRA) | 7/19 (36,8 %) | **19/19 (100 %)** |
| Infracciones únicas (NORMALIZADO) | 22 | **637** (media 33,5 por plan, máximo 83) |
| Infracciones únicas (CRUDO, sin normalizador) | 26 | **693** |
| Planes publicados que violan alguna regla | 0/19 (los rechazos se corrigieron en el reintento) | No aplica: no se publica nada |
| O11 (30 min, inviable para FitSense) | Sin llamada: limitación informada | El modelo generó plan, con 19 infracciones |

**Infracciones de BASE-R1 por categoría (NORMALIZADO):**

| Categoría | Infracciones |
|---|---:|
| Dosis (V24) | 389 |
| Patrones obligatorios | 57 |
| Series y repeticiones | 54 |
| Carga | 48 |
| Volumen por grupo | 41 |
| Distribución de ejercicios | 25 |
| Distribución de sesiones | 11 |
| Ejercicios por sesión | 7 |
| Frecuencia | 3 |
| Duración | 2 |

**Catálogo:** 0 ejercicios inexistentes o fuera del catálogo filtrado. El modelo eligió siempre ids de la lista.

**Comparación pareada por perfil (NORMALIZADO):**
- 12 perfiles violan solo en BASE, 0 solo en GRA y 7 en ambos.
- McNemar exacta: p = 0,00049.
- Es descriptiva: n = 19 y una sola corrida.

## Adaptación (10 ciclos)

| | GRA-R1 publicadas | BASE-R1 |
|---|---|---|
| Adaptaciones con ≥ 1 cambio no autorizado | 0/10 | **10/10** |
| Cambios no autorizados | 0 | **59** |

**Cambios de BASE-R1 por tipo:**

| Código | Cambio no autorizado | Cambios |
|---|---|---:|
| C1 | Sesión no señalada modificada | 22 |
| C3 | Subida de dosis de un ejercicio | 15 |
| C10 | Cambio no permitido por la intervención | 9 |
| C2 | Más trabajo en la sesión señalada | 5 |
| C6 | Ejercicio excluido por dolor que sigue en el plan | 3 |
| C8 | Duración excedida | 3 |
| C4 | Más dificultad | 1 |
| C11 | Ante dolor, más añadidos que retirados | 1 |

## Predicciones y archivos

**Predicciones:** 30/30 `succeeded`, USD 0,4763. Versión fijada solicitada (`9f4cd9ec…`); Replicate la informa como `hidden`.

**Archivos:**
- `RESULTADOS_GENERACION_BASELINE.csv` y `VIOLACIONES_GENERACION.csv`;
- `RESULTADOS_ADAPTACION_BASELINE.csv` y `CAMBIOS_NO_AUTORIZADOS.csv`;
- `COMPARACION_FITSENSE_VS_BASELINE.xlsx`;
- `PREDICCIONES_REPLICATE_BASELINE.csv`;
- `metricas.json`.

---
**Sustituido por los entregables finales** de la carpeta superior (BASE-R1 + BASE-R2). Las cifras de BASE-R1 de los finales son idénticas a las de esta carpeta: 637 infracciones y 19/19 planes en generación; 59 cambios y 10/10 ciclos en adaptación.
