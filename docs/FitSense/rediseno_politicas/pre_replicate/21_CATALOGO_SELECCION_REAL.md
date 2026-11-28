# 21 · Catálogo: problemas que comprometen la selección real de ejercicios

**Alcance:**
- Lo que pediste: 775, 973, 1320, 580 y los ejercicios aislados que cuentan como patrón principal. Además, por completitud, los demás de `12` §4.
- **No se ha modificado el catálogo.** Este documento solo mide y clasifica.

**Fuentes:**
- Catálogo exportado de las corridas K20 del cierre (`tablas/exercises.csv`, idéntico en todas, 1.376 ejercicios).
- Barridos finales `evidencia/barridos/REV2_cierre` (3.776 planes válidos) y `HIST_cierre` (3.641).
- Corridas K20 REV2 D (U, J, K, L, M).
- Script: `catalogo_impacto.py` (solo lectura). La versión de aislamiento con dosis de fuerza es `anexo_fase5/scripts/s16_slots_aislamiento.py`, aplicada a `REV2_cierre`.

## 1. Criterio de clasificación

| Clase | Significado |
|---|---|
| **ERROR COMPROBADO** | Se demuestra con los datos del propio catálogo y del código, sin mirar el GIF ni opinar sobre técnica deportiva: dos campos se contradicen, o el sistema usa el ejercicio para algo que su propia ficha niega |
| **REVISIÓN VISUAL** | Hay que ver el GIF para decidir qué ejercicio es de verdad. Las revisiones visuales anteriores fueron **asistidas por IA** y no cuentan como comprobación |
| **REVISIÓN PROFESIONAL** | Es una decisión deportiva: si el ejercicio debe contar para un grupo o patrón, o qué dificultad tiene |

**Indicadores de impacto** (por plan semanal válido del barrido):
- **Planes:** planes semanales en que aparece el ejercicio.
- **Único del grupo:** planes donde es el único ejercicio resistido de su grupo corporal, así que **toda** la dosis de ese grupo sale de él.
- **Único crédito:** planes donde es el único que cubre su familia de patrón (empuje, tracción, rodilla o cadera). Sin él, la familia quedaría sin cubrir.

## 2. Los cuatro que pediste

### 775 · «elbow lift - reverse push-up» (Elevación de codo y push-up invertido)

- **Ficha:** grupo `back`, músculo `upper back`, patrón `HORIZONTAL_PUSH`, no aislado, dificultad 1, peso corporal.
- **Instrucciones:** boca abajo, empujar con las palmas para levantar el tronco.

| Hallazgo | Clase |
|---|---|
| La ficha es contradictoria: grupo y músculo de espalda con un patrón de empuje. El sistema suma sus series a la dosis de **espalda** y, a la vez, lo trata como **empuje**. Las instrucciones no describen ninguna tracción | **ERROR COMPROBADO** |
| Qué ejercicio es (¿extensión de tronco tumbado?, ¿movilidad?) y qué patrón le corresponde | REVISIÓN VISUAL + PROFESIONAL |

| Impacto | REV2 | HIST |
|---|---:|---:|
| Planes | **1.441** (38 %) | 1.589 |
| Único del grupo espalda | **615** | 640 |
| Series que aporta | 6.958 | 7.314 |
| Único crédito de empuje | 0 | 0 |
| Sobre todo en | casa sin material (1.056) | |

**Usuarios K20 expuestos (REV2 D):** U 6/20, J 6/16, K 7/16, L 5/16, M 10/30.

**Conclusión:** es el problema de catálogo con más peso en la selección real. En 615 planes REV2, la «dosis de espalda» que se informa sale entera de un ejercicio que no trabaja la espalda como dice la ficha. **Afecta a la clasificación DOSIS_OBJETIVO / DOSIS_LIMITADA de esos planes.** La tracción (PULL) no se ve afectada: 775 no la acredita.

### 973 · «left hook. boxing» (Giro izquierdo. boxeo)

- **Ficha:** grupo `shoulders`, músculo `delts`, patrón `TRUNK_ROTATION_LATERAL_FLEXION`, peso corporal, dificultad 1.

