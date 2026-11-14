# Cierre de pendientes, seguridad y validación final del catálogo (orden de 14/11/2026)

Este documento recoge **qué se ha resuelto de verdad** en esta orden y **qué sigue necesitando a un profesional**, sin contar como resuelto lo que solo está documentado o contenido.

**Vocabulario de estados.** Cada decisión termina en uno de los cuatro estados de la orden:

| Estado | Significado |
|---|---|
| **Corregido y validado técnicamente** | Hay una corrección demostrada contra la animación completa del GIF, con migración y pruebas |
| **Correcto, con evidencia que descarta el hallazgo** | El valor vigente ya era el correcto |
| **Contenido preventivamente** | El riesgo está cerrado por una contención proporcional, pero la decisión de fondo sigue siendo profesional. **Contener no es validar** |
| **Bloqueado por validación especializada** | Sin cambio; solo un profesional (o el propietario) puede decidir |

Ninguna decisión tiene **validación profesional registrada**. Ninguna se marca como VERIFICADA.

Fuentes:
- `cierre/REGISTRO_DECISIONES_PENDIENTES.tsv`, generado por `herramientas/registro_decisiones.py`. Tiene una fila por decisión, con estas columnas: `estado_anterior`, `correccion`, `migracion`, `pruebas`, `estado_final`, `revisor` y `validacion_profesional`.
- `cierre/pendientes/EXPEDIENTE_SEGURIDAD.tsv`, generado por `herramientas/expediente_seguridad.py`.
- `cierre/pendientes/FICHAS_PROFESIONALES.xlsx`, generado por `herramientas/fichas_profesionales.py`.

## 1. Cifras de partida verificadas

| Cifra de la orden | Verificación |
|---|---|
| 1.219 activos / 1.376 registros | `huella_catalogo.tsv`: 1.219 filas. `CatalogoCalidadIT`: 1.376 registros |
| 442 ejercicios modificados por V52 | `cierre/adjudicacion/decisiones_v52.tsv` (procedencia V1–V52.1 en `auditoria_final/`) |
| 323 filas: 309 profesional, 6 propietario y profesional, 6 técnicas | Registro antes de esta orden. Además había 1 fila resuelta (675) y 1 consecuencia documentada (775) |
| 43 decisiones de seguridad prioritarias | 43 filas de prioridad 1, sobre **34 ejercicios distintos**: 1177, 930, 922, 1294, 1298 y 1192 aparecen más de una vez, por seguridad y por identidad |
| HALL-PROG-01 en #62; #60 y #61 en verde; V53 apagado; nada fusionado | Confirmado. Ver `INTEGRACION_SIN_DUPLICAR.md` |

## 2. Los seis hallazgos técnicos

Para cada uno se revisó la **animación completa** (hoja de fotogramas f1–fN de `herramientas/hojas_completas.py`). Se comparó con el nombre, las instrucciones, el equipo, la clasificación y las restricciones. Las correcciones están en **V52.1** (`V52_1__catalogo_cierre_pendientes_tecnicos.sql`). Cada bloque de esa migración exige el estado anterior exacto y es idempotente: si no encuentra lo esperado, falla.

| Id | Hallazgo | Evidencia del GIF | Resultado | Cambio |
|---|---|---|---|---|
| **231** | «Press desde pull-over»: VERTICAL_PUSH, espalda/dorsales | f1: brazos verticales sobre el pecho. f7: barra en el pecho. La barra nunca pasa por encima ni por detrás de la cabeza | **Corregido y validado técnicamente** | Nombre «Press de banca con barra y codos pegados»; brazos/tríceps, sinergista pecho, HORIZONTAL_PUSH, como 169, 201, 246, 851 y 1239. **La bandera OH se conserva**: quitarla relajaría una restricción, así que es decisión profesional |
| **367** | «Patada de tríceps en cable» con el deltoides posterior resaltado | El brazo, casi recto, va de colgar delante del muslo a quedar en línea con el tronco. No hay flexo-extensión de codo | **Corregido y validado técnicamente** | «Extensión de hombro con brazo estirado en polea baja»; hombros/deltoides, SHOULDER_EXTENSION (como 262) |
| **1303** (instrucciones) | Las instrucciones dicen «silla romana» y el aparato es una silla del capitán | «Silla romana» es el nombre habitual en español de la estación de elevaciones con apoyo de antebrazos. El texto coincide con el GIF | **Correcto, con evidencia que descarta el hallazgo** | Ninguno. El ejercicio sigue contenido por INC-CAT-C39 para «no colgarse» y está entre los 15 de V53 |
| **1323** | Las instrucciones mandaban sentarse «con los muslos bajo los rodillos» | Banco plano entre dos torres de polea alta, sin rodillos | **Corregido y validado técnicamente** | Esa frase pasa a «Siéntate en el banco situado entre las dos poleas altas, con los pies apoyados en el suelo», en las instrucciones y en los pasos. **Ya no queda elegible con instrucciones contradictorias** |
| **1182** | Equipo «plyo box / step platform» | Los pies están en el suelo. Lo que parecía un cajón son los pies vistos de frente (V3, con ampliación de f1 y f13) | **Corregido y validado técnicamente** | Peso corporal sin requisitos. FL se conserva |
| **1017** | Los gemelos se resaltan | f1 y f7 resaltan los gemelos | **Corregido y validado técnicamente** | Gemelos añadidos como secundarios |

