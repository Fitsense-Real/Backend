# Contratos de la API revisados

**Alcance:**
- `durationInfo` (por sesión);
- `planningPolicy` (plan);
- la actividad aeróbica informativa (`aerobicActivity`);
- el diagnóstico NOFACT-0.1;
- el contrato con la IA (`max_session_seconds`).

**Todos en el PR #17**, salvo `max_session_seconds`, que está también en la rama aislada. **Todos apagados por defecto.**

## 1. Indicadores y compatibilidad

| Campo | Indicador (por defecto) | Apagado | Prueba HTTP |
|---|---|---|---|
| `workouts[].durationInfo` | `fitsense.api.duracion-estimada.enabled` (false) | No aparece (`@JsonInclude(NON_NULL)`) | `ContratoDuracionApagadoIT` (**nueva**), `ContratoDuracionEncendidoIT` (**nueva**) |
| `planningPolicy` (plan) | El mismo | No aparece | Las mismas |
| `aerobicActivity` (plan) | `fitsense.metricas.aerobica.enabled` (false) | No aparece | Las mismas |
| `diagnosis` (422 de `/plan/generate`) | `fitsense.api.no-factible-estructurado.enabled` (false) | 422 de siempre con `message` | `NoFactibleApiApagadoIT`, `NoFactibleApiEstructuradoIT` |

**Retrocompatibilidad:**
- Con los indicadores apagados, el JSON de `POST /plan/generate` y de `GET /plan/current` **no contiene** ningún campo nuevo. Lo comprueba `ContratoDuracionApagadoIT`, que pasa.
- Los campos de siempre (`expectedDurationMinutes`, `adjustmentApplied`, `adaptation`…) no cambian de nombre, tipo ni significado.
- Encendidos, solo se **añaden** campos opcionales.

## 2. `durationInfo`

| Campo | Tipo | Significado |
|---|---|---|
| `availableMinutes` | entero o null | Minutos disponibles según la traza (`minutos_disponibles`). Null sin traza (plan anterior a V44) |
| `estimatedMinutes` | entero | Segundos estimados de la traza / 60, redondeado; mínimo 1 |
| `differenceMinutes` | entero o null | `availableMinutes − estimatedMinutes` |
| `policy` | texto | Versión de la política del plan |
| `reason` | enumerado | `DENTRO_DEL_TIEMPO`, `DURACION_POR_CONTENIDO`, `REDUCCION_AUTORIZADA`, `SESION_ACORTADA`, `SIN_DATOS` |
| `message` | texto | Mensaje neutral para el usuario |

### Renombrado `ESTRUCTURA_COMPLETA` → `DURACION_POR_CONTENIDO` (hecho)

**Consumidores verificados antes de cambiarlo** (búsqueda en `src/`, configuración y migraciones):
- solo `DuracionYAerobicaAssembler`, su prueba y el Javadoc de `DurationInfoResource`;
- no existe en `generation`, el indicador está apagado y no se ha desplegado;
- el repositorio de Flutter no está en esta sesión; no consume `durationInfo`, que nunca se ha emitido en producción.

**Cambio:**
- el código se renombra en el ensamblador y en su prueba;
- el Javadoc menciona el nombre anterior;
- los documentos de órdenes anteriores se **conservan sin cambios**: describen el estado de entonces.

**Por qué:** «estructura completa» insinuaba que la sesión es completa o suficiente, que es justamente lo que la revisión profesional debe juzgar. «Duración por contenido» describe solo de dónde sale la duración.

### Mensaje neutral (alineado con la orden)

| `reason` | Texto |
|---|---|
| `DENTRO_DEL_TIEMPO` | «Tu rutina tiene una duración estimada de X minutos. Dispones de hasta Y minutos para entrenar.» |
| `DURACION_POR_CONTENIDO` | Lo anterior + «La duración se calculó según los ejercicios disponibles y las restricciones de tu plan.» |
| `REDUCCION_AUTORIZADA` | Lo anterior + «Esta semana la sesión es más corta porque tu plan se adaptó.» |
| `SESION_ACORTADA` | Lo anterior + «Se acortó para ajustarse al tiempo que indicaste.» |
| `SIN_DATOS` | «Duración estimada: X minutos.» |

