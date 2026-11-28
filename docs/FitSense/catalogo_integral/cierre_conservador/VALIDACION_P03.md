# P-03 nivel 2: total frente a colocación

**No se ha activado ni modificado P-03.** La regla por colocación existe en el código, **apagada** (`fitsense.regeneracion.p03-por-colocacion=false`). Activarla es decisión del propietario.

## 1. Qué hace hoy P-03 nivel 2

Si la adaptación ante dolor queda NO_FACTIBLE, `SafeRegenerationService` regenera la semana con el motor de reglas:

- con el conjunto elegible que ya excluye lo que dolió;
- con dosis base y validación V1–V25;
- y solo la publica si su **trabajo total** (repeticiones y segundos) no supera el de la semana medida (`masTrabajoQueLaMedida`).

El contrato aprobado lo dice así: «la semana regenerada parte de las dosis base (**sin linaje** V31 hacia la semana medida)».

## 2. El contraejemplo (reproducido)

Prueba permanente: `P03ComparacionPorColocacionIT#contraejemplo`, participante 9850 (GAIN_MUSCLE intermedio, gimnasio).

1. Semana medida: el plan generado, con una colocación estructural **reducida** a 2 series (como un mantenimiento por fatiga).
2. Semana regenerada: la misma, con esa colocación **de nuevo a 3 series** el mismo día y **sin otra colocación** (la que dolió). El total **no sube**.

| | Política vigente (solo total) | Regla por colocación (apagada) |
|---|---|---|
| Resultado | **La acepta** (`masTrabajoQueLaMedida` devuelve `null`) | **La rechaza**, nombrando la colocación y el ejercicio («por colocación …») |

## 3. ¿Viola una garantía aprobada?

**No viola la garantía aprobada de P-03**, que es literalmente:

- no publicar más trabajo **total** que la semana medida;
- ningún ejercicio excluido;
- validación completa;
- dosis base.

Pero **sí es un riesgo residual** respecto a otra regla: la de «no hay restauración automática de una reducción» (P-A1, comparador por colocación de HALL-GM-01). Por esta vía, una reducción por fatiga podría volver a la base sin una progresión autorizada.

**¿Se ha observado en un recorrido real? No.**

- En los 40 escenarios REV2 (9701–9740), con las protecciones apagadas, con las encendidas y con la regla por colocación encendida, **ningún participante publica por P-03 nivel 2**: la columna `regeneracion` es `[]` o `[NO_FACTIBLE]`.
- En PLAN-HIST-1, `HerenciaD8IT.dolorConRegeneracionSegura` (9004) compara también por colocación, y pasa.

El riesgo está **demostrado como posible** (contraejemplo construido), pero **no observado**.

## 4. Efecto en la factibilidad

Medido con el código de producción de la candidata (commit `eccff00`, idéntico en `src/main` a `836a1f5`), con la regla por colocación encendida (`-Dfitsense.regeneracion.p03-por-colocacion=true`) en los 40 escenarios HALL-GM-01: **resultado idéntico, columna por columna**, al de la candidata con la regla apagada.

- 35 publicadas y 5 NO_FACTIBLE.
- Ningún NO_FACTIBLE nuevo.

Los 5 NO_FACTIBLE ya los rechaza la comprobación del total, y P-03 nivel 2 no llega a publicar ninguna semana en esos recorridos. Fuera de esos escenarios, la regla solo podría convertir en NO_FACTIBLE una regeneración que devuelve a la base una colocación reducida. Es exactamente lo que se quiere evitar.

## 5. Recomendación

**Activar la regla por colocación**, como cambio de política aprobado expresamente por el propietario. Motivos:

- Cierra la única vía conocida de restauración sin autorización.
- En lo medido no cuesta factibilidad.
- Es coherente con el comparador por colocación que ya usa HALL-GM-01.

Mientras no se apruebe:

- se **mantiene** la política vigente;
- el riesgo queda documentado en `RIESGOS_RESIDUALES.md`;
- el contraejemplo queda fijado por la prueba, que fallaría si alguien cambiara la política sin decidirlo.

Alternativa de ajuste, si se prefiere no rechazar: que la regeneración herede la dosis medida de las colocaciones que coinciden. Es un cambio mayor del contrato («sin linaje»), así que no se recomienda para esta versión.
