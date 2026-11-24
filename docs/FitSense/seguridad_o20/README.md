# Seguridad O20 · opción A: tras dolor, ninguna variante equivalente vuelve

**Órdenes:**
- «ORDEN — CORRECCIÓN DE SEGURIDAD O20 / SUST-SEG-1», 24/11/2026. Decisión: opción A.
- «ORDEN — AJUSTE FINAL DE SEGURIDAD O20». Acota la equivalencia y el dolor de sesión sin identificar (§2 y §2.1).

**Autorización:** realizar ajustes y pruebas. **NO MERGE.** Ni esta PR ni la #83 se fusionan sin la revisión final.

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

**Familia de movimiento equivalente** (`catalog/domain/services/FamiliaDeMovimientoEquivalente`, dominio puro). Dos ejercicios son equivalentes **solo** por uno de estos dos niveles:

| Nivel | Regla | Ejemplos |
|---|---|---|
| **1 · variante del mismo ejercicio** (`VARIANTE_MISMO_EJERCICIO`) | El mismo `name_en` sin mayúsculas, sin espacios sobrantes y sin el sufijo final `v. N`. Sin cambios en el ajuste final. Cubre las 29 familias activas | 537 «dumbbell front raise», 538 «… v. 2» y el duplicado histórico inactivo 28; «barbell upright row» v. 1/2/3; «lever shoulder press» v. 1/2/3 |
| **2 · mismo gesto con otro nombre, material o postura** (`MISMO_GESTO`) | Los dos nombres están en el mismo grupo de la **lista explícita** `src/main/resources/catalog/equivalencias_mismo_gesto.csv`: **118 grupos confirmados**, cada uno con su justificación | «front raise»: 537, 538, 99 band, 150 barbell, 334 cable, 703 y 707 *seated*. «upright row» con sus dos patrones registrados. «bicep curl» y «biceps curl» |
| **No equivalentes** | Todo lo demás, **aunque coincida el patrón y el músculo** | 578 «dumbbell lateral raise», 539 «full can lateral raise», 579 «lateral to front raise» y 746 «front raise above head», todos SHOULDER_RAISE con deltoides. 988 «lever chest press» frente a 992 «lever incline chest press v. 2», porque cambia el ángulo |

### 2.1 Ajuste final: el criterio del nivel 2

- **Antes**, el nivel 2 era una **heurística**: el mismo patrón, el mismo músculo y el mismo núcleo del nombre, sin material ni postura. Daba **515** ejercicios activos con algún equivalente.
- **Ahora** el backend lee **solo** la lista explícita, y el patrón y el músculo ya no son prueba suficiente. Hay **395** ejercicios activos con algún equivalente (`evidencia/familias_equivalentes_catalogo_activo.csv`, con el nivel y el grupo de cada relación).

**Criterio de confirmación.** La propuesta inicial la genera `herramientas/generar_equivalencias.py`; después se revisa a mano.

| Diferencia entre los miembros | Decisión |
|---|---|
| Solo material (dumbbell, barbell, cable, band, lever, smith, kettlebell…) | **Confirmada** |
| Lateralidad (one arm, alternate…) o apoyo sentado/de pie | **Confirmada** |
| Variante ortográfica del mismo nombre: «biceps»/«bicep», «close-grip»/«close grip», «tricep kickback»/«kickback» | **Confirmada** |
| Ángulo o apoyo del cuerpo (incline, decline, lying, kneeling) | **Pendiente entre ángulos.** Cada ángulo puede confirmarse por separado: «incline bench press» es un grupo y «bench press» otro |
| Núcleo genérico (curl, row, press, fly, extension, crunch, twist, squat, raise) | **Pendiente** |
| Revisado a mano como gesto distinto | **Pendiente.** «hack squat»: la barra va detrás de las piernas frente a cargar los hombros. «dip»: máquina sentado frente a un brazo. «triceps extension»: el nombre no dice la posición del brazo |
| Posible sinónimo con otras palabras | **Pendiente**, para revisión profesional: «bench press ~ chest press», «shoulder ~ military ~ overhead press», «stiff-leg ~ straight-leg ~ romanian deadlift» |

