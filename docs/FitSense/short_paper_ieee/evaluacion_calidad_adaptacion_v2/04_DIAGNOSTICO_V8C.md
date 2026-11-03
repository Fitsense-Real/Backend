# Diagnóstico de V8C en las sustituciones

**Código:** `ConservationValidator.revisarSustitucion` (sin cambios). Resto de validadores: `PlanDraftValidator` (V1–V25).

**Pruebas:**
- `V8CDiagnosticoSustitucionesTest`: 11 pruebas, todas aprobadas;
- la integración sin red `evidencia/GUION_COMBINADOS_2S` (`07`).

**V8C no se ha modificado.**

## 1. Qué comprueba exactamente (sustitución fuera de dolor)

| Pregunta de la orden | Respuesta | Evidencia |
|---|---|---|
| ¿Cómo relaciona retirados y añadidos? | Con dos conjuntos: `quitados` y `anadidos`, la diferencia entre la sesión medida y la propuesta. **No empareja** | Código, líneas ~1000–1025 |
| ¿Verifica cada pareja o el conjunto? | **El conjunto.** Cada añadido debe tener un patrón **contenido en el conjunto** de patrones retirados | Ídem. Prueba 2 |
| ¿Puede aceptar cambios de músculo principal? | **Sí.** No compara `targetMuscle` | Prueba 1: los 7 pares distintos observados (8 sustituciones), con datos reales, se aceptan |
| ¿Controla la dificultad relativa? | **No.** Solo V6 (estricto) limita al máximo de dificultad del perfil. La lista de sustitutos del prompt filtra «dificultad ≤», pero el validador no lo exige | Prueba 4: 300 (dif. 1) → 780 (dif. 2) se acepta |
| ¿Conserva la distribución de patrones? | **No.** Retirar un PUSH y un PULL y añadir dos PUSH pasa V8C | Prueba 2 e integración (U04, §3) |
| ¿Permite varios ejercicios con otro énfasis? | **Sí**, si los patrones están en el conjunto retirado | Prueba 1 e integración (U14) |
| ¿Limita el número de añadidos? | **Solo ante dolor** (no más añadidos que retirados, y el volumen no sube). Fuera del dolor, V8C no lo limita | Prueba 3. En la cadena completa, V24 (máximo de colocaciones) y la duración (V4/V20) actúan después |
| ¿Qué controla V8C y qué otros validadores? | **V8C:** permisos de la intervención, día, dosis que no sube, descanso igual, patrón por conjunto, carga, linaje y conservación de las no señaladas.<br>**Estricto:** elegibilidad (V1), dificultad máxima (V6), pisos (V9), `min_reps` (V16), duración (V4 y V20), dosis por capacidad o 4D (V24), cobertura (V25) | Código y mensajes reales de rechazo (`07`) |

**Control negativo:** sustituir por **otro** patrón (780 → 313) se rechaza (prueba 5). En la integración, U09 i1 sustituyó un HORIZONTAL_PUSH por MOBILITY y lo rechazaron V8C y V16.

## 2. ¿Funciona V8C conforme a su diseño?

**Sí.** Su comentario y su mensaje dicen «la sustitución tiene que ser del mismo patrón de movimiento», y eso es lo que hace, sobre el conjunto.

**Ninguna sustitución real (57) incumple lo que V8C dice comprobar. No hay ningún defecto de implementación demostrado.**

## 3. Comportamiento funcionalmente cuestionable (demostrado; no observado con Replicate)

**Ejemplo reproducible** (`GUION_COMBINADOS_2S`, U04, escenario #12):
- **Sesión S20 (GAIN_MUSCLE, VARIANTE):** se sustituyen 380 (HORIZONTAL_PUSH) → 56 y 389 (**VERTICAL_PULL**) → 72 (**HORIZONTAL_PUSH**). Los dos con la misma dosis 4D.
- **Resultado:** **aceptado por V8C y por V1–V25, y publicado** (APLICADA).
- **Patrones de la sesión:**
  - antes: [HP, **VPull**, Hinge, VPush, Knee, HP, **VPull**];
  - después: [HP, **HP**, Hinge, VPush, Knee, HP, VPull].

  Pasa de 2 tirones verticales a 1, y de 3 a 4 empujes.
- La cobertura 4C semanal se sigue cumpliendo (UPPER_PULL presente en otras sesiones), y por eso V25 no lo detiene.

**Causa:**
- V8C comprueba la pertenencia al conjunto de patrones retirados, no el multiconjunto ni la pareja.
- 72 es HORIZONTAL_PUSH y entre los retirados hay un HORIZONTAL_PUSH (380), así que se acepta aunque reemplace a un VERTICAL_PULL.

**Restricción vigente:** «cada añadido, un patrón de entre los retirados».

**Riesgo:**
- Una sesión puede perder un patrón (por ejemplo, el único tirón) dentro de una adaptación de «variante más sencilla».
- En las 57 sustituciones reales no ocurrió: la IA reemplazó siempre ejercicio por ejercicio del mismo patrón (mínimo de cambios de patrón = 0).
- El riesgo es de **diseño de la regla**, no observado en la práctica.

**Opciones de corrección (pendientes de tu autorización; no aplicadas):**

| Opción | Qué cambia | Compatibilidad | Efecto en los 4 objetivos |
|---|---|---|---|
| A. Multiconjunto de patrones | Exigir que la cuenta de cada patrón añadido no supere la de ese patrón retirado | Compatible con generación (V8C no se usa en generación) y con adaptación. Las 57 reales pasarían igual | Igual en todos: solo impide cambiar la distribución |
| B. Emparejamiento uno a uno | Cada añadido empareja con un retirado del mismo patrón (equivale a A con conteos) | Ídem | Ídem |
| C. Además, músculo principal | Exigir el mismo `targetMuscle` en la pareja | **Rechazaría 8 de las 57 reales**, y en ejercicios de catálogo discutible (580, 775, 780) bloquearía variantes. Requiere revisar antes la clasificación (P4 de PR #28) | En GM y FU las listas de sustitutos se reducirían. No recomendable como regla absoluta sin medirlo |
| D. Dificultad ≤ por pareja | Exigir que el sustituto no supere la dificultad del que reemplaza | Compatible. Las 57 reales pasarían (ninguna sube) | Ninguno en lo observado |

**Recomendación del diagnóstico:** si se decide endurecer V8C, A (o B) y D no rechazan ninguna sustitución real y cierran los huecos demostrados. C necesita primero revisar el catálogo y la valoración de un profesional.

**Cualquier cambio de V8C requiere tu autorización.**

## 4. La factibilidad V2 y estas limitaciones

V2 lista los sustitutos «que admiten la misma dosis» a partir de la misma lista que V1 (mismo patrón, dificultad ≤, elegibles). **No filtra por músculo ni avisa del énfasis**, y lo dice («el validador final sigue decidiendo»).

No promete conservar el músculo. Por tanto, su descripción no es incorrecta, pero **no ayuda a evitar las Q2/Q3**. Mejorarlo (filtrar u ordenar los sustitutos por músculo) sería un cambio del prompt: queda como propuesta en `08` y no se aplica durante el diagnóstico.
