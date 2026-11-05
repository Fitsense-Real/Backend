# 08 · Limitaciones y propuestas pendientes

## 1. Limitaciones

Cada limitación se clasifica en una de cuatro categorías:
- **Justificada:** la regla la produce a propósito y es razonable.
- **Catálogo:** falta material o hay una clasificación dudosa.
- **Diseño abierto:** hace falta una decisión.
- **Previa:** ya existía con PLAN-HIST-1.

| # | Limitación | Alcance medido | Clase | Qué se hace |
|---|---|---|---|---|
| L1 | INCREASE_STRENGTH intermedio y avanzado, gimnasio y casa con mancuernas, **1 día de 30 min**: sin plan. Los 4 patrones de V25 con la dosis de fuerza suman ~1.896 s, más que el máximo de 1.800 s | 4 de 3.780 perfiles | Justificada / diseño abierto | Nada sin decisión (propuesta P1) |
| L2 | `DOSIS_LIMITADA`: algún grupo o el aeróbico no llega al objetivo semanal | 2.385 de 3.776 planes | Catálogo / días | Queda en la traza (`dosis`). No se fabrica volumen |
| L3 | LOSE_WEIGHT intermedio y avanzado: mediana de 72 min de trabajo aeróbico por semana, frente al objetivo de 150 [SP] | LW I/A | Catálogo y V18 | Declarada. No se promete pérdida de peso |
| L4 | No hay tracción con peso corporal en el catálogo. En casa sin material no puede haber sesión PULL, y la división pasa a superior/inferior (P-G3) | Casa con peso corporal | Catálogo | No se inventan ejercicios. **No se asume** que una mesa o una toalla sean seguras |
| L5 | 775, 1261 y 1308 figuran como `back` con patrón HORIZONTAL_PUSH | Composición superior | Catálogo | Revisión profesional del catálogo (P6) |
| L6 | Sesiones cortas: 1.063 planes con alguna sesión `CORTA_RAZONABLE` (< 50 % de M). INCREASE_STRENGTH principiante ~20 min; J03 LOWER ~23 de 70 min | 28 % de los planes | Justificada (P-G1) | Clasificadas en la traza; revisión profesional (P2) |
| L7 | Desactivar en el catálogo un ejercicio **que está en un plan publicado** deja la semana siguiente NO_FACTIBLE si no se usa IA: la copia falla V1 | Cambios de catálogo durante el estudio | Previa (las dos políticas) | No se publica nada inactivo. Propuesta P7 |
| L8 | Recuperación tras P-A1: un paso cada dos semanas (semanas 4, 6 y 8), más lento que los dos pasos que describe la evidencia | GM con fatiga | Diseño abierto | Propuesta P3 |
| L9 | Al volver de REV2 a HIST, una copia en dosis de mantenimiento puede no llegar al 70 % de V20 (30 s en la prueba): D8 solo exime el ACORTAR | Solo si se revierte la política | Diseño abierto | Propuesta P4 |
| L10 | Reincorporación: si un acortamiento retira varios ejercicios, no vuelve ninguno; con acortamientos seguidos solo vuelve el más reciente (D-P8, «uno solo») | Acortamientos múltiples | Previa / diseño | Propuesta P5 (sin implementar ni probar en IT) |
| L11 | Con `capability-validation` apagada (por defecto), los objetivos por capacidades no validan la dosis (H14) | LW, GF, IS | Previa | Sin cambio |
| L12 | V26 en una semana derivada admite el volumen **heredado** de un plan histórico por encima del techo (no obliga a recortarlo) y solo impide subirlo | Planes HIST adaptados con REV2 | Diseño (transición) | Decisión explícita para no dejar NO_FACTIBLE a quien ya tiene plan. Probado en `v26SemanaDerivada` |
| L13 | Las IT de progresión escritas para HIST tienen 4 premisas que no se cumplen con REV2 (`06_VERIFICACION.md` §4) | Pruebas | Pruebas | Con REV2 se informan, no se ocultan. Habría que escribir variantes REV2 si se adopta |
| L14 | Tope de 4 sesiones semanales para principiantes (`MAX_SESSIONS_BEGINNER`, regla previa): un principiante que pide 5 días recibe 4. El oráculo K20 no lo modela (L05 de la cohorte L) | Principiantes con 5 o más días | Previa (las dos políticas) | Decidir si el oráculo incorpora la regla o si el producto la comunica al usuario; no se cambió nada |

## 2. Propuestas pendientes de validación profesional o de decisión

Ninguna está activada.

| # | Propuesta | Por qué | Coste o riesgo |
|---|---|---|---|
| P1 | Para L1, elegir una: (a) V25 admite 3 de 4 patrones cuando no caben, con aviso; (b) tolerancia de V4 aceptada de forma explícita por el usuario; (c) dosis de fuerza de 2 series con 1 día de 30 min | Cuatro perfiles sin plan | (a) cobertura incompleta; (b) excede el máximo declarado; (c) menos dosis |
| P2 | Revisar las cifras de dosis y techo: 6/10/12 objetivo GM; techos 12/20/24 por semana y 6/10 por sesión; 4 series por grupo en el resto; 45/150 y 30/60 min de aeróbico | Todas son [HP][PRO] | — |
| P3 | Recuperación tras fatiga en 2 pasos (80–85 % → 100 %) en lugar de 1 paso cada 2 semanas | Más cerca de la evidencia | Cambia la regla de un paso por ciclo (`ck_paa_un_paso`) |
| P4 | Que D8 reconozca `MANTENIMIENTO` como origen de reducción (exención de V20 al revertir a HIST) | L9 | Solo afecta a la reversión |
| P5 | Reincorporación con REV2: seguir buscando hacia atrás tras un retirado ya devuelto, de uno en uno y dentro de la reserva R2 | L10 | Afecta a todas las políticas si se generaliza |
| P6 | Reclasificar 775, 1261 y 1308; revisar los ejercicios de tracción con banda o en suspensión para casa | L4, L5 | Migración de catálogo con revisión visual |
| P7 | Ante un ejercicio desactivado en un plan publicado: reparación segura con sustituto equivalente estricto (las mismas reglas que P-A2) | L7 | Nuevo flujo |
| P8 | Tamaño de la dosis de mantenimiento P-A1 (3×10 → 2×10, −33 %). La evidencia da −33 a −50 % durante 1 semana | — | — |
| P9 | Fatiga persistente: avisos a las 2 semanas (revisar hábitos) y a las 3 (consultar a un profesional). Revisar los textos y los umbrales | — | — |
| P10 | Escala de impacto (BAJO / MODERADO / ALTO / DESCONOCIDO, por `high_impact` y palabras clave) y el «desconocido = no se ofrece sustituto» | P-C2 | — |