**Inventario trazable:**
- **Confirmadas:** el propio `equivalencias_mismo_gesto.csv`, con grupo, patrón, músculo, miembros y justificación.
- **Pendientes:** `evidencia/equivalencias_pendientes.csv`, con **53 filas**:
  - 37 entre ángulos;
  - 10 núcleos genéricos;
  - 3 revisadas como distintas;
  - 3 posibles sinónimos.

  **Una relación pendiente no excluye nada.**
- **Control de la lista:** al cargarla, el backend rechaza una lista ambigua (un miembro en dos grupos, un miembro sin normalizar, un grupo de un solo miembro o sin justificación). Una prueba comprueba que cada miembro existe en el catálogo activo.

### 2.2 Ajuste final: dolor de sesión sin identificar

| | Dolor identificado (`DOLOR_IDENTIFICADO`) | Dolor de sesión sin identificar (`DOLOR_SESION_SIN_IDENTIFICAR`) |
|---|---|---|
| Qué es | Una exclusión **confirmada** de ese ejercicio | Un **reporte general**: no se sabe cuál dolió |
| Ejercicio registrado | Excluido hasta la reactivación explícita (sin cambios) | **Cada** ejercicio de la sesión, excluido hasta su reactivación explícita (sin cambios: la regla de V8C) |
| Familia equivalente | **Permanente**, lo que dure la exclusión | **Provisional**: la adaptación o regeneración inmediata y la semana siguiente (`SEMANAS_FAMILIA_PROVISIONAL = 2`, contadas en semanas con plan, no en días). Después deja de aplicarse sola; el ejercicio de la sesión sigue excluido |
| Identificar después | — | `POST /users/me/pain-exclusions/{e}/identification`. Esa fila queda **confirmada** (`identified_at`, V65) y su familia pasa a ser permanente. No reactiva los demás de la sesión |

**Por qué así:**
- **Seguridad primero:** la regeneración inmediata no reproduce el gesto con otra variante.
- **Sin exclusiones permanentes de todas las familias** por una sesión que no se identificó.
- **Información para identificar después:** se conservan la sesión de origen, la fecha y el motivo original.
- **Nada confirmado se reactiva solo.** Si no hay regeneración segura, el resultado sigue siendo **NO_FACTIBLE con su causa**.

**API para Mobile:**
- Campos aditivos `confirmed` e `identifiedAt` en `GET /pain-exclusions`.
- Nuevo `GET /pain-exclusions/effective`, que distingue `DIRECT` de `EQUIVALENT`, confirmada de provisional, y explica el nivel y el grupo.
- Nuevo `POST …/identification`.
- Ver `ESPEC_MOBILE_EXCLUSIONES.md`. **Mobile no se modifica en esta PR.**

**Dónde se aplica:** en un solo sitio, `PainMemoryService.excluidosActivos(userId)`, que lee `exclusionesEfectivas`. Devuelve lo registrado (`ACTIVA`) **más su familia** (§2.2). Es la lectura que usan todos los caminos que aplican el dolor:

| Camino | Efecto |
|---|---|
| `prepare` (generación, adaptación y regeneración) | La familia sale del **conjunto elegible**. La IA no la ve; si la propone, **V1 la rechaza** (V1 no cambia). El motor de reglas, la progresión y la reincorporación no la encuentran |
| `SafetyExclusionResolver` → SUST-SEG-1 | Una variante heredada también se trata como excluida por dolor. **El sustituto nunca es de la familia**, porque se elige entre los elegibles |
| `PublishedPlanSafetyService` | Con `fitsense.seguridad.dolor-en-planes-publicados=true`, que es el valor por omisión, la API marca `safetyBlockReason` también en una variante presente en una sesión abierta |
| `SafeRegenerationService` (P-03) | La familia cuenta como retirada |

