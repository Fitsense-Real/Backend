# Riesgos restantes

| # | Riesgo | Gravedad | Estado | Qué haría falta |
|---|---|---|---|---|
| R1 | **Sin tracción con peso corporal sin material.** 48 perfiles (6–7 días, PUSH/PULL/LEGS) y los ciclos L4 y L8 quedan NO_FACTIBLE con EF | Alta (producto) | Detectado y declarado (VEF), **sin solución** | Decisión de producto: proponer declarar material (V39 ya tiene tracciones con barras de pared o anclaje), otra división para peso corporal (FULL_BODY o UPPER/LOWER) o un ejercicio de tracción sin material **validado profesionalmente**. **No** añadir ejercicios ni suponer material |
| R2 | **Las familias de patrones de EF son una regla técnica**, no validada profesionalmente. Por ejemplo, TRUNK_EXTENSION en PULL o SHOULDER_RAISE en PUSH | Media | Implementado como versión candidata | Revisión profesional de las familias y del ancla |
| R3 | **Posible exclusión excesiva de EF**: 65, 873 y 808 (habilidades con barra o balón, zona espalda) no cuentan como resistidos | Baja (exigen material; no afecta al PULL sin material) | Registrado | Revisión profesional |
| R4 | **Inconsistencias de material no excluidas**: 318, 1152, 1196, 1298 y 1077 siguen elegibles con peso corporal aunque su texto exige material; 303 y 305 tienen instrucciones de mancuerna | Media | Propuesta, sin cambios en la base | Aprobar los requisitos de equipo (como V39) o corregir las instrucciones |
| R5 | **320** (curl sin carga) sigue contando como hipertrofia | Media | Sin cambios | Revisión profesional de su capacidad |
| R6 | **Volumen de R0800**: 21 series de pierna en una sesión. EF no lo cambia | Media | Para revisión | Revisión profesional. **No** imponer un tope universal sin evidencia |
| R7 | **El PUSH sin material se queda en 4 colocaciones** con EF (sin hombro compatible) | Baja (esos planes ya son NO_FACTIBLE por el PULL) | Registrado | El mismo que R1 |
| R8 | **Prueba con semántica histórica**: `PoliticasP123IT.cuatroObjetivosEnLosLimites[13]` falla con C2-D (esperado) | Baja | Documentado | Parametrizar la aserción por versión **antes** de activar C2-D |
| R9 | **IA real con EF**: no se ha medido si un modelo real respeta la regla de patrones | Media | No probado (Replicate prohibido) | Campaña con IA real, con presupuesto aprobado |
| R10 | **V23 no bloquea al motor de reglas** (`ENFORCE_AI_ONLY`) y es semanal | Media | Sin cambios (análisis de dependencias en `AUDITORIA_V23_V25.md`) | Decisión. EF cubre la parte esencial por sesión |
| R11 | **Despliegue de V44 y V45**: ninguna se ha aplicado a Neon ni a Railway | — | Solo probado en PostgreSQL efímero | Aprobación y despliegue controlado |
| R12 | **Flutter**: el contrato de duración (mensaje neutral) no se muestra todavía | Media (condición para activar C2-D) | Especificado (`REVISION_CONTRATO_DURACION.md`) | Implementación en Flutter (fuera de esta orden) |
| R13 | **Revisión profesional**: muestra de 36 planes preparada, **ninguna evaluación registrada** | Alta (condición de activación) | Bloqueado: no hay evaluadores | 1–2 evaluadores cualificados con el instrumento de 12 criterios |
| R14 | **Textos científicos completos**: siguen sin leerse (los anfitriones no resolvían en la orden anterior) | Media | Sin cambios | Acceso a los textos |
| R15 | **LW-PREF-0.2**: 6 inválidos pasan a válidos, un efecto no buscado | Baja | Registrado | Revisión en la muestra (E-LW) |
| R16 | **Concurrencia multinodo real** no probada (solo dos contextos sobre una base efímera) | Baja | — | Prueba en un entorno autorizado |

**Lo que no es un riesgo nuevo:**
- La política por defecto `PLAN-HIST-1` produce exactamente lo mismo que antes:
  - 960 de 960 perfiles iguales;
  - campaña REPLAY idéntica (ver `REGRESION_FINAL.md`).
- Las versiones EF son nuevas, candidatas y no reinterpretan ningún plan existente.
