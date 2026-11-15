# Riesgos residuales de v1.1

Parten de los 12 de v1 (`../cierre_conservador/RIESGOS_RESIDUALES.md`). Aquí solo los cambios y los que siguen vigentes, sin repetir el detalle.

## 1. Cerrados o reducidos en v1.1

| Riesgo de v1 | Estado en v1.1 |
|---|---|
| **R3 · P-03 compara solo el total** | **Cerrado en la candidata:** P-03 por colocación está encendida en todas sus pruebas; el contraejemplo y el cambio de día se bloquean. **En producción sigue apagada** hasta una autorización expresa (ver R3' en la tabla siguiente) |
| **R6 · Reparación de planes publicados** | **Decidido:** una versión reparada respeta las restricciones actuales (plan 78: 1303 y 494). La versión histórica no se toca |

## 2. Vigentes

| # | Riesgo | Mitigación | Qué lo cerraría |
|---|---|---|---|
| R1 | Catálogo **sin validación profesional**: 312 decisiones abiertas | Contención de las incertidumbres materiales y valores más restrictivos | Revisión profesional |
| R2 | Error de interpretación de la IA en ejercicios A | Dos lecturas independientes, controles permanentes y huella | Revisión profesional |
| R3' | P-03 por colocación **apagada en producción** | El valor por defecto es seguro respecto de v1 (solo total). Activarla es un cambio de configuración (`FITSENSE_P03_POR_COLOCACION=true`) ya probado | Autorización expresa al desplegar |
| R4 | 853 ejecutado con rebote | El texto prohíbe saltar; P286 está abierta | Revisión profesional |
| R5 | **Las omisiones impuestas cuentan en la adherencia.** La causa no queda en la fila de la omisión; solo la de dolor se reconstruye, y depende de la configuración vigente | Limitación declarada; consulta de reconstrucción para el dolor | Integrar la **propuesta V57** antes de recoger datos reales (`TRAZABILIDAD_OMISIONES_SISTEMA.md`) |
| R7 | Nadie recibe los 15 ejercicios de apoyo suspendido | V53-B (#66) apagada; la contención se mantiene | Flujo en Flutter y decisión de activar B |
| R8 | 14 perfiles de casa con solo peso corporal sin plan (775 con banco) | NO_FACTIBLE legítimo por V20 | Ampliar el catálogo de peso corporal |
| R10 | Interruptores mal configurados al desplegar | Valores por defecto conservadores; V53 A y B a la vez impiden arrancar (`ModoApoyoSuspendido`) | Revisar la configuración en el despliegue |
| R11 | Todo medido con perfiles sintéticos | Escenarios deterministas y validador real | Evaluación con participantes |
| R12 | La contención reduce las alternativas al sustituir o reparar (NO_FACTIBLE o NO_VALIDA conservadores) | No se publica nada | Ampliar las alternativas seguras |
| R13 (nuevo) | `generation` puede avanzar antes del merge | La simulación se hizo sobre `623c4fa` | Repetir la simulación y comparar árboles (`ORDEN_MERGE_CONTROLADO.md` §3) |

No queda ningún **defecto técnico conocido** de seguridad sin corregir. Los riesgos vigentes son limitaciones de medición, de validación o de cobertura, documentadas.
