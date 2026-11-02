# Implementación de C2-D

**Versiones:** `PLAN-C2D-0.1` y `PLAN-C2D-V4-0.1`. Con `PLAN-HIST-1` y `PLAN-V4-0.1`, V20 es el de siempre.

## 1. Comportamiento

1. **Generación:** igual que siempre. El motor sigue intentando el 70 %, sin piso menor y sin regenerar. C2-D no toca el generador.
2. **Validación** (`PlanDraftValidator.validate`). Si la versión tiene C2-D y se cumple la condición estricta (`aplicaC2D`), se evalúa la saturación (`evaluarC2D` → `SaturacionContenidoV20`). La condición estricta es:
   - **todos** los problemas son de V20;
   - no hay orden de volumen;
   - no hay autorizaciones ni reducciones de V8C.
3. **Resultado de la evaluación:**
   - `SATURADA`: V20 deja de bloquear. El borrador se publica **sin ningún cambio**.
   - Cualquier otro estado: el plan sigue rechazado con los mismos mensajes de V20.
   - En ambos casos se registra en el log `fitsense.politica.c2d` (`C2D_EXCEPCION` o `C2D_SIN_EXCEPCION`, con estado, motivo y evaluaciones).
4. **Trazabilidad:** el plan publicado lleva `planning_policy` y, por sesión, `excepcion_c2d = true` y los segundos estimados (`PlanningPolicyTrace`).
5. **Rutas cubiertas:** todas las que validan con `PlanDraftValidator` y un contexto de esa versión:
   - generación inicial;
   - pipeline con reintentos de IA (la IA ve el mismo contrato: C2-D no cambia el snapshot);
   - semanas adaptadas sin autorizaciones;
   - **regeneración segura P-03**.

## 2. Lo que C2-D no hace

- No acepta nada que incumpla V4, V8C, V15, V18, V22, V23, V24, V25 o V1, este último con las exclusiones de dolor de P-02.
- No cambia dosis 4D, no quita ejercicios y no recorta la semana.
- No sube la dosis. La progresión no lo usa, y en P-03 sigue aplicando la regla de no añadir trabajo ante dolor.
- No crea un bypass global de V20. Sin saturación demostrada, V20 rechaza igual que antes.

## 3. Matriz de 960 perfiles con el validador real (`RESULTADOS_MATRIZ_C2D.csv`)

Mismos perfiles, orden, semillas y catálogo. **La parte histórica reproduce exactamente** la matriz anterior: user_id, resultado y segundos de la sesión más larga.

| Objetivo | PLAN-HIST-1 | PLAN-C2D-0.1 | PLAN-V4-0.1 | PLAN-C2D-V4-0.1 |
|---|---:|---:|---:|---:|
| GAIN_MUSCLE | 172 | **240** | 172 | 240 |
| INCREASE_STRENGTH | 236 | **240** | 236 | 240 |
| LOSE_WEIGHT | 230 | **234** | 230 | 234 |
| GENERAL_FITNESS | 237 | 237 | 237 | 237 |
| **Total** | **875** | **951** | **875** | **951** |
| GAIN_MUSCLE a 75 min | 0/60 | **60/60** | 0/60 | 60/60 |
| Planes distintos de la histórica | — | **0** | 0 | 0 |
| Aceptados por excepción | — | 76 | — | 76 |

**De 954 (experimental) a 951 (real): 3 perfiles menos.** Son R0040, R0328 y R0616 (GENERAL_FITNESS principiante, 1 día, 75 min).
- El verificador real añade el movimiento **SUSTITUIR**, que el laboratorio experimental no probaba.
- En los tres encuentra una sustitución válida por un ejercicio más largo: SUSTITUIR 1046 → 253, 1103 → 253 y 1263 → 1054.
- Por tanto **no están saturados** y C2-D no los acepta (`NO_SATURADA`). Es un control adicional, no un error.
- **Consecuencia:** el generador podría alargar esas sesiones con esa sustitución; hoy no lo hace. Queda como mejora del constructor, no de V20.

**Los 6 perfiles de LOSE_WEIGHT rechazados por V15 siguen rechazados.** C2-D no actúa porque V15 no es V20.

**Excepciones aceptadas: 76** (68 GAIN_MUSCLE, 4 INCREASE_STRENGTH, 4 LOSE_WEIGHT).

| % de M de la sesión más corta | 46 | 57 | 58 | 60 | 69 |
|---|---:|---:|---:|---:|---:|
| Planes | 4 | 56 | 8 | 4 | 4 |

**Coste:** de 0 a 164 evaluaciones del validador (media 26) y de 3 a 508 ms por semana (media 95). En GAIN_MUSCLE a 75 min la saturación se demuestra sin llamar al validador:
- el tope de 7 colocaciones impide añadir;
- todas las series están ya en 3;
- ninguna dosis base dura más que 3×10 o 3×13.

**V4-C en la matriz:** 0 planes del motor superan el límite en segundos. V4-C no cambia ningún resultado del motor de reglas; su efecto está en los borradores de IA (`IMPLEMENTACION_V4C.md`).

## 4. Pruebas

| Prueba | Qué comprueba |
|---|---|
| `PoliticaPlanificacionTest.condicionEstricta` | Solo con versión C2-D, solo si todo es V20; no se salta V4, V8C, V15, V18, V22, V23, V24, V25 ni V1 |
| `PoliticaPlanificacionTest.saturacion` / `noDemostrada` | SATURADA, NO_SATURADA (ANADIR) y NO_DEMOSTRADA (presupuesto o ejercicio ajeno) |
| `ImplementacionC2DV4CMatrizIT` | 960 perfiles × 4 versiones con el generador y el validador reales |
| `CicloRealC2DIT`, `CicloRealC2DV4IT` | Ciclo real de 10 semanas con la política configurada (`CICLO_REAL_P02_P03_C2D.md`) |