**Duración de la exclusión:**
- La familia es **derivada**, no se guarda.
- La de una exclusión confirmada dura exactamente lo que esa exclusión, que sigue `ACTIVA` hasta su **reactivación explícita** por la persona.
- La de un dolor de sesión sin identificar es provisional (§2.2).
- No puede quedar una variante excluida «huérfana».

**Sin alternativa fuera de la familia:** el mecanismo seguro existente, sin cambios:
1. retirada localizada en la sesión señalada por dolor (`RetiradaSeguridad`, que V8C ya admite);
2. en una sesión no señalada, nada se modifica por esa vía: el resultado es **NO_FACTIBLE con su causa** y P-03.

**No se modificó** V8C, V24, V1 ni ningún otro validador. Tampoco las reglas de adaptación, progresión y reincorporación, ni el catálogo. El único cambio de esquema es V65, que añade dos columnas que admiten NULL para la identificación posterior.

**Si el catálogo no responde:** se mantiene como mínimo la exclusión registrada (falla cerrado sobre lo directo) y queda el registro `DOLOR_FAMILIA_NO_CALCULADA`.

**Trazabilidad:**
- `DOLOR_FAMILIA usuario … variantes equivalentes excluidas {variante=ejercicio que dolió} (… provisionales [...])`;
- `DOLOR_IDENTIFICADO_DESPUES …` al identificar después.

## 3. Pruebas

| Prueba | Qué demuestra |
|---|---|
| `FamiliaDeMovimientoEquivalenteTest` (5) | **Nivel 1:** sufijos `v. N`, mayúsculas y espacios. **Nivel 2 confirmado:** front raise frente a *seated*, *band*, *cable* y *barbell*; nombre ortográfico; dos patrones; lateralidad; sentado/de pie. **No equivalentes con el mismo patrón y músculo:** lateral, full can, lateral to front, rear delt. **Ángulo** (chest press frente a incline), **sinónimo dudoso**, **genérico** y **revisado como distinto**. **Lista:** 118 grupos válidos, y una lista ambigua se rechaza entera |
| `PainMemoryFamiliaTest` (9) | La identificada excluye su familia, que es permanente y no caduca. Sesión sin identificar: excluye la sesión y su familia provisional durante dos semanas con plan; después solo la sesión. Identificada después: permanente. La confirmada manda sobre la provisional. Falla cerrado. Reactivación. `identificar` exige confirmación, no reactiva los demás y no confirma dos veces |
| `DolorFamiliaEquivalenteIT` (9, PostgreSQL y catálogo real) | **Premisa**, con nivel y justificación según el backend. **Elegibles:** solo sale la familia; 578 sigue. **Simétrico.** **V1** rechaza 538. **Ciclo O20 real**, con y sin restricciones. **Diferencias de nombre, postura y material** según el backend, con simetría (11 pares). **Dolor de sesión sin identificar en el ciclo real:** véase la fila siguiente. **Inventario**, con simetría y existencia de cada miembro de la lista |
| `DolorFamiliaEquivalenteIT.dolorDeSesionSinIdentificar` | Regeneración inmediata y semana siguiente sin la familia. Después, 538 vuelve y la sesión sigue fuera. Identificar después lo hace permanente, incluso 4 semanas después. Solo la reactivación lo devuelve |
| `DolorSinAlternativaIT` (1, catálogo reducido a propósito) | Sin sustituto fuera de la familia: **no se usa 538**. **NO_FACTIBLE** con su causa. P-03 lo rechaza porque subiría el trabajo |
| `HerenciaD8IT.dolorConRegeneracionSegura` | Con el criterio explícito, 988 «lever chest press» ya no queda bloqueado por el dolor en 992 «lever incline chest press v. 2», porque cambia el ángulo. **P-03 vuelve a publicar** (plan 16) y se cumplen todas las comprobaciones originales y la de la familia. **Sin bloqueos innecesarios** |
| `CicloCompletoHttpIT.dolorYReincorporacion` y `ContratoErroresApiIT` | El contrato HTTP: `confirmed`, `/effective` (DIRECT frente a EQUIVALENT provisional), `/identification` (400, 422, 200 y 404), y que identificar no reactiva nada |

