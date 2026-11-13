# Correcciones de seguridad del catálogo · contención INC-CAT-C39

**Origen.** Auditoría del caso real del usuario 39. El informe está en la rama `audit/caso-usuario-39` (`docs/FitSense/auditoria_caso_39/INFORME_CASO_39.md`, §I.3).

**Rama:** `fix/contencion-seguridad-catalogo`, desde `generation` `623c4fa`.

> **Actualización (13/11/2026): contención reducida de 26 a 8 sobre el catálogo integral corregido.** La rama incorpora las PR #56–#59 (V47–V50) y la contención queda en 8 ejercicios. Ver **§9** y `docs/FitSense/contencion_c39_reducida/`. Las secciones 1–8 describen la versión original de 26 y se conservan como historia.

**Alcance:**
- Solo seguridad: contención y pruebas.
- Sin cambios en PR-1.7, FIX-PR-2.1-EXP, PLAN-REV-2.0, PLAN-HIST-1, V15, V24, V8C, V23, V25–V27, reintentos, progresión, reincorporación, adaptación ni Replicate.
- Sin migraciones de datos, sin llamadas pagadas y sin despliegue.

**Nivel de evidencia:**
- **[C] Comprobado:** en el catálogo, el código o una prueba.
- **[R] Revisión visual documentada:** matriz de `auditoria_catalogo/revision_visual`. No la he vuelto a verificar: los GIF no están accesibles desde este entorno.
- **[P] Pendiente de revisión profesional.**

## 1. El problema

En V37 y V38 se **relajaron** banderas de seguridad (`requires_hanging`, `requires_overhead`, `requires_floor`) por lo que mostraba el GIF («restricción de más»). **No se actualizaron ni el nombre ni las instrucciones**, que siguen describiendo la ejecución restringida [C]. Algunos ejercicios nunca marcados tienen el mismo defecto [C].

La app muestra solo el **nombre**, el **equipo** y la dosis; no muestra el GIF ni las instrucciones (`workout_detail_page.dart`, `session_page.dart`). La IA ve el nombre.

Ejemplo [C]:
- Un usuario declara `allowHangingExercises=false`.
- El 316 «Dominadas con agarre estrecho» es elegible para él, porque V38 le quitó la bandera de colgarse.
- Sus instrucciones dicen «Cuélgate de una barra de dominadas…».

**Magnitud [C]** (comparación antes/después, §5): en una rejilla de 120 perfiles, **44 semanas publicadas por `generation` contenían al menos un ejercicio cuyo texto pide una acción que el perfil había restringido.**

## 2. Qué se hace (y qué no)

**Mecanismo: el existente, ampliado con una contención con nombre**, como INC-4C-03 e INC-CAT-1374:
- `planning/domain/services/ContencionTextoBandera` es el registro. Para cada id guarda las restricciones que lo activan y la contradicción citada.
- `ExternalCatalogService.elegibles` lo retira del **conjunto elegible**. Ese conjunto es la única fuente del motor de reglas y del catálogo que va a Replicate, y también lo usa la consulta silenciosa de escasez.
- `SafetyConflictPolicy` añade la causa con nombre `INC-CAT-C39` a `CAUSAS`, y `SafetyConflictChecker` la aplica. Esto marca y bloquea las sesiones abiertas de **planes ya publicados** y autoriza la sustitución obligatoria (SUST-SEG-1) en la semana derivada, con el mismo camino que INC-CAT-1374.
- Línea de log `EJERCICIOS_CONTENIDOS user=… motivo=INC-CAT-C39 ids=[…]`, como la del 1374.
- **Sin interruptor:** es seguridad, no medición.

**Solo se contiene para quien declara la restricción** que el texto pide incumplir. Para el resto de perfiles, el ejercicio sigue igual: su texto y su GIF describen movimientos que esos perfiles admiten.

**No se hace:**
- No se modifican banderas, nombres, instrucciones, dosis, patrones ni dificultad.
- No se desactivan ni se borran filas.
- No se crea ninguna regla ni restricción nueva: se aplican las restricciones que el perfil **ya declara** a lo que el texto le pide hacer.
- No se corrige ningún texto. Escribir instrucciones nuevas exige describir el movimiento del GIF, y aquí solo hay notas breves de una revisión visual previa. Hacerlo sería inventar. Queda **[P]**, con la contención activa hasta que una migración auditada alinee texto y bandera.

