# Integración de la pila del catálogo sin duplicar #62 (orden de 14/11/2026, fase 1)

## 1. Estado real (comprobado en GitHub y en local)

| PR | Rama | Base | Cabeza | Estado |
|---|---|---|---|---|
| #62 | `fix/sustituto-dosis-capacidad` | `generation` | `1da115a` | Borrador. HALL-PROG-01 (SUST-SEG-1 recibe una dosis de capacidad válida) más el refuerzo de V8C |
| #56 → #57 → #58 → #59 → #55 | pila del catálogo integral (V47–V50 y contención C39) | encadenadas sobre `generation` | — | Abiertas |
| #60 | `cierre/v51-v52-correcciones` | `fix/contencion-seguridad-catalogo` (#55) | `e34e892` | Borrador. V51 y V52. CI en verde |
| #61 | `cierre/v53-apoyo-suspendido` | `cierre/v51-v52-correcciones` (#60) | `99a3e89` | Borrador. V53: solo el dato; el comportamiento está apagado. CI en verde |
| **#63 (nueva)** | `cierre/pendientes-seguridad` | `cierre/v51-v52-correcciones` (#60) | — | Borrador. V52.1, 1294 retenido, expediente de seguridad, registro y fichas |
| **propuesta (nueva)** | `propuesta/reparacion-dificultad-publicada` | `cierre/pendientes-seguridad` (#63) | — | Borrador. **No fusionar sin aprobación**: plan 78 (interruptor apagado) y V54, constancia de bloqueo al vencer |

Nada se ha fusionado y no se ha tocado `generation`.

## 2. Commits que aparecen dos veces

| En #60 | En #62 | Relación |
|---|---|---|
| `b65452c` | `b0267d0` | **El mismo parche** (`git patch-id` idéntico): corrección SUST-SEG-1 |
| `caa6f3b` | `1da115a` | `caa6f3b` = `1da115a` + la frase de `Hipertrofia4Dv34IT` (14 perfiles) |

**Riesgo:** fusionar los dos sin orden deja el cambio dos veces en el historial. Pero **no en el código**: el árbol resultante es el mismo. Una sola rama no puede perder el refuerzo de V8C, porque está en las dos.

## 3. Orden de integración propuesto (sin duplicar #62 y sin perder V8C)

1. **#62 → `generation`.** Es independiente del catálogo y lleva HALL-PROG-01 junto con V8C.
2. **#56 → #57 → #58 → #59 → #55**, en ese orden. Cada una se rebasa sobre la anterior al fusionarse, o se fusionan en cadena.
3. **#63 → `cierre/v51-v52-correcciones`** (base de #60) y después **#60 → `generation`**. Git reconoce `b65452c` como ya aplicado: el conflicto de contenido es nulo, porque el parche es idéntico, y `caa6f3b` solo añade la frase de la prueba.
4. **#61 → `generation`.** V53 entra como dato con el interruptor apagado.
5. **La propuesta** (plan 78 y V54) solo si el propietario la aprueba.

**Comprobación en local.** Se construyó `cierre/integracion-final` como `generation` + fusión de #62 + fusión de #61 (que contiene #55–#60). Su **árbol es idéntico al de #61**: la integración no duplica código y conserva V8C. Al añadir #63, el árbol integrado es igual a #61 + #63. La regresión completa de esa integración está en `REGRESION_CIERRE_PENDIENTES.md`.

## 4. Diferencias con la última regresión validada

Desde la regresión validada de la orden de desbloqueo (#60 = `e34e892`, #61 = `99a3e89`), esto es todo lo que cambia:

| Cambio | Rama |
|---|---|
| V52.1: correcciones de 231, 367, 620, 1017, 1182 y 1323, más INV en 868 | #63 |
| 1294 en `RETENIDOS_INC_4C_03` | #63 |
| Referencia de 72 regenerada (25 planes cambian de contenido; 72 válidos) | #63 |
| Huella: 6 filas | #63 |
| `CatalogoIntegralTextosIT` también lee los bloques de V52.1 | #63 |
| Plan 78: interruptor `fitsense.reparacion.tolerar-dificultad-de-publicacion` (apagado) | propuesta |
| V54: `planned_workouts.system_block_cause` | propuesta |

Nada de esto cambia las políticas (V15, V20, V24, V8C, P-03), la adherencia ni la progresión.
