# H2-v4 · PR-2.2: optimización de latencia y corrección de V15

**Orden:** «ORDEN DE TRABAJO — H2-v4 (PR-2.2): OPTIMIZACIÓN DE LATENCIA Y CORRECCIÓN DE V15».
**Autorización:** investigar, implementar, probar y preparar una PR nueva. **NO MERGE. NO DESPLIEGUE. NO LLAMADAS PAGADAS.**

**Rama:** `feature/h2-v4-pr22`.
- Sale de PR-2.1 (`0e109094`), así que conserva su código, su evidencia y sus arneses.
- Se compara contra `generation`.
- PR-2.2 está **desactivada por omisión** (`PR-1.7`).

## Estado en una línea

**PR-2.2 conserva el contrato y la jerarquía de PR-2.1 y le quita cálculo al modelo:**
- marca en cada ejercicio lo que no cuenta, en vez de mandar listas de ids;
- dice la regla de la mitad, en vez de una tabla;
- explica que cardio y movilidad no cubren grupo;
- compacta las reglas 15, 21 y 22 y la comprobación.

**Resultado en pruebas sin coste:**
- es **entre 1.902 y 2.950 caracteres más corta que PR-2.1** en los 26 perfiles, y más corta que PR-2.0;
- siguiéndola al pie de la letra, las **103 salidas reales** y las 26 rutinas del motor de reglas quedan válidas, incluidas G17, G21 y G24.

**Sin llamadas pagadas. H2 no se declara resuelta:** falta la evaluación con gpt-5-mini real (`04`), que necesita autorización.

## Índice

| Entregable | Documento |
|---|---|
| Cierre de la PR #86, evidencia conservada, análisis de K2 y de G17, G21 y G24 | `01_CIERRE_PR86_Y_DIAGNOSTICO.md` |
| Cambios, reglas compactadas, estructura y comparación de versiones | `02_OPTIMIZACION_PR22.md` |
| Resultados por perfil, coherencia, simulaciones y regresión | `03_PRUEBAS_SIN_COSTE.md` |
| Evaluación nueva, criterios y presupuesto | `04_PROTOCOLO_Y_PRESUPUESTO.md` |
| Riesgos y limitaciones | `05_RIESGOS_Y_LIMITACIONES.md` |
| Criterios programados | `analizar_h2v4.py` |
| Análisis de latencia | `analisis_latencia.py` |

## Evidencia (`evidencia/`), toda sin coste

| Archivo | Contenido |
|---|---|
| `tamanos_prompt_26_perfiles_PR17_PR20_PR21_PR22.csv` | Comparación de tamaños por perfil |
| `prompts_y_cotas_26_casos_PR17_PR22.csv` | Modo `PROMPTS` de la evaluación propuesta |
| `diagnostico_G17_G21_G24.txt` | Ejercicio por ejercicio, A y B (PR-2.1), con los rechazos |
| `analisis_latencia.txt` | Correlaciones y la parte de la latencia que explica la entrada |
| `lector_literal_PR22.csv` y `tras_PR22/` | Las 103 salidas reales y las 26 rutinas: rechazos antes y después de aplicar PR-2.2 |
| `etapa2_simulado_PR22/` | Arnés de la etapa 2 con PR-2.2, Replicate simulado |
| `diff_produccion_PR21_a_PR22.diff` | Diff de producción |

## Aceptación (orden §12)

| # | Condición | Estado |
|---|---|---|
| 1 | Las contradicciones conocidas (C10–C13) siguen corregidas | Sí: sin «acércate», sin ceros, sin «bajar series» y con la salvedad de la cobertura (`H2v4RegresionesIT`) |
| 2 | LOWER_BODY comunica sus grupos disponibles | Sí: `body_parts_con_material` con `upper legs`, `lower legs` y `waist`; `min_body_parts`; cardio y movilidad marcados y «no cubren ningún grupo» |
| 3 | Regla de la mitad inequívoca | Sí: la regla exacta (`n / 2` desde 4) y `regla_mitad` en la sesión, equivalente a la tabla en los 26 perfiles |
| 4 | Dosis de V24 | Sí: 22.1 «copiada tal cual, nunca recortada»; las salidas de G02 y G03 quedan válidas siguiendo PR-2.2 |
| 5 | V23, V25 y V26 no se debilitan | Sí: los mismos techos, `slots_exigibles` y capacidades; 22.4 |
| 6 | Más compacta que PR-2.1 | Sí, en los 26 perfiles |
| 7 | PR-1.7 por omisión | Sí |
| 8 | Sin regresiones en las pruebas controladas | Sí (`03`) |
| 9 | Suite y CI en verde | §Regresión |
| 10 | Protocolo reproducible y presupuestado | `04` |

## Regresión

| Ejecución | Resultado |
|---|---|
| `mvn verify -Pintegration` completo, local | Pendiente de anotar al terminar |
| CI de GitHub (`verify`) | Ver la PR |