**V8C no cambia [C]:**
- Su código y sus reglas son los mismos.
- `SafetyConflictPolicy` es, por diseño, «la única fuente de las causas que autorizan a quitar un ejercicio ya prescrito… una retención o contención de seguridad con nombre».
- Añadir una contención con nombre es su punto de extensión previsto, igual que se hizo con INC-CAT-1374.
- La firma antigua de `causas(…)`, con cuatro argumentos, devuelve exactamente lo mismo (comprobado en `ContencionTextoBanderaTest`).

## 3. Ejercicios evaluados

Columnas: **Restr.** = restricción que activa la contención (C: colgarse, B: brazos sobre la cabeza, S: suelo). **Estado:** Confirmado = contradicción verificada entre texto y bandera; Pendiente = el movimiento real requiere revisión profesional.

### 3.1 Lista prioritaria de la orden

| ID | Problema | Evidencia | Acción | Estado |
|---|---|---|---|---|
| 316 | Nombre «Dominadas con agarre estrecho» e instrucciones «Cuélgate de una barra de dominadas»; V38 quitó colgarse y brazos arriba | [C] catálogo (V11 nombre, V9 instrucciones, V38 banderas); [R] A02: «remo invertido bajo la barra, talones en el suelo» | **Contener** (C, B) | Confirmado · movimiento [P] |
| 1301 | «Crunch invertido suspendido»: instrucciones «Cuélgate de una barra de dominadas con los brazos completamente extendidos»; V38 quitó colgarse y brazos arriba y pasó el equipo a correas | [C]; [R]: «manos en el suelo, pies en las correas» | **Contener** (C, B) | Confirmado · movimiento [P] |
| 1303 | Equipo «parallel bars» (lo que ve el usuario) e instrucciones «Túmbate sobre tu espalda»; V38 quitó suelo | [C]; [R] B10: «apoyo en la estación de fondos» | **Contener** (C por apoyo suspendido; S por el texto de suelo) | Confirmado · movimiento y apoyo suspendido [P] |
| 1152 | «Fondos invertidos»: instrucciones de fondos en paralelas; catálogo con peso corporal y suelo (V38) | [C]; [R] B06: «tumbado y sentado en el suelo; no usa paralelas» | **Contener** (C: siguiendo el texto, apoyo suspendido en paralelas) | Confirmado · [P] |
| 1309 | «Estiramiento de espalda alta»: «Eleva lentamente los brazos por encima de la cabeza»; V38 quitó brazos arriba | [C]; [R] B02: «brazos a la altura del hombro» | **Contener** (B) | Confirmado · [P] |
| 1322 | «Estiramiento de tríceps»: «Extiende un brazo por encima de la cabeza»; V38 quitó brazos arriba | [C]; [R] B07: «brazo cruzado ante el pecho» | **Contener** (B) | Confirmado · [P] |
| 1306 | «Elevación de piernas vertical en paralelas»: «Cuélgate de las barras paralelas»; nunca marcado como colgarse | [C]; [R]: «silla romana: apoyo en antebrazos» | **Contener** (C) | Confirmado · apoyo suspendido [P] |
| 448 | Nombre «Curl por encima de la cabeza sentado en cable»; brazos arriba = no; instrucciones de curl sentado | [C]; [R]: «brazos a la altura del hombro» | **Contener** (B) | Confirmado (nombre) · [P] |
| 853 | Nombre e instrucciones de media sentadilla; patrón de cardio (V38: marcha con rodillas altas) | [C]; [R]: «marcha con rodillas altas» | **Sin contención**: ambas ejecuciones son de pie y sin restricción declarable. Corrección documental [P] | Confirmado (documental) · sin riesgo para ninguna restricción |
| 1093 | «Empujar para correr»: «Comienza en posición de flexión… baja el pecho hacia el suelo»; V38 quitó suelo | [C]; [R] B02: «de pie elevando rodillas» | **Contener** (S) | Confirmado · [P] |

