# Seguridad O20 · opción A: tras dolor, ninguna variante equivalente vuelve

**Orden:** «ORDEN — CORRECCIÓN DE SEGURIDAD O20 / SUST-SEG-1», 24/11/2026.
**Decisión:** opción A.
**Autorización:** implementar y probar. **NO MERGE.**

**Rama:** `fix/sust-seg-1-familias-dolor`, desde `generation` en `e46dbe18`. PR de seguridad independiente, sin relación con la migración de Replicate (#83).

**Replicate:** sin llamadas. Todas las pruebas usan el motor de reglas y la IA de adaptación simulada.

## 1. El problema (O20)

- **Caso real:** evaluación gpt-5-mini, corrida `G5M_MEDIUM_P`, perfil O20.
- **Qué pasó:**
  - dolor en **537 «dumbbell front raise»** en la sesión del viernes;
  - 537 estaba también en la sesión del lunes, que se conservaba;
  - **SUST-SEG-1** lo sustituyó por **538 «dumbbell front raise v. 2»**, el mismo gesto.
- **Por qué:** la exclusión por dolor (P-02) era por `exercise_id`, y SUST-SEG-1 prefiere, a propósito, la misma zona, el mismo patrón y el mismo músculo.

**Reproducido sin IA** sobre `generation` sin cambios (`DolorFamiliaEquivalenteAntesIT`, `evidencia/ANTES_*`):

| | Sin restricciones | Con restricciones personales |
|---|---|---|
| Sustitución de SUST-SEG-1 | `"quitado": 537, "puesto": 538` en las dos sesiones | `"quitado": 537, "puesto": 538` en las dos sesiones |
| Resultado | El plan activo contiene 538 | El plan activo contiene 538 |

## 2. La corrección

**Familia de movimiento equivalente** (`catalog/domain/services/FamiliaDeMovimientoEquivalente`, dominio puro). Dos ejercicios son equivalentes si cumplen cualquiera de estos niveles:

| Nivel | Regla | Ejemplos |
|---|---|---|
| **1 · variante del mismo ejercicio** | El mismo `name_en` sin mayúsculas, sin espacios sobrantes y sin el sufijo final `v. N`. No exige el mismo patrón | 537 «dumbbell front raise», 538 «… v. 2» y el duplicado histórico inactivo 28 «Dumbbell front raise»; «barbell upright row» v. 1/2/3, con patrones distintos; «lever shoulder press» v. 1/2/3 |
| **2 · mismo gesto con otro material o postura** | El mismo patrón, el mismo músculo objetivo y el mismo **núcleo** del nombre: el nombre sin material (dumbbell, barbell, cable, band, lever, smith, kettlebell…) ni postura (seated, standing, incline, alternate, one arm…) | Las 7 elevaciones frontales con deltoides: 537, 538, 99 band, 150 barbell, 334 cable, 703 y 707 seated |
| **No equivalentes** | Otro movimiento del mismo patrón | 578 «dumbbell lateral raise», 539 «dumbbell full can lateral raise», 579 «lateral to front raise», 746 «front raise above head» |

**El nivel 2 va más allá de las 25 familias** que se identificaron en la evaluación, que son solo las del nivel 1.
- **Motivo:** con solo el nivel 1, «dumbbell seated front raise» (707) seguía siendo elegible y podía ser el primer sustituto para otro perfil. Eso contradice «no debe proponer otra variante equivalente que pueda reproducirlo».
- **Es conservador:** agrupa de más antes que de menos.
- **No es un criterio clínico.** El inventario completo, para revisarlo, está en `evidencia/familias_equivalentes_catalogo_activo.csv`: 515 ejercicios activos con al menos un equivalente. La relación es simétrica, como comprueba la prueba.
- **Si se prefiere solo el nivel 1**, basta con quitar el nivel 2 de `ExerciseQueryServiceImpl`, en un solo punto.

**Dónde se aplica:** en un solo sitio, `PainMemoryService.excluidosActivos(userId)`. Devuelve lo registrado (`ACTIVA`) **más su familia**. Es la lectura que usan todos los caminos que aplican el dolor:

| Camino | Efecto |
|---|---|
| `prepare` (generación, adaptación y regeneración) | La familia sale del **conjunto elegible**. La IA no la ve; si la propone, **V1 la rechaza** (V1 no cambia). El motor de reglas, la progresión y la reincorporación no la encuentran |
| `SafetyExclusionResolver` → SUST-SEG-1 | Una variante heredada también se trata como excluida por dolor. **El sustituto nunca es de la familia**, porque se elige entre los elegibles |
| `PublishedPlanSafetyService` | Con `fitsense.seguridad.dolor-en-planes-publicados=true`, que es el valor por omisión, la API marca `safetyBlockReason` también en una variante presente en una sesión abierta |
| `SafeRegenerationService` (P-03) | La familia cuenta como retirada |

**Duración de la exclusión:**
- La familia es **derivada**, no se guarda.
- Dura exactamente lo que la exclusión del ejercicio que dolió, que sigue `ACTIVA` hasta su **reactivación explícita** por la persona.
- No puede quedar una variante excluida «huérfana» ni volver antes.

**Sin alternativa fuera de la familia:** el mecanismo seguro existente, sin cambios:
1. retirada localizada en la sesión señalada por dolor (`RetiradaSeguridad`, que V8C ya admite);
2. en una sesión no señalada, nada se modifica por esa vía: el resultado es **NO_FACTIBLE con su causa** y P-03.

**No se modificó** V8C, V24, V1 ni ningún otro validador. Tampoco las reglas de adaptación, progresión y reincorporación, ni el catálogo, ni el esquema de base de datos.

**Si el catálogo no responde:** se mantiene como mínimo la exclusión registrada (falla cerrado sobre lo directo) y queda el registro `DOLOR_FAMILIA_NO_CALCULADA`.

**Trazabilidad:** línea `DOLOR_FAMILIA usuario … variantes equivalentes excluidas {variante=ejercicio que dolió}`.

## 3. Pruebas

| Prueba | Qué demuestra |
|---|---|
| `FamiliaDeMovimientoEquivalenteTest` (2) | Niveles 1 y 2: sufijos `v. N`, mayúsculas y espacios. Mismo gesto con otro material o postura. No equivalentes del mismo patrón. El patrón y el músculo son obligatorios en el nivel 2. Nombres vacíos |
| `PainMemoryFamiliaTest` (4) | La exclusión efectiva incluye la familia. Sin exclusiones no se consulta el catálogo. Si el catálogo falla, se mantiene lo registrado. Reactivado lo que dolió, su familia deja de estar excluida |
| `DolorFamiliaEquivalenteIT` (7, PostgreSQL y catálogo real) | **Premisa** del catálogo, comprobada con la regla y con el backend. **Elegibles:** con dolor en 537 salen 537 y toda su familia, y nada más; 578 sigue. **Simétrico:** dolor en 538 excluye 537. **La variante no se cuela:** V1 rechaza una propuesta con 538. **Ciclo O20 real**, con y sin **restricciones personales**: ningún miembro de la familia en ningún plan activo durante dos semanas, el sustituto es de otra familia y respeta las restricciones (sin suelo, sin brazos arriba, sin impacto). **Reincorporación:** la familia solo vuelve tras `reactivar`. **Inventario** de familias |
| `DolorSinAlternativaIT` (1, catálogo reducido a propósito) | Desactivados todos los sustitutos posibles fuera de la familia, con la misma función `descarte` de la política, y con 538 activo: **no se usa 538**. La adaptación queda **NO_FACTIBLE** con la causa «no hay sustituto elegible… no se publica una sustitución insegura». P-03 lo intenta y lo rechaza, porque subiría el trabajo tras dolor. No se publica nada inválido |

**Antes y después** (los mismos escenarios):

| | `generation` sin corregir | Con la corrección |
|---|---|---|
| `DolorFamiliaEquivalenteIT` | **5 de 6 fallan**: 538 elegible, simetría, V1 acepta 538, y en los dos ciclos O20 el plan contiene 538 | **7/7 en verde** (6 + inventario) |
| Sustituto de 537 en el ciclo O20 | **538** («front raise v. 2») | **539** («dumbbell full can lateral raise», otro movimiento del patrón SHOULDER_RAISE) |

**Regresión completa:** ver §5.

### Pruebas existentes ajustadas, y por qué

Ninguna se relajó: cada una conserva lo que protegía y suma la comprobación de la opción A.

| Prueba | Fallaba porque | Ajuste |
|---|---|---|
| `PoliticasP123IT` (4: `antesDelReinicio`, `despuesDelReinicio`, `reactivacion`, `dolorSimultaneoMismoEventoYOtroUsuario`) | Fijaban que `excluidosActivos` es exactamente lo registrado. Por ejemplo, 4 ejercicios registrados que ahora excluyen 20, como todas las variantes de «shoulder press» tras dolor en «lever shoulder press v. 3» | Lo registrado se comprueba con `excluidosActivosDirectos`: persistencia tras el reinicio, una fila por evento, aislamiento entre usuarios y reactivación. Además, la exclusión efectiva contiene lo registrado, y tras el reinicio **ningún miembro de la familia es elegible** |
| `HerenciaD8IT.dolorConRegeneracionSegura` | Su premisa era «P-03 nivel 2 publica para el participante 9004». **En `generation` sin corregir, esa semana publicada contenía 988 «lever chest press», el mismo gesto que 992 «lever incline chest press v. 2», que dolió** (diagnóstico ejecutado sobre `generation`). Con la familia excluida, la única regeneración sube una dosis (472: 2×10 → 3×10) y P-03 la rechaza | Si P-03 publica, se comprueba todo lo de antes y, además, que no aparece ninguna variante equivalente. Si no, el desenlace seguro: NO_FACTIBLE con el motivo de P-03, la exclusión conservada y nada de la familia en una semana nueva. Una exploración con el mismo escenario (participantes 9005–9040) no encontró ningún caso en que P-03 publique, porque el dolor de sesión excluye la sesión entera y sus familias. El camino de publicación de P-03 sigue cubierto por las otras pruebas que publican por P-03 (7 en la regresión) |

## 4. Lo que no cubre

- **Equivalencias que no tienen el mismo núcleo de nombre.** Por ejemplo, «bench press» frente a «chest press» son el mismo gesto con nombres distintos y quedan en grupos separados. Una lista revisada por un profesional (catálogo con `familia_movimiento`) sería la evolución natural. La regla actual cubre las variantes con nombre propio y el mismo gesto con otro material o postura.
- **La API de exclusiones de dolor** (`GET /users/me/pain-exclusions`) sigue listando solo lo registrado. La familia excluida no se muestra a la persona. Reactivar lo que dolió reactiva su familia.
- **Planes ya publicados antes de esta corrección:** la variante de una sesión abierta queda marcada por `safetyBlockReason`, con la misma ruta de antes y ahora también para la familia.

## 5. Regresión y CI

| Ejecución | Commit | Resultado |
|---|---|---|
| `mvn verify -Pintegration` completo, local | `d196a7a0` | Unitarias: **1.604, 0 fallos**, 1 omitida (antes 1.598, más las 6 nuevas). Integración: **875, 0 fallos**, 72 omitidas (opt-in; antes 867, más las 8 nuevas). **BUILD SUCCESS** (28 min 31 s) |
| Primera regresión, antes de ajustar las pruebas existentes | `28a186e7` | 5 fallos esperados por el cambio de semántica (§3, «Pruebas existentes ajustadas»). Ninguno en otras áreas |
| CI de GitHub (`verify`) | Cabeza de la PR | Ver la PR |
