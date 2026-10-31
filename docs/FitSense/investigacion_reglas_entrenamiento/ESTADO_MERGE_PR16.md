# Estado del merge del PR #16

| Punto | Resultado |
|---|---|
| PR | [Fitsense-Real/Backend#16](https://github.com/Fitsense-Real/Backend/pull/16): **fusionado** el 31/10/2026 a las 21:17 UTC |
| Rama destino | `generation` (no `main`) |
| Método | Merge commit, el mismo procedimiento que los PR #13–#15. Sin force push. `generation` no tiene reglas de protección (la API responde «Branch not protected»). No se omitió ningún control |
| Commit de integración | `afef98a` («Merge pull request #16 from Fitsense-Real/politicas-p123») |
| Head fusionado | `4e171d6`, con CI verde (check `verify`: success) |
| P-01 | **30–75 minutos para los cuatro objetivos** (`GoalType.MAX_SESSION_MINUTES = 75`, `DuracionPorObjetivoPolicy.MAXIMO = 75`). V4: 75 → 86. Probado con 15, 29, 30, 60, 75, 76, 90, 120 y 180. Los perfiles históricos fuera de rango no se modifican y reciben `PERFIL_FUERA_DE_P01` |
| Contenido | C-2, P-02, P-03, V43 y las cuatro correcciones del motor (D-1 a D-4) |
| Pruebas antes del merge | `mvn verify -Pintegration`: **939 unitarias** y **458 de integración**, 0 fallos, 32 opt-in omitidas. Las 4 unitarias de más respecto a la referencia (935) son los casos nuevos de P-01 (75/76/90) |
| Replicate | **0 llamadas reales**: `fitsense.ai.enabled=false`; ninguna petición a `api.replicate.com` en los logs |
| CI de `generation` tras el merge | Ejecución `41242869086` (push a `generation`, commit `afef98a`): **success**. La ejecución `41242872779` es del PR #1 (borrador `generation`→`main`, que no se toca) |
| Despliegues | Ninguno. La API de GitHub no registra deployments en el repositorio. En `generation` solo hay checks de GitHub Actions (`verify`), ninguno de Railway ni de otros proveedores. El CI no tiene secretos ni pasos de despliegue. Railway no despliega desde `generation` (confirmado por el responsable, `CATALOGO_V37.md`) |
| Migraciones remotas | Ninguna. Hasta el merge, V43 solo existía en `politicas-p123`; ahora está en el código de `generation`. **No se conectó a Neon ni a Railway** |

## Hasta dónde llega la evidencia sobre V43

- **No se ha consultado `flyway_schema_history` de Neon ni de Railway**, porque no está autorizado. Por tanto, «V43 no aplicada» se afirma con la evidencia disponible:
  - V43 no existía en ninguna rama desplegable hasta este merge;
  - no hay deployments;
  - Railway no despliega desde `generation`.
- **Riesgo residual:** `application.properties` apunta a Neon con `spring.flyway.enabled=true`. Un arranque **manual** del backend de `generation` contra Neon aplicaría V43. Para evitarlo, usar `SPRING_FLYWAY_TARGET=42` o `SPRING_FLYWAY_ENABLED=false` hasta decidir su aplicación.

## Lo que no se hizo

- No se fusionó nada en `main`.
- No se desplegó.
- No se tocó Neon, Railway ni Flutter.
- No se fusionó ningún cambio experimental: la investigación vive en `research/exercise-limits-v20-4d`.