### 3.2 Misma clase de defecto, encontrada con el barrido sistemático

Banderas relajadas en V37 o V38 con texto que pide la acción restringida, más ejercicios nunca marcados con texto explícito.

| ID | Problema | Evidencia | Acción | Estado |
|---|---|---|---|---|
| 1304 | Gemelo del 1303: «parallel bars» + «Túmbate sobre tu espalda»; V38 quitó suelo | [C]; [R] B10 | **Contener** (C, S) | Confirmado · [P] |
| 1153 | «Fondos en anillas»: «Comienza colgándote de las anillas»; colgarse = no | [C]; [R] D09 sin error (apoyo en anillas) | **Contener** (C) | Confirmado · apoyo suspendido [P] |
| 131 | Nombre e instrucciones «…por encima de la cabeza»; V38 quitó brazos arriba | [C] | **Contener** (B) | Confirmado · [P] |
| 400 | Nombre «Curl por encima de la cabeza en cable»; brazos arriba = no | [C]; [R]: «brazos a la altura del hombro» | **Contener** (B) | Confirmado (nombre) · [P] |
| 1272 | «Tracción snatch»: «Recibe la barra por encima de la cabeza»; brazos arriba = no | [C]; [R]: «brazos no pasan de la cabeza» | **Contener** (B) | Confirmado · [P] |
| 55, 1311 | Instrucciones «Siéntate en el suelo» / «Túmbate de lado»; V38 quitó suelo | [C]; [R]: «de pie» | **Contener** (S) | Confirmado · [P] |
| 224, 637, 741, 875, 1062, 1063 | Instrucciones «Túmbate…» (boca arriba o de lado, en esterilla o con «levanta las piernas del suelo»); V38 quitó suelo | [C]; [R]: «en banco» | **Contener** (S) | Confirmado · [P] |
| 1095 | «Comienza en posición de flexión… baja el pecho hacia el suelo»; V38 quitó suelo | [C]; [R] B03: «de pie» | **Contener** (S) | Confirmado · [P] |
| 1156, 1193 | «Siéntate en el suelo con las piernas extendidas»; V38 quitó suelo | [C]; [R]: asiento inclinado / taburete | **Contener** (S) | Confirmado · [P] |
| 1181 | «Siéntate en el suelo con la espalda contra una pared»; V38 quitó suelo | [C]; [R]: «sentadilla isométrica contra la pared» | **Contener** (S) | Confirmado · [P] |

### 3.3 Evaluados sin incidencia (no se contienen)

| ID | Motivo |
|---|---|
| 910 Remo invertido | Instrucciones coherentes con las banderas: talones en el suelo, barra a la cintura. Solo hay una ambigüedad práctica de equipo (`pull-up bar`). Sin incidencia de seguridad |
| 1298 Maltés en apertura | Su texto pide colgarse, pero ya está **retenido para todos** por INC-4C-03. No se duplica |
| 458, 777 (fondos en paralelas) | El texto y el GIF coinciden en apoyo, no en colgarse. Entran en el **vacío de apoyo suspendido** (§6), que necesita decisión profesional. Contenerlos sería crear una restricción nueva |
| 356, 389, 944 | Falsos positivos de «rodillas»: «almohadilla de rodillas» de la máquina, «flexiona las rodillas» |
| 914, 1297, 1300, 1307, 1312, 916 | Falsos positivos: «suspended» se refiere a correas TRX con apoyo de pies o manos; «pull-up cable machine» es el nombre de la máquina |

El barrido completo, con falsos positivos, está en `evidencia_seguridad/barrido_texto_bandera_triaje.csv` (142 filas) para el triaje profesional.

**Totales:**
- **26 ejercicios contenidos** (`ContencionTextoBandera.registro()`): 7 por colgarse, 8 por brazos arriba y 15 por suelo, con solapamiento.
- **0 correcciones de datos.**
- 1 corrección documental pendiente sin contención (853).

## 4. Pruebas

### 4.1 Unitarias

