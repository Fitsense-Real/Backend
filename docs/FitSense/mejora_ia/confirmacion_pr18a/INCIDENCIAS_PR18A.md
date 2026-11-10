# Confirmación pr-1.8a · Incidencias

| Id | Bloque | Qué pasó | Tratamiento | Efecto en los resultados |
|---|---|---|---|---|
| I-P-0 | Preparación | Replicate publicó una versión nueva del modelo (`81cb6179…`, 10/11/2026 a las 18:29 UTC) después de las rondas C1–C3 y A1 (`045f8102…`) | Se congela la versión nueva en el protocolo antes de cualquier llamada. El ejecutor no lanza ningún bloque si la versión cambia | A2 y A3 solo se comparan entre sí. A1, C1–C3 y la ronda 2 se presentan aparte |
| I-P-1 | Preparación | Docker no arrancaba: había un archivo `/var/run/docker.pid` antiguo, de un proceso que ya no existía | Se borró ese archivo y se arrancó `dockerd` | Ninguno |
| I-P-2 | A2, mitades M2 | **Fallo del ejecutor**: dentro de `pagado()`, la versión del modelo se guardaba en la variable `v`, la misma que contenía el nombre del prompt. Las mitades M2 recibieron `prompt-generacion=<id de versión>`, y el constructor de `PlanPromptBuilder` lo rechazó (`admite pr-1.7 o pr-1.8a`). **Falló antes de arrancar el contexto: 0 llamadas, USD 0** | Corregido (`local ver`). Las mitades M2 se ejecutan en `CONF_P18A_*_RA2_M2B`. Las carpetas fallidas `CONF_P18A_*_RA2_M2` (sin llamadas) se conservan como evidencia | Ninguno. Se comprobó que las M1 usaron el prompt correcto: PR-1.7 sin el bloque nuevo y pr-1.8a con `PR-1.8a-EXP` en los 10 perfiles |
| I-P-3 | A2, pr-1.8a M1 | O09 (`12xwc44xw9rmr0d1rxfa5cexkm`) superó los 150 s (176 s en total) y se canceló sin salida | Reserva de USD 0,05. O09 y O10 se completan en `CONF_P18A_G18A_RA2_M1B` | Ninguno sobre la validez |
