# 08 · 960 perfiles y REPLAY de 100 semanas

**Comando:**
```
mvnw -o -B -ntp verify -Pintegration -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=ComparacionPerfilesIT,CampanaP123Cohorte02IT -Dfitsense.comparacion.perfiles=true \
  -Dfitsense.campana.p123=true -Dfitsense.comparacion.salida=<dir>
```

## 1. Referencia

`generation` no ha cambiado desde la referencia histórica (`0974fb3`), así que **la referencia histórica y la actual son la misma**:
- `planes.csv` `d109a3daa156d6d28bdfbebf109e5b5066bee8213c3663b21ad7b177dd18a859`
- `colocaciones.csv` `d227848ba040e73af459b052ab518f67d0b80af55cc731cbe2a455f5abe265ef`

La REPLAY compara contra la evidencia guardada en `generation` (`docs/FitSense/campana_p123/evidencia/`).

## 2. Resultados

| Rama | Commit | 960: `planes.csv` | 960: `colocaciones.csv` | REPLAY | Cambia en la evidencia |
|---|---|---|---|---|---|
| PR A (H1) | `983e216` | **idéntico** | **idéntico** | **pasa** | solo `exclusiones_dolor.csv`: 2 filas, únicamente `recorded_at` y `reactivated_at` (reloj real) |
| PR B (textos) | ver `09` | ver `09` | ver `09` | ver `09` | ver `09` |

**Pruebas:** 3 ejecutadas, 2 superadas, 0 fallidas, 1 omitida (modo inspección, `fitsense.campana.p123.inspeccion`). La REPLAY en sí se ejecutó y pasó.

**Lo que no cambia, archivo por archivo:**

| Archivo de la REPLAY | Contenido | Resultado |
|---|---|---|
| `generacion.csv`, `planes.csv` | Planes iniciales y semanales, linaje `source_planned_workout_id` | Idénticos |
| `metricas.csv` | Aw, Aobs y cobertura de registro | Idéntico |
| `historial.csv` | Decisión semanal, estado V32, resultado API, intervenciones | Idéntico |
| `v32.csv` | Intentos de adaptación | Idéntico |
| `PASOS.csv` | Pasos de progresión con autorización y validación independiente | Idéntico |
| `regeneraciones.csv` | Regeneración segura | Idéntico |
| `exclusiones_dolor.csv` | Exclusiones por dolor | Igual, salvo dos marcas de tiempo |

**No se sobrescribe ninguna evidencia:** la REPLAY reescribe `exclusiones_dolor.csv` al ejecutarse y el cambio se descartó con `git checkout`. El diff está en `evidencia/replay_A_diferencias.diff`.

## 3. Interpretación

**H1 no cambia:**
- la generación inicial;
- la adherencia;
- las decisiones semanales;
- el linaje;
- las dosis.

Lo esperable, porque **ninguna propuesta registrada en la REPLAY añade dos ejercicios de un patrón retirando uno** (son 0 de 123 en el histórico real). La única diferencia autorizada de H1, el rechazo de esa distribución, no aparece en estos datos.

**No se modificó ninguna política** para conseguirlo.
