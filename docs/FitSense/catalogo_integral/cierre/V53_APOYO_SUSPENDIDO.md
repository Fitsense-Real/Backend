# V53 · Apoyo suspendido: significado propuesto, revisión de los 16 e impacto sobre permisos guardados

**Estado: PROPUESTA. El comportamiento está apagado y pendiente de vuestra aprobación.**

La orden de desbloqueo (14/11/2026) dice: «No actives el cambio hasta nuestra aprobación del comportamiento final». Por eso V53 queda partida en dos:

| Parte | Estado | Efecto |
|---|---|---|
| **Dato**: columna `exercises.requires_suspended_support` | Migrada por V53 | Ninguno sobre usuarios: solo describe el ejercicio |
| **Comportamiento**: que «no colgarse» excluya también ese dato | **Apagado** (`fitsense.seguridad.apoyo-suspendido-en-no-colgarse=false`, valor por defecto) | Con `false`, «no colgarse» significa exactamente lo mismo que antes de V53 |

Con el interruptor apagado, la rama V53 da los mismos planes que la PR A (V51 + V52). La prueba `ApoyoSuspendidoInterruptorIT` lo comprueba.

## 1. Significado propuesto (criterio conservador)

Hoy, «no colgarse» (`user_profiles.allow_hanging_exercises = false`, V25) excluye solo `requires_hanging`: el cuerpo cuelga del agarre.

**Propuesta.** «No colgarse» excluiría además el **apoyo suspendido sobre un aparato sin soporte de pies**: el cuerpo, sin los pies en el suelo ni en una plataforma, se sostiene sobre un aparato.
- Con los **brazos extendidos**: fondos en paralelas, barra o anillas, y muscle-up.
- Con los **antebrazos**: silla del capitán y elevaciones en paralelas.

**Por qué.** Carga el hombro con todo el peso corporal, como colgarse o más, y la caída desde el aparato es posible.

**Qué NO incluye:**
- El apoyo parcial en máquina (rodillas o pies en la plataforma de asistencia), porque no hay suspensión.
- Las posiciones con las **manos en el suelo**: planchas, maltés, pino, L-sit, y 1301, que lleva los pies en correas y las manos en el suelo. No hay aparato del que caer, y extender a ellas «no colgarse» estiraría aún más el significado del permiso.

**Cambio de significado de un permiso guardado.** Quien marcó «no colgarse» no aprobó este alcance. Encender el interruptor cambia lo que ese permiso excluye para todos los perfiles que ya lo tienen guardado. Es una decisión de producto y profesional, no técnica.

## 2. Revisión de los 16 candidatos

Fuente: `cierre/adjudicacion/apoyo_suspendido_v53.tsv`. Cada candidato tuvo tres miradas al GIF completo: segunda revisión, verificación independiente y coordinador. Aquí se ha vuelto a contrastar cada uno con el criterio conservador.

| Id | Ejercicio | Clase | ¿Cumple el criterio? |
|---|---|---|---|
| 457 | chest dip | Brazos extendidos | Sí |
| 458 | chest dip (on dip-pull-up cage) | Brazos extendidos | Sí |
| 459 | chest dip on straight bar | Brazos extendidos | Sí |
| 462 | captains chair straight leg raise | Antebrazos | Sí |
| 777 | elbow dips | Brazos extendidos | Sí |
| 920 | impossible dips | Brazos extendidos | Sí |
| 934 | korean dips | Brazos extendidos | Sí |
| 1069 | muscle-up (on vertical bar) | Brazos extendidos | Sí |
| 1153 | ring dips | Brazos extendidos | Sí |
| 1184 | side hip (on parallel bars) | Antebrazos | Sí |
| **1301** | suspended reverse crunch | Pies suspendidos | **No**: manos en el suelo y pies en correas; no hay aparato que sostenga el cuerpo desde arriba. **Se retira de V53** y sigue contenido por INC-CAT-C39 |
| 1303 | twisted leg raise | Antebrazos | Sí |
| 1304 | twisted leg raise (female) | Antebrazos | Sí |
| 1306 | vertical leg raise (on parallel bars) | Antebrazos | Sí |
| 1317 | triceps dip | Brazos extendidos | Sí |
| 1335 | wide-grip chest dip on high parallel bars | Brazos extendidos | Sí |

**Resultado: V53 marca 15** (antes 16). Los 8 de manos en el suelo (855, 857, 860, 861, 885, 974, 1298 y 1299) siguen documentados y sin marcar.

**Pendiente de validación profesional:**
- **1301.** ¿Debe «no colgarse» cubrir la suspensión de pies en correas? El criterio conservador no decide ese caso, y sigue contenido por INC-CAT-C39.
- **Las 8 posiciones con las manos en el suelo.** Son de dificultad 3 y no se marcan.

## 3. Impacto si se encendiera

### 3.1 En la generación (base de pruebas, catálogo V1–V53)

| Medida | Resultado |
|---|---|
| Elegibles sin «no colgarse» | Idénticos con el interruptor encendido o apagado |
| Elegibles con «no colgarse», encendido | Salen exactamente los de los 15 que eran elegibles, y nada más |
| Hechos de seguridad para validar sesiones ya publicadas | Apagado: `requires_suspended_support` siempre `false`. Encendido: `true` en los 15, con la causa propia `apoyo_suspendido` |
| Referencia de 72 planes y rejilla de 168 | Iguales con y sin V53 cuando se midió con el comportamiento encendido (`REGRESION_FINAL.md` §3). Ninguno de los 72 perfiles declara «no colgarse» |

### 3.2 Sobre permisos ya guardados (datos reales)

No se ha leído ni tocado ningún dato real. Para medirlo, el propietario ejecuta contra la base que decida el script de solo lectura `cierre/v53/impacto_permisos_v53_solo_lectura.sql` (`BEGIN TRANSACTION READ ONLY` … `ROLLBACK`). Devuelve:
1. Perfiles con «no colgarse» guardado.
2. Cuántos tienen alguno de los 15 en sesiones aún no hechas (`SCHEDULED` o `IN_PROGRESS`) de un plan activo. Al encender, el validador de sesiones futuras bloquearía esos ejercicios y se intentaría la reparación localizada.
3. Sesiones afectadas, con detalle por plan y sesión; solo identificadores internos.
4. Perfiles que ya ejecutaron alguno. Es solo informativo: el historial no se toca.

La prueba `ApoyoSuspendidoInterruptorIT` ejecuta este mismo script sobre el esquema V1–V53 para garantizar que corre y que no escribe.

**Plan 78.** Su jueves lleva el 1303. Hoy ya está bloqueado por INC-CAT-C39 (causa `INC-CAT-C39`). Al encender, la causa pasaría a `INC-CAT-C39+apoyo_suspendido`: el resultado para el usuario es el mismo.

## 4. Para aprobar

1. **El significado**: que «no colgarse» incluya el apoyo suspendido sobre un aparato sin soporte de pies, con el alcance de la §1.
2. **La lista de 15.**
3. **Qué hacer con los perfiles que ya tienen el permiso guardado**: aplicarlo sin más, avisarles, o pedirles que confirmen.

Tras la aprobación, se enciende con `fitsense.seguridad.apoyo-suspendido-en-no-colgarse=true`. No hace falta otra migración. Para desactivarlo basta con volver a `false`.