**Corregidos también en V52.1, con la misma comprobación:**
- **620**: identidad, grupo A. El brazo recto va de colgar hacia el suelo a quedar atrás, a lo largo del cuerpo; no se separa hacia el lado. Pasa a «Extensión de hombro a una mano tumbado boca abajo con mancuerna», SHOULDER_EXTENSION, con instrucciones nuevas.
- **868**: seguridad. En f5–f8 las piernas pasan por encima de la barra y la cadera queda por encima de la cabeza. Se **añade** `requires_inversion`. Solo restringe más: es una contención, y el profesional la confirma.

**Contenido en código: 1294 (Balanceo 360).** El texto describe círculos de brazos de pie; el GIF muestra un balanceo de 360° colgado de una barra. Entra en `RETENIDOS_INC_4C_03`, así que no se prescribe a nadie y bloquea las sesiones publicadas que lo contengan. Pruebas: `ExternalCatalogServiceRetencionTest`, `CatalogoElegibleIT` y `ContencionTextoBanderaIT`.

**Efecto de V52.1:**
- Huella: cambian solo las filas 231, 367, 620, 868, 1182 y 1323.
- Referencia de 72 planes: 72 válidos, 25 planes con contenido distinto (GIMNASIO: GM 8, IS 6, GF 3, LW 2; GM CASA 6), porque 231, 367 y 1182 cambian los conjuntos elegibles. Ninguno pierde el plan. Detalle en `generacion_referencia.txt`.
- No baja ninguna dificultad, no quita ninguna bandera y no activa ni desactiva ejercicios.

## 3. Las 43 decisiones de seguridad

Cada decisión tiene su fila en `pendientes/EXPEDIENTE_SEGURIDAD.tsv`, con estas columnas:
- id, movimiento representado, evidencia visual, nombre e instrucciones, banderas vigentes;
- restricción en riesgo, clasificación propuesta y justificación;
- elegibilidad en la rejilla de 168, necesidad de profesional y estado de contención.

**Resultado:**

| Estado final | Decisiones |
|---|---|
| Contenido preventivamente | **42** |
| Bloqueado por validación especializada, **sin contención** | **1**: A-V53, el significado de «no colgarse» |
| Validadas por un profesional | **0** |

**Las 43 siguen necesitando un profesional.**

**Revisión explícita de los casos de la orden:**

| Caso | Estado vigente | Conclusión |
|---|---|---|
| **868** | HG OH **INV** (V52.1) | Contenido por inversión, colgarse y brazos sobre la cabeza. El profesional confirma INV |
| **1294** | Retenido (INC-4C-03) | Contenido para todos. Texto pendiente de profesional |
| **1113** (V39) | Dificultad 1. R2 y V3 proponen 2 | **Bloqueado.** Decisión histórica: no se modifica sin aprobación. **Incertidumbre material**: elegible para principiantes en 120 de 168 perfiles |
| **1323** | OH ya marcado; texto corregido | Corregido. No necesita contención |
| **854** (D2) | HI conservado | Contenido. D2 no se modifica |
| **1083–1087** | 1083 HI FL KN ALTO; 1084 HI; 1085 HI OH; 1087 HI OH; **1086** OH, impacto BAJO, sin HI | 1083, 1084, 1085 y 1087: contenidos por HI (el vigente es el conservador; quitar HI es profesional). **1086: correcto con evidencia.** R2 ve que se incorpora con el balón por encima de la cabeza sin soltarlo ni atraparlo; no hay vuelo ni choque. OH ya está |
| **1133 / 1134 / 1175** | HI | Contenidos por HI |
| **171 / 377 / 840 / 841** | INV (declinado) | Contenidos por INV para mayores de 65 |
| **INC-4C-03** | 813, 858, 861, 873, 885, 1192, **1294**, 1298 | Retenidos para todos |
| **INC-CAT-1374** | 1374 | Contenido |
| **INC-CAT-C39** | 131, 400 y 448 (OH); 1153, 1301, 1303, 1304 y 1306 («no colgarse») | Contenidos solo para quien tiene la restricción |
| **Apoyo suspendido** | 15 de V53 | 1153, 1303, 1304 y 1306: contenidos por C39. 1069: contenido por HG. **Los otros 10 (457, 458, 459, 462, 777, 920, 934, 1184, 1317 y 1335) siguen elegibles para «no colgarse»**: contenerlos sería activar V53, y la orden lo prohíbe sin autorización |

