# 15 · Recomendación final y respuestas

## Recomendación

1. **Si se incorpora el límite, hacerlo con la estrategia C** (`preferencia`). Da las mismas garantías y el mismo coste que B (0 subidas; idénticos descartes, casos sin sustituto, REPLAY y laboratorio de 22 perfiles), y elige la variante de **igual** dificultad cuando existe: 18.758 frente a 15.881.
2. **No activarlo todavía por defecto.**
   - No se ha encontrado ningún problema de seguridad: con el límite nunca se publica nada inseguro ni se reintroduce lo excluido.
   - Pero el laboratorio de extremo a extremo demuestra un **coste funcional que tienes que aceptar o resolver antes**. En los 21 perfiles reproducidos donde el 135 duele en una sesión PUSH, en casa con mancuernas y bandas:
     - **14** reciben la semana regenerada por P-03;
     - **7** (6 de GENERAL_FITNESS) se quedan **una semana sin plan**.

     Con A todos reciben un sustituto de dificultad 2 o 3.
3. **Hay que decidir R-3** (14) antes de activar. Opciones:
   - **(a)** aceptar la semana sin plan en esos casos y activar C;
   - **(b)** autorizar como cambio **separado** la salida intermedia «retirar solo ese ejercicio y conservar el resto, sin añadir trabajo». Esta validación demuestra que hace falta para 7 de 21 perfiles; no se ha implementado porque la orden lo prohíbe sin aprobación;
   - **(c)** revisar con una persona profesional el hueco de catálogo del 135 (R-4), sin tocar el catálogo aquí.

   Mi recomendación es **(b) + C**: activar el límite cuando exista esa salida, o **(a) + C** si se prefiere no añadir mecanismos y se acepta la semana sin plan, que es segura y se recupera la semana siguiente.
4. El PR #38 sigue en **borrador** y con `off` por defecto. No se fusiona.

## Las 30 preguntas