| Hallazgo | Clase |
|---|---|
| Traducción: un *hook* es un gancho, no un giro | **ERROR COMPROBADO** |
| Su patrón es de rotación de tronco, pero sus series cuentan como dosis **directa de hombro** | **ERROR COMPROBADO** (contradicción interna) |
| Si un golpe sin carga es un estímulo válido de hombro | REVISIÓN PROFESIONAL |

| Impacto | REV2 | HIST |
|---|---:|---:|
| Planes | **976** | 560 |
| Único del grupo hombro | **917** | 537 |
| Series que aporta | 4.452 | 2.193 |
| Sobre todo en | casa sin material (914) | |

**Usuarios K20 expuestos (REV2 D):** U 0, J 3, K 5, L 4, M 8.

**Conclusión:** en casa sin material es, en la práctica, el único «hombro». REV2 lo usa casi el doble que HIST (976 frente a 560 planes); no se ha aislado la causa, aunque es coherente con que REV2 busque dosis por grupo. **En la práctica, REV2 es más sensible a este error que HIST.**

### 1320 · «triceps dips floor» (Dip de tríceps en piso)

- **Ficha:** grupo `upper arms`, patrón `HORIZONTAL_PUSH`, peso corporal **sin requisito de material**.
- **Instrucciones:** «Sit on the edge of a chair or bench…».

| Hallazgo | Clase |
|---|---|
| Las instrucciones exigen silla o banco y el requisito de material no lo recoge. La ficha `/exercises/{id}` muestra ese texto a quien entrena en casa sin material | **ERROR COMPROBADO** |
| Que el GIF no muestra silla | REVISIÓN VISUAL (la observación anterior fue asistida por IA) |
| Patrón: un *dip* se suele considerar empuje vertical, no horizontal | REVISIÓN PROFESIONAL |

| Impacto | REV2 | HIST |
|---|---:|---:|
| Planes | 359 | 401 |
| De ellos, casa sin material | 194 | — |
| Único crédito de empuje | 1 | 1 |

**Usuarios K20 expuestos (REV2 D):** J 1, L 1, M 4.

**Conclusión:** no altera la cobertura de patrones, pero sí el **contenido mostrado**: instrucciones imposibles de seguir sin una silla.

### 580 · «dumbbell incline breeding» (Curl inclinado con mancuerna)

- **Ficha:** grupo `chest`, músculo `pectorals`, patrón `HORIZONTAL_PUSH`, **no aislado**, dosis de fuerza 3×6, requiere mancuerna y banco.

| Hallazgo | Clase |
|---|---|
| La traducción «Curl» no corresponde al nombre original ni al músculo (pectoral) | **ERROR COMPROBADO** |
| Si es una apertura (*fly*): sería aislado y no un empuje, y la dosis de fuerza 3×6 no le correspondería | REVISIÓN VISUAL (confirmar que es una apertura) + PROFESIONAL |

| Impacto | REV2 | HIST |
|---|---:|---:|
| Planes | 53 | 76 |
| Único crédito de empuje | 1 | 1 |

**Usuarios K20 expuestos (REV2 D):** U 1, K 1.

**Conclusión:** impacto bajo en la selección. El error visible es el nombre.

## 3. Aislados que cuentan como patrón principal

**Hecho de código:** el catálogo marca **13 ejercicios activos** con `is_isolation=true` y patrón `KNEE_DOMINANT`:
- curl femoral: 327, 637, 915, 916, 1007, 1009, 1010, 1046, 1195, 1196, 1197, 1266;
- extensión de piernas: 1008.

`claseDe` y `MandatorySlot` ignoran `is_isolation`, así que cualquiera de ellos **cubre el patrón de rodilla** (el de sentadilla o zancada), igual que un ejercicio multiarticular.

| Hallazgo | Clase |
|---|---|
| El propio catálogo dice que son aislados y el sistema los usa como patrón principal | **ERROR COMPROBADO** (contradicción entre ficha y uso; previo a REV2) |
| 1195 «self assisted inverse leg curl»: las instrucciones describen llevar los muslos al pecho tumbado boca arriba (flexión de cadera), no un curl femoral | **ERROR COMPROBADO** (nombre e instrucciones no coinciden) |
| Si un aislado puede cubrir el patrón de rodilla (P15) | REVISIÓN PROFESIONAL |

**Impacto: planes cuyo patrón de rodilla lo cubren SOLO aislados**

