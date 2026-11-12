# B · Matriz de correcciones

**Estados:**

| Estado | Significado |
|---|---|
| **IMPLEMENTADA** | En V46 (rama aislada; sin merge ni despliegue) |
| **PROPUESTA** | Lista para aplicar si la apruebas |
| **PENDIENTE** | Requiere revisión visual humana o validación profesional antes de proponer un valor |

**Columnas:** «Planes» = planes válidos con el ejercicio, en REV2 / HIST (barridos del cierre). «Impacto» remite a `C`.

## 1. Cambios que solo afectan a lo que se muestra (no a la selección)

| Id | Atributo | Actual | Cambio | Evidencia | Planes | Clase | Aprobación | Riesgo | Estado |
|---|---|---|---|---|---|---|---|---|---|
| 973 | `name_es` | Giro izquierdo. boxeo | **Gancho de izquierda (boxeo)** | Nombre original «left hook», instrucciones («punching motion») y GIF | 976 / 560 | CORRECCIÓN OBJETIVA | No | Nulo en la selección (los barridos lo comprueban); cambia el texto del prompt de la IA | **IMPLEMENTADA (V46)** |
| 1263 | `name_es` | Sentadillas búlgaras | **Sentadilla dividida (zancada estática)** | Nombre original «split squats», instrucciones (pie trasero en el suelo) y GIF | 299 / 477 | CORRECCIÓN OBJETIVA | No | Igual | **IMPLEMENTADA (V46)** |
| 580 | `name_es` | Curl inclinado con mancuerna | «Apertura inclinada con mancuernas» **o** «Press inclinado con mancuernas» | «Curl» es erróneo con cualquier lectura. Las instrucciones dicen press y el GIF, apertura | 53 / 76 | ERROR COMPROBADO; el valor depende de la revisión visual | Revisión visual | Nulo | PENDIENTE |
| 1320 | `instructions_en/es` | «Sit on the edge of a chair or bench…» | Fondos en el suelo: sentado en el suelo, manos detrás de las caderas y dedos hacia delante; elevar la cadera extendiendo los codos y bajar flexionándolos | Nombre «floor» y GIF; equipamiento de peso corporal | 359 / 401 | ERROR COMPROBADO; **texto nuevo** | Revisión del texto | Nulo en la selección | PROPUESTA (no implementada: sería redactar instrucciones) |
| 853 | `name_es` e instrucciones | Media sentadilla / sentadilla | Según lo que se decida: «Marcha con elevación de rodillas» si manda el GIF, o volver a sentadilla (patrón y prescripción) si mandan el nombre y el texto | Contradicción de 2 frente a 2 fuentes | 1.648 / 852 | ERROR COMPROBADO; REVISIÓN VISUAL | Revisión visual | Si se reclasifica como sentadilla, **cambia la selección** | PENDIENTE |
| 1093 | `name_es` e instrucciones | Empujar para correr; flexión con impulso | Según el GIF (elevaciones de rodilla de pie) | Instrucciones frente al GIF | 433 / 856 | ERROR COMPROBADO; REVISIÓN VISUAL | Revisión visual | Nulo (cardio) | PENDIENTE |
| 1195, 1196, 1197, 915, 916, 327 | Instrucciones | Describen otros movimientos (§4 de `A`) | Instrucciones del curl nórdico (anclaje, descenso excéntrico controlado, apoyo con las manos) | Nombre «inverse leg curl» y GIF | 650 / 1.040 planes con alguna | ERROR COMPROBADO; texto nuevo | Revisión del texto y profesional | Nulo en la selección | PROPUESTA (va con la decisión de dificultad, §2) |

## 2. Cambios que alteran la selección (todos requieren tu aprobación)

