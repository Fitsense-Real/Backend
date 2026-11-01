# Saturación de C2-D: qué comprueba y qué no demuestra

Implementación: `SaturacionContenidoV20` (dominio puro), llamada por `PlanDraftValidator.evaluarC2D`.

## 1. Criterio de saturación

Una semana rechazada **solo** por V20 está SATURADA si, para **cada** sesión por debajo del piso (70 %), **ningún movimiento único** de estos cuatro tipos la mejora sin romper otra regla:

| # | Movimiento | Detalle |
|---|---|---|
| 1 | **ANADIR** | Un ejercicio elegible con su **dosis base aprobada** (4D en GAIN_MUSCLE; primera capacidad admisible de `CapabilityPrescriptionPolicy` en los demás objetivos). Solo si cabe en el tope de V24 (6/7/8) y en el techo de V4 de la versión |
| 2 | **SERIE** | Una serie más (hasta 3) a un ejercicio de series y repeticiones |
| 3 | **SUSTITUIR** | Un ejercicio de la sesión por otro elegible cuya dosis base dura más |
| 4 | **REDISTRIBUIR** | Un ejercicio de otra sesión de la semana pasa a la sesión corta y con ello baja el número de sesiones bajo el piso |

**Qué es una «mejora».** El validador **vigente completo** (las mismas validaciones que publican el plan, sin la excepción C2-D) no encuentra ningún problema distinto de V20, y la sesión dura más; en REDISTRIBUIR, hay menos sesiones cortas.

**Filtros previos.** Se descartan sin llamar al validador:
- los candidatos ya presentes en el día;
- los presentes en el día pegado (V18);
- los resistidos fuera del enfoque;
- los que no tienen dosis base;
- los que superan el techo de V4.

Cada descarte se cuenta (`descartes`).

**No se acepta automáticamente** si no se puede demostrar la saturación:

| Situación | Estado |
|---|---|
| Faltan datos (sin prescripción, sin perfil, o un ejercicio del borrador fuera del conjunto elegible) | `NO_DEMOSTRADA` |
| Se agota el presupuesto (`PRESUPUESTO = 4.000` evaluaciones del validador por semana) | `NO_DEMOSTRADA` |
| Existe alguna mejora | `NO_SATURADA` (el plan sigue rechazado por V20) |
| V20 no aplica (orden de volumen o sin piso) | `NO_APLICA` |

**Solo `SATURADA` levanta V20.**

## 2. Condiciones previas (`PlanDraftValidator.aplicaC2D`)

C2-D solo se evalúa si se cumplen las cuatro:
1. La versión de la política tiene C2-D.
2. Hay problemas y **todos** empiezan por `V20:`. Con cualquier otro código no actúa: V4, V8C, V15, V18, V22, V23, V24, V25 o V1 (exclusiones por dolor de P-02).
3. No hay una orden de volumen activa (D8 ya exime de V20).
4. El `ConservationReport` no trae autorizaciones, reducciones de V8C ni problemas. Lo autorizado se gestiona con su propia política.

Además:
- **No se modifica el borrador:** se publica tal como lo construyó el motor al 70 %.
- No se cambian dosis ni se eliminan ejercicios.
- La semana no se recorta.

## 3. Lo que demuestra y lo que no

| Se demuestra | No se demuestra |
|---|---|
| **Saturación local comprobada:** ningún movimiento único de los cuatro tipos, con el catálogo elegible y las reglas vigentes, mejora la sesión corta | **Optimalidad global:** no se prueban combinaciones de varios movimientos ni recomposiciones completas de la semana |
| Que las dosis son las aprobadas: los movimientos usan la dosis base, y el validador rechaza cualquier otra | Que la sesión sea **adecuada** para el objetivo. Eso es una valoración profesional, pendiente |
| Que la excepción queda trazada (`excepcion_c2d` por sesión) | Que una sustitución de varias piezas no permitiría llegar al 70 % |

## 4. Alcance práctico

- El constructor vigente **ya intenta** antes ampliar series, sustituir por duración y reubicar entre días (`CapabilityWeekBuilder`).
- La saturación vuelve a comprobarlo de forma independiente, con el validador, en el momento de aceptar.
- En la matriz de 960 perfiles se registran el estado, el motivo y las evaluaciones de cada caso (columnas `c2d_estado`, `c2d_evaluaciones` y `c2d_motivo` de `RESULTADOS_MATRIZ_C2D.csv`).
