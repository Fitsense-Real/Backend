# Regresión del cierre del backend (fase 6)

Todo se ejecutó en local con PostgreSQL efímero (Testcontainers `postgres:16-alpine`, o un contenedor Docker local en `127.0.0.1`).
- Sin Neon ni Railway.
- Sin Replicate: IA apagada en todas las ejecuciones.
- **Las pruebas omitidas no se cuentan como aprobadas.** §4 dice cuáles se omitieron y por qué, y cuáles se ejecutaron aparte.

## 1. Commits probados

| Combinación | Contenido | Commit local |
|---|---|---|
| **B** · `generation` + 5 PR | `generation` 7c19703 + #22 (5740124) + #23 (a251798) + #24 (2114704) + #25 (3f31a4b) + #26 (65cf12f) | 7442e95 |
| **C** · PR #17 + 5 PR | PR #17 3a27f0b + los mismos 5 PR | 3719591 |
| Referencia | `generation` 7c19703, sin cambios | — |

Las combinaciones B y C no se han subido: son **ramas locales de verificación**.
- En B se resolvió una línea de `EsquemaVigenteSobreCatalogoAnterior.SOLO_ESQUEMA` que tocan #22 y #25.
- En C, además, se adaptó `MigracionV45IT` del PR #17 (§3).

Cada PR se probó **también por separado**: el CI de GitHub (`verify`, que ejecuta las unitarias y las de integración) está en **verde** en la cabeza de los 5. Ver Fitsense-Real/Backend#22, Fitsense-Real/Backend#23, Fitsense-Real/Backend#24, Fitsense-Real/Backend#25 y Fitsense-Real/Backend#26.

## 2. Resultados