| Id | Atributo | Actual | Propuesta | Evidencia | Planes | Impacto medido (`C`) | Clase | Riesgo de regresión | Estado |
|---|---|---|---|---|---|---|---|---|---|
| **1195, 1196, 1197, 327, 915, 916** (curl nórdico) | `difficulty_level` | 1 | **Dificultad alta (3)**, o retirada para principiantes | Nombre, GIF; excéntrico exigente | 650 / 1.040 (289 / 468 de principiante) | No medido. Con el límite de dificultad del nivel saldrían de los planes de principiante | VALIDACIÓN PROFESIONAL | Medio: menos opciones de isquios en casa | PENDIENTE |
| 1195, 1196 | Requisitos | Peso corporal | Anclaje (banco o equivalente) | GIF; 1196 pide máquina en sus instrucciones | 279 y 261 | No medido | REVISIÓN VISUAL y PROFESIONAL | Medio en casa sin material | PENDIENTE |
| **775** | `movement_pattern` (y dosis) | `HORIZONTAL_PUSH`, dosis de espalda | Sin dosis resistida (movilidad o extensión de tronco sin dosis de espalda), o patrón y prescripción según la revisión | Grupo de espalda con patrón de empuje; instrucciones frente a GIF | 1.441 / 1.589 | `C` §2: **CF1** | ERROR COMPROBADO; VALIDACIÓN PROFESIONAL | Ver `C` | PENDIENTE |
| **973** | `movement_pattern` o `body_part` | Rotación de tronco contada como hombro | Sin dosis de hombro (tronco o movilidad) | Patrón de tronco con grupo de hombro | 976 / 560 | `C` §2: **CF1** | ERROR COMPROBADO; VALIDACIÓN PROFESIONAL | Ver `C` | PENDIENTE |
| 1261, 1308, 1259, 1146 | `movement_pattern` | Flexión o rotación de tronco, o empuje, contadas como espalda | Sin dosis de espalda | Patrón frente a grupo (S6/S8) | 342 / 370 / 225 / 89 en REV2 | `C` §2: **CF1b** | ERROR COMPROBADO; VALIDACIÓN PROFESIONAL | Ver `C` | PENDIENTE |
| 13 aislados `KNEE_DOMINANT` | Cobertura del patrón de rodilla (P15) | Acreditan el patrón | Alternativas A/B/C (`E`) | `is_isolation=true` en el propio catálogo | 324 planes REV2 con la rodilla cubierta solo por aislados | `C` §3: **CF2** (P15-B) | VALIDACIÓN PROFESIONAL | Ver `C` | PENDIENTE |
| 580 | Patrón, aislado y dosis | `HORIZONTAL_PUSH`, no aislado, fuerza 3×6 | Si es apertura: `HORIZONTAL_ADDUCTION`, aislado, sin fuerza | GIF (asistido) | 53 / 76 | Pequeño | REVISIÓN VISUAL y PROFESIONAL | Bajo | PENDIENTE |
| 1262, 1115 | `difficulty_level` | 1 | 2 | Flexión con gesto añadido | 385 / 472; 325 / 470 | No medido | VALIDACIÓN PROFESIONAL | Bajo | PENDIENTE |
| Regla de dosis por `body_part` | Código (`CalidadDosis`) | Cualquier patrón resistido suma al grupo | Que solo sumen los patrones coherentes con el grupo | Causa común de S6/S7/S8 | Sistémica | CF1b es una aproximación | VALIDACIÓN PROFESIONAL (es una regla) | Alto: cambia la clasificación de dosis de muchos planes | PENDIENTE |
| Asiento no modelado (S1) | Modelo de equipamiento | Los ejercicios «sentado» no exigen asiento | Declarar «silla o banco» o asumir que hay una silla en casa | 78 ejercicios usados | — | No medido | Decisión de producto | — | PENDIENTE |

## 3. Lo que NO se ha hecho, a propósito

- No se ha desactivado ningún ejercicio ni se han sustituido unos por otros.
- No se ha tocado ninguna dificultad, patrón, grupo, músculo, equipamiento ni prescripción.
- No se han redactado instrucciones nuevas en la base de datos.
- No se ha cambiado la política de adaptación, V24, V8C, HIST ni REV2.
