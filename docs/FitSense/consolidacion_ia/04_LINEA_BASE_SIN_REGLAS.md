# 04 · Línea base sin reglas (BASE-R1 y BASE-R2)

**Pregunta:** ¿qué propone el mismo modelo, con los mismos perfiles y el mismo catálogo filtrado, si se le retiran las reglas de planificación y adaptación de FitSense? Y, contadas con los validadores de FitSense, ¿cuántas infracciones habría?

**Carácter:** medir, no corregir. Las propuestas BASE **no se publicaron** ni se aplicaron en el sistema.

Fuentes en `generation`, carpeta `docs/FitSense/evaluacion_integral_O/baseline_sin_reglas/`:
- `PROTOCOLO_BASELINE_SIN_REGLAS.md` (PROT);
- `RESUMEN_RESULTADOS_BASELINE.md` (RES);
- `TABLAS_IEEE_BASELINE.md` (TAB);
- `PROMPTS_BASELINE.md`;
- CSV y `metricas.json`.

## 1. Identificación

| Elemento | Valor | Fuente |
|---|---|---|
| Fecha | 07/11/2026: congelación `ba2f16e` (21:24 UTC) y resultados `e0a6441` (22:09 UTC), según `git log`. El documento no la indica | git log |
| Código | Producción sin cambios. Solo se añade un modo de lectura al arnés (`6a507f8`, `src/test`). La comparación con GRA usa los entregables de `3883054` | PROT §1 |
| Modelo | `openai/gpt-5-structured` (gpt-5-mini), `Prefer: wait=25`, sondeo cada 3 s, límite de 150 s | PROT §1 |
| Versión | Se intentó **fijar la de GRA, `9f4cd9ec…`**, porque el endpoint había pasado a `045f8102…` el 06/11 a las 18:46 UTC. El proveedor aceptó las 60 predicciones, pero las devuelve como `"version": "hidden"`, así que **no se puede verificar** qué versión ejecutó | PROT §1 y §8; `PREDICCIONES_REPLICATE_BASELINE.csv` |
| Perfiles | O01–O20, O11 incluido (O11-F35 no) | PROT §1 |
| Prompts | Plantillas BASE de generación (huella `1c10c72aaffc9550`) y adaptación (`547fdc03f80b3c19`), y esquema neutro (`e1392664d3bde15c`). 60 prompts de 26 498 a 228 745 caracteres | `PROMPTS_BASELINE.md` |
| Predicciones | 60: 20 de generación + 10 de adaptación por corrida. 60/60 `succeeded`, 0 fallos y 0 no evaluables | RES §1 |
| Coste | BASE-R1 USD 0,4763 · BASE-R2 USD 0,4831 · **total USD 0,9594** | RES §1 |
| Estado | **BASE-R1 completa** (30/30). La carpeta `parcial_BASE_R1/` era provisional y tiene las mismas cifras. **BASE-R2 completa** (30/30): O16 se recuperó por id tras un corte de red, sin repetir la llamada | `parcial_BASE_R1/LEEME_PARCIAL_BASE_R1.md`; PROT §9 |

## 2. Qué se eliminó y qué se mantuvo (PROT §2)

| Se **mantuvo** | Se **eliminó** |
|---|---|
| El perfil completo (en texto) | `min_reps`, `slot`, `clase_dosis` y las dosis del catálogo |
| **El catálogo previamente filtrado** por equipamiento, restricciones y bloqueos | La división sugerida y los límites: max_reps, target, descanso, dosis_por_clase, colocaciones, minutos y slots |
| **Las restricciones personales y de equipamiento**, también escritas como dato | Las reglas 1–20, P-1.3, la fórmula de duración y 4C |
| El catálogo elegible completo de PostgreSQL, sin el recorte V24/B1 del prompt de GRA. Por ejemplo, O02 recibía 49 de 127 ejercicios y O06, 332 de 698 | La retroalimentación y los reintentos guiados: **una sola propuesta** por elemento |
| El contrato mínimo (`exercise_id`) y el esquema JSON | El normalizador, el respaldo y el bloqueo de factibilidad de O11. **No se aplicaron correcciones automáticas** |
| — | En adaptación: sesiones señaladas, intervenciones, candidatos, permisos y V8C |

**Validadores, solo para contar después:**
- **generación:** `PlanDraftValidator` V1–V27 (PROT §4);
- **adaptación:** oráculo independiente `herramientas/contar_baseline.py`, con categorías C1–C12 de cambio no autorizado (PROT §5).

## 3. Resultados de generación (19 factibles por corrida; O11 aparte)

Comparación con GRA-R1 y GRA-R2 (`02_GENERACION_INICIAL.md`), que se ejecutaron con la versión `9f4cd9ec…` y las reglas de FitSense.

