# Inventario de las reglas actuales (generación de sesiones y semanas)

Base: `generation` después de fusionar el PR #16, con P-01 en 30–75.

**Cómo se indica el origen de cada regla:**
- **Origen** sale del código, de las migraciones o de los documentos del repositorio. Nunca se supone.
- Si no se encuentra por qué se adoptó una regla, se marca **JUSTIFICACIÓN NO DOCUMENTADA**.
- «Spec externa» significa que la regla cita un documento de especificación (Step 4A/4C/4D, §19–20) que **no está en el repositorio**. Su justificación no se puede auditar desde aquí.

**Estados:**
- **F:** fundamentada (cita una fuente verificable).
- **D:** discutible (decisión de diseño explícita, «sin calibrar» o «provisional»).
- **SE:** sin evidencia suficiente o justificación no documentada.

## 1. Número de ejercicios (colocaciones)

| Regla | Archivo · símbolo | Valor | Alcance | Origen | Justificación declarada | Dependencias | Riesgo al modificarla | Evidencia | Estado |
|---|---|---|---|---|---|---|---|---|---|
| Tope 4D (V24) | `MvpDosePolicy.MAX_COLOC_DIA` | 6 | GAIN_MUSCLE, < 60 min | Spec externa «4D v2 §5» | «Se aplica SIEMPRE, incluso si el tiempo sobra» | V20, presupuesto 85 % | Volumen por sesión, duración | `PlanDraftValidator` V24 | SE |
| Tope 4D sesión larga | `MvpDosePolicy.MAX_COLOC_DIA_SESION_LARGA`, `MINUTOS_SESION_LARGA` | 7 (≥ 60 min) | GAIN_MUSCLE | `ADENDA_4D_v3_1.md` (24/10/2026) | **Parche de factibilidad**: «con seis colocaciones el contenido no llega al mínimo de V20» | V20 | Ídem | `ADENDA_4D_v3_1.md`, campañas | D: es consecuencia de V20, no un criterio de entrenamiento |
| Tope de capacidades (V24) | `CapabilityPrescriptionPolicy.MAX_COLOC_DIA` | 8 | INCREASE_STRENGTH, LOSE_WEIGHT, GENERAL_FITNESS | Comentario: «el de V20 del motor de reglas»; hereda `RuleBasedTrainingPlanGenerator.MAX_EXERCISES_PER_SESSION = 8` | Ninguna científica | V20, V25 | Ídem | — | SE: protección técnica del motor convertida en regla de validación |
| Colocaciones iniciales del motor | `RuleBasedTrainingPlanGenerator.EXERCISES_PER_SESSION` / `EXERCISES_SHORT_SESSION` / `SHORT_SESSION_MINUTES` | 5 / 4 / < 30 min | GAIN_MUSCLE (ruta 4D) | — | — | V20 (se añade después hasta el piso) | Bajo: es heurística | — | SE (heurística) |
| Principales por sesión | `CapabilityWeekBuilder.resistidosPrincipales` | INCREASE_STRENGTH 4 (3 si < 30); GENERAL_FITNESS 3 (2 si < 30); LOSE_WEIGHT 0 | Capacidades | — | — | V25, V23 | Composición | — | SE (heurística) |
| Mínimo de ejercicios | `PlanDraftValidator.MIN_EXERCISES_PER_SESSION`, `VolumeReducer.MIN_EXERCISES` | 2 | Todos | §19.3 (spec externa) | — | Reducción de volumen | — | — | SE |
| Cobertura semanal de 4 patrones (V23) | `validateWeeklySlotCoverage`, `SlotCoveragePolicy` | 4 slots (empuje, tracción, rodilla, cadera) | Todos salvo LOSE_WEIGHT (4A v6) | Step 4C §5 (spec externa) | Cubrir los patrones fundamentales en la semana | V25-IS, generación | Equilibrio de patrones | `SlotCoverageGate`, IT | D (coherente con guías de patrones; sin cita) |
| Fuerza en los 4 slots (V25) | `PlanDraftValidator` V25 INCREASE_STRENGTH | 1 colocación de fuerza por slot y semana | INCREASE_STRENGTH | 4A v6 | — | V23 | — | IT | D |
| Principales en la semana (V25) | `PlanDraftValidator` V25 | ≥ 1 por capacidad principal factible; LOSE_WEIGHT: cardio en cada sesión (26/10/2026, plan 55) | Capacidades | `GoalCapabilityPolicy` (4A v6) | — | V20 (obligaciones semanales) | Hace imposibles ciertas duraciones (§ obligaciones) | IT | D |
| Grupos por enfoque y regla de la mitad (V15) | `validateFocusCoverage` | ≥ min(2, disponibles, trabajo) grupos; ningún grupo > mitad si ≥ 3 grupos y ≥ 4 ejercicios | Todos | Evaluación 28/10/2026 (`EVALUACION_GENERACION_4_6_DIAS.md`) | Evitar sesiones de un solo grupo | Generación | Variedad | IT | D |
| Rellenos 4D | `MvpDosePolicy.SERIES_RELLENO`, `FRAC_SECUNDARIA_OBJETIVO` | 2 series; ≈ 25 % secundaria | GAIN_MUSCLE | 4D v2/v3 | — | Presupuesto | — | — | SE |
| Recuperación entre días pegados (V18) | Generadores (`bloqueados`, `RECUPERACION`) y validador V18 | Sin repetir ejercicio el día siguiente; pecho, espalda, hombros y piernas solo si el día anterior no los trabajó (complementos) | Todos | §19 (spec externa) | Recuperación 48 h | Composición | Recuperación | IT | D (coherente con ACSM 48 h; sin cita en el código) |
| Sesiones por semana | `PlanGenerationContext.MAX_SESSIONS_BEGINNER/OTHERS` | 4 / 6 | Todos | — | — | V2 | Recuperación | IT de frecuencia | SE |

