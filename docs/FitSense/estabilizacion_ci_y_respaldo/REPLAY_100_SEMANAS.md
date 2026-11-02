# Campaña REPLAY de 100 semanas

**Prueba:** `CampanaP123Cohorte02IT` (`-Dfitsense.campana.p123=true`), cohorte `FITSENSE_STRESS_P123_COHORT_02`:
- N01-N10 durante 10 semanas (100 semanas-persona);
- extensión hasta la semana 16;
- variantes posteriores.

**Entorno:**
- código: rama `lab/reloj-pruebas-opcionales`, **bc05ec9** (`generation` dd61837 más el reloj de pruebas);
- semana de inicio: 2026-11-02 (`RelojPruebas.SEMANA_REFERENCIA`, la misma de siempre);
- PostgreSQL efímero e IA **simulada** (respuestas fijas por guion);
- **0 llamadas a Replicate**;
- comprobación propia de que la URL es local y no es Neon ni Railway.

## 1. Resultado

| | Valor |
|---|---|
| Pruebas | 2 contabilizadas, **1 ejecutada y aprobada**, 1 omitida (`inspeccionN04`, *opt-in* de inspección), 0 fallos |
| Duración | 140,8 s |
| Errores de vencimiento | 0 |

## 2. Comparación con la evidencia histórica (`docs/FitSense/campana_p123/evidencia/`)

La prueba vuelve a escribir los 11 archivos de evidencia, que se compararon con `git diff` contra los registrados:

| Archivo | Qué contiene | Filas | Resultado |
|---|---|---|---|
| `generacion.csv` | Generación inicial de los 12 perfiles | 12 | **Idéntico** |
| `planes.csv` | Cada plan, sesión y ejercicio publicado, con linaje (`source_planned_workout_id`), estado y dosis | 2.066 | **Idéntico** |
| `historial.csv` | Una fila por ciclo: decisión, estado V32, resultado de la API, intervenciones, progresiones y exclusiones | 139 (100 principal + 12 extensión + 27 variantes) | **Idéntico** |
| `v32.csv` | Evidencia V32 completa (`conservation_report`) | 139 | **Idéntico** |
| `PASOS.csv` | Pasos de progresión y reincorporación con su autorización y validación independiente | 67 | **Idéntico** |
| `exclusiones_dolor.csv` | Memoria del dolor P-02 | 2 | **Idéntico salvo `recorded_at` y `reactivated_at`** (marcas de tiempo reales). Normalizadas las marcas, es igual. Se revirtió, como en las campañas anteriores |
| `regeneraciones.csv` | P-03 nivel 2 | 0 | **Idéntico** (vacío) |
| `metricas.csv`, `perfiles.csv`, `ids.csv`, `nombres_ejercicios.csv` | Métricas semanales, perfiles, ids y catálogo usado | — | **Idénticos** |

**No hay diferencias funcionales:** ni de ejercicios, ni de dosis, ni de decisiones, ni de estados, ni de linaje.

## 3. Lo que pide verificar la orden

| Aspecto | Evidencia en la REPLAY | Resultado |
|---|---|---|
| Generación y publicación semanal | 12 generaciones `PUBLICADO`; 139 ciclos con plan resultante | Igual que la evidencia histórica |
| Adaptaciones | V32: 128 `SIN_ORDEN`, 5 `SIN_EFECTO`, 3 `APLICADA`, 3 `RESPALDO` | Igual |
| Exclusiones por dolor | 2: una `ACTIVA` (usuario 7, ejercicio 129) y una `REACTIVADA` por confirmación explícita (usuario 9, ejercicio 791) | Igual (salvo marcas de tiempo) |
| Progresión | 65 pasos `PROGRESION`, 1 `VUELTA_ATRAS` | Igual |
| Reincorporación autorizada | 1 `REINCORPORACION`, tras la reactivación explícita (N08) | Igual |
| Linaje | `planes.csv` (con `source_planned_workout_id`) idéntico | Igual |
| Idempotencia | La campaña no repite ciclos; lo que demuestra es **reproducibilidad**: la misma ejecución da la misma evidencia byte a byte. La idempotencia de un ciclo repetido la cubren `CicloConcurrenteIT`, los ciclos repetidos de `PoliticaV4CCicloIT` y los seis `CicloReal*IT` (ver `REGRESION_PR17_ACTUALIZADO.md`) | Sin fallos |
| Ausencia de modificaciones inesperadas | Diff vacío salvo marcas de tiempo | Sí |

## 4. Relación con el diagnóstico del respaldo

La evidencia histórica de la REPLAY ya contenía **un RESPALDO con dolor resuelto por P-03**:
- usuario 7, semana del 2026-11-30, `ADAPT_LOCALIZED/RESPALDO {SEGURIDAD_ANTE_DOLOR}`, origen `CONSERVED`;
- `conservation_report.sustituciones = [{quitado: 129, puesto: 78, regla: SUST-SEG-1, motivo: dolor_registrado}]`.

Es el mismo comportamiento que hace fallar la aserción de `ValidacionMultisemanaIT` con otras semillas. Aquí se aceptó y se registró como evidencia de la política P-03 aprobada.

Los otros dos RESPALDO de la campaña no son de dolor:
- usuario 5: `REPROGRAMAR_DIAS`, con nota `FACTIBILIDAD_PREVIA`;
- usuario 8: `ACORTAR_O_REORGANIZAR` en `ADAPT_EXPANDED`.

Los dos son **copias exactas**: las sesiones señaladas aparecen como `conservadas` y sin sustituciones.