**Antes y después** (los mismos escenarios):

| | `generation` sin corregir | Con la corrección |
|---|---|---|
| `DolorFamiliaEquivalenteIT` | **5 de 6 fallan**: 538 elegible, simetría, V1 acepta 538, y en los dos ciclos O20 el plan contiene 538 | **9/9 en verde** (6 + inventario + diferencias de nombre, postura y material + dolor de sesión) |
| Sustituto de 537 en el ciclo O20 | **538** («front raise v. 2») | **539** («dumbbell full can lateral raise», otro movimiento del patrón SHOULDER_RAISE) |

**Regresión completa:** ver §5.

### Pruebas existentes ajustadas, y por qué

Ninguna se relajó: cada una conserva lo que protegía y suma la comprobación de la opción A.

| Prueba | Fallaba porque | Ajuste |
|---|---|---|
| `PoliticasP123IT` (4: `antesDelReinicio`, `despuesDelReinicio`, `reactivacion`, `dolorSimultaneoMismoEventoYOtroUsuario`) | Fijaban que `excluidosActivos` es exactamente lo registrado. Por ejemplo, 4 ejercicios registrados que ahora excluyen 20, como todas las variantes de «shoulder press» tras dolor en «lever shoulder press v. 3» | Lo registrado se comprueba con `excluidosActivosDirectos`: persistencia tras el reinicio, una fila por evento, aislamiento entre usuarios y reactivación. Además, la exclusión efectiva contiene lo registrado, y tras el reinicio **ningún miembro de la familia es elegible** |
| `HerenciaD8IT.dolorConRegeneracionSegura` | Su premisa era «P-03 nivel 2 publica para el participante 9004». Con la heurística de la primera versión, 988 «lever chest press» contaba como el mismo gesto que 992 «lever incline chest press v. 2», que dolió, así que P-03 no publicaba. **Con el ajuste final**, plano frente a inclinado es un cambio de ángulo y queda **pendiente**, no equivalente: P-03 vuelve a publicar | Si P-03 publica, se comprueba todo lo de antes y, además, que no aparece ninguna variante equivalente. Si no, el desenlace seguro: NO_FACTIBLE con el motivo de P-03, la exclusión conservada y nada de la familia en una semana nueva. **Con el ajuste final se recorre la primera rama** (P-03 publica el plan 16): el caso que la primera versión bloqueaba de más |

| `EliminacionDeCuentaIT.conHistorial` y `CicloCompletoHttpIT.dolorYReincorporacion` | Omiten una sesión por dolor y entrenan las otras con el plan leído **antes** de la omisión. Con la familia provisional, una variante equivalente presente en otra sesión abierta queda marcada por P-DOLOR-PUB (`safetyBlockReason`) y solo se puede registrar como omitida: falló en CI con `74a97b0e` («SESION_BLOQUEADA_POR_SEGURIDAD … (dolor_registrado)»). Que aparezca o no depende del plan generado, que varía con el id del usuario | Releen el plan tras la omisión, como haría la app, y registran como omitida (`PAIN_OR_DISCOMFORT`, sin trabajo) toda prescripción marcada, comprobando que la causa es `dolor_registrado`. El resto se entrena como antes |
| `PoliticaV4CCicloIT` (guion «DOLOR») y `RegeneracionPorColocacionRev2IT` (marcado P-DOLOR-PUB) | Mismo origen, revisado en todas las pruebas que omiten una sesión por dolor y siguen entrenando esa semana: `PoliticaV4CCicloIT` entrena las demás sin mirar lo marcado; `RegeneracionPorColocacionRev2IT` esperaba marcado exactamente lo registrado | `PoliticaV4CCicloIT` registra lo marcado como omitido (como ya hacía `RegeneracionPorColocacionRev2IT`). `RegeneracionPorColocacionRev2IT` espera marcada exactamente la exclusión efectiva (lo registrado y sus variantes equivalentes), que contiene lo registrado. `HerenciaD8IT` y `LinajeIntegridadIT` ponen el dolor en la última sesión y no lo necesitan |

