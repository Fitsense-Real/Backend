# Interacción de EF-0.1 con LW-PREF-0.2

**Fuente:** `MATRIZ_EF_LW02.csv` (laboratorio `efPorLw`).
- Los 240 perfiles LOSE_WEIGHT de la matriz de 960.
- 8 configuraciones: las 7 pedidas más la referencia sin LW de la séptima.
- Generador y validador reales, PostgreSQL efímero.
- **Todo apagado fuera del laboratorio.**

**Cómo se clasifica cada plan con LW frente a su misma política sin LW:**

| Clase | Significado |
|---|---|
| SIN_BRECHA_SIN_CAMBIO | Sin brecha y sin cambio |
| REGRESION_CAMBIO_SIN_BRECHA | Sin brecha, pero el plan cambia |
| CON_BRECHA_SIN_ALTERNATIVA | Con brecha; se conserva el original |
| CORREGIDO | La brecha llega a 0 |
| MEJORADO_PARCIAL | La brecha baja, pero no a 0 |
| REGRESION | Problema nuevo, V15 nuevo, incompatibilidad EF, menos cardio o más brecha |

## 1. Resultados

| Configuración | Válidos sin LW → con LW | Sin brecha sin cambio | **Cambio sin brecha** | Con brecha sin alternativa | **Corregidos** | Mejora parcial | **Regresiones** |
|---|---|---|---|---|---|---|---|
| 1 → 2 · HIST → HIST + LW | 230 → 236 | 84 | **0** | 91 | 34 | 31 | **0** |
| 3 → 4 · EF → EF + LW | 230 → 236 | 76 | **0** | 91 | 42 | 31 | **0** |
| 5 → 6 · C2D-EF → C2D-EF + LW | 234 → 240 | 76 | **0** | 91 | 42 | 31 | **0** |
| 7a → 7 · C2D-V4-EF → C2D-V4-EF + LW | 234 → 240 | 76 | **0** | 91 | 42 | 31 | **0** |

## 2. Comprobaciones de la orden

| Comprobación | Resultado (en las cuatro parejas) |
|---|---|
| No cambian planes sin brecha real | **0** cambios en planes sin brecha |
| No se introducen incompatibilidades de patrón | **0** planes con más colocaciones incompatibles que su referencia. Con EF, 0 incompatibilidades. Con HIST + LW, las 36 que existen son las mismas 36 de la referencia histórica (sin EF, no se juzgan) |
| No aparecen V15 nuevas | **0** |
| No disminuye el cardio obligatorio | **0** planes con menos minutos de trabajo de cardio |
| No se altera la dosis 4D | **0** planes con dosis distintas (mismo multiconjunto de series × repeticiones × segundos × descanso): LW cambia **qué** ejercicio, no la dosis |
| Válido → inválido | **0** |
| Un plan con brecha no se declara corregido si la capacidad sigue sin cubrir | «CORREGIDO» solo con brecha 0. Los 31 con brecha restante quedan «MEJORADO_PARCIAL» |
| La traza identifica la preferencia | Sí: `opciones.preferencia_cobertura_lw = "LW-PREF-0.2"` en los planes LOSE_WEIGHT persistidos (ciclo real `CicloRealC2DEFLW02IT`, `TRAZA_OPCIONES_PLAN-C2D-EF-0.1_LW-PREF-0.2.csv`). En los demás objetivos no se registra |

## 3. Interpretación

- **LW-PREF-0.2 conserva sus mejoras con EF y las amplía:** 42 corregidos frente a 34.
  - Con EF, 8 planes que sin EF no tenían brecha pasan a tenerla (164 frente a 156 con brecha), porque EF retira de la semana trabajo no computable.
  - La preferencia los corrige.
- **En los 4 casos, 6 planes inválidos pasan a válidos.** La alternativa tiene menos problemas y ninguno nuevo. No es un objetivo; se registra para la revisión profesional.
- **La traza registra que la opción estaba encendida, no si cambió el plan concreto.** Saber si un plan cambió exige compararlo con la referencia, como hace este laboratorio.
  - Si hiciera falta en producción, se propone añadir `aplicada: true|false` (cambio de traza menor, `DECISIONES_PENDIENTES.md`).

## 4. Versionado

- No se introduce ninguna versión global nueva: LW-PREF-0.2 sigue siendo una opción del generador, apagada y trazada.
- **Propuesta para activarla:** incorporarla a la versión de política que se apruebe (p. ej. una `PLAN-C2D-EF-LW-0.1` en V46) en lugar de mantenerla como interruptor independiente. Así la versión del plan explicaría por sí sola su composición.