## 2. Duración

| Regla | Archivo · símbolo | Valor | Origen | Justificación declarada | Estado |
|---|---|---|---|---|---|
| P-01 (admisión) | `GoalType.MIN/MAX_SESSION_MINUTES`, `DuracionPorObjetivoPolicy` | 30–75 min | Decisión funcional (orden de fusión del PR #16) | Decisión de producto | Decisión |
| V4 techo | `UserProfile.maxSessionMinutesAllowed` | floor(min × 1,15) | §19.3 | Tolerancia superior | D |
| **V20 piso** | `PlanDraftValidator.minimumSessionSeconds`; `session_minutes_floor_pct` | **70 %** de los minutos, en segundos | **V12 (MVP-1.1)**: «Una sesión declarada de 45 minutos salió con 25». Marcada **«SIN CALIBRAR»** | Coherencia entre lo declarado y lo planificado; no hay criterio de entrenamiento | **D (sin fundamento científico declarado)** |
| Exención D8 | `ConservationValidator`, `fechasConDuracionReducible` | Sin V20 en sesiones acortadas autorizadas (ACORTAR) y su linaje | D8 + C-2 | Adaptación legítima | F (diseño explícito) |
| V20 desactivada con orden de volumen | `minimumSessionMinutes` (ajuste activo → 0) | — | V14 | Reducciones de volumen | D |
| Estimador (V17) | `SessionDurationEstimator` | calentamiento + Σ(trabajo + (s−1)·descanso) + (n−1)·transición | §19.3 V17 | Reproducible | D |
| Calentamiento | `warmup_minutes` (V14) | 5 min | V14: **«estimaciones del desarrollador, SIN CALIBRAR»** | — | D |
| Segundos por repetición | `seconds_per_rep` (V14); `MvpDosePolicy.SEG_POR_REP` | 3 s | V14 «sin calibrar»; 4D: «DISEÑO, es D6 y no está decidida» | — | D |
| Transición | `transition_seconds` (V14) | 60 s | V14 «sin calibrar» | — | D |
| Tolerancia de V17 | `duration_tolerance_pct` (V14) | 20 % | V14 | — | D |
| Descanso máximo | `max_rest_seconds` (V20) | 180 s | V20: «tope de los 2–3 min de ACSM 2009 para multiarticulares»; **evita cumplir V20 inflando descansos** | F (ACSM 2009) |
| Presupuesto 4D | `MvpDosePolicy.FRACCION_UTIL` | 85 % de los minutos | Spec externa «4D v2 §5» | — | SE |

## 3. Dosis y volumen

| Regla | Archivo · símbolo | Valor | Origen | Estado |
|---|---|---|---|---|
| Hipertrofia 4D (GAIN_MUSCLE) | `MvpDosePolicy.Capacidad.HYPERTROPHY`, `ClaseDosis` | Estructural 3×10/90 s; zona pequeña +3 repeticiones (3×13); relleno 2×10/90 (zona pequeña 2×13, hasta 3 para V20; bíceps en PULL hasta 3) | 4D v2 §2 y v3.2/v3.3 (spec externa y adendas) | D (rangos coherentes con ACSM 2009: 8–12 repeticiones, 1–3 series, 1–2 min de descanso; valor fijo sin cita) |
| Fuerza | `CapabilityPrescriptionPolicy.dosis` | Principiante 2×8/120; resto 3×5/150 (las repeticiones se ajustan al mínimo de zona: 3×6) | «4D v2 §2» + ACSM 2009 para principiantes | F parcial (ACSM 2009) |
| Resistencia muscular | ídem | Principiante 2×15/45; resto 3×18/45; isométricos 3×40 s/45 | 4D v2 §2 | D |
| Cardio | ídem | 3 × 90 s (principiante) / 120 s, descanso 60 s | 4D v4 | SE (no se valida la intensidad; no hay columna) |
| Movilidad / equilibrio | ídem | 2×30 s/15–30, o 2×10/30 | 4D v4 | SE |
| Ampliación de series | `SERIES_MAX_AMPLIACION` | Hasta 3, solo para llegar a V20 | «ACSM 2009: 1–3 series» | **D: el motivo de subir series es V20, no la dosis** |
| Principiantes | `TargetPrescription.MAX_SETS_BEGINNER` | 2 series (salvo GAIN_MUSCLE) | ACSM 2009 | F parcial |
| Repeticiones por objetivo (V16) | V12 `by_goal` | LOSE_WEIGHT 12–20, GAIN_MUSCLE 8–15, INCREASE_STRENGTH 4–10, GENERAL_FITNESS 8–15 | V12: «criterio estándar, **sin calibrar**» | D |
| Límites absolutos de repeticiones | V18 `rep_limits` | 6–30; mínimos de zona: lower legs 12, waist 10 (V20) | V18: máx. 30 «DECISIÓN DE DISEÑO PROVISIONAL, sin referencia»; mínimos de zona «provisional, sin referencia» | D/SE |
| Mínimos absolutos | `PlanDraftValidator.MIN_SETS/MIN_REPS/MIN_DURATION_SECONDS` | 2 series, 6 repeticiones, 20 s | §19.3 V9/V10 | SE |
| Carga (V11) | `max_load_increase_pct` | +10 % por semana; no subir carga y volumen a la vez | Comentario: «ACSM 2009» | F (ACSM 2009: 2–10 %) |
| Progresión (D-P3) | `ProgresionDosisPolicy.REPS_EXTRA` | +2 repeticiones sobre la exacta; series hasta 3 | `DISENO_PROGRESION_Y_REINCORPORACION.md` | D |
| Reducciones | `VolumeReducer` | hasta −20 %: repeticiones (piso 6); −20 a −35 %: además −1 serie (piso 2); > −35 %: además −1 ejercicio (piso 2) | §20.5 (spec externa) | D |
| RPE alto | `PlanDraftValidator.HIGH_SESSION_RPE` | 8 | §19 | D |
| Volumen por grupo muscular y semana | — | **No existe ninguna regla** | — | **Laguna**: la evidencia mide la dosis en series por grupo y semana, y FitSense no la controla |

## 4. Dependencias críticas

```
P-01 (minutos M) ──► V20: ≥ 0,70·M ──► requiere Σ duración ≥ piso
                                       │
V24 tope (6/7/8) × dosis fijas (4D, capacidades) ──► techo de duración alcanzable
                                       │
V23/V25 obligaciones semanales ──► reducen la holgura por día
                                       ▼
            Si techo < piso  ⇒  semana imposible (CASO B)
```

- Con P-01 en 30–75, el único objetivo con duraciones admitidas e imposibles es **GAIN_MUSCLE a 66–75 min**:
  - techo: 2.739 s (7 colocaciones con 3×13/90);
  - piso de 75 min: 3.150 s.
- Los demás objetivos tienen plan con gimnasio a 75 min (`MATRIZ_FACTIBILIDAD_30_120.csv`), excepto combinaciones de equipo, días o restricciones (CASO B/C) en casa.

## 5. Hallazgos del inventario

1. **El 70 % de V20 es una regla de coherencia del producto, no de entrenamiento.** Nació para que «45 min» no fuera una sesión de 25 (V12) y está declarada «sin calibrar».
2. **El 8 de capacidades no tiene origen funcional.** Es la constante técnica del motor original, convertida en regla de validación.
3. **El 7 de GAIN_MUSCLE es un parche de V20** (adenda 4D v3.1), no un criterio de volumen.
4. **El 6 de 4D y el presupuesto del 85 % citan una especificación que no está en el repositorio:** justificación no auditable aquí.
5. **Las reglas de V20 y las de V24 tiran en sentidos opuestos:**
   - V20 exige rellenar tiempo;
   - V24 y las dosis fijas impiden rellenarlo;
   - el motor y el validador tuvieron que añadir parches (series hasta 3 «solo para V20», 7 colocaciones, sustitución por duración) que **existen para satisfacer V20**.
6. **Laguna principal:** no hay control del volumen por grupo muscular y semana, que es la variable que la evidencia usa para dosificar.