**Cambio de esta orden:**
- los textos llevan ahora **tildes**. Antes eran ASCII («duracion», «calculo», «segun»), incoherentes con NOFACT-0.1 y con `/limitations`, que ya las usaban;
- la redacción de `DURACION_POR_CONTENIDO` es exactamente la de la orden.

**Prueba:** `DuracionYAerobicaAssemblerTest` comprueba:
- el texto exacto;
- que no aparece ninguna palabra prohibida (`optim`, `óptim`, `suficiente`, `añade`, `anade`, `agrega`, `rellena`, `incumpl`, `perder`, `garantiza`, `todos`, `complet`, `admite`, `máximo`, `maximo`).

### Observación (sin cambio)

**Hay dos fuentes de minutos distintas:**
- `availableMinutes` y el límite que muestra la traza (`limite_v4`) usan los **minutos efectivos** de la semana (`effectiveSessionMinutes`);
- el validador V4-C usa los **minutos del perfil** (`profile.sessionMinutes`).

**Cuándo importa:** cuando una adaptación fija unos minutos efectivos (`forcedSessionMinutes`) distintos de los del perfil, por ejemplo ACORTAR. En ese caso la traza muestra un límite distinto del que se aplicó; con ACORTAR (menos minutos) es **menor**, más estricto. No se ha comprobado aquí si alguna adaptación puede forzar **más** minutos que el perfil.

**Evidencia:** `PoliticaV4CCicloIT` comprueba, con el límite **más estricto** de la traza, que todas las sesiones caben.

**Decisión pendiente** (`DECISIONES_PARA_APROBACION.md`):
- o se documenta así,
- o se añade el límite aplicado a la traza.

## 3. `planningPolicy`

- **Qué es:** la versión con la que se generó y validó el plan (`PLAN-HIST-1`, `PLAN-V4-0.1`, `PLAN-C2D-…`).
- **Origen:** la columna `planning_policy` (V44). Un plan anterior a V44 la tiene en NULL y se interpreta como `PLAN-HIST-1`.
- **Para la app:** solo trazabilidad, **no mostrar**.
- **Estabilidad:** las versiones siguientes del mismo plan, como una reparación, heredan la política.

## 4. `aerobicActivity` (informativa)

| Campo | Significado |
|---|---|
| `programmedWorkMinutes` | Minutos de **trabajo** cardiovascular programados (series × segundos) |
| `programmedBlockMinutes` | Lo mismo, incluyendo las pausas entre series |
| `executedMinutes`, `externalMinutes` | Null hasta que haya registro |
| `intensity` | `DESCONOCIDA` mientras no se registre |
| `equivalentMinutes` | Null con intensidad desconocida (no se inventa la equivalencia) |
| `message` | Aviso informativo, sin metas ni juicios |

**No es** un objetivo ni se usa para generar ni para validar.

## 5. NOFACT-0.1 (`diagnosis` en el 422)

**Revisado sin cambios:** campos, valores y textos tal como se describen en `cierre_final_pr17/CONTRATO_NO_FACTIBLE_Y_DURACION.md` §1.

**Pendiente:** la incoherencia de las listas de equipamiento (3 códigos en `/limitations`, 6 en NOFACT-0.1). La propuesta unificada por niveles está en `ESPECIFICACION_FLUTTER.md` §5. **No aplicada:** cambia un contrato que ya está en `generation`.

## 6. Contrato con la IA (`max_session_seconds`)

| Política | Generación (`PlanInputSnapshot`) | Corrección de sesión (`SessionFixInputSnapshot`) | Prompt |
|---|---|---|---|
| PLAN-HIST-1 | Sin el campo (JSON idéntico al de `generation`) | Sin el campo | Sin cambios |
| PLAN-V4-0.1 (y combinaciones con V4 en el PR #17) | `constraints.max_session_seconds = M × 69` (p. ej. 2.070 para 30 min) | El mismo campo | Una frase: «Ninguna sesión puede pasar de N s … el backend los calcula a partir de los ejercicios y rechaza la sesión aunque declares menos minutos» |

**El backend no confía en la IA:** valida en segundos con el validador real, declare lo que declare. Lo prueban `V4CValidadorRealTest.caso142` y `ContratoV4CIATest`.