## 4. Ejercicios elegibles con incertidumbre material

1. **Los 10 de apoyo suspendido sin contención** (§3). Afectan a los perfiles con «no colgarse». Ver `cierre/V53_DEFINITIVO.md`.
2. **1113**, dificultad 1 por V39, cuando R2 y V3 ven nivel 2.
3. **248 dificultades** que R2 propone **bajar**. No es una incertidumbre de seguridad: el nivel vigente es el más alto y llega a menos perfiles. V27 exige una persona para bajar.

## 5. Ejercicio 775

| Pregunta | Respuesta |
|---|---|
| ¿Lleva banco? | **Sí.** En f1–f12 los codos se apoyan sobre almohadillas elevadas. Vista por R2, V3, el coordinador y esta orden |
| Requisitos exactos | `bench` (`1:bench`) |
| ¿Qué capacidad se pierde sin banco? | La única resistencia de espalda (tracción) sin material. El catálogo no tiene ningún otro ejercicio de espalda con resistencia y solo peso corporal; los de peso corporal sin equipo que trabajan la espalda son movilidad o tronco (938, 1066, 1070, 1188, 1190, 1257–1261, 1308–1310) |
| Alternativas legítimas | No hay ninguna sin cambiar de equipo: 910, 911 y 912 (remo invertido) piden barra o Smith, y 914 y 1307 piden correas o anillas. Con un banco declarado, el perfil recupera el 775 |
| ¿Hay una semana válida sin cambiar las políticas? | **No**, en 14 perfiles GAIN_MUSCLE en casa solo con peso corporal: INTERMEDIATE 3 y 4 días con 58, 59, 60, 74 y 75 min; BEGINNER y ADVANCED 3 días con 60 y 75 min. Sin tracción, la sesión llega como mucho a 2.430 s (58–60 min) o a 3.090 s (74–75 min) |
| ¿Qué validador bloquea? | **V20**, el suelo del 70 % de los minutos. Ejemplos: 2.430 s < 2.436 s a 58 min; 2.430 s < 2.520 s a 60 min; 3.090 s < 3.150 s a 75 min. Con 57 min el plan es válido (2.430 ≥ 2.394) |
| Decisión | **NO_FACTIBLE legítimo y documentado.** No se toca V20 ni V25. `Hipertrofia4Dv34IT` exige exactamente esos 14 fallos, todos por V20 |

## 6. Dificultad e identidad

| Grupo | Dificultad | Identidad |
|---|---|---|
| **A, objetivas (resueltas con evidencia)** | **0.** Las 248 son propuestas de bajar un nivel, y V27 prohíbe bajarlo sin aprobación humana. Mantener el nivel más alto es lo conservador, no un relabel | **3**: 620 corregido (V52.1); 64 y 1261 confirmados por V3 y por esta orden, con visto bueno profesional opcional. Ya estaba resuelto 675 (V52) |
| **B, especializadas (ficha profesional)** | **249**: 248 propuestas de bajar + 1113 (V39) | **23** |

Las fichas de `pendientes/FICHAS_PROFESIONALES.xlsx` llevan:
- el GIF y la orden para generar la hoja de fotogramas completa;
- el nombre y las instrucciones vigentes, la clasificación vigente, la alternativa propuesta y la diferencia;
- la evidencia de R2 y V3, el riesgo de mantener el valor vigente y el impacto real (perfiles, rejilla y referencia);
- una pregunta concreta, con campos de respuesta vacíos.

**El número de pendientes no se ha reducido por reetiquetado.** Antes había 315 (309 + 6). Ahora hay 312: solo salen 620, 64 y 1261, con evidencia. Por bloques:
- 249 de dificultad;
- 36 de seguridad (35 contenidas y A-V53 sin contención);
- 23 de identidad;
- 4 de texto.

## 7. Resumen de estados (323 filas)

| Estado final | Filas |
|---|---|
| Corregido y validado técnicamente | 8 (A-231, A-367, A-1323, A-1182, A-1017, 620, 675 y la consecuencia de 775) |
| Correcto, con evidencia que descarta el hallazgo | 3 (A-1303T, 64, 1261) |
| Contenido preventivamente | 44 |
| Bloqueado por validación especializada | 268 |
| **Pendientes de profesional (contenidos + bloqueados)** | **312** |