## 4. Lo que no cubre, y decisiones para la revisión

- **Las relaciones pendientes no excluyen nada** (53 filas en `evidencia/equivalencias_pendientes.csv`). En particular:
  - **Cambio de ángulo.** Tras dolor en «lever incline chest press v. 2», «lever chest press» (plano) sigue siendo elegible, y así publica P-03 en `HerenciaD8IT`. Si se prefiere tratarlo como el mismo gesto, basta con unir los dos grupos en la lista: un cambio de datos, sin código.
  - **Posibles sinónimos** («bench press ~ chest press», «shoulder ~ military ~ overhead press», «stiff-leg ~ straight-leg ~ romanian deadlift»). Requieren un criterio profesional que esta PR no establece.
- **La lista no es un criterio clínico.** Es una revisión del catálogo por nombre, material y postura, versionada y trazable. Un ejercicio nuevo del catálogo no entra en ningún grupo de nivel 2 hasta que se añada a la lista; el nivel 1 sí lo cubre si es una variante `v. N`.
- **Duración de la familia provisional.** Es de dos semanas con plan, por la constante `SEMANAS_FAMILIA_PROVISIONAL`. Si no se publican planes nuevos, por ejemplo porque todo queda NO_FACTIBLE, la ventana no se cierra: es conservador.
- **Identificar no reactiva los demás ejercicios de la sesión.** Siguen excluidos y la persona los reactiva uno a uno (`/reactivation`). Es una decisión deliberada: también pudieron doler.
- **Mobile:** el contrato está listo (`ESPEC_MOBILE_EXCLUSIONES.md`), pero Mobile no lo muestra todavía. Hasta que lo haga, la persona ve en su historial las filas registradas, ahora con `confirmed`, y no ve las variantes equivalentes.
- **Planes ya publicados antes de esta corrección:** la variante de una sesión abierta queda marcada por `safetyBlockReason`, con la misma ruta de antes y ahora también para la familia.

## 5. Regresión y CI

| Ejecución | Commit | Resultado |
|---|---|---|
| `mvn verify -Pintegration` completo, local, primera versión | `d196a7a0` | Unitarias: **1.604, 0 fallos**, 1 omitida (antes 1.598, más las 6 nuevas). Integración: **875, 0 fallos**, 72 omitidas (opt-in; antes 867, más las 8 nuevas). **BUILD SUCCESS** (28 min 31 s) |
| Primera regresión, antes de ajustar las pruebas existentes | `28a186e7` | 5 fallos esperados por el cambio de semántica (§3, «Pruebas existentes ajustadas»). Ninguno en otras áreas |
| `mvn verify -Pintegration` completo, local, con el ajuste final | `aef6402f` | Unitarias: 1.612, 0 fallos. Integración: **48 errores**, todos por la misma causa: las pruebas que fijan un catálogo anterior no aplicaban V65 (`missing column [identification_evidence]`). Corregido en `EsquemaVigenteSobreCatalogoAnterior`, sin tocar código de producción |
| `mvn verify -Pintegration` completo, local, con el ajuste final | `568bb7e8` | Unitarias: **1.612, 0 fallos**, 1 omitida. Integración: **877, 0 fallos**, 72 omitidas (opt-in). **BUILD SUCCESS** (24 min 27 s) |
| CI de GitHub (`verify`) | `568bb7e8` | **success** |
| CI de GitHub (`verify`) | `74a97b0e` (solo documentación) | **failure** en `EliminacionDeCuentaIT.eliminacionCompleta`: el plan de ese usuario traía una variante equivalente en otra sesión abierta. Prueba adaptada (§3, «Pruebas existentes ajustadas») |
| `mvn verify -Pintegration` completo, local, con las pruebas adaptadas | `aeea6d0a` | Unitarias: **1.612, 0 fallos**, 1 omitida. Integración: **877, 0 fallos**, 72 omitidas (opt-in). **BUILD SUCCESS** (25 min 44 s) |
