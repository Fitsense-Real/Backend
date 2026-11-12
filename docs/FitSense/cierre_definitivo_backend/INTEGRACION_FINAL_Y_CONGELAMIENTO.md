# Integración final y congelamiento del backend MVP

**Orden:** «Integración final y congelamiento del backend MVP» (2026-11-02).
**Versión objetivo:** `backend-mvp-1.0`.
**Rama:** `generation`.
**Restricciones respetadas en toda la orden:**
- sin despliegues;
- sin migraciones remotas (Neon, Railway);
- sin Flutter;
- sin Replicate;
- sin cambios en las reglas de entrenamiento.

En este documento solo hay **resultados nuevos sobre el código integrado**. La evidencia anterior, obtenida en combinaciones locales temporales (`REGRESION_CIERRE_BACKEND.md`), se conserva tal cual.

## 1. Verificaciones previas (fase 1)

### 1.1. `generation` antes de integrar

| Comprobación | Resultado |
|---|---|
| Commit | `7c19703`, el mismo que se informó |
| CI | Verde (ejecuciones 116 y 117) |
| Fusiones anteriores | #18 bfd5756, #19 dd61837, #20 8f112d5, #21 7c19703 |
| Migraciones | Hasta V44 |
| Política por defecto | `PLAN-HIST-1`, con `allow-candidates=false` |
| Diferencias con lo informado | Ninguna |

### 1.2. PR técnicos

| PR | Cabeza verificada | CI en esa cabeza | Migración | Contrato | Riesgo revisado |
|---|---|---|---|---|---|
| Fitsense-Real/Backend#22 | 5740124 | ✅ | V44_1 | `AdaptationOutcome` (+6 campos) | `AdaptedWeekProducer` solo añade códigos; las mismas listas de problemas deciden NO_FACTIBLE |
| Fitsense-Real/Backend#23 | a251798 | ✅ | — | Errores 400/401 | Solo `GlobalExceptionHandler`, `TokenServiceImpl` y pruebas |
| Fitsense-Real/Backend#24 | 2114704 | ✅ | — | `/plan/current` | Solo `TrainingPlanController` y pruebas |
| Fitsense-Real/Backend#25 | 3f31a4b → **92cea48** → c285d14 → **dec918f** | ✅ en cada una | V44_2 | `/auth/password-reset/*` | Ver §2.2 (`dev-log`) |
| Fitsense-Real/Backend#26 | 65cf12f → **1da08a9** | ✅ en las dos | — | — | Solo configuración local y el guardián |

En los 5 PR:
- **Secretos:** ninguno. Lo que aparece son claves de prueba y el host de Neon que ya figura en `application.properties`.
- **Políticas de entrenamiento:** ningún cambio en el generador, el catálogo, el 1373, las dosis ni la dificultad.
- **Correcciones de la regresión anterior:** presentes en las cabezas (`SOLO_ESQUEMA` y `MigracionV44_2IT` independiente del orden).
- **El CI verde de un commit anterior no se dio por bueno para uno posterior:** cada PR se volvió a comprobar en su cabeza final.

### 1.3. Riesgo de despliegue automático

Sin acceso al panel de Railway. Lo que se pudo comprobar en modo lectura:

