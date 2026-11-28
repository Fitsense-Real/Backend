# 03 · Diagnóstico de H-G9 (GENERAL_FITNESS con una sesión de 30–35 min)

**Hallazgo original** (`AUDITORIA_REGLAS_FITSENSE.md` §7, prioridad alta):
- GENERAL_FITNESS intermedio o avanzado con 1×30–35 min: la IA no tiene solución y se publica el motor de reglas.
- La auditoría lo calculó a mano: 2139–2265 s frente a 1800 s.
- **No había perfiles reproducibles:** el hallazgo estaba «fuera de la cohorte» (O08 es GENERAL_FITNESS de 30 min, pero con 7 días). Por eso se construyó el caso mínimo que describe la auditoría.

**Reproducción:** `HG9GeneralFitnessIT`, con catálogo, estimador y validador reales, sin red.
- Perfiles GENERAL_FITNESS en gimnasio completo y sin restricciones: principiante, intermedio y avanzado, con 30, 35, 40 y 45 min, en 1 día.
- Para cada uno, la **cota inferior** de duración de cualquier propuesta que cumpla V23 + V24 + V25: la combinación más barata de ejercicio y dosis exacta por slot, con al menos STRENGTH y MUSCULAR_ENDURANCE, más CARDIO y MOBILITY. Ignora V15, V18 y el resto, que solo pueden subirla.
- Esa propuesta mínima se pasa por `PlanDraftValidator`, y después se genera de verdad con la IA apagada.

## 1. Resultados

| Nivel | Minutos | Cota inferior | Techo V4 (REV2: M × 60) | ¿Cabe? | Validador sobre la propuesta mínima | Generación real (respaldo) |
|---|---|---|---|---|---|---|
| Principiante | 30 / 35 / 40 / 45 | 1638 s | 1800 / 2100 / 2400 / 2700 s | Sí | — | RULE_ENGINE, 28 / 32 / 36 / 36 min |
| Intermedio | 30 | **2265 s** | 1800 s | **No** | **V4** (y V15, porque la mínima no reparte grupos) | RULE_ENGINE, 27 min |
| Intermedio | 35 | **2265 s** | 2100 s | **No** | **V4** (y V15) | RULE_ENGINE, 34 min |
| Intermedio | 40 / 45 | 2265 s | 2400 / 2700 s | Sí | V15 (artefacto de la mínima) | RULE_ENGINE, 37 / 44 min |
| Avanzado | 30 / 35 | **2265 s** | 1800 / 2100 s | **No** | **V4** (y V15) | RULE_ENGINE, 27 / 34 min |
| Avanzado | 40 / 45 | 2265 s | 2400 / 2700 s | Sí | V15 (artefacto) | RULE_ENGINE, 37 / 44 min |

Los 2265 s coinciden con el extremo superior del cálculo a mano de la auditoría.

## 2. Causa comprobada

**Tipo 1: una combinación de restricciones realmente inviable para la IA.**
- En nivel intermedio o avanzado, la dosis de fuerza (3×6 con 150 s de descanso) y la de resistencia (3×18 con 45 s) son más largas que las de principiante.
- Con una sola sesión, esa sesión tiene que llevar los cuatro slots (V23), las cuatro capacidades principales (V25) y sus dosis exactas (V24).
- El mínimo resultante (2265 s) supera el techo de V4 a 30 y 35 min.
- **Ninguna propuesta de la IA puede ser válida.** No es un defecto de cálculo, del contexto ni de los reintentos, y el prompt no lo puede arreglar.

**Por qué el motor de reglas sí publica:**
- V23 está en `ENFORCE_AI_ONLY`: rechaza el borrador de la IA, pero no el del motor de reglas.
- Es una decisión documentada en `application.properties`: el motor de reglas no tiene reintento, y rechazarlo dejaría sin plan a muchos participantes cuando Replicate falla.
- El plan de respaldo pasa V1–V27 con esa exención. Por tanto, **no es prueba de que la IA funcione en estos perfiles**.

## 3. Impacto

| Dimensión | Efecto |
|---|---|
| Coste | 3 llamadas de generación perdidas en cada generación inicial de estos perfiles (unos USD 0,07, con unos USD 0,023 por llamada de generación en GRA) |
| Disponibilidad | Hasta 3 × 150 s de espera en el peor caso antes del respaldo. Siempre se publica un plan |
| Calidad | El plan es del motor de reglas, no de la IA, y no exige V23. No es inseguro: pasa el resto de validadores |
| Seguridad | Sin riesgo: el plan publicado cumple V1–V27 con la exención documentada de V23 |
| Frecuencia | Solo en la generación inicial (o tras una semana sin plan); las semanas siguientes se derivan. Solo con 1 día y 30–35 min en nivel intermedio o avanzado |

## 4. Tratamiento

- **No se cambia código.** Evitar las llamadas exigiría una **regla nueva**: una comprobación previa de factibilidad para GENERAL_FITNESS, análoga a `FUERZA_SIN_TIEMPO` de INCREASE_STRENGTH. La orden no permite introducir reglas nuevas.
- **Propuesta separada, pendiente de decisión:**
  - **(a)** una comprobación previa que, cuando la cota supere M × 60, pase directamente al motor de reglas sin llamar a la IA. No cambia lo que se publica y ahorra coste y latencia; o
  - **(b)** un mínimo de minutos (P-01) de 40 para GENERAL_FITNESS intermedio o avanzado con un solo día. Cambia la política de perfil.
- **Mientras tanto:** el respaldo existente cubre el caso, y el riesgo queda documentado.
