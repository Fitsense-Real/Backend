# 11 · Seguridad y regresión

**Parte de desarrollo.** La de R/R′ se añadirá cuando se ejecuten.

**Código:** `eval/final-ieee-k20-v2` en `6eb63c5`: la candidata congelada más V2 y V2.1, con `prompt-version=v1` por defecto.

| Bloque | Resultado |
|---|---|
| Unitarias | **1.210 aprobadas, 0 fallidas, 0 omitidas**: las 1.185 de la congelada, 18 de V2 y 7 de V2.1 |
| Integración (`verify -Pintegration`, por defecto) | **522 ejecutadas: 480 aprobadas, 0 fallidas, 42 omitidas** (opt-in; las omitidas no cuentan como superadas) |
| H1, V8C, V20, V24, 4D, dolor, respaldo, progresión, reincorporación, linaje, idempotencia, concurrencia | Las mismas pruebas de integración que en la congelada, todas aprobadas |
| 960 perfiles (off/off y preferencia/on) | `planes.csv` `d109a3da…` y `colocaciones.csv` `d227848b…`: **idénticos** a la referencia |
| REPLAY 100 semanas (preferencia/on) | **Idéntica**, sin marcas de tiempo, a la de la evaluación congelada: solo N08 |
| D sobre U01–U20 | Huella **idéntica** a D congelada (`f9f67973…`) |
| IA simulada sobre U01–U20 (V2.1) | Huella **idéntica** a S congelada (`c0eab17e…`) |
| Información de V2/V2.1 frente a los validadores | 0 contradicciones (`05`) |
| Seguridad en desarrollo (D y S, U y H) | 0 planes inválidos publicados, 0 sesiones no señaladas alteradas, 0 aumentos de dosis o de dificultad, 0 incoherencias de texto (oráculo v2) |

**Diferencias esperadas por el cambio de prompt:** ninguna en lo que se publica sin IA o con IA simulada. Solo cambia el texto que recibe el modelo, y su efecto se medirá en DEV-R, R y R′.

No se modificó ninguna prueba histórica ni ningún validador.

**Evidencia:**
- `evidencia/regresion/regresion_6eb63c5_totales.txt`;
- resumen de los 960 perfiles y la REPLAY: `evidencia/regresion/960_replay_resumen.txt` y `replay_*.diff`.
