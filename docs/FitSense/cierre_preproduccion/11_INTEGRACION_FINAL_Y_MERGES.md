# 11 · Integración final y merges hacia `generation`

**Orden:** revisión final, seguridad y merge condicional de preproducción (27/11/2026).
**Autorización:** revisar, corregir los defectos necesarios, probar y fusionar de forma condicional #89–#95 en `generation`.
**Sin cambios:** no se ha desplegado Railway, `main` no se ha tocado, no se ha usado Replicate real y no se han modificado datos reales.

## 1. Verificaciones iniciales

| # | Verificación | Resultado |
|---|---|---|
| 1–2 | SHA de partida | `generation` = `77b9df3d6bd26f6196514c084d40bd952db77adc` (la base auditada). `main` = `7b8a6d34fc94c41ebd07d9d88616de70cd2055e9` |
| 3–5 | PR #89–#95 | Las siete abiertas, `mergeable=clean`. Base `generation` salvo #94, que va sobre la rama de #92. Sin conflictos |
| 6 | CI de cada cabeza | En verde en las siete: #89 `b490a4c`, #90 `afe2282`, #91 `13b17cd`, #92 `240ccca`, #93 `cb3a51b`, #94 `aff80cf`, #95 `d4afb70` |
| 7 | PR experimentales | #17, #28, #29, #30, #32, #33 y #39, todas marcadas NO FUSIONAR, más #1. **Ninguna se toca** |
| 8 | *Prompt* de generación | **PR-1.7** (`PlanPromptBuilder.VERSION`); ninguna PR lo cambia |
| 9 | gpt-5-mini directo | `fitsense.ai.base-url` apunta por omisión a `openai/gpt-5-mini/predictions` (formato *direct*); ninguna PR lo cambia |
| 10 | V66 | Solo la contiene #89 (`V66__operaciones_ia_sincrona_y_ciclo_semanal.sql`); ninguna otra PR crea migraciones |
| 11 | Consentimientos | Ningún documento declarado en `src/main/resources`; `FITSENSE_CONSENT_ENFORCE_ON_SIGNUP=false`. Los documentos sintéticos solo existen en `ConsentimientoExigidoIT` |
| 12 | Regresión anterior | 1.679 + 927, 0 fallos, 73 omitidas (sin #89). Esta vez se repite **con #89** (§4) |

## 2. Hallazgos de la revisión y defectos corregidos

| # | PR | Hallazgo | Gravedad | Corrección | Commit |
|---|---|---|---|---|---|
| H-1 | **#89** (y `generation` desde V59/#72) | **Una operación adoptaba la predicción de OTRA operación** del mismo usuario cuando coincidían la huella de la entrada y la ventana de 24 h (`DiarioDePredicciones.reutilizable` y `adoptar`, llamados desde `ReplicateTrainingPlanGenerator.callConDiario`). La identidad se apoyaba solo en el contenido, justo lo que prohíbe la orden (§3.1). #89 extendía este camino a la ruta síncrona y al ciclo semanal. **Entre usuarios distintos no podía ocurrir** (filtro por `user_id`) | **Bloqueante para #89** | Se eliminan esa vía y la ventana `reuse-window-hours`. La identidad de una predicción pasa a ser **su operación y su intento** (`deIntento`) o **el id de su propia respuesta** (#88). Una huérfana se recupera adoptando **la operación** por su `operation_key` persistida (V66). El libro ya no tiene ninguna consulta de filas de otra operación | `bf49947` (rama de #89) |
| H-2 | #93 | B5 renovaba `weekly_user_metrics.calculated_at` en cada recálculo. `ADHERENCIA.md` documenta que es la fecha del **primer** cálculo y los análisis se apoyan en ello | Media: semántica documentada | Se revierte (`WeeklyUserMetrics` vuelve a la de `generation`). La prueba vigila que se conserva. `recalculated_at` queda propuesto con migración tras V66 | `92b32bf` (rama de #93) |
| H-3 | #95 | El `.dump` del script de respaldo se escribía con el `umask` por omisión (644), legible por otros usuarios del sistema | Media: datos personales | `umask 077` y `chmod 600`. Ensayado: permisos 700/600, ninguna clave en la salida, y salida 1 ante una restauración que no coincide | `8eb9150` (rama de #95) |

**Pruebas que se invierten a propósito con H-1.** Afirmaban la adopción insegura y ahora afirman lo contrario; el motivo está en cada prueba:

| Prueba | Antes | Ahora |
|---|---|---|
| `DiarioDePrediccionesReplicateTest` E8 (dos) | La nueva operación del mismo usuario adopta, 0 POST | No adopta ni consulta: 1 POST propio, sin `reused_from` |
| `ReanudacionConReplicateSimuladoIT.venceDeVerdadDuranteUnaPrediccion` (R10) | "1 adoptada + 2 nuevas" | 1 + 3 propias |
| `ReanudacionConReplicateSimuladoIT.reintentoManualAdopta` (E15) | "3 adoptadas, 0 POST" | 3 + 3 propias |
| `GeneracionAsincronaResilienciaIT` E8/E15 | Probaba `reutilizable`/`adoptar` en JDBC | La nueva operación no ve las filas de la anterior |

**Coste aceptado.** Una petición nueva, por ejemplo un reintento manual tras un fallo, paga su propia predicción. La acota el límite por usuario (B5, también en la ruta asíncrona).

## 3. Identidad de predicciones (§3.2): casos y pruebas

Todas con Replicate **simulado**: un servidor local o `MockRestServiceServer` con el cliente de producción.

| # | Caso | Pruebas |
|---|---|---|
| 1 | Usuarios A y B con solicitudes idénticas | `ReplicateRecuperacionTransporteTest` S01 (dos), S12 (adaptación); `OperacionesIaV66IT` V59-07 (base real); `DiarioDePrediccionesReplicateTest.noSeReutilizaDeOtroUsuarioNiOtraPeticion` |
| 2 | Dos generaciones independientes del usuario A con contenido idéntico | **`DiarioDePrediccionesReplicateTest.noAdoptaLaTerminadaDeOtraOperacion`, `noAdoptaLaEnCursoDeOtraOperacion`** (nuevas); **`ReanudacionConReplicateSimuladoIT` R10 y E15** (base real, invertidas); `GeneracionAsincronaResilienciaIT.libroSinReutilizacionEntreOperaciones` |
| 3 | Una generación y una adaptación con entradas coincidentes | **`DiarioDePrediccionesReplicateTest.generacionYAdaptacionNoSeConfunden`** (nueva) |
| 4 | Reinicio después de recibir el id | V59-01/V59-04, V59-03b (segundo contexto de Spring), V59-17; S13 |
| 5 | Reinicio antes de recibir el id | V59-06 (no se reenvía), S09 |
| 6 | POST con respuesta incompleta | T02, T04 y S04 (solo el id completo y de primer nivel, confirmado por GET); T03 y T05 (sin id: incierta) |
| 7 | Predicción completada con el backend apagado | V59-05 (se recupera por su id, se valida y se publica; 0 POST) |
| 8 | Dos procesos recuperando la misma operación | V59-08 (una sola adopción), T12, S11 |
| 9 | Resultado antiguo cuando el plan ya cambió | V59-10 (la petición cambió: `DESCARTADA`); `PublicacionConcurrenteIT` (#92: no se publica encima) |
| 10 | Predicción no confirmada que nunca se publica como IA | S15 (respaldo `RULE_ENGINE`, 1 POST), V59-06; E12-11 (#94: 1 POST, `NO_CONFIRMADA`, respaldo) |

**Garantías estructurales tras H-1:**
- `DiarioDePredicciones` solo devuelve filas por `(operation_id, attempt)`.
- `verificarPorId` exige el id de la propia respuesta y que la entrada sea la misma (#88).
- Nunca se busca por contenido, ni se lista ni se elige entre candidatas.

## 4. Regresión integral previa a los merges (§11)

Rama local temporal `tmp/integracion-7pr` (`5f2cfbf645fe1145c4fa8ce84baa88db1c9661b7`), sin subir. Es `generation` `77b9df3` más las siete PR con sus cabezas definitivas: #90 `afe2282`, #89 `bf49947`, #92 `240ccca`, #94 `aff80cf`, #93 `92b32bf`, #91 `13b17cd` y #95 `8eb9150`. Las siete se fusionaron sin conflictos.

`mvn -B verify -Pintegration` sobre esa rama, con Docker y PostgreSQL 16 de Testcontainers:

| Fase | Ejecutadas | Fallos | Errores | Omitidas |
|---|---|---|---|---|
| Unitarias (surefire) | 1.680 | 0 | 0 | 1 |
| Integración (failsafe) | 943 | 0 | 0 | 72 |

**BUILD SUCCESS**, en 28:54 min.
- Las omitidas son laboratorios, barridos y corridas de evidencia con IA **real**. Quedan apagados mientras no se active su propiedad del sistema, y no se activó ninguna.
- Incluye, con #89 ya combinada: `Estudio12SemanasIT`, `MigracionV65aV66IT`, `OperacionesIaV66IT` (15), `ReanudacionConReplicateSimuladoIT` (12), `CierreSemanalRecuperableIT`, `PublicacionConcurrenteIT`, `ConsentimientoExigidoIT` y `LogsSinDatosSensiblesIT`.
- **No sustituye a la regresión posterior a los merges** (§5.2), que se repite sobre la cabeza final de `generation`.

## 5. Merges

Cada merge se hizo con `PUT /pulls/{n}/merge`, `merge_method=merge` y **el SHA completo de la cabeza revisada** (`sha=`): si la rama hubiera cambiado entre la revisión y el merge, GitHub lo habría rechazado. Antes de cada merge se comprobó `mergeable=clean` y el CI en verde de esa misma cabeza. Después de cada uno se comprobó:
- que el merge quedó en `MERGED`;
- que `generation` avanzó al commit de merge, con padres la `generation` anterior y la cabeza de la PR;
- que el árbol fusionado es el previsto (`git merge-tree`) y que contiene exactamente los ficheros de la PR;
- que las pruebas afectadas pasan sobre el commit de merge;
- que el CI del *push* a `generation` está en verde;
- que `main` sigue en `7b8a6d34fc94c41ebd07d9d88616de70cd2055e9`.

### 5.1 Secuencia

| Orden | PR | Cabeza fusionada | Commit de merge en `generation` | Pruebas afectadas tras el merge (unit + IT, 0 fallos) | CI del merge |
|---|---|---|---|---|---|
| 1 | #90 B1 | `afe22828187d9825218bfa9e1f54aa4bcb6a8731` | `25bcf27e88b223e041387746debcfcd5ab6c280c` | 19 + 30: límites, guardas, `LogsSinDatosSensiblesIT`, `SeguridadRolesIT` | verde |
| 2 | #89 V59/V66 | `bf49947111359820ef61d2070411e4fd00ea6186` (con H-1) | `069a6031964ceec8ce083d7ae1dc1a9fdf63f64c` | 57 + 66: libro de predicciones, transporte, `OperacionesIaV66IT`, `MigracionV65aV66IT`, `ReanudacionConReplicateSimuladoIT`, `GeneracionAsincronaResilienciaIT`, `CicloConcurrenteIT` | verde |
| 3 | #92 B3 | `240ccca9533ccdd3c14f6f7d22f2b6d2473a8ca1` | `51e8d7ed85cb1b4c28a308a8759da5f49ebbcdff` | 46 + 37: `CierreSemanalRecuperableIT`, `PublicacionConcurrenteIT`, V66 y ciclo concurrente combinados | verde |
| 4 | #94 B6 | `c7704445e67715ee9ce6c444a779da15cacc57c3` | `571462283c6399e0079a1749fd653eaafb5722f9` | `Estudio12SemanasIT` (18 escenarios) sobre el mismo árbol `01bce680`, más la prueba inversa (§6) | verde |
| 5 | #93 B5 | `92b32bfee1603a808fb4de1324843d8d3bc39e5c` (con H-2) | `a5a7a0cdd9f6a1b8d19da02b8598043ce9553913` | 35 + 83: `OperacionYExportacionIT`, `AdminExportControllerTest` y, porque toca el ciclo y la generación, otra vez V66, B3, asíncrona, reanudación y `Estudio12SemanasIT` | verde |
| 6 | #91 B2 | `13b17cdc33e4e1bae1da0b0e9af7d063389407d8` | `eb13db9200652ad81347a5abd63fc2fba4dfd1f4` | 2 + 50: `DocumentosLegalesTest`, `ConsentimientoExigidoIT` (con versiones **sintéticas**), `ConsentimientoYEdadIT`, `ParticipacionOpcionAIT`/`BIT`, `SeguridadRolesIT`, `Estudio12SemanasIT`, `OperacionYExportacionIT` y `LogsSinDatosSensiblesIT`. La configuración por omisión no cambia: exigencia apagada, ningún documento y política A | verde |
| 7 | #95 B4 | la cabeza que contiene este documento | se registra en el informe final de la orden | — | — |

Un documento no puede contener el SHA del commit que lo fusiona. Por eso el merge de #95 y la regresión final (§5.3) constan en el informe de cierre de la orden y no aquí.

### 5.2 #94: cambio de base sin reescribir historia

#94 iba sobre la rama de #92. Una vez fusionada #92:
- se cambió su base a `generation` (`PATCH /pulls/94`). El diff quedó en **solo los 4 ficheros de B6**: `Estudio12SemanasIT`, `08`, el CSV y la prueba inversa;
- como el cambio de base no lanza el CI, la rama se actualizó con `generation` (`PUT /pulls/94/update-branch`, con `expected_head_sha`). Esto hace un merge, no un *rebase*: equivale a rebasarla sin reescribir la historia de una rama publicada. Nueva cabeza: `c7704445`;
- el árbol de esa cabeza (`01bce680b0647e2a983d72b3089ccffd8bd236b0`) es **idéntico** al que se probó en local, y también al que quedó en `generation` tras el merge.

### 5.3 Regresión final

`mvn -B verify -Pintegration` completo sobre la cabeza final de `generation`, después del merge de #95, más su CI. El resultado está en el informe final de la orden.

## 6. Simulación de 12 semanas

- **Ejecución normal** sobre el árbol fusionado: los 18 escenarios E12-01..E12-18 en verde (80 s). El CSV reconstruido coincide fila a fila con la evidencia publicada (`evidencia/estudio_12_semanas.csv`, 157 líneas, columnas de participante a adherencia). La simulación es reproducible.
- **Prueba inversa** con la ventana de recuperación a 0, es decir, sin B3: **falla**, con exactamente los mismos fallos que `evidencia/prueba_inversa_sin_recuperacion.txt`:
  - los participantes de Lima pierden S4;
  - U09 pierde además S8;
  - E12-16 pierde las semanas anteriores a la retirada;
  - falla la comprobación de avisos *push* de U01.

  El cambio fue temporal, en un *worktree* local, y se deshizo. La evidencia publicada no se sobrescribió.
- **Nota de método.** El arnés construye el planificador con la ventana como argumento, así que la variable `FITSENSE_WEEKLY_CYCLE_CATCH_UP_HOURS` no le llega. Un primer intento con esa variable pasó en verde. No contaba como prueba inversa y se repitió cambiando el argumento.
- **Lo que no demuestra:** que la IA real genere planes aceptables. Replicate está simulado por un servidor local y sus salidas se rechazan a propósito: las semanas salen del motor de reglas. **No se ha validado IA real.** Ver `08` §3.

## 7. Migraciones

| Comprobación | Resultado |
|---|---|
| V66 sin colisiones | Única migración nueva, sobre V65; ninguna otra PR crea migraciones |
| Operaciones existentes legibles | `MigracionV65aV66IT` (V59-15) aplica V66 sobre datos con el esquema anterior; las filas `FIRST_PLAN` se conservan y cumplen las restricciones nuevas |
| Compatibilidad con V59 | Mismas columnas de *lease* y libro; `ux_pgo_una_activa_por_usuario` se limita a `FIRST_PLAN` |
| Sin pérdida de datos | V66 solo añade columnas y restricciones y rehace un índice; no borra ni reescribe filas |
| Despliegue interrumpido | V66 es una sola migración transaccional de PostgreSQL (DDL transaccional): o se aplica entera o no se aplica. Si la aplicación no arranca, se despliega el *build* anterior, compatible con las columnas nuevas. Si falla después, se restaura la copia previa (`06` §1, obligatoria antes de V66) |
| Bases reales | **No se ha aplicado V66 a ninguna base real.** Solo a PostgreSQL de Testcontainers |

## 8. Contratos para Mobile (matriz definitiva)

**Con la configuración por omisión, la app actual sigue funcionando.** Ningún campo deja de existir, el registro sin versiones sigue dando 201 y la exigencia de consentimientos está apagada. No se asume que haya pantallas nuevas fusionadas en Mobile.

| Área | Endpoint o cambio | Respuesta | Qué debe hacer Mobile | ¿Activo por omisión? |
|---|---|---|---|---|
| Límites | `POST /auth/login`, `POST /auth/register`, `POST /users/me/consents` | **429** con `Retry-After` (s) y `details[0]="DEMASIADAS_SOLICITUDES"`. En login, tras 5 fallos con el mismo correo, el mensaje es fijo y no revela si la cuenta existe | Mostrar la espera; no reintentar de forma automática | Sí |
| Límite de generación | `POST /users/me/plan/generate` (síncrono) y `POST /users/me/plan/generation` (asíncrono, desde #93) | **429** con `Retry-After` y `details=[]` (el contrato que ya tenía la ruta síncrona) | Lo mismo | Sí |
| Documentos | `GET /api/v1/legal-documents` (sin JWT) | `{enforcedOnSignup, privacyRecord, documents:[{type, version, title, url, status, effectiveFrom}]}`. **Hoy, lista vacía** | Mostrar los documentos cuando existan | Sí (vacío) |
| Registro | `acceptedTermsVersion`, `acceptedPrivacyVersion` y **`acceptedStudyConsentVersion`** (nuevo, ≤ 20) | Versión no vigente: **422** "La version aceptada de X no es la vigente (V)". Versión de consentimiento sin participar: 422 | Enviar exactamente la `version` de `/legal-documents` | Campos opcionales; exigencia apagada |
| Participación | `acceptsStudyParticipation` (casilla **sin marcar por omisión**) | `studyParticipant` en `/users/me`; historial en `GET /users/me/consents`, con **`approvedDocuments`** (nuevo) | — | Política **A** por omisión: sin participar no hay plan (422 al pedirlo). **B preparada, sin activar.** Ninguna está aprobada formalmente para el estudio |
| Retirada | `POST /users/me/consents/STUDY_PARTICIPATION/withdraw` o `POST /users/me/withdraw` | 204 o 200 | — | Sin cambios |
| Plan cambiado | Generación o adaptación cuyo plan cambió durante la operación | **409** `details[0]="PLAN_CAMBIADO_DURANTE_LA_GENERACION"` | Volver a leer `GET /users/me/plan/current` | Sí |
| Recuperación de contraseña | `POST /auth/password-reset/request` y `/confirm` (código de 43 caracteres) | 202 siempre; 204, 400 o 429 | Pantallas de solicitud y código; tras el 204, descartar el JWT | El código existe; **sin proveedor de correo, no llega ningún correo** |
| Generación asíncrona | `POST /users/me/plan/generation` → 202 (nueva) o 200 (existente); `GET …/latest`, `GET …/{operationId}` | `{operationId, status (PENDING/PROCESSING/COMPLETED/FAILED/NO_FACTIBLE), terminal, weekStartDate, planId, reasonCode, message, retryable, …, pollAfterSeconds=3}` | Consultar cada `pollAfterSeconds` hasta `terminal` | Sin cambios de contrato. Las operaciones síncronas y de ciclo de V66 no son visibles en `/latest` |
| Adaptaciones | `GET /users/me/plan/adaptations`, `GET /users/me/interventions` | Sin cambios | — | — |
| O20 | `GET /users/me/pain-exclusions`, `/effective`, `/confirmation-requests`; `POST …/{exerciseId}/identification` y `…/reactivation` | Sin cambios | — | — |

**La activación conjunta de los consentimientos** (documentos aprobados, compilación de Mobile compatible y `FITSENSE_CONSENT_ENFORCE_ON_SIGNUP=true`) es **otra etapa**.

## 9. Riesgos y decisiones pendientes

| # | Riesgo o decisión | Situación tras los merges |
|---|---|---|
| 1 | **V66 en una base real** | No aplicada a ninguna. Antes, copia verificada (`06` §1) y aplicarla primero en preproducción |
| 2 | **Coste de H-1** | Una petición nueva, como un reintento manual, paga su propia predicción. Lo acota el límite por usuario (B5). El tope **global** de gasto sigue pendiente de decisión |
| 3 | **IA real no reevaluada** tras H-1 ni tras la combinación | Ninguna prueba de esta orden la usa. Requiere autorización de gasto |
| 4 | **Consentimientos** | Exigencia apagada y sin documentos. Activarla requiere documentos aprobados (P-6), Mobile compatible y la decisión A/B (P0-04). **No se ha activado** |
| 5 | **Correo** | Sin proveedor (P-10), la recuperación de contraseña no envía nada |
| 6 | **Railway** | Sin desplegar. El *proxy* de confianza (`forward-headers-strategy=native`) y las variables `prod` se verifican en el panel antes del primer despliegue (`06`) |
| 7 | **Copias** | Script ensayado solo con bases sintéticas. Falta ensayar una restauración contra la base de preproducción |
| 8 | **Mobile** | No se asume que haya pantallas nuevas fusionadas. Con la configuración por omisión, la app actual sigue funcionando (§8) |
| 9 | **Índice `ix_pgp_reutilizable`** (V59) | Ya no lo usa ninguna consulta tras H-1. Se mantiene porque una migración aplicada no se edita. Se puede retirar en una migración posterior a V66 |
| 10 | **Piloto** | Terminar la integración **no** lo declara listo. Faltan las decisiones del estudio (`10` §2) |

## 10. Llamadas pagadas

**Cero.**
- Todas las pruebas con IA usan un Replicate local o *mocks*, con *tokens* ficticios.
- En el entorno no hay `REPLICATE_API_TOKEN`.
- No se ha consultado la API de Replicate.
