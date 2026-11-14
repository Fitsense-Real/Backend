# V53 · Revisión definitiva del apoyo suspendido (orden de 14/11/2026, fase 4)

**Estado: PROPUESTA. El interruptor `fitsense.seguridad.apoyo-suspendido-en-no-colgarse` sigue en `false`. No se ha activado nada.**

Este documento sustituye la §4 de `V53_APOYO_SUSPENDIDO.md` como base de la decisión. La lista de 15 y la columna `requires_suspended_support` no cambian.

## 1. Las cinco situaciones, separadas

La clasificación sale de la animación completa (R2 y V3 en `adjudicacion/`) y de las banderas vigentes (`huella_catalogo.tsv`).

| # | Situación | Mecánica y riesgo | Ejercicios activos | Restricción vigente |
|---|---|---|---|---|
| **1** | **Colgarse de una barra** (el cuerpo cuelga del agarre) | Tracción del hombro con todo el peso, agarre, posible caída | 47 con `requires_hanging`: 61, 62, 65, 858, 865–871, 873, 913, 937, 1068, 1069, dominadas, asistidas en máquina (74–77, 995), 113… | «No colgarse» los excluye (V25) |
| **2** | **Sostener el peso corporal sobre barras o aparatos**, sin los pies apoyados | Compresión del hombro con todo el peso (o más, en el fondo del fondo), caída desde el aparato | **Los 15 de V53**. Con los brazos extendidos: 457, 458, 459, 777, 920, 934, 1069, 1153, 1317, 1335. Con los antebrazos: 462, 1184, 1303, 1304, 1306 | **Ninguna específica.** 1069 por HG; 1153, 1303, 1304 y 1306 contenidos por INC-CAT-C39 para «no colgarse»; **los otros 10 son elegibles** |
| **3** | **Pies apoyados** (manos en barra, aparato o banco; pies o rodillas en el suelo o en una plataforma) | Parte del peso en los pies, sin caída relevante | Remos invertidos 910, 911 y 912 (talones en el suelo o en un banco); fondos en banco 313, 314, 1078, 1146 y 1318; asistidos de rodillas 72, 73 y 78; máquinas sentado 1047 y 1051 | FL o KN cuando procede. Correcto |
| **4** | **Pies suspendidos** (pies en correas, manos en el suelo) | Plancha con los pies colgados: carga de hombro como una plancha alta, sin aparato del que caer | 1301 | FL. Contenido por INC-CAT-C39 para «no colgarse» |
| **5** | **Correas o soportes con parte del cuerpo apoyada** | Las correas sostienen las manos o un pie; el resto del cuerpo apoya en el suelo | 914 y 1307 (remo en correas o anillas, talones en el suelo), 1297 (flexión con las manos en correas y los pies en el suelo), 1300 (caída abdominal de pie), 1312 (zancada búlgara con un pie en la correa) | FL u OH cuando procede. Sin restricción específica |

**Conclusión técnica:**
- Solo la situación **2** comparte con la **1** lo que justifica «no colgarse»: todo el peso del cuerpo sostenido por los hombros sobre un aparato, con posible caída.
- Las situaciones 3 y 5 llevan apoyo de pies, y las banderas vigentes las tratan bien.
- La situación 4 es una plancha: no hay aparato que sostenga el cuerpo desde arriba. Es decisión profesional, y sigue contenida.

## 2. ¿Una sola preferencia o un atributo nuevo?

| Opción | Qué significa | A favor | En contra |
|---|---|---|---|
| **A. Ampliar «no colgarse»** (V53 tal cual, interruptor encendido) | «No colgarse» excluiría también la situación 2 | Ya preparada; sin migración nueva; sin cambios en Flutter | **Cambia el significado de un permiso que la persona ya dio.** Quien marcó «no colgarse» pensaba en colgarse, no en fondos. Mezcla tracción y compresión, dos mecánicas distintas |
| **B. Atributo propio «apoyo suspendido»** (recomendada) | Un permiso nuevo, `allow_suspended_support`, que excluye la situación 2. «No colgarse» no cambia | El significado de cada permiso es explícito y lo puede editar la persona; la columna del catálogo ya existe (V53) | Necesita una migración de perfil y, después, la pregunta en Flutter (fuera de esta orden) |
| C. Un atributo por situación (1–5) | Cinco permisos | Máxima precisión | Las situaciones 3–5 no tienen un riesgo que justifique un permiso propio; más preguntas al usuario sin beneficio |

**Propuesta: B**, con una transición conservadora para los permisos ya guardados (§3). Las situaciones 3, 4 y 5 **no** reciben un atributo nuevo. La 4 (1301) queda como pregunta profesional y sigue contenida.

## 3. Impacto (si se aprobara la opción B)

| Ámbito | Impacto |
|---|---|
| **Usuarios nuevos** | Hasta que Flutter tenga la pregunta, `allow_suspended_support` toma el valor de `allow_hanging_exercises`. Quien declara «no colgarse» tampoco recibe la situación 2. Después, la pregunta es explícita |
| **Permisos guardados** | **No se reinterpreta en silencio.** El nuevo atributo se inicializa con el valor de «no colgarse» (lo conservador) y se marca como *pendiente de confirmar*, para pedir la confirmación en la app. Primero, el propietario mide el alcance real con `cierre/v53/impacto_permisos_v53_solo_lectura.sql` (solo lectura). No se ha ejecutado contra datos reales |
| **Generación** | Igual que la opción A para quien tiene «no colgarse», mientras no confirme otra cosa. Rejilla de 168: **168 de 168** con plan, tanto apagado como encendido. Cambian los elegibles de **16 perfiles** (8 «sin colgarse» y 8 «sin todo»): salen **10 ejercicios** (457, 458, 459, 462, 777, 920, 934, 1184, 1317 y 1335). Los otros 5 ya no eran elegibles en esos perfiles. Ninguno de los 72 perfiles de referencia ni de los 312 de hipertrofia declara «no colgarse» |
| **Adaptación** | Los sustitutos salen del conjunto elegible, que ya aplicaría el atributo: no hace falta tocar la adaptación. V24 y V8C sin cambios |
| **Planes publicados** | Las sesiones abiertas con alguno de los 10 se bloquearían para esos perfiles, con la causa propia `apoyo_suspendido`, y se intentaría la reparación localizada. El plan 78 no cambia: su 1303 ya está bloqueado por INC-CAT-C39 |
| **Sesiones bloqueadas** | Cada activación de una restricción puede dejar sesiones bloqueadas que venzan. Con V54 (propuesta, `cierre/pendientes/SESIONES_BLOQUEADAS.md`) quedaría constancia de la causa. **Recomendación: activar solo con la reparación localizada disponible y después de medir el alcance con el script** |
| **Factibilidad** | Sin pérdida en la rejilla de 168. Cada uno de los 16 perfiles conserva alternativas de empuje y de tronco sin apoyo suspendido |

## 4. Mientras no se decida

- El interruptor sigue apagado.
- **Los 10 ejercicios citados siguen elegibles para «no colgarse».** Es la incertidumbre material abierta del catálogo (A-V53 en el registro, estado «Bloqueado por validación especializada», sin contención).
- Contenerlos ahora sería activar V53, y la orden lo prohíbe sin autorización.

## 5. Para aprobar

1. **Opción A o B.** Se recomienda B.
2. La lista de 15 (situación 2) y que 1301 (situación 4) quede fuera.
3. La transición de los permisos guardados: heredar de «no colgarse» y pedir confirmación.
4. Activarla después de medir el alcance con el script de solo lectura.
