# Matriz de 26 frente a 8 ejercicios (INC-CAT-C39)

Datos del catálogo final (V47–V50). La matriz completa, con nombre, equipo y texto, está en `matriz_26_vs_8.tsv`, generada por `herramientas/matriz_26_vs_8.py`.

Restricciones: **COLG** = sin colgarse (`requires_hanging`), **CAB** = sin brazos sobre la cabeza (`requires_overhead`), **SUE** = sin suelo (`requires_floor`).

## Resumen

| Resultado | Ids | Total |
|---|---|---|
| **Se mantiene** | COLG: 1153, 1306 · CAB: 131, 400, 448 | 5 |
| **Se mantiene en parte** | 1301 (COLG; sale CAB), 1303 y 1304 (COLG; sale SUE) | 3 |
| **Liberado por completo** | 316, 1152, 1272, 1309, 1322, 55, 224, 637, 741, 875, 1062, 1063, 1093, 1095, 1156, 1181, 1193, 1311 | 18 |
| **Contenidos tras la reducción** | 1153, 1301, 1303, 1304, 1306, 131, 400, 448 | **8** |

## Matriz

| Id | Nombre (V50) | 26 | 8 | Banderas HG/OH/FL (V50) | ¿Pide el texto lo liberado? | Resultado |
|---|---|---|---|---|---|---|
| 1153 | Fondos en anillas | COLG | COLG | — | — | Se mantiene: apoyo suspendido |
| 1306 | Elevación de piernas vertical en paralelas | COLG | COLG | — | — | Se mantiene: apoyo suspendido |
| 1301 | Crunch invertido suspendido | COLG CAB | COLG | FL | no (CAB) | En parte: pies suspendidos |
| 1303 | Elevación de piernas torsionada | COLG SUE | COLG | — | no (SUE) | En parte: apoyo en antebrazos |
| 1304 | Elevación de piernas torsionada (femenino) | COLG SUE | COLG | — | no (SUE) | En parte: apoyo en antebrazos |
| 131 | Curl de bíceps a una mano con banda y brazo a la altura del hombro | CAB | CAB | — | — | Se mantiene: curl alto |
| 400 | Curl por encima de la cabeza en cable | CAB | CAB | — | — | Se mantiene: curl alto; el nombre aún dice «por encima de la cabeza» |
| 448 | Curl por encima de la cabeza sentado en cable | CAB | CAB | — | — | Se mantiene: curl alto; el nombre aún dice «por encima de la cabeza» |
| 316 | Remo invertido con agarre estrecho en barra baja | COLG CAB | — | FL | no | Liberado |
| 1152 | Press a cobra desde el suelo | COLG | — | FL | no | Liberado |
| 1272 | Tracción snatch | CAB | — | — | no | Liberado |
| 1309 | Estiramiento de espalda alta | CAB | — | — | no | Liberado |
| 1322 | Estiramiento de tríceps | CAB | — | — | no | Liberado (nombre en PENDIENTE_VISUAL) |
| 55 | Círculos de tobillo | SUE | — | — | no | Liberado |
| 224 | Abdominal con press de barra en banco declinado | SUE | — | OH | no | Liberado |
| 637 | Femorales acostado con mancuerna | SUE | — | — | no | Liberado |
| 741 | Elevación lateral de un brazo acostado con mancuerna | SUE | — | — | no | Liberado |
| 875 | Patadas flutter | SUE | — | — | no | Liberado |
| 1062 | Elevación de cadera acostado | SUE | — | — | no | Liberado |
| 1063 | Crunch negativo | SUE | — | — | no | Liberado |
| 1093 | Elevación de rodilla con patada frontal alterna | SUE | — | — | no | Liberado |
| 1095 | Empuje y tracción con peso corporal | SUE | — | — | no | Liberado |
| 1156 | Remo sentado con banda de espalda recta | SUE | — | — | no | Liberado |
| 1181 | Crunch lateral sentado contra pared | SUE | — | — | no | Liberado |
| 1193 | Estiramiento sentado del piriforme | SUE | — | — | no | Liberado |
| 1311 | Abductor de cadera externo con pierna recta | SUE | — | — | no | Liberado |

«¿Pide el texto lo liberado?» usa los mismos patrones que la prueba I de la PR #55. La prueba `ContencionTextoBanderaIT.l_liberadosJustificados` lo comprueba sobre la base migrada. Ninguna liberación queda injustificada.

## Por qué siguen los 8

- **Colgarse (5).** El GIF muestra **apoyo suspendido**: el cuerpo sin apoyo en el suelo, sostenido por brazos extendidos (1153), por antebrazos en silla romana (1303, 1304, 1306) o con los pies en correas (1301).
  - `requires_hanging` no distingue ese apoyo de colgarse.
  - Para quien declaró «no puedo colgarme», que la contraindicación se extienda o no al apoyo suspendido es una **decisión profesional**.
  - La propuesta está en `catalogo_integral/PROPUESTA_APOYO_SUSPENDIDO.md`.
  - Mientras tanto se contiene: no se libera por factibilidad.
- **Brazos sobre la cabeza (3).** Curl «alto»: brazo abierto a la altura del hombro y manos hacia la cabeza.
  - Queda en el límite de la definición de `requires_overhead`.
  - En 400 y 448 el nombre sigue diciendo «por encima de la cabeza»: el texto y la bandera aún se contradicen.

## Efecto sobre la elegibilidad

| Variante sobre el catálogo V50 | Perfiles LOSE_WEIGHT / principiante / casa («sin suelo», «sin las tres») | Prohibidos en planes publicados |
|---|---|---|
| Sin contención (pila #56–#59) | PLAN | 0 en la rejilla de 168: ninguna semana con un ejercicio incompatible según bandera o nivel (`catalogo_integral/IMPACTO_ELEGIBILIDAD.md`) |
| PR #55 original (26) | **NO_FACTIBLE** (los dos) | 0 |
| **Reducida (8)** | **PLAN** (los dos) | 0 |

Detalle en `PERFILES_RECUPERADOS.md` y en la comparación de 120 perfiles (`evidencia/`).
