# Decisiones que requieren aprobación

Ninguna se ha aplicado. Cada una indica la opción recomendada, que **no** está activada.

| ID | Decisión | Opciones | Recomendación | Evidencia o motivo | Impacto si se aprueba |
|---|---|---|---|---|---|
| D-01 | Incoherencia de V4: el perfil usa `floor(M × 1,15)` y el validador `Math.round(M × 1,15)` (30 → 34 frente a 35; 45 → 51 frente a 52) | a) dejarla; b) unificar en `floor`; c) unificar en `round` | b), en una versión nueva | Ingeniería: la tolerancia declarada al usuario debe coincidir con la validada | Algunos planes de 30/45 min con 35/52 min dejarían de ser válidos. **Hoy prohibido (V4 no se modifica)** |
| D-02 | Adoptar C2: piso de V20 acotado por la estructura | a) no; b) sí, con mínimo del 55 %; c) sí, con mínimo por objetivo (p. ej. GAIN_MUSCLE 55 %, resto 65 %) | c) | V20 es C, sin evidencia de duración mínima [S19, S17] | 875 → ~950 planes válidos; GAIN_MUSCLE 75 min 0 → 56/60; 925 planes idénticos |
| D-03 | Mostrar al usuario la duración estimada real cuando sea menor que la declarada | Sí / no | Sí, si se aprueba D-02 | Coherencia con el motivo original de V20 (V12) | Contrato de API. Flutter fuera de esta orden |
| D-04 | Descanso de 180 s en básicos pesados de intermedio y avanzado | Sí / no | Opcional, tras revisión profesional | [S06, S08]; el ACSM 2026 no encuentra efecto del descanso [S01] | Más duración por sesión; cambia la dosis 4D |
| D-05 | Registrar el RIR o RPE de la última serie | Sí / no | Sí, en una versión con datos nuevos | [S37, S38]; IS7 es D | Migración, API y Flutter |
| D-06 | Dosis aeróbica de LOSE_WEIGHT (LW2 = E) | a) medir y avisar; b) contar actividad externa; c) alargar bloques | a) ya; c) tras revisión profesional | [S04, S05, S32] | a) sin cambio de plan; c) cambia 4D y puede reabrir V20/V24 |
| D-07 | Fortalecer todos los grupos grandes ≥ 2 d/semana en LOSE_WEIGHT | Sí / no | Revisión profesional | OMS [S05] | Cambia la obligación semanal (4A/4C). Menos holgura |
| D-08 | Nombre y validación del 3×5 (prescrito como 3×6) | a) documentar 3×6; b) excepción de 5 repeticiones para fuerza | a) | Incoherencia interna T10/T11 | b) toca el validador |
| D-09 | Texto «alta intensidad» → «peso que deje 1–3 repeticiones en reserva» | Sí / no | Sí | [S39, S10] | Texto visible para el usuario |
| D-10 | Parámetros del modelo E (10/4 series, 4 × días, techo de 10 por sesión y 20 por semana, techo de tiempo = M) | Aprobar / rechazar / revisar | Mantener sin aprobar | Son inferencias y un preprint [S03]; no son normas | Ninguno mientras E esté desactivado |
| D-11 | Versionado de la política de planificación (clave `planning_policy` y versión en `weekly_training_plans`) | Sí / no | Sí, antes de activar D-02 o E | Requisito de no retroactividad | 1 migración. **No se aplica en Neon ni en Railway** sin orden |
| D-12 | Abrir el acceso de red a los textos completos (pmc.ncbi.nlm.nih.gov, pubmed.ncbi.nlm.nih.gov, doi.org, api.crossref.org, europepmc.org, journals.lww.com, link.springer.com, iris.who.int) | Sí / no | Sí | Las 53 fuentes se leyeron solo como resumen | Permite verificar las cifras del ACSM 2026 y de Pelland 2026 en métodos y resultados |
| D-13 | Revisión por un profesional del ejercicio | Sí / no | **Sí, imprescindible** antes de D-04, D-06c y D-07 | No hubo evaluación humana | — |
| D-14 | Regla de 48 h (V18): mantenerla como decisión conservadora | Mantener / relajar | Mantener | No se localizó una fuente primaria; no causa rechazos en el laboratorio | — |

**Cómo se cambia la configuración de red:** en la configuración del entorno, apartado *Network access* (https://code.claude.com/docs/en/cloud-environments#network-access).