- `ContencionTextoBanderaTest` (9): conjuntos exactos por restricción; A, B y C; suelo; restricciones que no aplican; trazabilidad (26 entradas, cada una con su contradicción); sin solapes con INC-4C-03 ni INC-CAT-1374; causa de seguridad (`esMotivoDeSeguridad` / `esMotivoDeSustitucion`); la firma de cuatro argumentos no cambia.
- `ExternalCatalogServiceContencionC39Test` (13): el filtro solo actúa para quien declara la restricción, y la consulta normal y la silenciosa de escasez coinciden. El 910 nunca se contiene.
- Las existentes de INC-CAT-1374, INC-4C-03 y SUST-SEG-1 pasan sin cambios.

### 4.2 Integración (`p2d/ContencionTextoBanderaIT`)

Usa el catálogo real (V1–V46), PostgreSQL efímero y el motor de reglas; sin Replicate. Son 12 pruebas, todas en CI:

| Escenario | Resultado [C] |
|---|---|
| **A** Prohíbe colgarse (gimnasio del usuario 39) | Ningún elegible con `requires_hanging` ni contenido por colgarse. Lo contenido solo por brazos arriba o suelo (1309, 1322, 1093) sigue llegando |
| **B** Prohíbe brazos sobre la cabeza | Ni bandera ni texto. El 1306 (contenido solo por colgarse) sigue |
| **C** Prohíbe colgarse, brazos arriba y suelo | Ningún elegible con esas banderas ni ninguno de los 26 |
| Sin restricciones | Los contenidos siguen elegibles: no se excluye de más |
| **D** IA | La instantánea real del prompt (`PlanInputSnapshot`) no contiene ningún contenido, en las cuatro combinaciones |
| **E** Motor de reglas | Las semanas publicadas no contienen ningún contenido ni bandera restringida |
| **F** Plan publicado, sesión pendiente | Copia controlada con el 1303 en una sesión: al prohibir colgarse, la sesión se marca `safetyBlocked`, motivo `INC-CAT-C39`, y `start` se rechaza sin pasar a `IN_PROGRESS`; las demás se inician. La reparación explícita, sin sesiones empezadas, publica la versión 2 sin el 1303 (ni contenidos ni banderas) y deja la versión 1 intacta salvo su estado `REPLACED` |
| **G** Sesión en curso | Registrar el 1303 como hecho se rechaza y omitirlo se admite. La reparación devuelve `SESION_EN_CURSO`: no se sustituye nada, no hay versión nueva y no se pierde ningún registro |
| **H** Factibilidad (4 objetivos × 2 niveles × gimnasio/casa, con las tres restricciones) | Ninguna semana con un prohibido. Un único NO_FACTIBLE, **fijado en la prueba**: LOSE_WEIGHT principiante en casa (§5). Cualquier otro rompe la prueba |
| **I** Coherencia texto-bandera | En **todo el catálogo activo**, ningún ejercicio pide en su texto colgarse, brazos sobre la cabeza o tumbarse o sentarse en el suelo sin tener la bandera o estar retenido o contenido. **Una migración futura que relaje una bandera sin alinear el texto rompe esta prueba.** Además, la contención no modifica banderas ni el estado activo de los 26 |

## 5. Impacto: comparación antes/después

`p2d/ContencionC39ComparacionIT` (opt-in) se ejecutó sobre `generation` (antes) y sobre esta rama (después), con la misma rejilla y la misma semilla. La rejilla son 120 perfiles: 4 objetivos × 3 niveles × gimnasio/casa × 5 combinaciones de restricciones. Evidencia en `evidencia_seguridad/comparacion_*.csv` y `comparacion_resumen.txt`.

| Resultado | Perfiles |
|---|---|
| Idénticos (elegibles, desenlace y semana ejercicio a ejercicio) | 33 |
| Cambio esperado: solo se retiran ids contenidos para su restricción y no cambia nada más en la elegibilidad | 85 |
| Cambios de elegibilidad no explicados por la contención | **0** |
| Semanas de **antes** con un ejercicio cuyo texto pide una acción restringida | **44** |
| Semanas de **después** con alguno | **0** |
| Pasan de plan a NO_FACTIBLE | **2** |

