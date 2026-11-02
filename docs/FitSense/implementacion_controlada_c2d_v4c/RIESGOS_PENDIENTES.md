# Riesgos pendientes antes de activar

## Técnicos

| # | Riesgo | Situación | Mitigación propuesta |
|---|---|---|---|
| T1 | **V44 se aplica al desplegar** esta rama | Solo esquema, columnas nullable y sin datos. Probada en base efímera | Orden explícita de despliegue. Aplicarla primero en un entorno de staging |
| T2 | **V4-C cambia la aceptación de borradores de IA** | Una IA que declara menos de lo que estima será rechazada y pasará al reintento o al motor de reglas. Con el motor de reglas no cambia nada (0 de 960) | Activar `PLAN-V4-0.1` y medir rechazos de IA en una campaña con IA simulada antes de producción |
| T3 | **Coste de C2-D** | Hasta 508 ms y 164 evaluaciones por semana en la matriz; presupuesto de 4.000 | Vigilar el log `C2D_SIN_EXCEPCION` con estado `NO_DEMOSTRADA` |
| T4 | **Saturación local, no global** | Una combinación de varios movimientos podría llegar al 70 % | Documentado. Una mejora del constructor podría recuperar los 3 perfiles de GENERAL_FITNESS con SUSTITUIR |
| T5 | **Minutos aeróbicos ejecutados sin integrar** | El campo existe y vale `null` | Leer `session_exercises` desde el contexto de ejecución en una orden aparte |
| T6 | **Preferencia de LOSE_WEIGHT sin versión** | Es un indicador de generación y no queda en la traza | Darle una versión de política antes de activarla |
| T7 | **Contrato de duración y Flutter** | El backend está listo detrás de un indicador; Flutter no está hecho | **Condición de activación de C2-D** |
| T8 | **ACORTAR y D8 no se ejercitaron con IA en el ciclo de C2-D** | Cubiertos por las baterías existentes con la política histórica | Campaña con IA simulada y `PLAN-C2D-0.1` antes de activar |

## Funcionales y científicos

| # | Riesgo | Situación |
|---|---|---|
| F1 | **Sesiones muy cortas publicadas por C2-D** | 46 % (4 perfiles GAIN_MUSCLE con peso corporal) y **34,7 % tras el dolor (L8)**. Técnicamente saturadas; **adecuación no demostrada** |
| F2 | **PULL sin material sin ninguna tracción** | El catálogo de peso corporal no tiene HORIZONTAL_PULL ni VERTICAL_PULL elegibles. Los ejercicios de «espalda» usados son de tronco o movilidad (1261, 1308, 1259, 1066) y un empuje (775). El 315 tiene instrucciones con mancuerna y equipo de peso corporal |
| F3 | **Volumen concentrado en fuerza (R0800)** | 21 series de pierna en una sesión, 138 por semana. Ya ocurre en la versión vigente |
| F4 | **LOSE_WEIGHT sin fuerza para grupos grandes** | 148 de 230. La preferencia mejora 48, pero altera 82 planes sin brecha |
| F5 | **Cardio programado inferior a 150 min/semana** | Sin datos de actividad externa ni de intensidad, no se puede evaluar. No se aumenta la dosis |
| F6 | **Evidencia científica solo de resúmenes** | Ningún texto completo accesible (red bloqueada) |
| F7 | **Sin revisión profesional** | Instrumento y muestra ampliada listos. **Ninguna evaluación registrada** |

## Requisitos de activación (propuestos)

1. Revisión profesional de la muestra ampliada, con criterios de consenso confirmados.
2. Contrato de duración visible en la app.
3. Decisión sobre los ejercicios de catálogo dudosos (F2) o exclusión de los perfiles afectados.
4. Despliegue de V44 autorizado.
5. Campaña con IA simulada bajo `PLAN-V4-0.1` y `PLAN-C2D-0.1`.
