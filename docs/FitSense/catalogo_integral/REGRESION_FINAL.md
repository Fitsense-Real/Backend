# Regresión final y segunda auditoría

## 1. Regresión completa (orden de desbloqueo, 14/11/2026)

Ejecutada con `sh mvnw -o verify -Pintegration` en local: todas las pruebas unitarias y de integración, cada una sobre una base PostgreSQL 16 nueva con Flyway.

| Rama (cabeza probada) | Catálogo | Unitarias | Integración | Fallos | Omitidas |
|---|---|---|---|---|---|
| PR A `cierre/v51-v52-correcciones` (52442ae) | V1–V52 | **1.468, 0 fallos** | **722, 0 fallos ni errores** | **0** | 1 + 50 (opcionales: laboratorios, campañas y capturas) |
| PR B `cierre/v53-apoyo-suspendido` (b202ca4) | V1–V53, interruptor apagado | **1.468, 0 fallos** | **727, 0 fallos ni errores** | **0** | 1 + 50 |
| #62 `fix/sustituto-dosis-capacidad` (1da115a) | `generation` + corrección | **1.431, 0 fallos** | **617, 0 fallos ni errores** | **0** | 1 + 46 |

Después de la cabeza probada de la PR A solo cambian documentos.

**CI de GitHub** en las cabezas de #60, #61 y #62: **verde** (`verify`).

| Comprobación de la orden | Resultado |
|---|---|
| Cero regresiones no explicadas | **Sí.** La primera pasada con la corrección encontró 2 (O12 `subeSeries`): era un efecto colateral real, que se cerró reforzando V8C sin tocar la prueba (`HALL_PROG_01.md` §4 bis). En la pasada final: 0 |
| Cero violaciones de seguridad | **Sí.** Pasan todas las pruebas de V24, V8C, P-03, P-02 (lo que dolió no vuelve), contención y elegibilidad (oráculo independiente) |
| Cero aumentos de dosis no autorizados | **Sí.** O12 `subeSeries` se rechaza (RESPALDO); pasan `SubidaPorSustitucionTest`, `SubidaIaEnQuitadoTest` y `SustitutoDosisCapacidadTest` («nunca más trabajo») y las unitarias V8C |
| Los dos p02 correctos | **Sí,** en las tres ramas y en las 5 semillas probadas |
| Los cuatro objetivos conservan factibilidad cuando hay solución válida | **Sí.** Rejilla de 168 perfiles: 168 de 168 con plan en PR A y PR B (también encendido). Hipertrofia de 30 a 75 min: solo pierden el plan los 14 perfiles de casa con peso corporal que dependían del falso «775 sin banco», y no tienen solución válida con su equipo |
| 72 planes de referencia válidos y cambios justificados | **Sí.** 72 de 72 VALIDA. Cambian 30, todos de casa, por 775 con banco (§3) |
| V51–V53 reproducibles | **Sí.** Segunda auditoría desde una base nueva (§4) |
| Las PR existentes no pierden garantías | **Sí.** Ninguna prueba de las PR #55–#59 ni de `generation` se ha debilitado. #62 pasa la regresión completa sobre `generation` |

### 1.1 HALL-PROG-01 (resuelto)

Ver `HALL_PROG_01.md`: reproducción por semilla, causa, corrección y refuerzo de V8C. Sustituye el análisis anterior de esta sección, que atribuía el fallo a la aparición con el cierre. El defecto ya existía en el motor.

## 2. Pruebas modificadas en el cierre y por qué

No se ha convertido en verde ninguna prueba cambiando su expectativa para tapar un defecto. Cada cambio responde a un dato corregido y verificado, o fija una prueba de escenario al catálogo sobre el que se construyó.

