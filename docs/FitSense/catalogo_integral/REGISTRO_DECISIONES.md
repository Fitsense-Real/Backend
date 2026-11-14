# Registro único de decisiones pendientes

**Archivo verificable:** `cierre/REGISTRO_DECISIONES_PENDIENTES.tsv`. Lo genera `cierre/herramientas/registro_decisiones.py` desde la raíz del repositorio. El script es determinista y se puede volver a ejecutar.

**Origen:**
- Las 313 filas de `cierre/REVISION_PROFESIONAL_LISTA.tsv` (cierre FIN-3), con claves `P001`–`P313` en el mismo orden.
- 4 decisiones nuevas que nacen de la orden de desbloqueo: `A-1083`, `A-V53`, `A-1301` y `A-SUELO8`.
- 6 hallazgos colaterales de los verificadores que no estaban en la lista (`REVISION_VISUAL_COMPLETA.md` §5): `A-231`, `A-367`, `A-1303T`, `A-1323`, `A-1182` y `A-1017`.

**Sin firmas inventadas.** Los campos de respuesta (`decision`, `valor`, `nombre_responsable`, `credenciales_o_funcion`, `fecha` y `observaciones`) están **vacíos** en todas las filas. Ninguna fila está aprobada por una persona.

## 1. Qué contiene cada fila

| Columna | Contenido |
|---|---|
| `clave` | Identificador estable de la decisión |
| `bloque` y `prioridad` | 1 seguridad, 2 identidad y textos, 3 dificultad, 4 equipo (el orden que pide la orden) y 5 otros |
| `quien_decide` | `PROFESIONAL`, `PROPIETARIO y PROFESIONAL`, `TECNICO` (se resuelve con otra ronda R2 + V3 sobre el GIF, sin profesional) o `NINGUNO` cuando una comprobación técnica ya lo resolvió |
| `estado` | `ABIERTA`, `CONTENIDA` (contradicción neutralizada de forma conservadora a la espera de la decisión) o `RESUELTA_TECNICA` |
| `vigente_conservador` | Si el valor que el catálogo usa HOY, mientras nadie decide, es el más prudente (`SI`, `NO` o `NO_DETERMINADO`) |
| `valor_vigente` | Dificultad, banderas, impacto y equipo leídos de la huella real de la base (`huella_catalogo.tsv`) |
| `uso_real` | En cuántos de los 168 perfiles de la rejilla es elegible, en cuántos planes aparece y cuántas veces sale en la referencia de 72 |
| `comprobacion` | Prueba, columna o consulta que permite verificar el estado sin fiarse del documento |

## 2. Resultado

| Medida | Valor |
|---|---|
| Filas | **323** (313 + 4 + 6) |
| Ejercicios distintos | **296** (los 291 de la lista, 1083 y 231, 367, 1323, 1182 y 1017) |
| Abiertas | **320** |
| Contenidas | **1** (A-1083) |
| Resueltas por comprobación técnica | **2** (P018 · 775 y P285 · identidad de 675; la dificultad de 675 sigue abierta) |
| Necesitan un profesional | **309** filas |
| Necesitan al propietario y a un profesional | **6** filas (el significado de «no colgarse»: A-V53 y las 5 contenidas por INC-CAT-C39) |
| Se resuelven con una comprobación técnica más (R2 + V3) | **6** (los hallazgos colaterales) |
| No necesitan a nadie más | **2** (775: consecuencia documentada; 675: visto bueno opcional) |

### Por prioridad

| Prioridad | Filas | Contenido |
|---|---|---|
| 1 · Seguridad | 43 | Contenciones INC-4C-03 e INC-CAT-C39, banderas HI, INV y AX, apoyo suspendido, 1083, olímpicos dudosos, D2 (854) e identidades con consecuencia de seguridad (922, 930, 937, 1068, 1177, 1192, 1294 y 1298) |
| 2 · Identidad y textos | 28 | Identidades no decidibles con el GIF (64, 620 y 1261 solo necesitan visto bueno: la V3 las vio CLARAS), los 4 textos no verificables, la clase de las 8 posiciones con las manos en el suelo y 4 hallazgos colaterales técnicos (231, 367, 1303 y 1323) |
| 3 · Dificultad | 249 | 248 bajadas propuestas por la R2, que V27 no permite aplicar sin una persona, y 1113 (subida retenida por el dictamen de V39) |
| 4 · Equipo | 2 | 775 (resuelta) y 1182 (técnica) |
| 5 · Otros | 1 | 1017: músculos secundarios (técnica) |

### Lo que resolvió una comprobación técnica

| Clave | Ejercicio | Cómo se comprueba |
|---|---|---|
| P018 | 775 exige banco (codos sobre apoyos elevados), visto en el GIF por R2, V3 y el coordinador | Huella `requisitos = 1:bench`. `Hipertrofia4Dv34IT` fija los 14 perfiles que pierden el plan y su motivo (V20). La orden de 14/11 decidió que no se conserva la factibilidad con un equipo falso |
| P285 | 675: la ficha se renombró a lo que muestra el GIF (sentadilla sumo, sin vuelo) | `decisiones_v52.tsv` y la huella |
| A-1083 | Contradicción R03 neutralizada: HI añadido de forma conservadora y CHECK «ALTO implica HI» | Huella y restricción `ck_exercises_alto_implica_high_impact` (V52). Sigue abierta la pregunta de clase (DEC-HI-LANZAMIENTOS) |

### Lo que una comprobación técnica acota, pero no decide

- **Dificultad (248).** El valor vigente es el más alto: el ejercicio llega a menos perfiles y no hay riesgo de seguridad. La comprobación de uso dice a cuántos perfiles afecta la espera:
  - 199 son elegibles en algún perfil de la rejilla;
  - 81 aparecen en algún plan;
  - 30 salen en la referencia de 72.

  Los números exactos están en la columna `uso_real`.
- **1294.** El texto vigente es erróneo frente al GIF. No es elegible en ninguno de los 168 perfiles ni aparece en la referencia, y HG y OH lo excluyen de quien restringe colgarse o los brazos arriba. El riesgo es bajo, pero el texto sigue siendo una contradicción material.

### Filas cuyo valor vigente NO es el conservador (riesgo mientras no se decida)

| Clave | Ejercicio | Por qué | Mitigación vigente |
|---|---|---|---|
| P001 | 868 «Pike colgado» | Se propone añadir INV y hoy no la tiene | HG: «no colgarse» lo excluye. Dificultad 3 |
| P312 | 1294 «Balanceo 360» | Texto erróneo | No elegible en la rejilla; HG y OH |
| P313 | 1113 «Flexión pike a cobra» | R2 y V3 proponen nivel 2; el dictamen de V39 lo mantiene en 1 | Decisión previa documentada; requiere al profesional |
| A-1323 | 1323 «Jalón en dos poleas altas con agarre paralelo» | El texto menciona unos rodillos que no aparecen en el GIF | Ninguna: es una corrección de texto técnica (R2 + V3) pendiente |

Además, en A-231 y A-367 (clasificación dudosa) no se puede saber si el valor vigente es el conservador hasta hacer la revisión técnica.

## 3. Cómo se responde

Una persona con la función adecuada rellena, por fila, `decision` (APRUEBA / RECHAZA / MODIFICA), `valor`, `nombre_responsable`, `credenciales_o_funcion`, `fecha` y `observaciones`. Cada respuesta se aplica con una migración versionada y protegida, generada como V52, y la huella y este registro se recalculan.

**Mientras queden filas de prioridad 1 abiertas, el catálogo no puede declararse cerrado.**
