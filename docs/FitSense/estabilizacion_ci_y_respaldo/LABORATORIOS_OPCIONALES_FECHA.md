# Laboratorios opcionales y fecha (fase 4)

**PR independiente:** Fitsense-Real/Backend#20, rama `lab/reloj-pruebas-opcionales` desde `generation` dd61837. **En borrador y sin fusionar.**

**Commits:** bc05ec9 (laboratorios y diagnóstico) y d4717b9 (`LinajeIntegridadIT` y `PoliticasP123IT`, el mismo cambio que 97befb6 del PR #17). **Alcance:** solo `src/test` (7 archivos, +532/−8), sin cambios en `src/main`.

## 1. Riesgo de cada laboratorio

| Laboratorio | Semana | ¿Ejecuta sesiones? | Riesgo |
|---|---|---|---|
| `CampanaP123Cohorte02IT` (REPLAY de 100 semanas) | Fija, 2026-11-02 | Sí (`StartWorkoutSessionCommand`) | Con la semana vencida, «ya venció». Además, el barrido horario de vencidos actúa con el reloj real |
| `CampanaMvp8UsuariosIT` | Fija, 2026-11-02 | Sí (3 caminos) | El mismo |
| `ReincorporacionIaRealIT` | Fija, 2026-11-02 | Sí | El mismo |

**Las tres dejarían de poder ejecutarse a partir del 2026-11-09** (la primera sesión, la del lunes 2, vence hoy a las 23:59 hora local).

## 2. Corrección (la misma que el PR #19)

- `S1 = RelojPruebas.SEMANA_REFERENCIA`, el mismo valor 2026-11-02: no cambian las semillas ni los `user_id`.
- `RelojPruebas.abrirVentanaDeEjecucion(jdbc, u)` justo antes de cada `StartWorkoutSessionCommand`. Solo mueve `expires_at` al futuro para el usuario de la prueba.
- `@MockitoBean OverdueWorkoutScheduler`.

**No se ha tocado nada más:** ni perfiles, ni ejercicios, ni dosis, ni restricciones, ni estados, ni el guion, ni los valores esperados.

**Además:**
- `RelojPruebas` admite `-Dfitsense.pruebas.semana-referencia` **solo para diagnósticos**; sin la propiedad, 2026-11-02;
- `DiagnosticoRespaldoMultisemanaIT` es nuevo (ver `DIAGNOSTICO_RESPALDO_MULTISEMANA.md`).

## 3. Resultados (PostgreSQL efímero, 0 llamadas a Replicate)

| Ejecución | Contabilizadas | Ejecutadas | Aprobadas | Fallidas | Omitidas | Notas |
|---|---|---|---|---|---|---|
| `CampanaP123Cohorte02IT`, REPLAY (2026-11-02) | 2 | 1 | 1 | 0 | 1 (`inspeccionN04`, *opt-in*) | Evidencia idéntica a la registrada (ver `REPLAY_100_SEMANAS.md`) |
| `CampanaMvp8UsuariosIT`, ensayo seco (`-Dfitsense.campana.ia=false`, 2026-11-02) | 1 | 1 | 1 | 0 | 0 | 0 llamadas, 0 violaciones de linaje, 0 cortes |
| `ReincorporacionIaRealIT`, modo `seco` (2026-11-02) | 1 | 1 | 1 | 0 | 0 | `HISTORIAL_REINCORPORACION.csv`, `TRAZA_EJERCICIO_RETIRADO.csv`, `llamadas.jsonl` y `resumen.json` **idénticos byte a byte** a `cierre_mvp/evidencia/seco_LOSE_WEIGHT_60min_3dias/` |
| Los dos anteriores con una semana **ya vencida** (2026-10-19) | 2 | 2 | 2 | 0 | 0 | **0 errores de vencimiento.** Prueba de que el reloj funciona. Otras semillas: la reincorporación sigue completa (retirada → 6 progresiones → mantenimiento → reincorporación entrenada) |
| `ValidacionMultisemanaIT` con 2026-10-26 (vencida) | 57 | 55 | 53 | **2** | 2 | Los 2 de la aserción de respaldo (diagnóstico). **0 errores de vencimiento** |
| `ValidacionMultisemanaIT` con 2026-11-09 | 57 | 55 | 54 | **1** | 2 | El mismo motivo |
| `ValidacionMultisemanaIT` con 2026-11-02 | 57 | 55 | 55 | 0 | 2 | — |
| `DiagnosticoRespaldoMultisemanaIT` | 1 | 1 | 1 | 0 | 0 | 0 incumplimientos de política |

**Las omitidas no cuentan como aprobadas.** Los fallos de `ValidacionMultisemanaIT` con semanas distintas de la fijada son **esperados y explicados** (Resultado 2 del diagnóstico). No se ha cambiado la aserción.

**Comprobación de que no se llamó a Replicate:** 0 líneas `REPLICATE_TIMING` o `api.replicate.com` en los registros.
- Mvp8: `fitsense.ai.enabled=false`.
- Reincorporación `seco`: la generación inicial se rechaza antes de llamar, y `pedirRespuesta` devuelve la corrección simulada sin llamar al método real (`ReincorporacionIaRealIT.espiar`).

## 4. Qué queda sin verificar

| Elemento | Motivo |
|---|---|
| `ReincorporacionIaRealIT` con IA real (`-Dfitsense.reincorporacion.ia=true`) | Consume Replicate: **no autorizado**. Solo se ejecutó el modo `seco` |
| `CampanaMvp8UsuariosIT` con IA real | El mismo motivo |
| `CampanaMvp8UsuariosIT`, conjunto `estres` (ensayo seco) | **Falla al crear el perfil**: «La duración por sesión debe estar entre 30 y 75 minutos (objetivo GENERAL_FITNESS); indicaste 15». Uno de sus perfiles es de 15 min y la política P-01 (PR #16) lo rechaza desde entonces. **No depende de la fecha** y fallaría igual en `generation`. No se ha cambiado el perfil, porque eso sería alterar las condiciones del escenario. Para volver a ejecutarlo hay que decidir qué perfil lo sustituye |
| `CampanaP123Cohorte02IT.inspeccionN04` | *Opt-in* de inspección; no forma parte de la REPLAY |
