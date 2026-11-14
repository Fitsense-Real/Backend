# Plan 78: reparación con la dificultad de publicación (PROPUESTA, interruptor apagado)

**Estado: propuesta preparada y probada. Cambia el contrato de reparación aprobado, así que no se activa sin aprobación del propietario.**

## 1. El problema, separado por tipo

El plan 78 (usuario 39, principiante, «no colgarse») tiene cuatro sesiones. Hoy:

| Tipo | Ejercicios | Qué exige la orden | Tratamiento vigente | Tratamiento propuesto |
|---|---|---|---|---|
| **Incompatibilidad de seguridad actual** | 1303, jueves: apoyo suspendido; contenido por INC-CAT-C39 para «no colgarse» | La seguridad actual manda | La sesión del jueves está bloqueada (no se puede iniciar) | Igual. La reparación lo **sustituye** por un ejercicio del conjunto elegible real |
| **Cambio de clasificación posterior a la publicación** | 916, jueves: V48 subió su dificultad de 1 a 3. 910, viernes: V52 la subió de 1 a 2 | Conservar la trazabilidad de la clasificación al publicar, sin que se imponga a la seguridad actual | La reparación revalida la semana entera contra el catálogo de hoy. V1 y V6 rechazan 916 y 910 (`NO_VALIDA`), así que no se publica nada y el jueves sigue bloqueado | Se **toleran**, y queda constancia en la versión nueva (`output_snapshot.tolerated_publication_difficulty = [910, 916]`) |
| **Sesiones históricas o completadas** | La semana ya empezada | No modificar entrenamientos completados | `SEMANA_EMPEZADA`: no se repara nada | Igual |
| **Ejercicios futuros sustituibles** | Solo el 1303 | No sustituir en silencio toda la rutina | — | Una única sustitución localizada, sin aumento de dosis (V8C) |

## 2. Qué hace el interruptor

El interruptor es `fitsense.reparacion.tolerar-dificultad-de-publicacion`, en `false` por defecto. El cambio está en `PublishedPlanSafetyRepairService`.

Con el interruptor en `true`, se toleran **solo** los ejercicios ya publicados que cumplen todo esto:
- no se sustituyen;
- no tienen causa de seguridad;
- hoy no están en el conjunto elegible;
- **sí** están en el conjunto elegible del mismo perfil calculado con dificultad máxima 3.

Ese último cálculo aplica todas las restricciones de seguridad: equipo, banderas, bloqueos por dolor, INC-4C-03, INC-CAT-1374 e INC-CAT-C39. Es decir, el ejercicio solo deja de ser elegible por su dificultad.

**La tolerancia cubre solo V1 y V6 de esos ejercicios.** Cualquier otro problema sigue rechazando la versión nueva: seguridad, V8C, dosis, o V1 de otro ejercicio. Los sustitutos salen siempre del conjunto elegible real, con la dificultad del perfil.

**Se mantiene:** V24 y V8C no se relajan; los entrenamientos completados no se tocan; la versión 1 queda intacta.

## 3. Pruebas (`Plan78ContencionReducidaIT`)

| Prueba | Interruptor | Resultado |
|---|---|---|
| `semanaSinEmpezar` | apagado | `NO_VALIDA` por V1 (916, 910); versión 1 intacta; el jueves sigue bloqueado. **Sin cambios** |
| `reparacionLocalizada` | apagado | Sin 916 ni 910: `REPARADA`, solo cambia el 1303. **Sin cambios** |
| `semanaEmpezada` | apagado | `SEMANA_EMPEZADA`; el jueves vence `SKIPPED`. Con V54, además, queda constancia de la causa (`SESIONES_BLOQUEADAS.md`) |
| **`propuestaDificultadDePublicacion`** (nueva) | **encendido** | `REPARADA`: solo cambia la fila del 1303; 916 y 910 se conservan; el sustituto no está contenido ni restringido; sin más series ni repeticiones; versión 1 intacta; la versión 2 no bloquea ninguna sesión; el snapshot lista `[910, 916]` |

También se pasaron `ProteccionPlanesPublicadosIT`, `ContencionTextoBanderaIT` y `ReparacionLinajeIT` (28 pruebas, todas en verde).

## 4. Respuesta a la orden

- **¿Puede repararse correctamente el plan 78?** Sí, con el interruptor encendido, mientras su semana no haya empezado. Con el contrato vigente (apagado), no: el 916 y el 910 impiden publicar.
- **Impacto de activarlo:** solo afecta a las reparaciones de seguridad de planes publicados cuyos ejercicios subieron de dificultad después de publicarse. El conjunto de planes afectados en datos reales se mide con la consulta de solo lectura de `PLANES_PUBLICADOS_C39.md` §2. Aquí no se han leído datos reales.
- **Para aprobar:** que un ejercicio publicado y seguro hoy pueda seguir en la versión reparada aunque su dificultad haya subido después, con la constancia en el snapshot.
