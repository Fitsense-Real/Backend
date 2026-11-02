# Fusión del PR #21 en `generation`

**PR:** Fitsense-Real/Backend#21, «D1: el respaldo se comprueba con sus invariantes; prueba explícita del respaldo con dolor».

**Autorización:** orden de cierre del backend, fase 1, condicionada a un CI final verde y a que no haya despliegue.

## 1. Comprobaciones previas (las 9 de la orden)

| # | Comprobación | Resultado |
|---|---|---|
| 1 | Base | `generation` (8f112d5) |
| 2 | Corrección autorizada | Sí: D1 de la orden de cierre del respaldo |
| 3 | CI verde en la cabeza | `verify` **success** en e033025 (ejecución 113) |
| 4 | Sin conflictos | `mergeable_state = clean` |
| 5 | `src/main` sin cambios | `git diff 8f112d5 7c19703 -- src/main` vacío. Son 4 archivos, todos en `src/test` (+986/−5) |
| 6 | No se retira ninguna comprobación de seguridad | Sí: la igualdad absoluta de RESPALDO se sustituye por invariantes **más estrictas** en lo que importa (ver §2). Sin dolor, no publicar sigue siendo un fallo |
| 7 | `RespaldoConDolorIT` incluido y ejecutado | Sí: forma parte del CI (no es *opt-in*). Casos A, B y C con las semanas 26-oct, 2-nov y 9-nov: 0 fallos |
| 8 | Sin cambios de producción | Sí: solo pruebas |
| 9 | Sin despliegue ni migraciones | Ninguno. Sin Railway, sin Neon, sin migraciones nuevas |

## 2. Invariantes revisadas (`InvariantesRespaldo`)

| Caso | Exige |
|---|---|
| **A. RESPALDO sin dolor** | La semana medida con las fechas +7, **idéntica**: mismas sesiones, enfoques, minutos, ejercicios y dosis |
| **B. RESPALDO con dolor** | Lo mismo, salvo **una sustitución SUST-SEG-1** del ejercicio con exclusión P-02 activa. La sustitución debe cumplir todo esto: <br>• mismo tipo de prescripción; <br>• misma clase de slot; <br>• misma posición; <br>• sin subir series, repeticiones ni segundos; <br>• sin subir el trabajo total de la sesión; <br>• trazada en V32 con la regla `SUST-SEG-1`; <br>• nota `REGENERACION_SEGURA` y origen `CONSERVED`. <br>La semana siguiente no reincorpora lo que dolió |
| **C. Sin solución segura** | V32 en estado `NO_FACTIBLE` y con motivo. Además: **ningún plan activo** para la semana nueva y las exclusiones por dolor **siguen activas** (no se levanta una restricción para poder publicar) |

**Siguen fallando, como antes:**
- sustituir un ejercicio no excluido;
- un cambio sin traza;
- otro tipo o slot;
- más dosis o trabajo;
- otros minutos;
- publicar lo que dolió;
- no publicar sin dolor.

## 3. Fusión y verificación posterior

| | Valor |
|---|---|
| Commit de fusión en `generation` | **7c19703** (2026-11-02 16:08 UTC) |
| CI en 7c19703 | **success** en la ejecución 116 (*push* a `generation`) y en la 117 (PR de `generation`) |
| Política por defecto | `PLAN-HIST-1` (`PlanningPolicySelector`, `@Value("${fitsense.planning.policy:PLAN-HIST-1}")`) |
| `allow-candidates` | `false` por defecto |
| Políticas experimentales | Ninguna activa. C2-D, EF, LW-PREF, REP-SUST y NOFACT-0.1 **no están en `generation`**: solo en el PR #17 |
| Migraciones | `generation` llega hasta V44; sin migraciones nuevas por el PR #21 |
| Despliegue | Ninguno |