| # | Pregunta | Respuesta | Dónde |
|---|---|---|---|
| 1 | ¿Qué pasó exactamente en las 24 colocaciones del ejercicio 135? | Es el 135 (dif 1, shoulders, HORIZONTAL_PULL, slot UPPER_PULL) en sesiones PUSH de casa con mancuernas y bandas, con 6 o 7 días.<br>• **Con A:** SUST-SEG-1 elige 780/781 (dif 2) para intermedios y 663 (dif 3) para avanzados.<br>• **Con B y C:** no hay candidato. Los 7 de dificultad 1 son de back, una zona que PUSH no admite, y los de shoulders son de dificultad 2-3. | 02, 04 §3 |
| 2 | ¿A qué objetivos y niveles corresponden los 22 perfiles? | LW 5, GM 5, FU 7 y GF 7 colocaciones; 13 intermedias y 11 avanzadas; ningún principiante. | 02 |
| 3 | ¿Qué hace actualmente el backend si SUST-SEG-1 no encuentra sustituto? | Declara el problema; el respaldo da NO_FACTIBLE (`SIN_SUSTITUTO_SEGURO`); P-03 nivel 2 regenera por reglas sin lo excluido; si no puede (trabajo superior o excluido que reaparece), deja NO_FACTIBLE con motivo. En un plan ya publicado, la sesión queda bloqueada (`SIN_SUSTITUTO`). | 03 §1 |
| 4 | ¿Puede mantenerse el ejercicio que provocó dolor? | **No.** Hay cuatro capas independientes: el elegible (P-02), V1, V8C y P-03 (`EXCLUIDO_REAPARECE`). Comprobado con A, B y C. | 03 §3, 07 F |
| 5 | ¿Existe una salida segura ya implementada? | **Sí:** NO_FACTIBLE y P-03 nivel 2. No existe una salida intermedia («conservar el resto de la sesión»). | 03 §4 |
| 6 | ¿Es necesario modificar el respaldo? | **No por seguridad.** Por función, una salida intermedia evitaría 7 semanas sin plan en 21 perfiles; queda como decisión (R-3) y no se implementa. | 08, 14 |
| 7 | ¿Cuántos casos se resuelven mediante otro criterio de selección? | **0 de 24.** El orden no crea candidatos. | 04 §3 |
| 8 | ¿Cuántos continúan sin sustituto? | **24/24** con B y C. De extremo a extremo: 14 perfiles con regeneración, 7 con semana sin plan y 1 sin plan inicial válido (R0548, independiente del límite). | 08 |
| 9 | ¿Qué ocurre al ordenar por dificultad? | C elige igual antes que menor: 2.877 colocaciones pasan de menor a igual; los sustitutos menores bajan de 3.266 a 389. No hay subidas, igual que B. | 04 §2 |
| 10 | ¿Ese orden cambia otras prioridades de seguridad? | **No.** Va detrás de zona, patrón y músculo, y solo entre candidatos que cumplen todas las obligaciones. B y C dan idénticos recuentos de zona, patrón y músculo. | 04 §1-2 |
| 11 | ¿La nueva protección mantiene H1? | **Sí.** Multiconjunto por defecto; `h1SigueActuando` rechaza por H1 con las tres estrategias. | 06 §2 |
| 12 | ¿Se impide cualquier aumento de dificultad registrada en VARIANTE? | **Sí**, con B y C, por emparejamiento dentro de cada patrón (escenarios 5, 6, 8, 9 y 10). La igualdad sigue permitida. | 06 |
| 13 | ¿Se impide cualquier aumento en sustituciones por dolor? | **Sí**, en la IA (V8C) y en SUST-SEG-1. | 07 C, G; 09 |
| 14 | ¿Se impide reintroducir ejercicios excluidos? | **Sí**, ya con A; el límite no lo cambia. | 03 §3 |
| 15 | ¿La IA puede proponer un cambio inválido que termine publicado? | Con B y C, no en lo que se valida: dificultad, patrón, dosis, V20, V24, 4D y exclusiones. N08 y TD-11 a TD-14: 3 rechazos y conservación o respaldo. | 06, 09 |
| 16 | ¿SUST-SEG-1 se comporta de forma determinista? | **Sí.** Orden total con el id como último criterio: 100 órdenes de inserción dan un único resultado (`casoI`); el barrido reproduce A y B con 0 diferencias. | 07 I, 04 |
| 17 | ¿El caso N08 queda correctamente protegido? | **Sí:** 124 (dif 2) rechazado en los 3 intentos, y respaldo con 1003 (dif 1), con la misma dosis. | 09 |
| 18 | ¿Cómo cambian las semanas posteriores de N08? | Las 8 semanas siguientes llevan 1003 en lugar de 124; el resto, idéntico; linaje intacto; métricas iguales. | 09 |
| 19 | ¿Cuántas de las 123 sustituciones históricas siguen válidas? | **123/123** (59/59 sesiones) con B y con C. | 12 §1 |
| 20 | ¿Qué cambia en la REPLAY? | Solo N08 (usuario 9): semana 2 y sus 8 semanas siguientes. on y preferencia son idénticos. | 09 |
| 21 | ¿La generación inicial sigue idéntica? | **Sí:** hashes de los 960 perfiles idénticos con off, on y preferencia. | 11 §2 |
| 22 | ¿V20 y V24 permanecen intactos? | **Sí**, sin cambios de código; TD-13 y TD-14 muestran que siguen bloqueando con el límite. | 06, 11 |
| 23 | ¿Los cuatro objetivos continúan funcionando? | **Sí.** GF concentra el coste restrictivo (6 de los 7 perfiles con semana sin plan). | 11 |
| 24 | ¿Hay regresiones inesperadas? | **No.** La única fallida con on y preferencia es la prueba del valor por defecto, que es esperada. Con off, 0 fallos. | 12 |
| 25 | ¿Cuál es el impacto real sobre `SIN_EFECTO`, `RESPALDO` y `NO_FACTIBLE`? | • `SIN_EFECTO` como estado V32: 0 cambios (la sesión 62 de N08 sí pasa a figurar en `no_effect_sessions` dentro de un RESPALDO, R-7).<br>• `RESPALDO`: +1 en la REPLAY (N08 pasa de AI a RESPALDO); en el laboratorio de 22 perfiles, 21 → 0.<br>• `NO_FACTIBLE`: 0 → 21 en el laboratorio (14 resueltos por P-03 en el mismo ciclo, 7 sin plan esa semana).<br>En el barrido son exposiciones simuladas, no eventos. | 08, 09 |
| 26 | ¿Cuál de las estrategias A, B o C recomiendas? | **C.** | §Recomendación |
| 27 | ¿Conviene activar el límite de dificultad por defecto? | **Todavía no:** primero hay que decidir R-3 (semana sin plan en 7 de 21 perfiles). Después, sí, con C. | §Recomendación |
| 28 | ¿Qué casos requieren criterio profesional? | El 135 y sus alternativas (¿un remo vertical de dif 2 es un sustituto razonable para un remo de deltoides traseros con banda en una sesión PUSH con dolor?); si la «dificultad registrada» es el criterio adecuado ante dolor; si regenerar la semana completa es aceptable frente a retirar solo el ejercicio. | 14 |
| 29 | ¿Cuál sería el mecanismo de reversión? | `FITSENSE_DIFFICULTY_GUARD=off` (o `fitsense.adaptation.difficulty-guard=off`) y reinicio. Con off el comportamiento es idéntico al de `generation`, verificado por los hashes de los 960 perfiles, la REPLAY y 0 diferencias en el barrido. Sin migraciones que deshacer. | 13 |
| 30 | ¿Qué bloqueos quedan antes de incorporar el PR #38 al MVP? | 1. Tu revisión y la decisión sobre R-3.<br>2. Elegir el valor (`preferencia`) y cuándo activarlo.<br>3. Si se elige (b), la salida intermedia como PR separado, con sus pruebas.<br>4. Opcional: persistir los motivos de los rechazos de la IA (R-5).<br>5. Fuera de alcance: R0548, plan inicial no válido por V20 (R-9).<br>No hay bloqueos técnicos: CI está en verde y no hay migraciones. | 13, 14 |