**Los dos NO_FACTIBLE:** LOSE_WEIGHT, principiante, en casa, «sin suelo» y «sin las tres».
- Su semana anterior **dependía** del 1093 y del 1156, cuyas instrucciones son una flexión en el suelo y sentarse en el suelo.
- Sin ellos, V25 (cardio por sesión en LOSE_WEIGHT) no se puede cumplir, y el sistema lo **detecta y no publica**, en lugar de compensarlo con un prohibido.
- Es el comportamiento que pide el escenario H. Para el usuario es una regresión real, y la solución correcta es la revisión profesional del 1093: si el GIF es el correcto («de pie elevando rodillas»), al corregir el texto sale de la contención y se recupera la factibilidad.

## 6. Planes ya publicados: el plan 78 y riesgos pendientes

**El JSON real del plan 78 (`plan78_real.json`) no está disponible en este entorno:**
- no consta en la conversación;
- la carpeta de subidas está vacía.

No sustituyo una comparación exacta por una inferida. Lo que sigue se basa en la reproducción de la auditoría, que coincide con todas las huellas del log real, y en el comportamiento comprobado en las pruebas F y G.

| Sesión del plan 78 (reproducida) | Ejercicios de la lista | Efecto para el usuario 39 (prohíbe colgarse y brazos arriba; admite suelo) |
|---|---|---|
| Lun 16 (`IN_PROGRESS`) | 1093 | Ninguno: el 1093 solo se contiene para quien prohíbe el suelo. La sesión no se toca |
| Jue 19 | 1303, 853 | **1303 → `INC-CAT-C39`**: la sesión se marca bloqueada y no se puede iniciar. El 853 no se contiene |
| Vie 20 | 1152 | **1152 → `INC-CAT-C39`**: bloqueada |
| Dom 22 | — | Ninguno |

**Cómo se comportan los mecanismos actuales [C]:**

| Situación | Comportamiento |
|---|---|
| Detección | En cada lectura del plan y en cada `start`, `PublishedPlanSafetyService` + `SafetyConflictChecker` revisan las sesiones abiertas (`SCHEDULED` e `IN_PROGRESS`) con la seguridad de **hoy** |
| Sesión `SCHEDULED` con un contenido | Se marca (`safetyBlocked`, `safetyBlockReason=INC-CAT-C39`) y `start` se rechaza (prueba F) |
| Sesión `IN_PROGRESS` sin sesión abierta | También se marca. Se puede volver a iniciar si no tiene contenidos |
| Sesión realmente abierta con un contenido | No se sustituye. Registrar el ejercicio como hecho se rechaza y omitirlo se admite (prueba G) |
| Con registros de ejecución | Se conservan: nada los toca (prueba G) |
| Reparación por versión nueva | `PublishedPlanSafetyRepairService` conserva V8C y la validación completa, y crea una versión nueva sin tocar la anterior (prueba F). **Solo repara semanas sin sesiones en curso ni terminadas** (`SESION_EN_CURSO` / `SEMANA_EMPEZADA`) |
| Sustitución silenciosa | **No puede ocurrir**: ni en sesiones empezadas ni en semanas empezadas |
| Semana siguiente | El ciclo semanal sustituye el contenido por SUST-SEG-1, con la causa `INC-CAT-C39` y la excepción de V8C ya existente |

**Riesgos pendientes, conocidos y documentados (no se ocultan):**
1. **La reparación no tiene disparador en producción [C].** `repairPublishedPlanForSafety` solo lo invocan las pruebas. En producción, la protección es el **bloqueo**: la sesión no se puede hacer, pero no se sustituye en la semana en curso.
2. **Las sesiones bloqueadas que vencen pasan a `SKIPPED` sin motivo [C]** (tarea de las 00:30) y cuentan como no realizadas en la adherencia y en la adaptación siguiente, sin culpa del usuario. Es una limitación previa, que ya afecta a INC-CAT-1374 y a cualquier restricción declarada después de publicar el plan.
3. **Plan 78:** con la sesión del lunes en `IN_PROGRESS` (H6), una reparación devolvería `SESION_EN_CURSO`. Cuando venza, pasaría a `SEMANA_EMPEZADA`, de modo que las sesiones del jueves y el viernes quedarían bloqueadas y vencerían como `SKIPPED`. La semana siguiente ya saldría sin el 1303 ni el 1152.
4. **Exposición real sin medir:** no tengo acceso a la base real. Consulta de solo lectura para medirla:

