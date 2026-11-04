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

## 4D v3.4 integrada (C-7, aprobada)

**Código:** `eval/final-ieee-k20-v2` en `d01bb19`, que añade la fórmula de 4D v3.4, las pruebas nuevas y el texto de V24. El HEAD posterior solo cambia un comentario de `EvaluacionK20IT`.

| Bloque | Resultado |
|---|---|
| Unitarias | **1.214 aprobadas, 0 fallidas, 0 omitidas**: las 1.210 anteriores y las 4 de `MvpDosePolicyTopeTest` |
| Integración (`verify -Pintegration`) | **523 ejecutadas: 482 aprobadas, 0 fallidas, 41 omitidas** (opt-in). Antes eran 522/480/42; se añaden `Hipertrofia4Dv34IT` (nueva) y `GeneracionR0548V20IT` (deja de ser opt-in y ahora exige el éxito) |
| Ninguna prueba histórica cambiada | Ninguna prueba existente exigía el tope de 7 por encima de 60 min. Los mensajes de V24 hasta 60 min no cambian (`Plan60HipertrofiaIT`, `DuracionForzadaIT`) |
| Tope por minuto (30–75) | 6 / 7 (60) / 8 (61–68) / 9 (69–75). Con el estimador real, el tope llega al piso de V20 y uno menos no llega. Dentro del presupuesto del 85 % y del techo de V4 |
| Rejilla GM 30–75 (`Hipertrofia4Dv34IT`, 312 perfiles) | **270 semanas publicadas, 0 violaciones** de tope, dosis 4D, V20, V4 o V13. **42 fallos, todos de 53–59 min por V20** (C-8, no corregido, afirmado por la prueba). Evidencia: `evidencia/v34/rejilla_gm` |
| Matriz de 960 (preferencia/on) | `planes.csv` `9fbf3085…` y `colocaciones.csv` `2c96f885…`, **idénticos al prototipo**. Frente a la candidata sin v3.4 solo cambian las **120 filas de GM a 75 min**: 112 pasan a válidas y 8 siguen inválidas. PLAN-HIST-1: **875 → 931** |
| REPLAY 100 semanas | **Idéntica** a la candidata sin v3.4 y al prototipo; solo cambian las marcas de tiempo de reloj (`exclusiones_dolor.csv`) |
| R0548 | **Plan válido**: 6 sesiones (tope semanal), ≤ 9 colocaciones, dosis 4D |
| D sobre U01–U20 | **20/20 PASS**, huella `f9f67973…` **idéntica** a la D congelada |
| IA simulada V2.1 sobre U01–U20 | **20/20 PASS**, huella `c0eab17e…` **idéntica** a la S congelada |
| D e IA simulada sobre H01–H16 | **16/16 PASS** en ambas. **H05 PASS** |
| Perfiles de la matriz sin plan válido | **29**, registrados sin alterar con regla y familia (`evidencia/v34/PERFILES_SIN_PLAN_VALIDO.csv`): 16 de peso corporal con 6–7 días, 7 principiantes de 75 min, 6 de V15 con LOSE_WEIGHT de 1 día |
| Seguridad (D y S, U y H, oráculo v2) | 0 planes inválidos publicados, 0 sesiones no señaladas alteradas, 0 aumentos de dosis o de dificultad, 0 incoherencias de texto |

**Distribución de los planes GM (rejilla):**
- Colocaciones por sesión: 4–6 (30–52 min), 7 (60), 8 (61–68) y 9 (69–75).
- Cuota máxima de un grupo en una sesión: 0,50 o menos (V15).
- Ningún grupo se trabaja en días consecutivos.
- Las series semanales de pecho y muslo suben hasta 36 con 3 días a 61–75 min (27 a 60 min). No hay regla que lo limite; queda en `13` y en la adenda (§4.2).

**Evidencia:** `evidencia/v34/` (`regresion_completa_totales.txt`, `960/`, `rejilla_gm/`, `D_U`, `D_H`, `S21_U`, `S21_H`, `PERFILES_SIN_PLAN_VALIDO.csv`).
