# Impacto en la factibilidad (matrices sobre el commit candidato `836a1f5`)

Clasificación de cada desenlace:

- **PLAN_VALIDO:** publica y la semana valida.
- **NO_FACTIBLE_LEGITIMO:** no publica, con un motivo de factibilidad del catálogo o de la política, y no se publica nada inseguro.
- **ERROR_TECNICO:** excepción no prevista.
- **VIOLACION_SEGURIDAD:** un contenido o un ejercicio incompatible en lo publicado o en el conjunto elegible.
- **REGRESION_INESPERADA:** se pierde un plan sin un motivo legítimo.

## 1. Resumen

| Matriz | Perfiles / escenarios | PLAN_VALIDO | NO_FACTIBLE_LEGITIMO | ERROR_TECNICO | VIOLACION_SEGURIDAD | REGRESION_INESPERADA |
|---|---|---|---|---|---|---|
| Rejilla de 168 (contención encendida) | 168 | **168** | 0 | 0 | 0 | 0 |
| Rejilla de 168 (contención apagada, referencia) | 168 | 168 | 0 | 0 | — | — |
| Rejilla de 312 de hipertrofia (regresión) | 312 | **298** | **14** (V20; casa con solo peso corporal, 775 con banco; anterior a esta versión) | 0 | 0 | 0 |
| Referencia de 72 (conservadora) | 72 | **72** | 0 | 0 | 0 | 0 |
| HALL-GM-01, protecciones encendidas | 40 | **35** (0 aumentos) | **5** (V24 + P-03 «sube el trabajo») | 0 | 0 | 0 |
| HALL-GM-01, protecciones apagadas (referencia #65) | 40 | 36 (0 aumentos) | 4 | 0 | 7 participantes con trabajo registrado en lo que dolió (lo que corrige P-DOLOR-PUB) | — |
| Casos de seguridad, dolor, adaptación, progresión y reincorporación | Regresión completa (753 pruebas de integración) | Todas pasan | — | 0 | 0 | 0 |

Evidencia:

- `evidencia/rejilla_168/` (las dos rejillas e `impacto_168.tsv`, perfil a perfil);
- `evidencia/rejilla_312/`;
- `COMPARACION_72_PLANES.xlsx`;
- `evidencia/hall_gm_01/`.

**Determinismo:** las rejillas y los 40 escenarios se repitieron sobre `836a1f5` con un resultado idéntico al de las ejecuciones previas con el mismo código de producción.

## 2. Rejilla de 168

**Perfiles:** 4 objetivos × 3 niveles × gimnasio/casa × 7 restricciones (LIBRE, SIN_COLGARSE, SIN_CABEZA, SIN_SUELO, SIN_RODILLAS, SIN_IMPACTO, SIN_TODO). Cada perfil tiene 4 días (lunes, miércoles, viernes y sábado) y 45 minutos.

**Resultados:**

- Contención encendida: **168/168 con plan válido**. Contención apagada: 168/168.
- **Pérdida de cobertura: ninguna.** Los 4 perfiles LOSE_WEIGHT de casa que se perdían con el 853 contenido se recuperan con V56 (`REVISION_853.md`).
- El conjunto elegible cambia en los 168 perfiles: el 1311, entre otros, era elegible para todos. El plan cambia en 163. Siguen igual 1 de INCREASE_STRENGTH y los 4 de LOSE_WEIGHT/HOME que dependen del 853.
- **Contenidos en el plan o en el conjunto elegible con la contención: 0** en los 168.

**Lo que la contención retira de los planes que se generarían sin ella:**

| Contenido usado sin la contención | Planes de la rejilla |
|---|---|
| Identidad no determinada (19) | 20 |
| Apoyo suspendido (15) | 9 |
| 1113 en principiantes | 7 |

**Restricciones combinadas (SIN_TODO):** 24/24 con plan válido.

## 3. Casos específicos

| Caso | Resultado | Clasificación |
|---|---|---|
| **Los 4 LOSE_WEIGHT (853)** | Con el 853 contenido: SIN_PLAN (V5 + V25, sin cardio suficiente). Con V56: **4/4 PLAN** (`REVISION_853.md` §4) | PLAN_VALIDO |
| **Los 14 GAIN_MUSCLE (775)** | Siguen sin plan los mismos 14 de casa con solo peso corporal (3 días: 58–60 y 74–75 min intermedio, 60 y 75 principiante y avanzado; 4 días: intermedio). Motivo V20: sin el 775 (que exige banco), el contenido no llega al mínimo. `Hipertrofia4Dv34IT` exige que sean exactamente estos y solo por V20 | NO_FACTIBLE_LEGITIMO (anterior a esta versión) |
| **Colgarse y apoyo suspendido** | Los 15 de apoyo suspendido no se recomiendan a nadie sin el permiso confirmado (V53-B apagado). Con «no colgarse», `requires_hanging` sigue fuera. `PermisoApoyoSuspendidoIT#conContencionConservadora`: sin permiso, ninguno; confirmado (si se activara #66), los 10 | Sin pérdida de cobertura en la rejilla (9 planes cambian de ejercicio) |
| **Principiantes (1113)** | No se recomienda a principiantes ni a quien no declara nivel. 7 planes de principiante lo usaban; todos siguen con plan | PLAN_VALIDO |
| **Restricciones combinadas** | SIN_TODO 24/24 con plan. `ContencionTextoBanderaIT` C (sin colgarse, brazos arriba ni suelo): nada ambiguo de esas tres restricciones | PLAN_VALIDO |
| **Reparación con linaje (`ReparacionLinajeIT`) con la contención** | NO_VALIDA: el sustituto del 1374 (104, relleno de zona pequeña, 2x13) no puede recibir más que los 2x10 del quitado (V8C). No se publica nada | NO_FACTIBLE_LEGITIMO (pérdida de factibilidad acotada, `RIESGOS_RESIDUALES.md` R12) |
| **Reparación del plan 78** | REPARADA; sustituye el 1303 (inseguro) y el 494 (contenido), sin más dosis | PLAN_VALIDO |
| **HALL-GM-01** | 5 NO_FACTIBLE con la contención (antes 4). Otros participantes, porque cambia el conjunto elegible. Ninguna semana publicada sube el trabajo, ni total ni por colocación | NO_FACTIBLE_LEGITIMO |

## 4. Pérdidas de cobertura

- **Generación inicial:** **ninguna** en las rejillas de 168, de 312 ni en la referencia de 72 respecto a la versión sin contención.
- **Adaptación y reparación:** pérdidas puntuales y conservadoras, porque hay menos alternativas para sustituir:
  - HALL-GM-01: 5 NO_FACTIBLE de 40, frente a 4 sin la contención (otros participantes);
  - el escenario de reparación con linaje: NO_VALIDA.
- En ningún caso se publica algo inseguro ni con más trabajo.

## 5. Restricciones que no se relajaron

V15, V20, V24, V25, V8C, D8 y P-03 están intactas: no se cambió ningún validador ni ninguna política.

- Las rejillas validan con el validador real.
- Los NO_FACTIBLE nombran a V20, V24 o P-03 como motivo.
- V53, V53-B, #64 y la regla P-03 por colocación siguen apagados.
