# Protocolo · Confirmación final de pr-1.8a frente a PR-1.7 (generación inicial)

**Fecha:** 10/11/2026 · **Estado:** CONGELADO antes de la primera llamada pagada (huellas en `SHA256SUMS_CONGELACION_PR18A.txt`).
**Rama:** `experimento/confirmacion-pr18a-final`, creada desde `origin/generation` `543932a` (con #43 y #44 ya fusionadas).

## 1. Pregunta

¿Genera pr-1.8a con Replicate más planes válidos al primer intento que PR-1.7, sin empeorar:
- V24, la estructura y los respaldos;
- V4 en LOSE_WEIGHT;
- la dosis ni la calidad de los planes?

Respuesta posible:
- **A.** Recomendar pr-1.8a.
- **B.** Mantener PR-1.7.
- **C.** Favorable, no concluyente.

Si sale C, no se continúa automáticamente.

## 2. Elementos congelados

| Elemento | Valor |
|---|---|
| Código ejecutado | `experimento/confirmacion-pr18a-final`, el commit de congelación. Es `generation` `543932a` más lo siguiente: pr-1.8a apagada en `PlanPromptBuilder` (solo admite `pr-1.7` y `pr-1.8a`), el modo `solo-generacion` del arnés (solo prueba) y `PromptPr18aTest` |
| PR-1.7 | Sin cambios. Los prompts de los 20 perfiles son **idénticos byte a byte** a los de `generation`, a los de `ab04e67` y a los enviados en C2. Huella conjunta `faa8ea03…4453` |
| pr-1.8a | **El mismo texto evaluado en A1.** Los prompts de los 20 perfiles son idénticos byte a byte a los de `ab04e67` y a los 19 enviados a Replicate en A1. Huella conjunta `50179e9c…2f9e`. Por defecto está apagada |
| Variantes excluidas | pr-1.8, pr-1.8b y pr-1.8c no existen en esta rama. Tampoco v2.2a ni v2.4 |
| Validadores | V1–V27 de generación, V24 y V25 sin cambios. V8C es el corregido (#44); la generación no pasa por él |
| Catálogo y filtros | Migraciones idénticas a `generation` |
| Perfiles | `O_PERFILES.csv`, SHA-256 `9b996825…2106`. Los 19 factibles (O01–O20 sin O11). O11 es la inviabilidad esperada: no se llama y no se cuenta |
| Política | PLAN-REV-2.0 |
| Modo | `generacion-ia=SI`, `solo-generacion=true`. Solo el plan inicial: sin semanas, sin adaptación y sin publicación fuera del laboratorio (base PostgreSQL efímera) |
| Parámetros de Replicate | Los del backend: `openai/gpt-5-structured`, timeout de 150 s y hasta 3 intentos con la corrección del backend. Los mismos que en C1–C3 y A1 |
| **Versión del modelo** | `81cb6179c0b94d630ee207cba05f83fdcc5726404a70b001abce4ff860dde3dd` (creada el 10/11/2026 a las 18:29 UTC). Se comprueba antes y después de cada bloque. Si cambia, el bloque no se ejecuta y se informa |
| Instrumento | `analisis_mej_r2.py` (`c328532b…`), `entregables_conf_ia.py` (`49665e6a…`), `entregables_mej_r2.py` (`2e5feb66…`) y `depurar_respaldos_proveedor.py` (`492c5a6e…`), sin cambios respecto a la confirmación anterior |

### Cambio de versión del proveedor
- A1, C1–C3 y la ronda 2 se ejecutaron con la versión `045f8102…`. Hoy, a las 18:29 UTC, Replicate publicó `81cb6179…`, y el modelo responde con la última versión.
- **A2 y A3 se comparan solo entre sí:** las dos condiciones, con la misma versión.
- **A1 y las rondas anteriores no se juntan** con A2 y A3. Se presentan aparte, como evidencia descriptiva de otra versión.

## 3. Diseño

### Rondas A2 y A3
Cada ronda ejecuta los 19 perfiles con las dos condiciones, alternando el orden:
- **A2:** PR-1.7 → pr-1.8a.
- **A3:** pr-1.8a → PR-1.7.

### Mitades
- Cada condición se ejecuta en dos mitades de perfiles: M1 (O01–O10) y M2 (O12–O20), en ese orden.
- Es solo una necesidad del control de presupuesto: el arnés reserva USD 0,05 por cada llamada posible.
- Cada perfil se ejecuta **una vez** por ronda y condición.

### Registro
- el prompt de cada intento;
- la respuesta original;
- la validación con rechazos y códigos;
- los reintentos;
- el plan publicado (IA o respaldo);
- la traza de dosis y tiempo;
- tokens, coste y latencia;
- la versión del modelo antes y después de cada bloque.

### Compleción (regla I-MEJ-1, sin cambios)
| Caso | Qué se hace |
|---|---|
| Timeout o cancelación sin salida | El perfil afectado y los que no llegaron a ejecutarse se completan en un bloque con sufijo nuevo |
| Predicción terminada que el backend no pudo leer (error de E/S) | Se descarga de la cuenta y se valida gratis con el validador real. **No se repite** |
| Respaldo publicado por fallo del proveedor | Sus métricas de dosis y tiempo se excluyen en las dos condiciones (`depurar_respaldos_proveedor.py`) |

No se repite ninguna respuesta válida ni se elige ninguna.

## 4. Presupuesto

| Concepto | USD |
|---|---|
| Saldo del bloque de USD 10 de CONF-IA (gastado 7,0950) | **2,9050** |
| Coste observado por ronda y condición (con la versión anterior) | PR-1.7: 0,708–0,787 (media 0,745) · pr-1.8a: 0,50 (A1) |
| **Estimación A2 + A3** | ≈ 2,50 (1,25 por ronda) |
| Reserva para incidencias (timeouts a 0,05 de reserva, compleciones) | ≈ 0,40 |

**Reglas:**
- El tope total es de USD 10,00, sumando todas las carpetas `CONF_*`. Así nunca se supera el saldo.
- A3 empieza solo si quedan **≥ USD 1,45** (estimación de A3 más 0,20).
- Si una ronda no puede completar sus dos condiciones con el saldo, **queda fuera del análisis**. No se presentan comparaciones parciales.
- Si el saldo no alcanza para A3, la decisión se toma con A2 y se informa la limitación.

## 5. Medidas

**Por perfil, ronda y condición:**
- primera propuesta válida (indicador principal);
- propuestas, rechazos y reintentos;
- códigos (V15, V26, V24, V23, V4 y otros);
- plan publicado (IA o respaldo);
- respaldo por rechazos agotados;
- estado de dosis (DOSIS_LIMITADA / DOSIS_OBJETIVO);
- series por grupo y grupos bajo el objetivo (mejorables / inevitables, contra la referencia determinista);
- aeróbico;
- clases de sesión (insuficiente, corta o completa);
- % de M;
- latencia y coste.

**Por objetivo:** GAIN_MUSCLE, INCREASE_STRENGTH, LOSE_WEIGHT y GENERAL_FITNESS, con atención especial a V4, CARDIO y duración en LOSE_WEIGHT.

Se usan solo los indicadores que ya existen. No hay indicadores nuevos en el backend.

## 6. Análisis

- **Unidad: el perfil.** Las repeticiones de un perfil no son independientes.
- **Prueba principal:** Wilcoxon de rangos con signo, bilateral, sobre d_i = (válidas a la primera con pr-1.8a en A2+A3) − (válidas a la primera con PR-1.7 en A2+A3), con los 19 perfiles.
- **Además:**
  - McNemar exacta por ronda;
  - IC 95 % de Wilson por condición;
  - resultados de A2, de A3 y acumulados;
  - variabilidad entre rondas;
  - desglose por objetivo.
- **Complementario (no decide):** A1, C1–C3 y la ronda 2, de la versión `045f8102…`, presentados aparte.
  - pr-1.8a se eligió en la ronda 2, así que esa evidencia participó en la selección.
  - A1 fue la primera confirmación.

## 7. Criterios de decisión (fijados antes de ver resultados)

| # | Criterio | Se cumple si (A2+A3) |
|---|---|---|
| 1 | Validez a la primera | Wilcoxon p < 0,05 a favor de pr-1.8a |
| 2 | V24 | Hallazgos de pr-1.8a ≤ los de PR-1.7 |
| 3 | Estructura | Estructurales (V2, V13, V18, V23, V25) ≤ control, y respaldos por rechazos agotados ≤ control |
| 4 | Publicación | Ningún plan publicado con violaciones |
| 5 | Dosis y tiempo | % de M y grupos mejorables sin empeoramiento significativo (Wilcoxon por perfil, p < 0,05 en contra); ninguna dosis forzada al alza |
| 6 | **V4 en LOSE_WEIGHT** | Válidas a la primera de pr-1.8a en LOSE_WEIGHT (5 perfiles × 2 rondas) ≥ las de PR-1.7. Si son menos, es una **regresión relevante** |
| 7 | Calidad | Sesiones INSUFICIENTE de pr-1.8a ≤ control. DOSIS_LIMITADA se informa por objetivo |
| 8 | Consistencia | Diferencia de válidas a la primera (pr-1.8a − PR-1.7) ≥ 0 en A2 **y** en A3 |

**Decisión:**
- **A. Recomendar pr-1.8a:** se cumplen los 8 criterios.
- **C. Favorable, no concluyente:** se cumplen del 2 al 8, la suma de d_i es > 0 y p ≥ 0,05.
- **B. Mantener PR-1.7:** falla alguno de los criterios 2 a 8, o la suma de d_i es ≤ 0.

Los criterios no se relajan tras ver los resultados.

## 8. Regresión gratuita previa

En la rama, `verify -Pintegration` completo:
- unitarias, incluida `PromptPr18aTest`;
- integración: V1–V27, V24, V25, V8C, los cuatro objetivos, PLAN-REV-2.0, adaptación, progresión, recuperación y reincorporación.

Resultado en `PRUEBAS_PREVIAS_PR18A.md`. Sin regresión verde no hay llamadas pagadas.

## 9. Restricciones

- No se activa nada por defecto, no se hace merge ni se despliega.
- No se tocan V24, V8C, los objetivos, el catálogo, la progresión, la reincorporación, Neon ni la app móvil.
- No hay adaptación pagada.
- pr-1.8a y PR-1.7 no se modifican durante el experimento.