```sql
-- Sesiones abiertas de planes ACTIVOS con un ejercicio contenido para la restricción que el perfil declara
SELECT p.user_id, p.plan_id, pw.scheduled_date, pw.status, pwe.exercise_id
  FROM weekly_training_plans p
  JOIN planned_workouts pw ON pw.plan_id = p.plan_id AND pw.status IN ('SCHEDULED','IN_PROGRESS')
  JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id
  JOIN user_profiles up ON up.user_id = p.user_id
 WHERE p.status = 'ACTIVE' AND (
       (NOT up.allow_hanging_exercises  AND pwe.exercise_id IN (316,1301,1306,1153,1303,1304,1152))
    OR (NOT up.allow_overhead_exercises AND pwe.exercise_id IN (316,1301,131,400,448,1272,1309,1322))
    OR (NOT up.allow_floor_exercises    AND pwe.exercise_id IN (55,224,637,741,875,1062,1063,1093,1095,1156,1181,1193,1303,1304,1311)))
 ORDER BY pw.scheduled_date;
```

**Corrección específica propuesta (no implementada aquí):** toca adherencia y estados, terreno de H6, que la orden deja para otra PR.
- **(a)** Disparar la reparación por versión nueva al leer el plan, o en la tarea nocturna, para semanas **sin ninguna sesión empezada ni terminada**. El mecanismo ya garantiza V8C, la validación completa y la no sustitución de lo empezado.
- **(b)** Al vencer una sesión bloqueada por seguridad, registrar `skip_reason = SAFETY_BLOCKED` y excluirla del denominador de adherencia y de las señales de adaptación.

Pruebas necesarias:
- (a) se dispara solo sin sesiones empezadas, es idempotente y nunca dos versiones activas;
- (a) no se dispara con `IN_PROGRESS` ni con terminadas;
- (b) una sesión bloqueada vencida no baja la adherencia ni dispara REDUCE_VOLUME;
- (b) una sesión no bloqueada vencida sigue contando igual.

## 7. Regresión y comportamiento evaluado

- **Sin cambios:** PR-1.7, FIX-PR-2.1-EXP, PLAN-REV-2.0, PLAN-HIST-1, V15, V24, V8C, V23 y V25–V27, reintentos, progresión, reincorporación, adaptación, presupuesto de IA y sondeo de Replicate. No se ha tocado ningún archivo de esas piezas; el diff solo afecta a `ExternalCatalogService`, `SafetyConflictPolicy`, `SafetyConflictChecker` y la clase nueva `ContencionTextoBandera`.
- **Diferencias esperadas en los planes:** solo para perfiles que declaran colgarse, brazos arriba o suelo prohibidos, y solo porque se retiran los ids contenidos (§5: 85 perfiles con cambio esperado y 0 cambios no explicados).
- **Resultado de la regresión completa local** (`mvnw -o verify -Pintegration`, PostgreSQL 16 en Testcontainers, sin Replicate):
  - Unitarias: 1.439 ejecutadas, 0 fallos, 0 errores (1 omitida, ya lo estaba en `generation`).
  - Integración: 630 ejecutadas, 47 omitidas (laboratorios opt-in, ya lo estaban), 3 fallos. Los tres eran cambios de expectativa provocados por la propia contención, no defectos, y se actualizaron como cambios documentados:
    1. `CatalogoElegibleIT` (2 pruebas): su oráculo SQL independiente no conocía INC-CAT-C39 y seguía esperando 55, 1093, 1095, 1181 y 1311 en perfiles sin suelo. Se le añade la contención como lista propia de la prueba (sin leer `ContencionTextoBandera`), igual que ya hacía con INC-CAT-1374. Vuelve a 3/3: igualdad exacta id por id en los 432 casos de la matriz.
    2. `Usuario31ReproduccionIT.catalogoActual`: el perfil 31 prohíbe suelo y colgarse. Al salir los ids contenidos de su conjunto elegible, la selección determinista del motor de reglas cambia (sábado 1320 → 314, domingo 107 → 1027). Ninguno de los cuatro está contenido y 1320 sigue elegible (lo comprueba la primera aserción). La prueba ahora exige 776 y 314 y que no aparezca ningún id contenido para sus restricciones. Vuelve a 2/2.
  - Tras las dos correcciones, las dos clases afectadas pasan; el resto de la suite no se tocó. El CI de la PR vuelve a ejecutar la suite completa.