| Bloque | Combinación | Ejecutadas | Fallos | Omitidas | Resultado |
|---|---|---:|---:|---:|---|
| Unitarias | B | 1.041 | 0 | 0 | ✅ |
| Integración (todas las IT del CI) | B | 488 | **1** → corregido | 36 | ✅ tras la corrección (§3) |
| Matriz de 960 perfiles × 5 políticas | B | 1 | 0 | 0 | ✅ **idéntica byte a byte** a `generation` 7c19703 (`planes.csv` 761d67cf…, `colocaciones.csv` fa9fa3ed…, 961 líneas) |
| Campaña REPLAY (100 semanas, cohorte 02) | B | 1 (+1 de inspección omitida) | 0 | 1 | ✅ evidencia idéntica salvo marcas de tiempo |
| Unitarias | C | 1.059 | 0 | 0 | ✅ (PR #17: 1.046 + 13 nuevas) |
| Integración (todas) | C | 514 | **1** → corregido | 57 | ✅ tras la corrección (§3) |
| Baterías por política ×4 (`PLAN-HIST-1`, `PLAN-C2D-0.1`, `PLAN-C2D-EF-0.1`, `PLAN-C2D-V4-EF-0.1`) | C | 4 × 225 | 0 | 4 × 2 | ✅ (antes 4 × 223 aprobadas; ahora también 223 aprobadas + las mismas 2 omitidas) |
| Ciclo real 10 semanas × 6 políticas | C | 6 | 0 | 0 | ✅ |
| Matriz de 960 perfiles × 5 políticas | C | 1 | 0 | 0 | ✅ **idéntica byte a byte** a la referencia del PR #17 (`planes.csv` c582fce2…, `colocaciones.csv` ee9d2111…). Válidos: HIST 875 · C2D 951 · C2D-V4 951 · C2D-EF 902 · C2D-V4-EF 902; 48 perfiles EF con VEF |
| Campaña REPLAY | C | 1 (+1 omitida) | 0 | 1 | ✅ igual que en B |
| Migraciones V44, V44_1, V44_2 y V45 | C | 4 | 0 | 0 | ✅ |
| P-2D: invariantes en la base (`InvariantesV33IT`, `PublicacionAtomicaIT`, `DisparadorDiferidoIT`) | C | 7 | 0 | 0 | ✅ Antes se omitían; ahora se ejecutaron contra un PostgreSQL efímero local, migrado a V45 y marcado `PRUEBAS` |

**Diferencias de comportamiento: ninguna.**
- La generación (960 perfiles × 5 políticas) y la campaña de 100 semanas dan lo mismo con y sin los 5 PR.
- La única diferencia de la campaña son las columnas de fecha de `exclusiones_dolor.csv` (`recorded_at`, `reactivated_at` y la evidencia con la hora), que cambian en cada ejecución. Ese cambio se descartó (no se ha hecho commit).

## 3. Fallos encontrados y corregidos

| # | Fallo | Causa | Corrección | Verificación |
|---|---|---|---|---|
| 1 | 37 errores de IT en el CI de #22 y #25 (`missing column reason_codes`) | Las pruebas con catálogo anclado (`spring.flyway.target=35..41`) aplican aparte las migraciones de solo esquema, y V44_1/V44_2 no estaban en `SOLO_ESQUEMA` | Añadidas a la lista (5740124, bb3f724) | Reproducido en local; 37/37 en verde; CI verde |
| 2 | `MigracionV44_2IT` esperaba 1 migración y aplicó 2 en B | Daba por hecho que V44_1 no existía | La prueba migra hasta la última versión anterior a 44.2, sea cual sea (3f31a4b, en el PR #25) | CI verde en #25; en B y C pasa |
| 3 | `MigracionV45IT` (PR #17) esperaba 1 migración y aplicó 3 en C | La misma suposición: que V45 va justo después de V44 | **Solo en la combinación local** (3719591): la prueba parte de la última versión anterior a V45 | En C pasan V44, V44_1, V44_2 y V45 |
| 4 | Login con credenciales erróneas → **500** (en `generation`) | Sin manejador para `InvalidCredentialsException` | PR #23 → 401, con el mismo mensaje exista o no la cuenta | `ContratoErroresApiIT` y curl |

**Consecuencia para el PR #17.** Cuando absorba `generation` con #22 y #25 fusionados, necesitará dos cosas:
- el arreglo nº 3 de `MigracionV45IT`;
- la línea `SOLO_ESQUEMA` con 44.1, 44.2 y 45.

Están probados en la combinación C, pero **no se han subido al PR #17**: la orden no autoriza tocarlo. Ver `ESTADO_PR17_Y_EVALUACION.md`.

## 4. Pruebas omitidas y por qué

Las 36 de B y las 57 de C son todas ***opt-in***: necesitan una propiedad del sistema, un recurso externo o una autorización. **Ninguna se omite por fallar.**

| Grupo | Pruebas | En B | En C | Motivo | Cubiertas aparte |
|---|---|---:|---:|---|---|
| **IA real (Replicate)** | `CorreccionReplicateEvidenciaIT`, `A01AdaptacionRealIT`, reincorporación con IA real, campaña MVP 8×8, campaña con Replicate en los 4 objetivos | 5 | 5 | Consumo de Replicate **no autorizado** | No. La campaña REPLAY reproduce respuestas reales grabadas |
| **Base Neon de solo lectura** | Bloqueo del ciclo en Neon, lectura directa V24 ×2, `SemillaLecturaIT` | 4 | 4 | Se conectan a Neon: **prohibido** en esta orden | No |
| **P-2D en base marcada** | `InvariantesV33IT` (3), `PublicacionAtomicaIT` (2), `DisparadorDiferidoIT` (2) | 7 | 7 | Necesitan `fitsense.pruebas.url` | **Sí: 7/7 en C** (§2) |
| **Laboratorios e investigación** (generan evidencia, no comprueban el contrato) | `GeneracionReferenciaCandidataIT`, `ImpactoRestriccionesIT`, `ProgresionFactibilidadIT`, `DiagnosticoV20ComposicionIT` (2), evaluación de 4-6 días (3), `ProgresionReservaIT`, `AnalisisSustitucion1373IT` (2), factibilidad de 30-120 min (2), evaluación limpia de 72 perfiles, `DiagnosticoRespaldoMultisemanaIT` | 15 | 15 | Bandera de laboratorio | No en esta etapa; ya se ejecutaron en las etapas que las crearon. Ninguna toca el código cambiado |
| **Comparación de 960 perfiles** | `ComparacionPerfilesIT` | 1 | 1 | Bandera | **Sí** (B y C) |
| **Campaña P-1/2/3 (REPLAY)** | `CampanaP123Cohorte02IT` (2: campaña + inspección) | 2 | 2 | Bandera | **Sí** la campaña. La inspección es manual |
| **Pendiente de decisión ya resuelto** | `ValidacionMultisemanaIT.cambiaSuPerfil` (días, equipamiento) | 2 | 2 | Reproduce, cambiando el perfil **por SQL**, lo que pasaba antes de congelar el perfil | Se ejecutó: falla como está previsto (NO_FACTIBLE). Por la API no puede ocurrir: el perfil queda congelado tras el primer plan (`PerfilCongeladoIT`, en verde) |
| **Laboratorios del PR #17** | Ciclo real de 10 semanas × 6 | — | 6 | Bandera | **Sí: 6/6** |
| | Cierre del PR #17 (48 perfiles, sustitución, EF×LW, muestra ciega) | — | 7 | Bandera | No en esta etapa. Se ejecutaron en el cierre del PR #17 y la matriz de 960 lo confirma byte a byte |
| | Matriz 960 con políticas reales (4) y con/sin EF (4) | — | 8 | Bandera | Cubiertas por la comparación de 960 perfiles de C |

## 5. Áreas de la orden y dónde quedan cubiertas

| Área | Pruebas |
|---|---|
| Autenticación y perfil | `ContratoErroresApiIT` (6): login 401; JWT caducado, de otra firma, mal formado o manipulado → 401; caducidad 7 días. Más `PerfilCongeladoIT` y las IT de registro y perfil existentes |
| Generación | Matriz de 960 × 5 idéntica; baterías por política |
| Ejecución, dolor, adherencia | `AdherenciaIT`, `ContinuidadSemanalIT`, `RespaldoConDolorIT` (con los campos nuevos de #22) |
| Adaptación y respaldo | `AdaptacionCompletaIT`, `AdaptacionPorCausasIT`, `AdaptacionRegresionIT`, `RespaldoConDolorIT` A/B/C |
| Progresión y reincorporación | `ProgresionIT`, `ProgresionCienPorCienIT`, `HerenciaD8IT`, campaña REPLAY |
| NO_FACTIBLE estructurado | `AdaptationOutcomeContratoTest` (9, con la compatibilidad JSON de los 9 campos de siempre); `RespaldoConDolorIT` caso C (códigos, sesión afectada, restricción 1373); `MigracionV44_1IT` |
| Concurrencia e idempotencia | `CicloConcurrenteIT`, `ProteccionPlanesPublicadosIT`, `PublicacionAtomicaIT`, `DisparadorDiferidoIT`. Recuperación: token de un solo uso con bloqueo `PESSIMISTIC_WRITE` |
| Seguridad | `ContratoErroresApiIT`: recursos ajenos 404, entrada mal formada 400. `LocalProfileGuardTest` (4). `RecuperacionContrasenaIT` (5): sin enumeración, un solo uso, caducidad, límite 3/h, invalidación |
| Fechas y zonas horarias | `ZonaHorariaUsuarioIT` (3): Lima/Tokio en los bordes del día; horario de verano Madrid 25-10-2026 y Nueva York 01-11-2026 |
| Copia de seguridad y migraciones | `MigracionV44IT`, `MigracionV44_1IT`, `MigracionV44_2IT`, `MigracionV45IT`; arranque del *jar* de B en base limpia (46 migraciones) y segundo arranque con «Schema up to date»; el *jar* de C aplica 47 en base limpia |
| Cliente HTTP independiente | `scripts/smoke-local.sh`: 20/20 con curl contra el *jar* de B. Más la verificación manual de login 401, recuperación (202 igual con y sin cuenta, 204, reutilización 400, contraseña nueva 200 y antigua 401). Ver `GUIA_EJECUCION_LOCAL.md` |

**Cambios de DTO con su prueba de compatibilidad:**
- `AdaptationOutcome`: los 9 campos de siempre, en el mismo orden y con el mismo JSON (`AdaptationOutcomeTest.camposDelContrato` y `AdaptationOutcomeContratoTest`).
- Los errores 400/401 nuevos usan el `ApiErrorResource` existente.
