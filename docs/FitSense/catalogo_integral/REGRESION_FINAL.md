# Regresión final y segunda auditoría

## 1. Regresión completa

Ejecutada con `sh mvnw -o verify -Pintegration`: todas las pruebas unitarias y las de integración, cada una sobre una base PostgreSQL 16 nueva con Flyway V1–V53, o V1–V52 en la PR A.

PENDIENTE_TABLA_REGRESION

### 1.1 Fallos y su causa

PENDIENTE_FALLOS

**HALL-PROG-01** (`ProgresionIT.p02ProgresionSinElExcluido` y su gemela `ProgresionRev2IT`). **No lo introduce el cierre:**

| Estado | p02 |
|---|---|
| `generation` (623c4fa) | Pasa (48/48 en las dos clases) |
| Cabeza de #56 (d8ffe4c, V47 y V48) | **Falla** |
| Base del cierre (57075e7, #55 sobre #56–#59) | **Falla** |
| Cierre con el catálogo fijado en V50 | **Falla** |
| Cierre (V51–V53) | **Falla** |

Causa, con trazas:
1. La progresión sube el ejercicio marcado a 3x19.
2. El usuario lo señala con dolor y la semana conservada lo sustituye.
3. El sustituto hereda la dosis progresada 3x19, que V24 rechaza por no ser la dosis de ninguna capacidad. En `generation` es el ejercicio 72.
4. En `generation`, la regeneración segura lo resuelve.
5. Con el catálogo de #56, la semana regenerada con dosis base sube los segundos (1.140 → 1.260). P-03 nivel 2 la rechaza y la semana queda NO_FACTIBLE (V32).

No se relaja P-03 ni V24, no se cambia la prueba y no se toca el motor de adaptación.

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

PENDIENTE_AUDITORIA

## 5. Otras verificaciones

| Verificación | Resultado |
|---|---|
| Huella de la base frente a las decisiones (PR A y rama final) | 442 = 442 |
| Registro maestro frente a la huella real | 0 discrepancias de textos y dificultad en 1.219 activos |
| Matriz de coherencia (13 reglas) | Ver `MATRIZ_COHERENCIA_FINAL.xlsx`: 0 incumplimientos estructurales y 1 contradicción abierta (1083) |
| Rejilla de 168 perfiles | 168 → 168 con plan (`IMPACTO_ELEGIBILIDAD.md`) |
| Hipertrofia de 30 a 75 minutos | Como con V50, con 775 aplazado |