| Prueba | Cambio | Motivo |
|---|---|---|
| `CatalogoCalidadIT` y `huella_catalogo.tsv` | Columnas de impacto, estiramiento, apoyo y firma de textos; huella recapturada | Cambian exactamente los 442 ejercicios decididos (comprobado con un script contra `decisiones_v52.tsv`) |
| `CatalogoAtributosExplicitosIT` (nueva) | Paridad de V51 con la regla por nombre | Las 14 excepciones son las correcciones de V52 enumeradas una a una |
| `AtributosExplicitosV51Test` (nueva) | El nombre no decide si hay atributo | Contrato de V51 |
| `ProcedenciaCatalogoIT` (nueva, opcional) | Procedencia por atributo y versión | Línea base y segunda auditoría |
| `CatalogoIntegralTextosIT` | Los textos reescritos por V52 ya no tienen que ser los de V49 | Se leen de las cabeceras de V52, sin una lista a mano |
| `CatalogoIntegralSeguridadIT` | 503 sale de la lista de V47 | V52 le quita una OH que no tiene, con R2, V3 y el coordinador |
| `CatalogoElegibleIT` (oráculo) | Atributos explícitos y apoyo suspendido | El oráculo debe reflejar el contrato vigente |
| `ContencionTextoBanderaIT` | Perfil INTERMEDIATE en dos casos | 1301, 1303, 1304 y 1306 suben a nivel 2 (V3) y salían por nivel antes de llegar a la contención que se prueba. La causa vuelve a ser `INC-CAT-C39` porque V53 está apagado |
| `Plan78ContencionReducidaIT` | El 910 entre los problemas de la reparación; sustituto 433 | El 910 sube a nivel 2 en V52 |
| `RetiradaLocalizada*IT`, `LinajeIntegridadIT` y `TextoRespaldoSustitucionIT` | Fijadas a V50 con el esquema vigente | Son escenarios construidos con planes de V50: el mecanismo no cambia, pero la premisa («el plan lleva el 135») desaparece |
| `EsquemaVigenteSobreCatalogoAnterior` | Script de solo esquema V51–V53 | Las pruebas fijadas a un catálogo anterior necesitan las columnas nuevas |
| `GeneracionReferenciaIT` | Referencia regenerada (dos veces: cierre y orden de desbloqueo) | Ver §3 |
| `Hipertrofia4Dv34IT` | Exige que fallen exactamente 14 perfiles de casa solo con peso corporal, y solo por V20 | 775 exige banco (orden de desbloqueo). Cualquier otro fallo sigue rompiendo la prueba |
| `CatalogoElegibleIT` (oráculo) | «Sin impacto» excluye también `impact_level = 'ALTO'` | Contención de 1083 |
| `ApoyoSuspendidoInterruptorIT` (nueva, PR B) | Dato, apagado, encendido, causa y script de solo lectura | V53 tras un interruptor |
| `SustitutoDosisCapacidadTest` y `SubidaIaEnQuitadoTest` (nuevas) | Dosis del sustituto y refuerzo de V8C | HALL-PROG-01 |

## 3. Referencia de generación (`GeneracionReferenciaIT`, 72 planes)

| Medida | Cierre (V52 inicial) | Tras la orden de desbloqueo |
|---|---|---|
| Planes que cambian | 70 de 72 respecto de V50 | **30 de 72** respecto de la referencia del cierre, **todos de casa** (los 36 de gimnasio, idénticos) |
| Planes VALIDA | 72 de 72 | **72 de 72** |
| Causa | Subidas de nivel, estiramientos y banderas retiradas | **775 exige banco**: sale de los 10 planes de casa que lo llevaban, y el resto es el reparto determinista sobre el conjunto elegible sin 775 |
| Con V53 encendido o apagado | Idéntica | Idéntica |

Diff: `git log -p -- docs/FitSense/evidencia/progresion/generacion_referencia.txt`.

## 4. Segunda auditoría desde una base nueva (repetida el 14/11/2026)

`ProcedenciaCatalogoIT` (`-Dfitsense.catalogo.procedencia=true`) creó una base PostgreSQL nueva en la cabeza de la PR B y aplicó **V1–V53 una a una**, registrando cada atributo de cada ejercicio después de cada versión. Salida: `cierre/auditoria_final/` (`procedencia_cambios.tsv.gz`, `procedencia_resumen.tsv` y `SHA256SUMS`).

| Versión | Ejercicios que cambian | Cambios de atributo | Comprobación |
|---|---|---|---|
| V51 | 1.376 | 2.752 | Exactamente `impact_level` e `is_stretch` en todos |
| V52 | **442** | 904 | **Los ids coinciden exactamente con `decisiones_v52.tsv`** (563 decisiones, incluidas las de 1083 y 775) |
| V53 | 1.376 | 1.376 | La columna nueva: `true` en exactamente los **15** de V53 y `false` en los otros 1.361 |

Las migraciones se reproducen desde cero sin error y el resultado coincide con lo decidido, ejercicio a ejercicio. La restricción `ck_exercises_alto_implica_high_impact` se crea sin error, porque ningún activo queda ALTO sin HI.

## 5. Otras verificaciones

| Verificación | Resultado |
|---|---|
| Huella de la base frente a las decisiones (PR A y rama final) | 442 = 442 |
| Registro maestro frente a la huella real | 0 discrepancias de textos y dificultad en 1.219 activos |
| Matriz de coherencia (13 reglas) | Ver `MATRIZ_COHERENCIA_FINAL.xlsx`, regenerada con el registro único |
| Rejilla de 168 perfiles | 168 → 168 con plan en PR A, PR B apagado (idéntica) y PR B encendido (`IMPACTO_ELEGIBILIDAD.md` §5) |
| Hipertrofia de 30 a 75 minutos | 298 de 312 con plan. Los 14 sin plan son de casa con peso corporal, por 775 con banco (V20, legítimo) |
