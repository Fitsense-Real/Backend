# 01 · Factibilidad de la retirada localizada en los siete perfiles

Esta evaluación se hizo **antes de implementar**, con el laboratorio opt-in `RetiradaLocalizadaFactibilidadIT` (commit `d4a05c9`). No cambia producción.

Condiciones:
- Mismos perfiles, semana (2026-11-02), ids y semillas congelados del laboratorio de 22 perfiles.
- Para cada perfil, primero el flujo real con el límite: B (`on`) y C (`preferencia`), con resultados **idénticos** entre sí.
- Después, sin publicar nada, se toma la **misma copia** de la semana medida que haría el respaldo, con SUST-SEG-1 real. De ella se retira **solo** el 135, sin sustituto, y el estimador recalcula los minutos de esa sesión.
- Esa semana pasa por los validadores **reales**:
  - V8C de continuidad, que antes de la implementación no conocía la retirada: sus objeciones se separan de las demás;
  - la validación estricta completa (V1–V25, V20, V24/4D, V23).

Evidencia: `evidencia/factibilidad_retirada_{B,C}.csv` (los 21 perfiles).

## Los siete perfiles sin plan con B y C

| Perfil | Objetivo · nivel · días · min | Ejercicios de la sesión del 135 | Minutos | V20 (duración / mínimo) | V24 / 4D | Mínimo de ejercicios | Estructural | Slot cubierto en la semana | Trabajo semanal (reps) | Otras sesiones | Veredicto |
|---|---|---:|---:|---|---|---|---|---|---|---|---|
| **R0521** | GF · intermedio · 6 · 30 | 5 → 4 | 34 → 27 | ✔ | ✔ | ✔ | sí (UPPER_PULL) | ✔ | 774 → 756 | sin cambios | **VÁLIDA** |
| R0524 | GF · intermedio · 6 · 75 | 8 → 7 | 54 → 47 | ✘ 2.841 s / 3.150 s (faltan 309 s) | ✔ | ✔ | sí | ✔ | 1.334 → 1.316 | sin cambios | **RECHAZADA: V20** |
| R0527 | GF · avanzado · 6 · 60 | 7 → 6 | 45 → 38 | ✘ 2.301 s / 2.520 s (faltan 219 s) | ✔ | ✔ | sí | ✔ | 1.304 → 1.286 | sin cambios | **RECHAZADA: V20** |
| R0545 | GM · intermedio · 7 · 30 | 4 → 3 en **dos** sesiones | 24 → 19 · 26 → 21 | ✘ 1.110 s / 1.260 s y 1.230 s / 1.260 s | ✔ | ✔ | sí | ✔ | 682 → 622 | **el 135 también está en una sesión NO señalada** | **RECHAZADA: sesión no señalada** (y V20 en las dos) |
| R0570 | GF · intermedio · 7 · 45 | 5 → 4 | 34 → 27 | ✘ 1.599 s / 1.890 s (faltan 291 s) | ✔ | ✔ | sí | ✔ | 860 → 842 | sin cambios | **RECHAZADA: V20** |
| **R0573** | GF · avanzado · 7 · 30 | 5 → 4 | 34 → 27 | ✔ | ✔ | ✔ | sí | ✔ | 848 → 830 | sin cambios | **VÁLIDA** |
| R0574 | GF · avanzado · 7 · 45 | 6 → 5 | 36 → 31 | ✘ 1.869 s / 1.890 s (faltan **21 s**) | ✔ | ✔ | sí | ✔ | 948 → 894 | sin cambios | **RECHAZADA: V20** |

Aspectos comunes a los siete:
- **Dosis y descansos:** ninguno cambia; solo desaparece el 135.
- **Segundos de trabajo:** iguales.
- **Exclusiones por dolor:** el 135 sigue `ACTIVA`.
- **Linaje:** cada sesión conserva su `source_planned_workout_id`.
- **V8C:** objeta solo la retirada no declarada («desaparecen los ejercicios [135]» y la bajada de minutos). Es lo que la implementación tenía que autorizar, con condiciones (02).

**Conclusión de la fase 3: hay casos válidos.** De los siete, 2 (R0521 y R0573) pueden publicarse retirando solo el 135 sin incumplir ninguna restricción vigente. Los otros 5 no:
- **4 los rechaza V20:** la sesión sin el 135 queda por debajo del 70 % de los minutos declarados del perfil, a veces por muy poco (21 s). **No se toca V20.**
- **1 (R0545) tiene el 135 también en una sesión no señalada**, y la retirada no modifica sesiones no señaladas. Además, ambas sesiones incumplirían V20.

La retirada solo pasa V20 en los perfiles de **30 minutos**, donde quitar un ejercicio deja la sesión por encima del mínimo.

## Los 14 perfiles con regeneración P-03

| Veredicto | Perfiles |
|---|---|
| **VÁLIDA** | 1: **R0561** (FU · avanzado · 7 · 30), 32 → 25 min |
| RECHAZADA: V20 | 12 |
| RECHAZADA: sesión no señalada | 1: R0536, que además incumple V20 en las dos sesiones |

## Hallazgo de diseño

**El 135 es una colocación ESTRUCTURAL (slot UPPER_PULL) en los 21 perfiles.** Retirarlo sin sustituto quita una colocación estructural de su sesión. En los 21 el slot sigue cubierto por otro ejercicio de la semana (sesiones PULL), y V23 y la cobertura 4C lo aceptan.

La orden dice «no se elimina una colocación estructural obligatoria sin autorización». La implementación lo trata así:
- solo el **dolor registrado** autoriza la retirada;
- V8C exige que el slot siga cubierto en la semana; si no, la rechaza (prueba `slotSinCobertura`).

**Esta regla requiere tu aprobación expresa** (10, decisión 2).
