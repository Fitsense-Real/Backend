# Contenciones: reevaluación definitiva con la evidencia del cierre

Criterio: una contención sale del código solo cuando un dato auditado la hace innecesaria **y** su salida no relaja nada que dependa de una validación profesional. Ninguna sale porque haga falta para generar un plan.

## 1. INC-CAT-C39, PR #55: 8 ids

| Id | Restricción | Evidencia del cierre | Decisión |
|---|---|---|---|
| 1153 | Colgarse | R2 y V3 V07: apoyo sobre las anillas con los brazos extendidos (ALTA) | **Se mantiene**. V53 marca el dato; si se aprueba V53, la entrada es redundante y puede retirarse en una PR posterior |
| 1301 | Colgarse | R2 y V3 V08: pies en correas, PIES_SUSPENDIDOS (ALTA) | Igual que 1153 |
| 1303 | Colgarse | R2 y V3 V09: silla de capitán, ANTEBRAZOS (ALTA) | Igual que 1153 |
| 1304 | Colgarse | R2 y V3 W07: ANTEBRAZOS (ALTA) | Igual que 1153 |
| 1306 | Colgarse | R2 y V3 W07: ANTEBRAZOS (ALTA) | Igual que 1153 |
| 131 | Brazos sobre la cabeza | R2 (ALTA): brazo a la altura del hombro, nunca por encima. Banderas correctas | **Se mantiene**: la contención se creó por un límite de la definición pendiente de criterio profesional, y la IA no puede concederse esa validación |
| 400 | Brazos sobre la cabeza | R2 (ALTA): igual que 131 | **Se mantiene** |
| 448 | Brazos sobre la cabeza | R2 (ALTA): igual que 131 | **Se mantiene** |

Con V53 aprobada, un perfil «sin colgarse» excluye los 5 primeros por dos vías equivalentes (dato y registro). Sin V53, solo los excluye el registro: por eso no se retiran todavía.

## 2. INC-4C-03 (`ExternalCatalogService.RETENIDOS_INC_4C_03`): 7 ids

Criterio de salida escrito en el código: «revisión controlada de nivel, requisitos y unidad».

| Id | Estado tras V48–V52 | Decisión |
|---|---|---|
| 813 Bandera | Dificultad 3. Requisitos: barra vertical (V50). DURATION (V52). Texto según el GIF (V52) | Criterio cumplido. **Propuesta de liberación**, no aplicada |
| 858 Palanca frontal | Dificultad 3, barra fija, DURATION (V52), texto V52 | Igual que 813 |
| 861 Plancha completa | Dificultad 3, peso corporal, DURATION (V52), texto V52. Apoyo en el suelo, documentado y no marcado | Igual que 813 |
| 873 Remo en palanca frontal | Dificultad 3, barra fija, SETS_REPS (correcto: repeticiones dinámicas), texto V52 | Igual que 813 |
| 885 Parada de manos | Dificultad 3, INV, FL, OH, DURATION (V52), texto V52 | Igual que 813 |
| 1192 Skin the cat | Identidad **AMBIGUA**: el GIF solo muestra un colgado invertido estático | **Se mantiene retenido** |
| 1298 Maltés en apertura | Identidad **AMBIGUA** | **Se mantiene retenido** |

Liberar los cinco primeros amplía el conjunto elegible de los perfiles avanzados. Es un cambio de comportamiento: se deja para tu aprobación expresa y no se aplica.

## 3. INC-CAT-1374

El dato ya está corregido:
- V37: colgarse, brazos arriba y barra.
- V49: nombre e instrucciones.
- V52: dificultad 1 → 2.

La contención exige barra y que el perfil no restrinja colgarse ni brazos arriba, lo mismo que ya imponen sus banderas, y el ejercicio no es de alto impacto. **Es redundante e inocua.** Se mantiene sin cambios; retirarla es un cambio de código sin efecto que puede hacerse cuando se apruebe el cierre.

## 4. Resumen

| Contención | Antes | Después del cierre | Cambio de código |
|---|---|---|---|
| INC-CAT-C39 | 8 | 8 (5 redundantes si se aprueba V53) | Ninguno |
| INC-4C-03 | 7 | 7 (5 con propuesta de liberación) | Ninguno |
| INC-CAT-1374 | 1 | 1 (redundante) | Ninguno |
