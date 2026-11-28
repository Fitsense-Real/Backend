# Auditoría de invariantes

**Instrumento:** `ValidacionIndependiente`.
- Reconstruye cada autorización de dosis desde V31/V32 **en crudo**, sin los resolutores del backend.
- Valida cada semana publicada con el validador real.
- Se suman la comprobación de frecuencia y fechas, la de exclusiones por dolor y la de duración.

## 1. Clasificación de las semanas

| Clasificación | 100 semanas principales | Extensión N01/N09 (12) | Variante N01B/N09B (27) |
|---|---:|---:|---:|
| VALIDA | 28 | 0 | 2 |
| VALIDA_CON_AUTORIZACIONES | 72 | 12 | 25 |
| INVALIDA | **0** | **0** | **0** |
| NO_CONCLUYENTE | **0** | **0** | **0** |

Los 12 planes iniciales también son VALIDA (`generacion.csv`).

**Campaña repetida tras el cierre del PR #16** (P-01 definitiva, C-2 y el guardia de P-03): las cifras de clasificación **no cambian**. La única diferencia en las 139 semanas es el estado de N03 S4 (SIN_EFECTO con 1 llamada, en lugar de RESPALDO con 3); su semana publicada es la misma.

**Aviso sobre el instrumento:** en la primera ejecución, 7 semanas de N03 (S4–S10) salieron INVALIDA (V20). Se investigó: era un **defecto del instrumento**, no del backend (C-1 en `ERRORES_Y_CORRECCIONES.md`). Se corrigió con una regresión; no se cambió ningún validador del backend.

## 2. Invariantes

| Invariante | Resultado |
|---|---|
| P-01 definitiva (30–120 en los cuatro objetivos): los perfiles de la cohorte (30–60) admitidos; el negativo (15 min) rechazado antes de generar | ✔ |
| V4: techo = minutos × 1,15 (30 → 34, 60 → 69, 120 → 138) | ✔ sin cambios |
| D8: una sesión acortada puede bajar de 30 min (N03: 15) y la generación inicial no la hereda | ✔ |
| C-2: una señalada idéntica a una sesión con D8 conserva la exención solo por identidad | ✔ (N03 S4) |
| P-02: ninguna semana publica un ejercicio excluido | ✔ 139/139 (`publica_excluido = false`) |
| P-03: ninguna semana sin plan por un NO_FACTIBLE por dolor; ninguna publicación insegura | ✔ (N06 S4 resuelto en el mismo ciclo) |
| V8C y V1–V25 | ✔ (validación estricta del backend e independiente) |
| 4D v3.2/v3.3 | ✔ (V24 sin rechazos en lo publicado) |
| ADAPT-2 | ✔ (D1: un parcial sin M acorta; sin datos no adapta) |
| V31/V32 y linaje | ✔ una evidencia V32 por ciclo; todas las dosis reconstruibles |
| V35 y pasos | ✔ 48 pasos y 1 vuelta atrás principales; ninguno sin registrar |
| Frecuencia por nivel | ✔ 100/100 (N03: 4 con 7 días; nadie con 7) |
| Sesiones en días disponibles y sin fechas repetidas | ✔ |
| Duración real ≤ techo (minutos × 1,15) | ✔ ninguna sesión por encima |
| Dosis autorizada | ✔ toda dosis es BASE, PASO, HEREDADA, REDUCCION o REINCORPORACION; 0 SIN_AUTORIZACION |
| Volumen ≤ 1,10 × base; paso ≤ 5 % de la sesión | ✔ máximo 1,10 y 5,00 % |
| Sin restauración automática | ✔ la única reincorporación (N09B) consta como paso de las reglas |
| Exclusiones de dolor persistentes | ✔ N06 hasta el final; N08 solo hasta su reactivación explícita |
| Perfil sin modificar | ✔ `perfiles.csv` igual que en el alta; ninguna escritura sobre `user_profiles` |
| M sin cambios | ✔ no hay mecanismo para cambiarla (P-04 no implementado) |
| Bloqueos de N04 | ✔ 293, 404 y 1133 nunca aparecen |
| Sin datos ≠ 0 % | ✔ N07 S3: INSUFFICIENT_DATA y semana conservada |
| Llamadas a Replicate | **0** |
