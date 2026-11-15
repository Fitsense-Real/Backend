# Dolor y planes publicados en la candidata v1.1

El detalle de P-DOLOR-PUB (diseño, los 10 casos y la consulta de trazabilidad) está en `../cierre_conservador/VALIDACION_DOLOR_PUBLICADO.md`. Aquí se verifica que **sigue igual en v1.1**, con P-03 por colocación encendida, y se cierra la decisión sobre el plan 78.

## 1. Protección ante dolor (P-DOLOR-PUB, obligatoria)

| Requisito de la orden | Comprobación en v1.1 |
|---|---|
| Un ejercicio con dolor activo no se puede registrar como completado | `RegeneracionPorColocacionRev2IT#dolorMarcadoEnSesionesAbiertas`: el registro con series o repeticiones se rechaza (`dolor_registrado`), también al reintentar |
| El ejercicio afectado queda identificado | La API marca la prescripción con `dolor_registrado` y la sesión lleva `AVISO_DOLOR` |
| Los demás ejercicios seguros siguen disponibles | La sesión no se bloquea entera, se puede empezar y los demás se registran con trabajo |
| No se publica ninguna sustitución inválida | El plan publicado no cambia: sin sustitutos ni dosis nuevas |
| Los planes históricos no se modifican retroactivamente | Las sesiones terminadas no se marcan y lo registrado queda igual (`PublishedPlanSafetyServiceDolorTest#terminadas` y la IT) |
| No se registra trabajo en ejercicios excluidos por dolor | En los 40 escenarios HALL-GM-01, `trabajo_tras_dolor = 0` |
| Siguen los bloqueos por otras causas | Causa combinada `requires_floor+dolor_registrado` y sesión bloqueada (`#seSumaALaOtraCausa`) |
| Adaptación NO_FACTIBLE | `#halGm01ConContencion` (9717) y 9703: no se publica nada, la semana vigente se conserva, y lo que dolió no admite trabajo mientras dura la exclusión (`trabajoTrasDolor = 0`) |

## 2. Reparación del plan 78 y ejercicio 494

**Criterio técnico aprobado por el propietario:** una versión reparada del plan respeta las restricciones actuales del catálogo.

| Requisito | Comprobación (`Plan78ContencionReducidaIT`) |
|---|---|
| La versión histórica queda intacta | «la version 1 no se toca»: su contenido es idéntico antes y después |
| Las sesiones completadas no se modifican | La reparación no versiona una semana empezada (`#semanaEmpezada`: SEMANA_EMPEZADA, limitación documentada en la propia prueba) |
| La versión reparada no incluye ejercicios excluidos de nuevas recomendaciones | Sustituye **exactamente** el 1303 (inseguro, INC-CAT-C39) y el 494 (INC-CAT-CONS-v1, identidad). Ningún sustituto está contenido |
| Sin aumento de dosis | Sin más series, repeticiones ni duración en cada fila sustituida (V8C) |
| Sin ignorar restricciones de seguridad | Los sustitutos no requieren colgarse ni brazos arriba; la versión 2 no bloquea nada |
| Si no hay reparación válida | NO_VALIDA, sin publicar, según el contrato: `#semanaSinEmpezar` (el plan 78 original, con 916 y 910 por encima del nivel actual, queda NO_VALIDA y el jueves sigue bloqueado) y el escenario de linaje con la contención (v1 R12). Con la semana empezada, `#semanaEmpezada` (SEMANA_EMPEZADA) |

**PR #64 (`fitsense.reparacion.tolerar-dificultad-de-publicacion`):** sigue **apagada** por defecto. Se prueba solo de forma aislada:

- `Plan78ContencionReducidaIT#propuestaDificultadDePublicacion` la enciende por reflexión y la restablece en `finally`;
- el resto de la clase y de la suite la usa apagada.

Sus excepciones (916 y 910) no se extienden a ninguna otra política.

## 3. Resultado

Todo lo anterior forma parte de la regresión completa de `12c6897`, con P-03 por colocación encendida. Ver `REGRESION_INTEGRADA_FINAL.md`.
