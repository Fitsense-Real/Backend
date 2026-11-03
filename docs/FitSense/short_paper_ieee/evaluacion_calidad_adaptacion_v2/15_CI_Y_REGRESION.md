# CI y regresión

## 1. CI de GitHub del PR #29 (fase 9.1–9.2)

| Campo | Valor |
|---|---|
| SHA del PR #29 | `51c145b2887e621acc52fc81605f58e64edd0a5c` |
| Rama base | `lab/experimento-kora-20usuarios` (PR #28, `0f17ac7`). #28 es antecesor de #29 |
| *Workflow* | `.github/workflows/ci.yml`. Se dispara con `pull_request` **sin filtro de rama**; el filtro `main`/`generation` es solo para `push` |
| Ejecución | [41577492912](https://github.com/Fitsense-Real/Backend/actions/runs/41577492912), job `verify` (`mvn -B -ntp verify -Pintegration`) |
| SHA ejecutado | `51c145b` = cabeza del PR ✔ |
| Fecha | 03/11/2026, de 04:19:58 a 04:33:22 UTC |
| Resultado | **success** |
| Pruebas | Integración: **489 ejecutadas, 0 fallos, 37 omitidas** (*opt-in*). Unitarias: aprobadas (`BUILD SUCCESS`). El recuento exacto no se pudo extraer porque el proxy de esta sesión bloquea la descarga del ZIP de registros |
| Checks pendientes | Ninguno |

**No hizo falta ninguna ejecución manual ni cambiar el *workflow*.** Ningún PR se fusionó.

**El informe anterior decía que el PR #29 no tendría CI: era un error (H10 en `08`).**

**CI de esta rama:** se ejecuta en su propio PR borrador (base: `experiment/adaptation-prompt-v2`). El resultado se añade como comentario del PR cuando termina.

## 2. Regresión local de esta rama (fase 9.3)

**Rama:** `experiment/adaptation-v2-quality-validation`.
- **Sin cambios en `src/main` ni en `pom.xml`** respecto a `51c145b`.
- Solo cambia `src/test`: el laboratorio (*opt-in*) y la nueva prueba de diagnóstico de V8C.

**Condiciones:** base PostgreSQL efímera (Testcontainers), indicador V2 apagado (configuración por defecto `v1`), **ninguna llamada a Replicate**.

| Bloque | Aprobadas | Fallidas | Omitidas | Notas |
|---|---:|---:|---:|---|
| Unitarias (`verify -Pintegration`, surefire) | **1.072** | 0 | 0 | 1.061 del PR #29 + 11 de `V8CDiagnosticoSustitucionesTest` |
| Integración completa (failsafe): generación, V1–V25, V8C, V20, V24, adherencia, adaptación, RESPALDO, NO_FACTIBLE, dolor (P-02), progresión, reincorporación, concurrencia, idempotencia, seguridad y contratos de la API | **452** | 0 | **37** | 489 ejecutadas. Las 37 omitidas son *opt-in* (Replicate real, Neon, laboratorios con bandera). **No se cuentan como aprobadas** |
| 960 perfiles (`ComparacionPerfilesIT`) | 1 | 0 | 0 | **Idéntico byte a byte** a la base: `planes.csv` `d109a3da…`, `colocaciones.csv` `d227848b…` (los mismos *hashes* que en `51c145b` y en la base `0f17ac7`) |
| REPLAY de 100 semanas (`CampanaP123Cohorte02IT`) | 1 | 0 | 1 (inspección manual, *opt-in*) | **Igual.** Solo cambian 2 líneas de `exclusiones_dolor.csv`, y solo en las marcas de tiempo (se comparó normalizándolas). Se descartó sin *commit* |

**V1 apagado conserva su comportamiento histórico:**
- el prompt C1 es idéntico byte a byte al de la etapa C (`experimento_prompt_v2/06` §3);
- los 960 perfiles y la REPLAY, iguales;
- las 1.061 unitarias previas y las 452 IT aprobadas de siempre siguen aprobadas.
