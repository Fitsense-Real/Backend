# Regresión de progresión, reincorporación y linaje

Fecha: 16/11/2026. Rama `fix/reparacion-linaje-determinista` (PR #73), basada en `generation` (`2578719`).

No se ha cambiado ninguna política. El diff de la rama contra `generation` solo toca dos archivos:

- `ReparacionLinajeIT.java` (prueba);
- la documentación de esta carpeta.

Siguen sin cambios la progresión, la reincorporación, V24, V8C, SUST-SEG-1, la adherencia y el catálogo.

## 1. Qué casos pide la orden y qué prueba los cubre

`CicloCompletoHttpIT` está en la rama `validacion/backend-integral`; las demás clases están en `generation`.

| Caso | Pruebas que lo cubren | Cómo lo comprueban |
|---|---|---|
| Domingo y lunes | `TrainingWeekTest` (18) | Cada día de la semana de referencia, el domingo anterior y el lunes siguiente. `covers()` excluye los dos bordes. |
| Cambio de mes | `TrainingWeekTest`; `ReparacionLinajeIT.repararYHeredarEnCambiosDeMesYAno` (**nueva**) | Semanas del 28/09 al 04/10. En la nueva prueba, el escenario completo arranca el 16/11/2026, así que S1+2 cae en el cambio a diciembre. |
| Cambio de año | `TrainingWeekTest` (31/12 al 04/01, año que empieza en lunes, bisiestos, `next()`/`previous()` 60 semanas); `repararYHeredarEnCambiosDeMesYAno` (**nueva**) | La nueva prueba arranca el 14/12/2026: S1+2 = 28/12, una semana que cruza al 2027, y H6 se cumple en 2027. |
| Semanas consecutivas | `ProgresionCienPorCienIT` (5), `ProgresionIT` (24), `ProgresionRev2IT` (24), `PendientesProgresionIT` (6) y su REV2 (6), `CicloCompletoHttpIT` (8) | Dos semanas al 100 % dan una progresión. La adaptación sigue en el 80 %. Hay recuperación de series y de carga, y ciclos simultáneos. |
| Adaptaciones | `PoliticasP123IT` (39), `AdaptacionCompletaIT`, `AdaptacionPorCausasIT`, `HerenciaD8IT` (6), `HerenciaReduccionV20Test` (14) | La herencia D8 se corta cuando cambia la sesión o hay dolor. Ninguna colocación recupera una dosis reducida sin autorización. |
| Progresión | `ProgresionV24V8CTest` (18), `ProgresionIT`, `ProgresionCienPorCienIT`, `O12DolorProgresion{Apagada,Encendida}IT` | La dosis progresada cumple V24 y V8C. Lo que dolió no vuelve, con la progresión encendida o apagada. |
| Ejercicios retirados | `RetiradaLocalizada*IT` (6 clases), `RetiradaLocalizadaV8CTest` (13), `V8CRetiradasYDescansoTest` (25), `SinSustitutoDolor*IT` | V8C solo admite una retirada si es exactamente la autorizada (H-A1). |
| Reincorporación autorizada | `ReincorporacionTrasProgresionTest` (7), `Reincorporacion4DTest` (5), `PoliticasP123IT` (reactivación con `confirmNoPain=true`), `CicloCompletoHttpIT.dolorYReincorporacion` | Solo una PROGRESIÓN posterior al acortamiento, de la misma sesión, habilita la reincorporación. La dosis sale de 4D. |
| Reincorporación no autorizada | `ReincorporacionTrasProgresionTest`, `HerenciaReduccionV20Test`, `PoliticasP123IT` (`confirmNoPain=false` → 422), `CicloCompletoHttpIT` | Sin autorización no se recupera nada, y la reactivación sin confirmar se rechaza. |
| Reparación del historial | `ReparacionLinajeIT` (3) y `ReparacionLinajeRev2IT` (3), `LinajeIntegridadIT` y su REV2 | P-P2: la semana progresada se repara con la dosis intacta. H6: el ciclo sigue el linaje por la versión reparada. El caso NO_VALIDA no publica nada (**nueva**). |

## 2. Pruebas nuevas o modificadas

En `ReparacionLinajeIT`, que hereda `ReparacionLinajeRev2IT` con PLAN-REV-2.0, todas las aserciones originales se
conservan.

- **`repararYHeredar`.** El escenario original, ahora con S1 = `RelojPruebas.SEMANA_REFERENCIA` (02/11/2026) en vez de
  `hoy + 1 semana`. La premisa de P-P2 es ahora explícita: se elige un relleno con la misma capacidad que el sustituto
  (2x13/90).
- **`repararYHeredarEnCambiosDeMesYAno` (nueva).** El mismo escenario con S1 = 16/11/2026 (cambio de mes) y S1 =
  14/12/2026 (cambio de año).
- **`sinSustitutoConLaMismaDosisNoPublicaNada` (nueva).** Convierte un relleno de **otra** capacidad (2x10/90). Comprueba
  que:
  - el resultado es NO_VALIDA por V24;
  - no se publica ningún plan;
  - el número de planes no cambia;
  - el plan activo sigue siendo S1+2;
  - la dosis progresada queda intacta.

  Es el comportamiento conservador aprobado (`IMPACTO_FACTIBILIDAD.md`, R12). Antes solo se veía cuando la prueba
  fallaba.

## 3. Resultados

### Regresión completa de la rama (`mvnw verify -Pintegration`)

| Suite | Pruebas | Fallos | Errores | Omitidas |
|---|---|---|---|---|
| Unitarias | 1540 | 0 | 0 | 1 |
| Integración | 759 | 0 | 0 | 57 |

Tiempo: 24 min 18 s. Las 57 omitidas son las opt-in de siempre: laboratorios, campañas con Replicate real y barridos.

### Subconjunto de progresión, reincorporación, linaje, dolor, sustitución y adherencia

El subconjunto tiene 58 clases, con 484 pruebas, 0 fallos, 0 errores y 12 omitidas (opt-in). Las clases principales:

| Clase | Pruebas | Omitidas |
|---|---|---|
| `ProgresionIT` / `ProgresionRev2IT` | 24 / 24 | 0 |
| `PoliticasP123IT` | 39 | 0 |
| `ProgresionCienPorCienIT` | 5 | 0 |
| `PendientesProgresionIT` / Rev2 | 6 / 6 | 0 |
| `ReparacionLinajeIT` / Rev2 | 3 / 3 | 0 |
| `LinajeIntegridadIT` / Rev2 | 1 / 1 | 0 |
| `HerenciaD8IT` | 6 | 2 |
| `O12DolorProgresionApagadaIT` / `EncendidaIT` | 3 / 3 | 0 |
| `RespaldoConDolorIT` | 3 | 0 |
| `RetiradaLocalizada{Limite,Preferencia}IT` | 4 / 4 | 0 |
| `ReincorporacionTrasProgresionTest` / `Reincorporacion4DTest` | 7 / 5 | 0 |
| `ProgresionV24V8CTest` | 18 | 0 |
| `V8CRetiradasYDescansoTest` / `V8CConservacionPatronH1Test` | 25 / 32 | 0 |
| `DosisIaV8CTest` / `HerenciaReduccionV20Test` | 40 / 14 | 0 |
| `LimiteDificultadDolorTest` / `SustitucionSeguridadTest` | 22 / 22 | 0 |
| `SustitutoDosisCapacidadTest` / `SustitutoGmSinMasTrabajoTest` | 9 / 4 | 0 |
| `TrainingWeekTest` | 18 | 0 |
| `AdherenciaIT` / `AdherenciaRiesgoIT` / `AbandonoIT` | 13 / 5 / 10 | 0 |

### Robustez frente a la fecha

`ReparacionLinajeIT` (3 pruebas) se repitió con la semana de referencia cambiada
(`-Dfitsense.pruebas.semana-referencia`) a 23/11/2026, 04/01/2027 y 07/06/2027. Pasó las tres veces. Con la prueba
antigua, esas tres semanas daban NO_VALIDA (`DIAGNOSTICO_REPARACION_LINAJE.md`, barrido).

### GitHub Actions

PR #73, `verify` en verde: 16/11/2026, 04:01–04:23 UTC.

## 4. Conclusión

La reparación del linaje preserva las políticas:

- **REPARADA.** La dosis progresada se conserva y la semana siguiente la hereda (H6).
- **Sin sustituto de la misma dosis.** No se publica nada y no se sube el trabajo.
- **Fechas.** Domingo/lunes y los cambios de mes y de año no alteran el resultado.

Riesgo latente, no corregido aquí: 38 clases de integración siguen calculando la semana con `LocalDate.now()`. Hoy
pasan, pero son del mismo tipo que la que falló.
`docs/FitSense/validacion_integral/PENDIENTES_PRIORIZADOS.md` las registra con prioridad.