| Evidencia | Resultado |
|---|---|
| API de GitHub `GET /deployments` | **Vacía**: ningún despliegue registrado en toda la historia del repositorio |
| Comprobaciones en los commits de `generation` (de #15 a #21) | Solo `github-actions` y `claude`. Ninguna aplicación de despliegue |
| Rama por defecto del repositorio | `main`, sin commits desde el 21-09-2026 |
| Configuración de despliegue en el repositorio | Ninguna (`railway.json`, `Procfile`, `nixpacks`, `Dockerfile`). Workflows: solo `ci.yml` |
| Confirmación previa del responsable | «Railway no despliega desde `generation`» (`CATALOGO_V37.md`, `INFORME_FINAL_V39.md`, `INFORME_V40.md`) |
| Historial | Siete fusiones anteriores en `generation`, ninguna desplegada |

**Conclusión:** se descartó razonablemente un despliegue automático desde `generation` y se procedió.

**Riesgo residual:** que la configuración de Railway hubiera cambiado sin dejar rastro en GitHub. Si así fuera, al arrancar se aplicarían V44_1 y V44_2. Las dos son aditivas: dos columnas NULL y una tabla nueva, sin tocar datos.

**Confirmación pendiente:** antes de cualquier despliegue, que confirmes en el panel de Railway la rama vigilada y si el despliegue automático está activo.

## 2. Fusiones (fase 2)

| Orden | PR | Commit de fusión | CI de `generation` tras la fusión |
|---|---|---|---|
| 1 | #22 | `10b6da720422e442afc3c4ab42c12bbb2736dc64` | ✅ ([push](https://github.com/Fitsense-Real/Backend/actions/runs/41535060481), [PR #1](https://github.com/Fitsense-Real/Backend/actions/runs/41535051733)) |
| 2 | #23 | `18413f6f5afa38375c7a3ad2f9b9ada52088c789` | ✅ ([push](https://github.com/Fitsense-Real/Backend/actions/runs/41537207771), [PR #1](https://github.com/Fitsense-Real/Backend/actions/runs/41537200303)) |
| 3 | #24 | `c2fee7dc8dc3cf361ff2b406db5991b358b985c1` | ✅ ([push](https://github.com/Fitsense-Real/Backend/actions/runs/41539202350), [PR #1](https://github.com/Fitsense-Real/Backend/actions/runs/41539193068)) |
| 4 | #25 | `8d9f0127f65fe43014295198c6fee8fc16f311fe` | ✅ ([push](https://github.com/Fitsense-Real/Backend/actions/runs/41541198992), [PR #1](https://github.com/Fitsense-Real/Backend/actions/runs/41541191375)) |
| 5 | #26 | `114eb89508565fbd3c4392f205bd00aea85afd96` | Ver §5 |

**Cómo se hizo cada fusión:**
- con *merge commit*, nunca con *rebase*;
- con `expectedHeadSha`, para fusionar exactamente la cabeza comprobada;
- una a una, y sin pasar a la siguiente hasta tener verde el CI de `generation`.

Las ramas de #25 y #26 se actualizaron desde `generation` (merge) antes de fusionarlas, para que su propio CI cubriera exactamente lo que se iba a fusionar.

### 2.1. Conflicto V44.1/V44.2

Era el único conflicto, y apareció al actualizar el #25 tras fusionar el #22, en `EsquemaVigenteSobreCatalogoAnterior.SOLO_ESQUEMA`.
- **Resolución:** se conservaron las dos entradas, en orden: 42, 43, 44, **44.1**, **44.2**.
- No se tocó el comportamiento de Flyway: `outOfOrder` sigue igual.
- `MigracionV44_2IT` ya parte de «todo lo anterior a V44_2», así que vale con y sin V44_1.

### 2.2. Cambio añadido al #25: `dev-log` solo en local (92cea48)

La orden exige que `dev-log` quede limitado al entorno local (fase 3.4). El código no lo garantizaba: bastaba la variable `FITSENSE_PASSWORD_RESET_DELIVERY=dev-log` para activarlo en cualquier entorno.

**Corrección mínima:**
- fuera de los perfiles `local` y `local-dev`, la aplicación **no arranca** y explica el motivo;
- el valor por defecto (`none`) y las pruebas no cambian.

**Pruebas:**
- `PasswordResetNotifiersTest` (2);
- arranque real del *jar* sin perfil local y con `dev-log`: rechazado (§4).

## 3. Contratos integrados (fase 3)

| PR | Comprobado en el árbol final | Evidencia |
|---|---|---|
| #22 NO_FACTIBLE | Los 6 campos están presentes.<br>Los 9 de siempre no cambian de nombre, orden ni JSON.<br>Las filas antiguas no tienen código: no se infiere nada del texto.<br>La sesión afectada solo aparece si es del mismo usuario.<br>Caso 1373: `SIN_SUSTITUTO_SEGURO`, sin plan publicado y con las exclusiones activas | `AdaptationOutcomeContratoTest` (9), `AdaptationOutcomeTest`, `RespaldoConDolorIT` A/B/C, `MigracionV44_1IT` |
| #23 Errores y JWT | Login incorrecto → 401, con el mismo mensaje exista o no la cuenta.<br>JSON mal formado → 400; identificador mal formado → 400.<br>JWT caducado, de otra firma, mal formado o manipulado → 401.<br>La duración del JWT es configurable (7 días por defecto).<br>Recursos ajenos → 404 | `ContratoErroresApiIT` (6) y curl (§4) |
| #24 Zona horaria | `/plan/current` usa el día local del usuario con el servidor en UTC.<br>Lima: domingo 23:30 → semana vigente; lunes 00:30 → siguiente.<br>Tokio.<br>Horario de verano en Madrid (25-oct) y Nueva York (1-nov).<br>**Cambio de mes** (30-nov a 6-dic) y **cambio de año** (28-dic-2026 a 3-ene-2027), con vencimiento a las 23:59 de Lima | `ZonaHorariaUsuarioIT` (3). Mes y año con una prueba **temporal**, sin subir (5/5) |
| #25 Recuperación | 202 idéntico con y sin cuenta.<br>Token de un solo uso; en la base, solo su SHA-256.<br>Caduca a los 30 minutos; límite de 3 por hora.<br>Confirmación 204; reutilización 400.<br>Contraseña nueva 200; la antigua 401.<br>V44_2.<br>**Sin proveedor de correo: no operativa para usuarios reales** | `RecuperacionContrasenaIT` (5), `MigracionV44_2IT`, `PasswordResetNotifiersTest` (2) y curl contra el *jar* (§4) |
| #26 Entorno local | Perfil `local-dev`, PostgreSQL local, IA apagada, sin credenciales de Replicate.<br>`LocalProfileGuard` rechaza Neon y Railway **sin abrir ninguna conexión** (0 líneas de Hikari o Flyway).<br>Humo 20/20 | `LocalProfileGuardTest` (4) y arranque real (§4) |

La configuración de producción (`application.properties`) solo cambió en las tres propiedades de recuperación del #25:
- `delivery` = `none`;
- `ttl` = 30 minutos;
- `max` = 3 solicitudes por hora.

## 4. Migraciones (fase 4)

Todo con el *jar* del árbol final y PostgreSQL 16 efímero en `127.0.0.1`.

| # | Prueba | Resultado |
|---|---|---|
| 1 | Base limpia | **46 migraciones**, hasta v44.2. Últimas: 42, 43, 44, **44.1**, **44.2**. Todas con `success` |
| 2–4 | Validación y reinicio | «Successfully validated 46 migrations»; «Schema is up to date. No migration necessary»; la aplicación arranca (Hibernate `validate`) |
| 7 | Restricciones e índices | `ck_paa_reason_codes_solo_sin_plan`, `ck_sra_reason_code_solo_no_factible`, `uq_prt_token_hash`, `ck_prt_hash_sha256`, `ck_prt_caducidad`, `ck_prt_uso`, `ix_prt_user_created` y la FK `user_id → users` |
| 8–9 | Desde un esquema histórico (V44) con datos | Se aplican 2 migraciones (44.1 y 44.2). **Los usuarios y los planes anteriores no cambian** (huella md5 igual antes y después) |
| 10 | Catálogo fijado (`spring.flyway.target=35..41` + `SOLO_ESQUEMA`) | Dentro de la regresión completa: 0 fallos |
| — | Prueba de humo (curl) | 20/20 |
| — | Guardián con URL de Neon o Railway | Arranque rechazado, 0 conexiones |
| — | `dev-log` sin perfil local | Arranque rechazado con el mensaje esperado |

V45 sigue solo en el PR #17.

## 5. Regresión final (fase 5)

**Código probado: el árbol `3c006b87129c5a43bca11cc0601be2b23877adae`.**
- Es el árbol de `generation` en `114eb89`: idéntico byte a byte, comprobado con `git rev-parse`.
- En local se reprodujeron las 5 fusiones en el mismo orden y con la misma resolución del conflicto. El árbol resultante coincide con el de GitHub.

| Bloque | Ejecutadas | Aprobadas | Fallidas | Omitidas |
|---|---:|---:|---:|---:|
| Unitarias | 1.043 | 1.043 | 0 | 0 |
| Integración (todas las del CI) | 488 | 452 | 0 | 36 |
| P-2D en base marcada (efímera) | 7 | 7 | 0 | 0 |
| Zona horaria con mes y año (temporal) | 5 | 5 | 0 | 0 |
| 960 perfiles, `PLAN-HIST-1` | 1 | 1 | 0 | 0 |
| 960 perfiles, arnés propio de `generation` (`PLAN-HIST-1` + `PLAN-V4-0.1`) | 1 | 1 | 0 | 0 |
| REPLAY de 100 semanas | 1 | 1 | 0 | 1 (inspección manual) |

**Las 36 omitidas son *opt-in*** y no se cuentan como aprobadas. Ver `REGRESION_CIERRE_BACKEND.md` §4: IA real 5, Neon 4, P-2D 7 (ejecutadas aparte), laboratorios 15, comparación 1, campaña 2 y pendientes 2.

**Comparación histórica, que compara ejercicios, dosis, series, repeticiones, segundos, descansos y validación:**
- `PLAN-HIST-1`: **idéntica byte a byte** a `generation` 7c19703 (`planes.csv` 761d67cf…, `colocaciones.csv` fa9fa3ed…, 960 planes).
- Arnés propio de `generation`: **idéntico** a 7c19703 (d109a3da… / d227848b…, 1.920 planes: 875 válidos por política).
- No se mezcla con las cinco políticas candidatas del PR #17.

**REPLAY: idéntica.**
- Generaciones, planes, decisiones, adaptaciones, estados V32, regeneraciones, progresiones, métricas, historial e identificadores (linaje) coinciden.
- Solo cambian 2 líneas de `exclusiones_dolor.csv`, y son **iguales al normalizar las fechas** de registro y reactivación. Se descartaron sin commit.

**Criterio de aceptación, todo cumplido:**
- 0 fallos sin explicar;
- ningún cambio histórico;
- ninguna política experimental activa;
- ninguna publicación inválida;
- ninguna reincorporación automática (las exclusiones siguen activas en el caso C);
- ejercicios y dosis sin cambios;
- migraciones coherentes;
- CI verde en el commit integrado (§2 y el informe final).

## 6. PR #17 (fase 8)

- Sigue en **borrador, sin fusionar**, con sus indicadores apagados. No se ha modificado en GitHub.
- Al fusionar el #22 quedó **en conflicto** con `generation` (`mergeable_state = dirty`), como estaba previsto, en la misma línea de `SOLO_ESQUEMA`.
- Ajustes que necesitará: `ESTADO_PR17_Y_EVALUACION.md` §4 bis.
  - `MigracionV45IT` debe partir de lo anterior a V45.
  - `SOLO_ESQUEMA` debe quedar con 44.1, 44.2 y 45.
  - La secuencia es V44 → V44.1 → V44.2 → V45.
- **Compatibilidad comprobada en local sin subir nada** (combinación C de `REGRESION_CIERRE_BACKEND.md`, sobre las cabezas anteriores):
  - unitarias 1.059;
  - IT 514, con 57 omitidas;
  - baterías 4 × 225;
  - 960 × 5 idéntica;
  - REPLAY idéntica;
  - migraciones V44 a V45: 4/4.

  El único cambio posterior en el código de producción (`dev-log`) no toca nada del PR #17.

## 7. Pendientes antes de trabajar con usuarios reales (fase 9)

**Ninguno bloquea la integración local ni la integración con Flutter.** No se implementan en esta orden.

| # | Pendiente | Área | Criticidad para usuarios reales | Riesgo si falta | Depende de |
|---|---|---|---|---|---|
| 1 | Confirmar la rama de despliegue y el despliegue automático en Railway | Operación | **Alta** | Desplegar o migrar sin querer | Tú (panel de Railway) |
| 2 | Copia de seguridad y procedimiento de recuperación de la base | Operación | **Alta** | Pérdida de datos del estudio | Neon (rama o instantánea) |
| 3 | Invalidar sesiones al retirarse del estudio y al cambiar la contraseña | Seguridad | **Alta** | Un JWT robado sigue valiendo hasta 7 días | Diseño (marca «válido desde») |
| 4 | Proveedor de correo, o recuperación manual por el equipo | Funcionalidad externa | **Alta** si la recuperación se ofrece en la app | Sin correo, la recuperación no llega | Proveedor y credenciales en Railway |
| 5 | Recuperación del ciclo semanal si se pierde la ejecución de las 03:00 | Operación | Media-alta | Una semana sin plan tras un reinicio | Aprobar el diseño |
| 6 | Comprobación de salud | Operación | Media | Railway no detecta caídas | Decisión (Actuator con solo `health`) |
| 7 | Límite de peticiones por IP | Seguridad | Media | Fuerza bruta en el login | Pasarela o Railway |
| 8 | Quitar Neon como valor por defecto de `application.properties` | Configuración | Media | Arranques accidentales contra Neon | Variables de Railway |
| 9 | Cierre de sesión en el servidor | Seguridad | Baja-media | Depende del 3 | Diseño |
| 10 | Licencia y alojamiento de las imágenes de los ejercicios | Funcionalidad externa | Media (sin imágenes, el MVP funciona) | Uso no autorizado de material con © | Licencia |
| 11 | Autorizar la IA real y su presupuesto | Funcionalidad externa | Baja (el motor de reglas y el respaldo funcionan) | — | Tú |
| 12 | Decidir si la progresión se enciende en el entorno de pruebas | Configuración | Media | Planes sin progresión | Tú |
| 13 | A1–A6: aprobación, 2 profesionales, envío del paquete ciego, respuestas, evaluación y decisión sobre C2-D y EF | Evaluación | Solo para las políticas candidatas | — | Tú y los evaluadores |

## 8. Versión `backend-mvp-1.0` (fase 7)

- **Es una etiqueta anotada sobre el commit final validado de `generation`:** la fusión de este PR de documentación, con su CI en verde.
- El SHA completo, la fecha y el CI quedan en la anotación de la etiqueta y en el informe final.
- **Contenido:**
  - migraciones V1–V44, V44_1 y V44_2;
  - política por defecto `PLAN-HIST-1`, con `allow-candidates=false`;
  - los contratos de `CONTRATOS_FINALES_PARA_FLUTTER.md`.
- **Lo que no es:**
  - no es un despliegue;
  - no es una *release* con artefactos;
  - no es una certificación profesional;
  - no habilita el estado C.
