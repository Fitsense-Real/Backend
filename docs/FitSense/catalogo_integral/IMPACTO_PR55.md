# Impacto de las correcciones sobre la PR #55 (contención INC-CAT-C39)

La PR #55 contiene preventivamente 26 ejercicios para los perfiles que declaran «sin colgarse», «sin brazos por encima de la cabeza» o «sin suelo». El motivo es que sus nombres o instrucciones pedían esa acción y sus banderas, relajadas en V37/V38 a partir del GIF, no la reflejaban. La auditoría integral revisó los 26 GIF:
- En **los 26** el GIF y la bandera coinciden. Lo que estaba mal era el texto de la fuente.
- V49 reescribe ese texto según el GIF.

## Clasificación de los 26

| ID | Contención en la PR #55 | Qué muestra el GIF | Texto tras V49 | Resultado | Acción recomendada |
|---|---|---|---|---|---|
| 316 | colgarse + cabeza | Remo invertido en barra baja, talones en el suelo | «Remo invertido con agarre estrecho en barra baja» | Datos corregidos, seguridad demostrada | Retirar de la contención |
| 1152 | colgarse | Extensión de brazos boca abajo en el suelo (cobra); `requires_floor` ya marcada | «Press a cobra desde el suelo» | Datos corregidos | Retirar |
| 1272 | cabeza | Tirón de arrancada hasta la cadera, sin recepción por encima de la cabeza | Instrucciones sin recepción | Datos corregidos | Retirar |
| 1309 | cabeza | Brazos al frente a la altura de los hombros | Instrucciones corregidas | Datos corregidos | Retirar |
| 1322 | cabeza | Estiramiento con el brazo cruzado delante del pecho | Instrucciones corregidas; nombre «de tríceps» en duda | Datos corregidos (nombre pendiente visual) | Retirar de la contención por cabeza |
| 55, 224, 637, 741, 875, 1062, 1063, 1093, 1095, 1156, 1181, 1193, 1311 | suelo | De pie, en banco, en banco declinado, contra la pared o en taburete | Instrucciones sin suelo | Datos corregidos | Retirar |
| 1303, 1304 | colgarse + suelo | Silla romana, con apoyo de antebrazos y los pies sin apoyo | Instrucciones de silla romana | Suelo: corregido. Colgarse: apoyo suspendido (clase B) | Retirar la parte de suelo y **mantener colgarse** hasta la decisión profesional |
| 1306 | colgarse | Silla romana, con elevación de rodillas | Instrucciones de silla romana | Apoyo suspendido (clase B) | **Mantener** |
| 1153 | colgarse | Fondos en anillas, con apoyo sobre los brazos extendidos | «Apoyo sobre las anillas…» | Apoyo suspendido (clase B) | **Mantener** |
| 1301 | colgarse + cabeza | Pies en correas y plancha sobre las manos | Instrucciones de plancha con pies en correas | Cabeza: corregido. Colgarse: pies suspendidos (clase C) | Retirar cabeza y **mantener colgarse** |
| 131, 400, 448 | cabeza | Curl «alto»: brazo abierto a la altura del hombro y manos hacia la cabeza | Instrucciones corregidas | La posición del brazo es límite con la definición de «por encima de la cabeza» | **Mantener** hasta la revisión profesional |

Balance:
- **18 de 26 se recuperan por completo** y 2 más (1301, 1303/1304) recuperan parte de su contención.
- Quedan **8 contenidos**: 1153, 1301, 1303, 1304 y 1306 por colgarse; 131, 400 y 448 por cabeza. Su estado es PENDIENTE_PROFESIONAL (ver `PROPUESTA_APOYO_SUSPENDIDO.md`).

## Prueba de elegibilidad tras las correcciones

Se usó una rama experimental local (`exp/c39-sobre-catalogo-corregido`, no publicada). Contiene la rama de esta auditoría más los dos commits de la PR #55, y se ejecutó la misma rejilla de la PR (`ContencionC39ComparacionIT`: 120 perfiles, misma semilla).

| Variante | LOSE_WEIGHT / principiante / casa / sin suelo | LOSE_WEIGHT / principiante / casa / sin las tres | Violaciones (texto o banderas) |
|---|---|---|---|
| PR #55 tal cual (26 contenidos) sobre el catálogo corregido | NO_FACTIBLE | NO_FACTIBLE | 0 |
| Contención reducida a 8 sobre el catálogo corregido | **PLAN** (valida V25 con 1093, 853 y 1117) | **PLAN** | **0** |

Evidencia: `evidencia/c39_26_corregido.csv` y `evidencia/c39_8_corregido.csv`.
- Ningún plan de la variante reducida contiene un ejercicio contenido ni uno con una bandera que el perfil excluya.
- Entre las dos variantes cambia el conjunto elegible de 87 perfiles, solo porque se devuelven los ejercicios recuperados.
- Los dos perfiles recuperan su plan sin relajar V25 ni ninguna bandera.

## Qué hacer con la PR #55

1. **No fusionarla tal cual después de las correcciones.** Tras V49 contiene 18 ejercicios cuyo único defecto era el texto. Seguiría bloqueando sin motivo y dejaría los dos perfiles en NO_FACTIBLE.
2. **Opción recomendada:** fusionar primero las PR del catálogo integral (1 → 2 → 3 → 4). Después, actualizar la PR #55:
   - reducir el registro a los 8 restantes, con su motivo actualizado (apoyo suspendido, curl alto);
   - adaptar sus pruebas, que hoy verifican los 26.
   El mecanismo de la PR (causa INC-CAT-C39, bloqueo de sesiones publicadas, SUST-SEG-1) sigue siendo útil para esos 8 y para cualquier contención futura.
3. **Alternativa:** si la decisión profesional sobre apoyo suspendido es la opción 2 de la propuesta, los cinco de colgarse pasan a cubrirse con `requires_hanging` y la PR #55 se queda solo con 131, 400 y 448, o con ninguno si se aclara la definición de «por encima de la cabeza».

Esta auditoría no modifica la PR #55.
