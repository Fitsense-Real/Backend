# Propuesta de política fundamentada

**Estado:** propuesta. **Nada de lo que sigue está activado, fusionado ni desplegado.** Cada punto requiere aprobación (`DECISIONES_QUE_REQUIEREN_APROBACION.md`).

**Principio:** conservación fundamentada.
1. Primero evidencia.
2. Después decisiones.
3. Por último, cambios controlados y versionados, sin efecto retroactivo.

## 1. Lo que no cambia

- **4A v6, 4C y todas las dosis 4D.**
- **V4**, sin modificar. Su incoherencia `floor`/`round` queda como decisión aparte.
- **V24** en la ruta histórica.
- **P-01 30–75**, P-02 y P-03.
- **D8, C-2 y la exención V20 por ACORTAR**, junto con el equipamiento, los bloqueos y la recuperación.
- **La progresión:** D-P3, 5 % por paso, techo de 1,10 × V_base (D-P5) y banda del 60–125 %.
- **La reincorporación.**
- **El perfil congelado y M.**
- **La generación inicial histórica**, para los planes y versiones existentes.

## 2. Propuesta por niveles

### Nivel 1: documentación (sin cambio de comportamiento)

- Etiquetar como **decisión de ingeniería** las reglas C:
  - topes 6/7/8;
  - 85 %;
  - 70 %;
  - ×1,15;
  - transición de 60 s;
  - V18;
  - mínimos;
  - banda del 60–125 %.
- Textos de la nota de fuerza: «pocas repeticiones con un peso que deje 1–3 repeticiones en reserva» en lugar de «alta intensidad» [S39, S10]. **Afecta al texto que ve el usuario; requiere aprobación.**

### Nivel 2: métricas informativas (sin cambio de plan)

1. **Minutos aeróbicos equivalentes por semana** (moderado × 1, vigoroso × 2) en LOSE_WEIGHT y GENERAL_FITNESS. Se avisa al usuario o al profesional si no llegan a 150 [S04, S05].
2. **Series semanales por grupo grande** (`VolumenSemanal`), solo como métrica. **No se proponen mínimos universales.**

### Nivel 3: evolución mínima C2 (cambio de comportamiento acotado)

- **Política** `PISO_V20_ACOTADO` (nombre provisional), en una versión nueva de `calculation_configs`:
  - solo cuando **V20 es la única regla incumplida**, el piso baja en pasos de 5 desde el 70 % hasta un mínimo por objetivo;
  - en el laboratorio bastó un mínimo del 50 %; para GAIN_MUSCLE a 75 min hizo falta el 55 %.
- **Efecto medido:** 875 → 950 planes válidos de 960; 925 idénticos.
- **Salvaguardas:**
  - no se aplica a sesiones acortadas, que ya están exentas por D8;
  - no autoriza a saltarse V8C ni V24;
  - registra el piso usado en el plan.

### Nivel 4: ajustes de dosis (solo tras revisión profesional)

| Ajuste | Evidencia | Por qué no se aplica ya |
|---|---|---|
| Permitir 180 s en básicos pesados de intermedio y avanzado | [S06, S08]; el ACSM 2026 no encuentra efecto del descanso [S01] | Mejora opcional. Cambia la dosis 4D y la duración |
| Alargar el cardio de LOSE_WEIGHT | [S04, S05, S32] | Cambia la dosis 4D. Puede reabrir V20 y V24 y quitar tiempo de fuerza |
| Fortalecer todos los grupos grandes ≥ 2 d/semana en LOSE_WEIGHT | [S05] | Cambia la obligación semanal (4A/4C) |
| Registrar el RIR de la última serie en los ejercicios principales | [S37, S38] | Requiere una columna nueva, la API y Flutter, que no se modifica en esta orden |
| Resolver 3×5 frente al mínimo de 6 repeticiones | Incoherencia interna | Afecta al validador (T10/T11) |

### Nivel 5: modelo E (experimental)

- Se mantiene **desactivado** (`ParametrosModeloE.habilitado = false`, versión `E-0.1-EXPERIMENTAL-SIN-APROBAR`) y aislado de la ruta de producción, lo que comprueba una prueba.
- Solo pasaría a candidato tras:
  - aprobar sus parámetros;
  - sustituir V20 y V24 **únicamente en su versión**;
  - adaptar el prompt de la IA;
  - superar una batería espejo.

## 3. Por qué esta propuesta y no otra

- **Conserva todo lo que tiene clase A, B o C** (41 de 42 reglas).
- **Ataca la única E (LW2) sin cambiar la dosis**: primero se mide; el cambio de dosis queda para la revisión profesional.
- **Resuelve el problema operativo de V20** con el cambio menor demostrado (C2), sin tocar V24 histórico.
- **No convierte un preprint en norma** (techo por sesión) y **no inventa mínimos semanales universales**.
