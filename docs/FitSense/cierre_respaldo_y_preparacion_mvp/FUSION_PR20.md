# Fusión del PR #20 en `generation`

**PR:** Fitsense-Real/Backend#20, «Laboratorios opcionales con reloj de pruebas y diagnóstico del respaldo multisemana». Rama `lab/reloj-pruebas-opcionales` → `generation`.

## 1. Comprobaciones previas (fase 1)

| # | Condición | Resultado | Evidencia |
|---|---|---|---|
| 1 | Apunta a `generation` | **Sí** | Base dd61837 (= `generation`) |
| 2 | CI verde en su último commit | **Sí** | d4717b9: `verify` **success** (ejecución #106). bc05ec9: #104 success |
| 3 | Sin conflictos | **Sí** | `mergeable_state = clean`; base de fusión = cabeza de `generation` |
| 4 | Solo pruebas y documentación | **Sí** | 7 archivos, todos en `src/test` (+532/−8) |
| 5 | `src/main` intacto | **Sí** | `git diff generation...PR -- src/main`: 0 líneas |
| 6 | Las correcciones de fecha no alteran los escenarios | **Sí** | Semana = `RelojPruebas.SEMANA_REFERENCIA` (2026-11-02, la misma que tenían fija). No cambian perfiles, guiones, dosis ni valores esperados. Reincorporación seca idéntica byte a byte a su evidencia |
| 7 | La REPLAY conserva los resultados históricos | **Sí** | Evidencia idéntica salvo las marcas de tiempo de `exclusiones_dolor.csv` (`estabilizacion_ci_y_respaldo/REPLAY_100_SEMANAS.md`) |
| 8 | No se eliminan ni relajan pruebas | **Sí** | 0 líneas eliminadas con `assert*`, `@Test`, `@ParameterizedTest`, `@Enabled*` o `@Disabled` |
| 9 | Sin despliegue automático asociado | **Sí** | El único workflow es `.github/workflows/ci.yml` (`mvn verify`, Testcontainers, sin secretos ni pasos de despliegue). No hay `deploy` ni `railway` en `.github/` |

**El reloj de pruebas no escribe en bases remotas.** `RelojPruebas.abrirVentanaDeEjecucion` ejecuta una única sentencia:

```sql
UPDATE planned_workouts pw SET expires_at = GREATEST(pw.expires_at, now() + interval '7 days')
FROM weekly_training_plans p WHERE pw.plan_id = p.plan_id AND p.user_id = ?
```

- **Sobre qué base actúa:** el `JdbcTemplate` de la prueba, conectado por `@ServiceConnection` al PostgreSQL de **Testcontainers** de esa clase.
- **A quién afecta:** el `user_id` lo crea la propia prueba (`INSERT INTO users …@pruebas.invalid`). No hay URL, credenciales ni perfil que apunte a Neon o Railway. `CampanaP123Cohorte02IT` además comprueba que la URL es local y no contiene `neon` ni `railway`.

## 2. Fusión

| | Valor |
|---|---|
| Paso previo | Se quitó el estado de borrador (GitHub no fusiona borradores) y el prefijo «[BORRADOR · NO FUSIONAR]» del título |
| Método | *Merge commit*, como en los PR #18 y #19 |
| Cabeza fusionada | d4717b9437bc8cd60df77600df8c61c15a4b1269, comprobada con `expectedHeadSha` |
| **Commit de `generation`** | **8f112d5deb2bdef428a054a4be824bdf570deca2** |
| Árbol | **Idéntico** al de d4717b9 |
| `src/main` frente a dd61837 | Sin cambios |

## 3. Después de la fusión

| Comprobación | Resultado |
|---|---|
| CI de `generation` (`push`, #109, 8f112d5) | **success** |
| CI del PR #1 (rama `generation`, #110) | **success** |
| Cambios de producción | Ninguno |
| Migraciones remotas | Ninguna |
| Políticas activadas | Ninguna: `PLAN-HIST-1` y `allow-candidates=false` sin cambios |
| Suscripción a la actividad del PR #20 | Cancelada (PR cerrado) |

**Límite que se mantiene:** que Railway no despliegue desde `generation` sigue apoyándose en la confirmación documentada del responsable. No hay acceso a su panel.
