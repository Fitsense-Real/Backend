# Regresión final y segunda auditoría

## 1. Regresión completa

Ejecutada con `sh mvnw -o verify -Pintegration`: todas las pruebas unitarias y las de integración, cada una sobre una base PostgreSQL 16 nueva con Flyway V1–V53, o V1–V52 en la PR A.

| Rama | Unitarias | Integración | Fallos | Omitidas |
|---|---|---|---|---|
| `cierre/catalogo-final` (V51 + V52 + V53) | 1.454, 0 fallos | 722 | **2 errores** (HALL-PROG-01) | 1 unitaria y 50 de integración (opcionales: laboratorios, campañas y capturas) |
| `cierre/v51-v52-correcciones` (V51 + V52) | 1.454, 0 fallos | 722 | **2 errores** (los mismos) | Las mismas |

Las pruebas de V24 y V8C pasan todas en las dos ramas. Unitarias: `CargaV8CTest`, `DolorSinMasTrabajoV8CTest`, `DosisIaV8CTest`, `ProgresionV24V8CTest`, `RetiradaLocalizadaV8CTest`, `V8CConservacionPatronH1Test`, `V8CDiagnosticoSustitucionesTest` y `V8CRetiradasYDescansoTest`. De integración: `EvaluacionDirectaV24IT`, `EvaluacionLecturaV24IT`, las 6 `RetiradaLocalizada*IT` y las 4 `SinSustitutoDolor*IT`. Ningún validador se ha modificado en el cierre.

### 1.1 Fallos y su causa

Solo fallan dos pruebas, las mismas en las dos ramas:
- `ProgresionIT.p02ProgresionSinElExcluido`: `NoSuchElementException` en la línea 524, porque la semana 4 queda sin plan;
- `ProgresionRev2IT.p02ProgresionSinElExcluido`: la misma prueba con REV2.

**HALL-PROG-01** (`ProgresionIT.p02ProgresionSinElExcluido` y su gemela `ProgresionRev2IT`).

| Dónde | Estado | Resultado de p02 |
|---|---|---|
| **CI de GitHub** | Cabeza de #56 (05:50 UTC) | Pasa |
| **CI de GitHub** | Base del cierre, #55 sobre #56–#59 (57075e7: 16:10 y otra vez a las 22:16 UTC) | **Pasa** |
| **CI de GitHub** | #60 (V51 + V52) y #61 (+ V53), 21:55 UTC | **Falla** |
| Local | `generation` (623c4fa) | Pasa |
| Local | Cabeza de #56 y base 57075e7 | **Falla** |
| Local | Cierre, también con el catálogo fijado en V50 | **Falla** |

**En la CI de GitHub el fallo aparece con el cierre**: la base pasa y #60 falla, con el mismo código de prueba. En local falla ya en la base. Esa diferencia de entorno no está aislada: no se explica por la versión de Postgres (falla también con 16.16, la de la CI) ni por el reloj de la JVM (falla también fijado a las 16:15 UTC). Así que **no se puede afirmar que el fallo sea ajeno al cierre**.

Mecanismo observado en local, con trazas:
1. La progresión sube el ejercicio marcado a 3x19.
2. El usuario lo señala con dolor y la semana conservada lo sustituye.
3. El sustituto hereda la dosis progresada 3x19, que V24 rechaza por no ser la dosis de ninguna capacidad. En `generation` es el ejercicio 72.
4. En `generation`, la regeneración segura lo resuelve.
5. Con el catálogo nuevo, la semana regenerada con dosis base sube los segundos (1.140 → 1.260). P-03 nivel 2 la rechaza y la semana queda NO_FACTIBLE (V32).

No se relaja P-03 ni V24, no se cambia la prueba y no se toca el motor de adaptación. **Es un bloqueo de #60**: su CI está en rojo.

La CI de GitHub de #60 y #61 coincide con la regresión local: 722 pruebas de integración y solo estos 2 errores.

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
| `ContencionTextoBanderaIT` | Perfil INTERMEDIATE en dos casos; causa `INC-CAT-C39+apoyo_suspendido` | 1301, 1303, 1304 y 1306 suben a nivel 2 (V3) y salían por nivel antes de llegar a la contención que se prueba |
| `Plan78ContencionReducidaIT` | El 910 entre los problemas de la reparación; sustituto 433 | El 910 sube a nivel 2 en V52 |
| `RetiradaLocalizada*IT`, `LinajeIntegridadIT` y `TextoRespaldoSustitucionIT` | Fijadas a V50 con el esquema vigente | Son escenarios construidos con planes de V50: el mecanismo no cambia, pero la premisa («el plan lleva el 135») desaparece |
| `EsquemaVigenteSobreCatalogoAnterior` | Script de solo esquema V51–V53 | Las pruebas fijadas a un catálogo anterior necesitan las columnas nuevas |
| `GeneracionReferenciaIT` | Referencia regenerada | Ver §3 |

## 3. Referencia de generación (`GeneracionReferenciaIT`, 72 planes)

| Medida | Valor |
|---|---|
| Planes que cambian | 70 de 72 |
| Planes VALIDA | 72 de 72 |
| Con y sin V53 | Idéntica |

Ejercicios que más salen: 1262, 1115 y 1099 (12 apariciones cada uno), y 1334 y 1097 (9).

Ejercicios que más entran: 1138 (13), 1179 (12), 933 y 1096 (10) y 1155 (9).

El cambio se debe a las subidas de nivel, a los ejercicios que dejan de ser estiramiento y a las banderas retiradas. Diff: `git diff -- docs/FitSense/evidencia/progresion/generacion_referencia.txt`.

## 4. Segunda auditoría desde una base nueva

`ProcedenciaCatalogoIT` (`-Dfitsense.catalogo.procedencia=true`) creó una base PostgreSQL nueva y aplicó **V1–V53 una a una**, registrando cada atributo de cada ejercicio después de cada versión. Salida: `cierre/auditoria_final/` (`procedencia_cambios.tsv.gz`, `procedencia_resumen.tsv` y `SHA256SUMS`).

| Versión | Ejercicios que cambian | Cambios de atributo | Comprobación |
|---|---|---|---|
| V51 | 1.376 | 2.752 | Exactamente `impact_level` e `is_stretch` en todos |
| V52 | **442** | 900 | **Los ids coinciden exactamente con `decisiones_v52.tsv`** |
| V53 | 1.376 | 1.376 | La columna nueva: `true` en exactamente los 16 de V53 y `false` en los otros 1.360 |

Las migraciones se reproducen desde cero sin error, y el resultado coincide con lo decidido, ejercicio a ejercicio.

## 5. Otras verificaciones

| Verificación | Resultado |
|---|---|
| Huella de la base frente a las decisiones (PR A y rama final) | 442 = 442 |
| Registro maestro frente a la huella real | 0 discrepancias de textos y dificultad en 1.219 activos |
| Matriz de coherencia (13 reglas) | Ver `MATRIZ_COHERENCIA_FINAL.xlsx`: 0 incumplimientos estructurales y 1 contradicción abierta (1083) |
| Rejilla de 168 perfiles | 168 → 168 con plan (`IMPACTO_ELEGIBILIDAD.md`) |
| Hipertrofia de 30 a 75 minutos | Como con V50, con 775 aplazado |
