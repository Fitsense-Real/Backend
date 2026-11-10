# Confirmación pr-1.8a · Incidencias

| Id | Bloque | Qué pasó | Tratamiento | Efecto en los resultados |
|---|---|---|---|---|
| I-P-0 | Preparación | Replicate publicó una versión nueva del modelo (`81cb6179…`, 10/11/2026 a las 18:29 UTC) después de las rondas C1–C3 y A1 (`045f8102…`) | Se congela la versión nueva en el protocolo antes de cualquier llamada. El ejecutor no lanza ningún bloque si la versión cambia | A2 y A3 solo se comparan entre sí. A1, C1–C3 y la ronda 2 se presentan aparte |
| I-P-1 | Preparación | Docker no arrancaba: había un archivo `/var/run/docker.pid` antiguo, de un proceso que ya no existía | Se borró ese archivo y se arrancó `dockerd` | Ninguno |
| I-P-2 | A2, mitades M2 | **Fallo del ejecutor**: dentro de `pagado()`, la versión del modelo se guardaba en la variable `v`, la misma que contenía el nombre del prompt. Las mitades M2 recibieron `prompt-generacion=<id de versión>`, y el constructor de `PlanPromptBuilder` lo rechazó (`admite pr-1.7 o pr-1.8a`). **Falló antes de arrancar el contexto: 0 llamadas, USD 0** | Corregido (`local ver`). Las mitades M2 se ejecutan en `CONF_P18A_*_RA2_M2B`. Las carpetas fallidas `CONF_P18A_*_RA2_M2` (sin llamadas) se conservan como evidencia | Ninguno. Se comprobó que las M1 usaron el prompt correcto: PR-1.7 sin el bloque nuevo y pr-1.8a con `PR-1.8a-EXP` en los 10 perfiles |
| I-P-3 | A2, pr-1.8a M1 | O09 (`12xwc44xw9rmr0d1rxfa5cexkm`) superó los 150 s (176 s en total) y se canceló sin salida | Reserva de USD 0,05. O09 y O10 se completan en `CONF_P18A_G18A_RA2_M1B` | Ninguno sobre la validez |
| I-P-4 | A2, pr-1.8a O09 | O09 vuelve a superar los 150 s en `_M1B` (`pvvd4zrmydrmt0d1rxtae8pmvm`) y en `_M1C` (`5e50jp43y5rmr0d1ry6b5pfw9w`): **tres timeouts seguidos**, 177 s cada uno, todos cancelados sin salida. PR-1.7 no falla en O09. O10 se completa en `_M1D` | **Análisis principal:** O09 queda fuera de A2 en las dos condiciones (no hay respuesta de pr-1.8a). **Sensibilidad:** se informa contando el timeout como «no válido al primer intento». **Regla para A3, fijada antes de ejecutarla:** O09 de pr-1.8a se intenta una sola vez | Posible efecto de latencia de pr-1.8a en O09 con la versión nueva del modelo. Se informa como hallazgo: en producción, un timeout lleva al plan de respaldo |
| I-P-5 | A3, ejecución | Con un saldo de USD 1,527 al empezar A3, la reserva de peor caso del arnés (USD 0,05 por llamada posible) no deja terminar una mitad completa al final de la ronda | A3 se ejecuta en tandas de 5 perfiles (`correr_A3.sh`), en el mismo orden pr-1.8a → PR-1.7. Cada perfil se ejecuta una vez | Ninguno sobre el método |
| I-P-6 | A3, pr-1.8a | Fallos del proveedor en las tandas:<br>• O03 (`srm955nj95rmt0d1ry9tthkw80`): timeout<br>• O07, intento 2 (`v0msvp47tnrmw0d1rydb9p482w`) y O08 (`fc4s0cwfdsrmw0d1rydb35za50`): `failed` en ~1,5 s<br>• O09 (`3bn62amrsnrmy0d1rydaz02mq8`): timeout, el cuarto seguido; según la regla de I-P-4 no se reintenta<br>• O19: error de E/S en el POST<br>Los perfiles de detrás en cada tanda (O04, O05, O10, O20) no llegaron a ejecutarse | Con un saldo de USD 0,2935, por debajo del mínimo de 0,30 del ejecutor, **no se pueden completar** ni pr-1.8a (O03, O04, O05, O08, O10, O20) ni PR-1.7 (O17–O20, `DETENIDO`) | **A3 queda INCOMPLETA y fuera de la decisión** (§4 del protocolo). Sus datos se presentan solo de forma descriptiva |
| I-P-7 | A3, pr-1.8a O19 | La predicción del error de E/S (`bzq6kgh979rmt0d1rynaxb1c60`, `succeeded`, 22:53) se encontró en la lista de la cuenta y se descargó antes de caducar. Su prompt es idéntico a `O19_generacion_i1.txt` | Validada gratis (`EVAL_A3_O19`): **VÁLIDA** en crudo y normalizada | Cuenta como válida al primer intento en los datos descriptivos de A3 |
| I-P-8 | Vigilancia | El descargador de huérfanas se detuvo al llegar al límite de 2 h y no se relanzó | Revisión manual de la lista de la cuenta al terminar A3. Solo faltaba la de I-P-7, ya recuperada | Ninguno |

## Gasto (saldo del bloque de USD 10)

| Concepto | Predicciones | USD |
|---|---|---|
| Esta campaña (A2 completa + A3 incompleta) | 99 | 2,6115 |
| Total del bloque (CONF-IA + esta campaña) | 435 | 9,7065 |
| Saldo sin usar | — | 0,2935 |
