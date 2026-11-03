# Regresión del backend con el indicador apagado

**Rama:** `experiment/adaptation-prompt-v2`. Configuración por defecto: `fitsense.ai.adaptation.prompt-version=v1`.

**Base de datos:** PostgreSQL efímera (Testcontainers). **Ninguna llamada a Replicate** en la regresión.

## Resultados

| Bloque | Resultado |
|---|---|
| Unitarias (`mvnw test`) | **1.061 ejecutadas, 0 fallos, 0 omitidas** (1.043 de la base + 18 de `SessionFixPromptV2Test`) |
| Integración completa (`-Pintegration verify`): generación, V1–V25, V8C, V24, adherencia, adaptación, RESPALDO, P-02/P-03, progresión, reincorporación, concurrencia, idempotencia, seguridad y contratos de la API | **489 ejecutadas, 0 fallos, 37 omitidas.** Son las mismas cifras que en la base de PR #28 (`22_REPRODUCIBILIDAD_Y_REGRESION.md` §3). Las 37 omitidas son *opt-in* (laboratorios con bandera), **no se cuentan como aprobadas** |
| 960 perfiles (`ComparacionPerfilesIT`) | **Idéntico byte a byte** a la misma prueba ejecutada sobre la base `0f17ac7` (rama del PR #28 = `generation` `0974fb3` + laboratorio): `planes.csv` `d109a3da…` (1.921 líneas) y `colocaciones.csv` `d227848b…` |
| REPLAY de 100 semanas (cohorte 02, `CampanaP123Cohorte02IT`) | **Igual.** Solo cambian 2 líneas de `exclusiones_dolor.csv`, y solo en las marcas de tiempo (`2026-10-31 17:35…` → `2026-11-03 04:16…`). Se descartaron sin *commit*, como en las regresiones anteriores |
| Prompt de adaptación apagado | **Idéntico byte a byte** a los 8 prompts enviados en la etapa C original (`06` §3) |
| Mismo guion con V1 y V2 (GUION) | El mismo resultado en la base (`06` §2) |

**Nota sobre los 960 perfiles:** el `planes.csv` de la referencia histórica (`761d67cf…`, 961 líneas) es el de otra versión de la prueba. Para no comparar con un formato distinto, se ejecutó **la misma prueba sobre la base, en un *worktree* temporal**, y se compararon las salidas.

## Cambios en `src/main` y efecto con el indicador apagado

| Archivo | Cambio | Efecto apagado |
|---|---|---|
| `SessionFixPromptBuilder` | Indicador `prompt-version`; bloques V2 solo con `v2` | Ninguno: mismo texto (pruebas 1 y 3) |
| `FactibilidadAdaptacion` (nuevo) | Cálculo descriptivo para V2 | No se invoca |
| `SessionFixInputSnapshot` | `of(full, version)`; `of(full)` delega con `FIX-PR-1.3` | Ninguno |
| `ReplicateTrainingPlanGenerator` y `ReplicateSessionFixGenerator` | Etiqueta de la petición con la versión real | La misma etiqueta (`FIX-PR-1.3 …`) |
| `PlanDraftValidator` | `MIN_SETS`, `MIN_REPS` y `MIN_DURATION_SECONDS` pasan de `private` a `public` (solo lectura) | Ninguno: mismos valores y misma lógica |
| `application.properties` | `fitsense.ai.adaptation.prompt-version=v1` | El valor por defecto |

**Ningún validador, umbral, fórmula, catálogo, dosis ni política ha cambiado.**