## 8. Siguientes etapas (sin implementar)

1. **Revisión profesional** de los 26 contenidos y del triaje: con el GIF delante, decidir qué texto o qué bandera es correcto y preparar la migración auditada. Prioridad: el 1093 y el 1156, por los dos NO_FACTIBLE. Cada id sale de la contención solo con esa migración.
2. **Decisión sobre el apoyo suspendido** (alternativas A–D del informe de auditoría, §I.3).
3. **H2:** coherencia del prompt con V15 (diseño H2-B y evaluación).
4. **H6:** estados de entrenamiento, con las correcciones (a) y (b) de §6.
5. **H3:** experiencia de los 30 s en la app.
6. **Reparación localizada** (investigación).

## 9. Reducción a 8 sobre el catálogo integral corregido (PR #56–#59)

La auditoría integral del catálogo revisó fotograma a fotograma los GIF de los 26 ejercicios contenidos.
- En **los 26**, el GIF y la bandera coinciden: lo erróneo era el texto de la fuente.
- V49 reescribió ese texto según el GIF.

La contención se reduce así:

| Qué | Ids | Por qué |
|---|---|---|
| **Se mantiene: colgarse** | 1153, 1301, 1303, 1304, 1306 | Apoyo suspendido: el cuerpo, sin apoyo en el suelo, queda sostenido por los brazos o los antebrazos. `requires_hanging` no lo cubre y la clasificación es una decisión profesional (`catalogo_integral/PROPUESTA_APOYO_SUSPENDIDO.md`). |
| **Se mantiene: brazos sobre la cabeza** | 131, 400, 448 | Curl «alto»: el brazo queda a la altura del hombro y las manos van hacia la cabeza, en el límite de la definición de `requires_overhead`. El nombre de 400 y 448 sigue diciendo «por encima de la cabeza». |
| **Se libera por completo** | 316, 1152, 1272, 1309, 1322, 55, 224, 637, 741, 875, 1062, 1063, 1093, 1095, 1156, 1181, 1193, 1311 | Su texto, ya corregido, describe el GIF y no pide la acción restringida, o la bandera ya la declara: 224, brazos arriba; 316, 1152 y 1301, suelo. |
| **Se libera en parte** | 1301 (brazos arriba), 1303 y 1304 (suelo) | El texto ya no pide esa acción; se conserva la parte de colgarse. |

**Criterio, comprobado por la prueba `ContencionTextoBanderaIT.l_liberadosJustificados`.** Para cada restricción que deja de aplicarse:
- el texto actual (nombre e instrucciones) ya no pide esa acción, según los mismos patrones de la prueba I;
- o la bandera del catálogo la declara, y entonces la excluye el filtro de seguridad normal.

Ningún id sale porque haga falta para generar un plan.

**Efectos comprobados** (detalle en `docs/FitSense/contencion_c39_reducida/`):
- **LOSE_WEIGHT / principiante / casa** («sin suelo» y «sin las tres») publica una semana válida (prueba R): sin prohibidos, con cardio de pie en cada sesión, sin relajar V25 ni ninguna bandera.
- La rejilla de la prueba H ya no tiene ningún NO_FACTIBLE.
- **Plan 78** (`p2d/Plan78ContencionReducidaIT`): solo se bloquea la sesión del jueves, por el 1303. La del viernes (1152 liberado, 910) ya no.

**Limitaciones que deja a la vista** (no se corrigen aquí):
- **La reparación del plan 78 no publica.** Revalida la semana entera y la misma sesión lleva el 916, que V48 subió a dificultad 3: V1 la rechaza.
- **Con la semana empezada no se repara.** La sesión bloqueada vence como SKIPPED sin motivo y cuenta en la adherencia.