| | LOSE_WEIGHT | GENERAL_FITNESS | GAIN_MUSCLE | INCREASE_STRENGTH | **Total** |
|---|---:|---:|---:|---:|---:|
| REV2, cualquier dosis | 127 | 89 | 64 | 44 | **324** |
| HIST, cualquier dosis | 54 | 58 | 43 | 26 | **181** |

**Con dosis de fuerza en IS/GF (definición de `12` §4):** REV2 **194**, HIST **208**. La cifra de `12` §4 y `18` P15 se calculó sobre `REV2_final` (`d4556b1`); en `REV2_cierre` sale la misma (194).

**Por ejercicio en REV2 (planes / único crédito de rodilla):** 1195 279/78 · 1196 261/69 · 1266 157/37 · 1008 51/21 · 1007 54/19 · 327 57/16 · 1009 53/16 · 1046 41/15 · 915 43/10 · 916 39/10 · 1010 22/4 · 637 20/2 · 1197 0/0.

**Exposición K20 (REV2 D, pares usuario–ejercicio aislado):** U 7, J 3, K 4, L 4, M 12.

**Conclusión:** contando cualquier dosis, REV2 tiene unos 1,8 veces más planes con la rodilla cubierta solo por aislados (324 frente a 181). La diferencia está sobre todo en LOSE_WEIGHT (127 frente a 54); la causa no se ha aislado. **El oráculo y V24/V25 los aceptan como cobertura válida**, así que la evaluación no los marcará como fallo (ver riesgos, `24`).

## 4. Resto de hallazgos de `12` §4

| Ejercicio | Hallazgo | Clase | Planes REV2 (único del grupo) |
|---|---|---|---|
| 1261 «standing pelvic tilt», 1308 «two toe touch» | Patrón de flexión de tronco, contado como series de **espalda** | **ERROR COMPROBADO** (contradicción); si cuentan, PROFESIONAL | 342 (5) y 370 (13) |
| 1263 «split squats» → «Sentadillas búlgaras» | Una búlgara apoya el pie trasero elevado; el nombre original no lo dice | **ERROR COMPROBADO** (traducción) | 299 (único crédito de rodilla en 83) |
| 853 «half knee bends» → «Media sentadilla» | Clasificado como cardio; según la revisión asistida, es una marcha | REVISIÓN VISUAL | 1.648 (cardio, sin series) |
| 1093 «push to run» | Nombre, instrucciones y GIF no coinciden | REVISIÓN VISUAL | 433 (cardio) |
| 1262, 1115 | Flexiones con gesto añadido en dificultad 1 | REVISIÓN PROFESIONAL | 385 y 325 |
| Dificultad 1 de todo el catálogo | Sale de la regla de palabras clave de V10; solo hay 4 aprobaciones humanas | REVISIÓN PROFESIONAL | — |

## 5. Resumen para decidir

| Prioridad | Ejercicio | Qué compromete | Qué hace falta |
|---|---|---|---|
| 1 | 775 | Dosis de espalda de 615 planes REV2; está en el 38 % de los planes | Visual + profesional para el patrón; corrección de grupo/patrón (migración, con tu aprobación) |
| 2 | 973 | Dosis de hombro de 917 planes REV2 en casa | Profesional; probablemente, otro ejercicio de hombro sin material |
| 3 | Aislados de rodilla (13) | Cobertura del patrón de rodilla: 324 planes REV2 (194 de IS/GF con fuerza) | Profesional (P15); cambia la semántica, requiere aprobación |
| 4 | 1320 | Instrucciones imposibles sin silla (194 planes de casa sin material) | Instrucción o requisito de material; visual |
| 5 | 580, 1263, 973 (nombre) | Nombres mostrados incorrectos | Traducción (sin efecto en la selección) |

**Efecto en la evaluación con IA real:**
- Ninguno de estos errores hace que el oráculo o V8C rechacen un plan.
- Sí pueden hacer que se acepte como **pertinente** una sustitución entre ejercicios mal clasificados, por ejemplo 775 por otro «espalda» que en realidad es de empuje.
- Por eso el protocolo (`23` §5) marca aparte cualquier adaptación que **toque** estos ejercicios y no la cuenta como pertinente sin revisión.