| Medida (primera propuesta, sin corregir) | BASE-R1 | BASE-R2 | GRA-R1 | GRA-R2 | Fuente |
|---|---|---|---|---|---|
| Planes con ≥ 1 infracción | **19/19** | **19/19** | 7/19 | 6/19 | TAB, Tabla A |
| Infracciones únicas, **CRUDO** | 693 | 720 | 26 | 19 | TAB; `metricas.json` |
| Infracciones únicas, **NORMALIZADO** | 637 (máx. 83 por plan) | 659 (máx. 80) | 22 | 19 | TAB |
| McNemar exacta, BASE frente a GRA | p = 0,00049 (12 solo BASE, 0 solo GRA, 7 ambas) | p = 0,00024 (13 / 0 / 6) | — | — | TAB |
| Planes **publicados** con infracciones | (no se publica) | (no se publica) | 0/19 | 0/19 | TAB |

### Crudo y normalizado
- **Crudo:** la propuesta tal como la devuelve el modelo.
- **Normalizado:** tras las correcciones mecánicas que el backend aplica igual a cualquier propuesta. Por ejemplo, la duración declarada se iguala a la calculada.
- En BASE-R1, la duración pasa de 58 infracciones en crudo (57 de V17) a 2 en normalizado.

### Desglose de BASE-R1 + R2 (normalizado, RES §2)
| Regla | Infracciones |
|---|---|
| Dosis V24 | 783 |
| Patrones | 117 |
| Carga V11 | 109 |
| Series y repeticiones | 107 |
| V26 | 87 |
| Distribución de ejercicios | 47 |
| Distribución de sesiones | 24 |
| Ejercicios por sesión | 12 |
| Frecuencia V2 | 6 |
| Duración | 3 |
| Descansos | 1 |

- 83 de las 783 infracciones V24 son de ejercicios que el prompt de GRA no habría ofrecido.
- 0 ejercicios inexistentes, sin equipamiento, restringidos o bloqueados: el filtro del catálogo funcionó en las dos condiciones.

## 4. Resultados de adaptación

| Medida | BASE-R1 | BASE-R2 | GRA-R1 | GRA-R2 | Fuente |
|---|---|---|---|---|---|
| Ciclos con ≥ 1 cambio no autorizado | **10/10** | **10/10** | 0/10 | 0/10 | TAB, Tabla B |
| Cambios no autorizados (total) | 59 | 48 | 0 | 0 | RES §3 |
| SIN_EFECTO | 0 | 0 | 0 | 1 (O18) | RES §3 |

Desglose por categoría:
- **BASE-R1:** C1 22, C3 15, C10 9, C2 5, C6 3, C8 3, C4 1, C11 1;
- **BASE-R2:** C1 22, C3 7, C10 9, C2 4, C4 3, C6 2, C8 1.

## 5. Incidencias (PROT §7–§9, RES §4)
- **Cloudflare 1010:** bloqueaba el User-Agent de Python. Se corrigió antes de la primera predicción.
- **Salida `{"json_output": …}`:** se desenvuelve igual que lo hace el backend.
- **Corte de red en O16 (BASE-R2):** la predicción se recuperó por id, sin repetirla, y el registro de presupuesto se reconstruyó.
- **I-1:** el contador contaba como infracción un aviso «DIVISION» sin código. Corregido; efecto de −5 hallazgos por etapa en BASE-R1 y −1 en GRA-R2. Se conserva el conteo original.
- **Oráculo de adaptación:** usaba la tabla de permisos histórica. Se corrigió antes de pagar.

## 6. Cómo leer estos resultados
- **Lo que demuestra:** sin las reglas internas, el mismo modelo propone planes que **todos** infringen las políticas de FitSense, sobre todo de dosis. En adaptación cambia lo que no está autorizado. Con las reglas (GRA), la mayoría de las propuestas son válidas a la primera y **ningún plan publicado** tiene infracciones.
- **Lo que no demuestra:**
  - **calidad deportiva profesional:** los validadores miden el cumplimiento de las políticas de FitSense, no la opinión de un entrenador;
  - **efecto sobre usuarios reales:** los perfiles y la adherencia son sintéticos.
- **Límite de la comparación:** la versión del modelo en BASE no es verificable (`hidden`). Se solicitó la misma que en GRA y el proveedor la aceptó.

## 7. Evidencia e integridad
- **ZIP:** `baseline_sin_reglas/EVIDENCIA_BASELINE_SIN_REGLAS.zip`, 197 archivos; se abre sin errores.
- **Huellas:** `sha256sum -c SHA256SUMS.txt` da **24 OK, 0 fallos y 0 faltantes**.
