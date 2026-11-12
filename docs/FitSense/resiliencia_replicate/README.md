# Resiliencia de la integración con Replicate (R-1 a R-5)

**Alcance:**
- Estabilización de la integración existente con Replicate: generación inicial (PR-1.7) y adaptación (V2.1).
- **Sin cambios** en reglas de entrenamiento, validadores (V1–V27, V8C, V24), prompts, PLAN-REV-2.0, progresión, catálogo, migraciones, Railway ni Neon.
- **Sin llamadas reales a Replicate (USD 0).**
- Base: `generation` `3374213f3ab67271dcba383000c707657c556403`.

| Problema | Prioridad | PR | Rama |
|---|---|---|---|
| R-1 Respuestas inválidas sin respaldo | Crítica | #49 (junto con R-3) | `fix/replicate-respuestas-y-timeouts` |
| R-3 Timeouts HTTP | Alta | #49 | ídem |
| R-2 Predicciones que siguen tras el timeout | Alta | #50 (apilada sobre #49) | `fix/replicate-cancelacion-predicciones` |
| R-5 Duplicados, concurrencia y consumo | Media (necesaria antes de abrir pruebas) | #52 | `fix/generacion-duplicados-y-limite` |
| R-4 Estados de adaptación | Media | #51 | `fix/adaptacion-estado-sin-ia` |

**Orden de fusión recomendado:**
1. #49 (R-1 y R-3);
2. #50 (R-2): al fusionar la #49, se redirige a `generation`;
3. #52 (R-5) y #51 (R-4), que son independientes entre sí;
4. esta PR de documentación.

**Ninguna PR está fusionada:** quedan para revisión.

| Documento | Contenido |
|---|---|
| `01_DIAGNOSTICO_R1_R5.md` | Punto exacto del código, protecciones existentes y causa de cada problema |
| `02_RESPUESTAS_INVALIDAS_Y_RESPALDO.md` | R-1: clasificación de respuestas, reintentos y respaldo |
| `03_TIMEOUTS_Y_CANCELACION.md` | R-3 y R-2: límites HTTP, cancelación y respuestas tardías |
| `04_ESTADOS_ADAPTACION.md` | R-4: tabla de estados y evidencia |
| `05_CONTROL_SOLICITUDES_Y_COSTOS.md` | R-5: exclusión, límite de frecuencia y supervisión del gasto |
| `06_PRUEBAS_Y_REGRESIONES.md` | Pruebas nuevas, fallos antes de la corrección, regresión y CI |
| `07_RECOMENDACION_FINAL.md` | ¿Lista para un entorno controlado de pruebas? Limitaciones que quedan |
