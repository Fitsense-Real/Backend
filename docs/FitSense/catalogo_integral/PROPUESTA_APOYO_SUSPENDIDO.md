# Propuesta de clasificación: colgarse, apoyo suspendido y apoyo parcial en máquina

**Estado:** propuesta para revisión profesional. **No se ha creado ninguna bandera ni regla nueva.** Mientras no haya decisión se mantiene la contención existente donde ya la había (INC-CAT-C39 sobre 1153, 1301, 1303, 1304 y 1306).

## 1. El vacío

La única bandera relacionada es `requires_hanging`: «el cuerpo cuelga del agarre, pies sin apoyo; incluye asistidas». La revisión visual de los 1.219 activos distinguió cuatro situaciones que esa bandera no separa:

| Clase propuesta | Qué muestra el GIF | Ejemplos (id) | Bandera actual |
|---|---|---|---|
| A · Colgado de la barra o de las anillas | El peso cuelga de las manos, con los brazos por encima de la cabeza | dominadas, elevaciones colgado, 1374 | `requires_hanging` (y casi siempre `requires_overhead`) |
| B · Apoyo sobre los brazos extendidos (`BRAZOS_EXTENDIDOS`) | El peso descansa sobre manos o antebrazos en paralelas, estación de fondos, silla romana o anillas, con los pies sin apoyo y los brazos por debajo de los hombros | 457, 458, 459, 462, 777, 920, 934, 1069, 1153, 1184, 1303, 1304, 1306, 1317, 1335 | ninguna (1069 sí tiene `requires_hanging` por su fase colgado) |
| C · Pies suspendidos (`PIES_SUSPENDIDOS`) | Pies en correas o anillas y manos en el suelo (plancha) | 1301 | `requires_floor` |
| D · Apoyo parcial en máquina (`MAQUINA_PARCIAL`) | Rodillas o pies sobre la almohadilla de asistencia | 72, 73, 74, 75, 76, 77, 78, 995 | `requires_hanging` en las dominadas asistidas (74–77, 995); ninguna en los fondos asistidos (72, 73, 78) |

## 2. Por qué importa

Quien declara «no colgarse» puede tener una razón de hombro, de agarre o de miedo a la caída:
- La clase **B** carga el hombro en depresión con todo el peso del cuerpo. Desde el punto de vista del hombro es tan exigente como colgarse, o más, aunque el agarre sea distinto.
- La clase **C** no carga el agarre, pero exige estabilidad escapular en plancha.
- La clase **D** reduce la carga.

Hoy, un perfil «sin colgarse» recibe B y C (salvo los cinco contenidos por C39) y no recibe A.

## 3. Opciones para la decisión profesional

1. **Mantener la definición actual.** B, C y D son elegibles para «sin colgarse». Habría que retirar la contención de 1153, 1301, 1303, 1304 y 1306 con evidencia; su texto ya está corregido en V49.
2. **Ampliar el significado de «no colgarse» a A + B** (recomendación técnica conservadora). Se marcan con `requires_hanging` los 15 ejercicios de la clase B mediante una migración trazable, y C y D siguen como hoy.
   - Impacto: los perfiles «sin colgarse» pierden fondos en paralelas y elevaciones en silla romana.
   - El riesgo de dejar sin ejercicios el hueco de empuje es bajo: hay más de 150 empujes horizontales y verticales elegibles.
3. **Crear una restricción nueva** (`requires_arm_support`), con su pregunta en el perfil.
   - Es la opción más precisa, pero cambia el esquema, el cuestionario y Flutter, así que queda fuera del alcance de esta orden.

## 4. Lo que queda hecho y lo que no

- Hecho: inventario de las cuatro clases con evidencia visual (columna `apoyo_suspendido` en `revision_visual/revision_LNN.tsv` y en el registro maestro).
- Hecho: textos de 1153, 1301, 1303, 1304 y 1306 corregidos en V49 (ya no dicen «cuélgate»).
- No hecho, por decisión expresa de la orden: ninguna bandera nueva ni ningún cambio de significado de `requires_hanging`.
- Pendiente: decisión profesional entre las opciones 1, 2 y 3. Hasta entonces, la contención C39 de esos cinco ejercicios se recomienda mantener (ver `IMPACTO_PR55.md`).
