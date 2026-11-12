# Generación inicial de los 10 usuarios nuevos

**Cohorte:** `FITSENSE_STRESS_P123_COHORT_02`.
**Semana 1:** 2/11/2026.
**Evidencia:** `evidencia/generacion.csv` y `evidencia/planes.csv`.

## Origen de los planes

- Los diez planes se generaron con el **motor de reglas** (`RULE_ENGINE`), en 1 intento cada uno.
- `fitsense.ai.enabled=false`: **ninguna llamada a Replicate**. El espía de `ReplicateTrainingPlanGenerator.requestOutput` confirma que no hubo ninguna invocación.
- No se usaron planes de campañas anteriores.

## Resultado por usuario

| Usuario | Objetivo y nivel | Días disponibles | Min | Elegibles | Sesiones (tope) | Sesiones: enfoque, minutos y ejercicios | Validación independiente |
|---|---|---|---:|---:|---:|---|---|
| N01 | GAIN_MUSCLE intermedio | mar, sáb | 30 | 628 | 2 (6) | FULL_BODY 26 min/4 · FULL_BODY 24/4 | VALIDA (8 dosis base) |
| N02 | GAIN_MUSCLE avanzado, casa | lun, mar, jue, sáb, dom | 60 | 327 | 5 (6) | PUSH, PULL, LEGS, UPPER y LOWER, 42–43 min/7 | VALIDA (35) |
| N03 | LOSE_WEIGHT principiante, casa, sin impacto | lun–dom (7) | 30 | 175 | **4 (4)** | UPPER y LOWER ×2, 23 min/3 | VALIDA (12) |
| N04 | LOSE_WEIGHT intermedio, 3 bloqueados | lun, mar, mié, vie, sáb, dom | 45 | 644 | 6 (6) | PUSH, PULL, LEGS ×2, 36 min/4 | VALIDA (24) |
| N05 | INCREASE_STRENGTH principiante, casa | dom | 60 | 192 | 1 (4) | FULL_BODY 43 min/8 | VALIDA (8) |
| N06 | INCREASE_STRENGTH avanzado, sin brazos arriba | lun, mié, vie, sáb | 60 | 657 | 4 (6) | UPPER 43/6 · LOWER 44/8 ×2 | VALIDA (28) |
| N07 | GENERAL_FITNESS principiante, peso corporal, sin impacto | mié | **30** | 156 | 1 (4) | FULL_BODY 28 min/6 | VALIDA (6) |
| N08 | GENERAL_FITNESS intermedio, sin rodillas ni suspensión | lun, jue, dom | 45 | 639 | 3 (6) | FULL_BODY 34–37 min/5–6 | VALIDA (17) |
| N09 | GAIN_MUSCLE intermedio | mié, dom | 30 | 628 | 2 (6) | FULL_BODY 26/4 · 26/5 | VALIDA (9) |
| N10 | LOSE_WEIGHT intermedio | mar–dom (6) | 30 | 647 | 6 (6) | PUSH, PULL, LEGS ×2, 22 min/2 | VALIDA (12) |

**Resultado: 10 de 10 planes iniciales válidos.** Los dos usuarios de la variante (N01B y N09B, fuera de la cohorte) también fueron válidos.

- **Duración:** ninguna sesión supera el techo del perfil (minutos × 1,15). La duración declarada es la del estimador real (E1 de la campaña anterior).
- **Frecuencia:**
  - N03 declara 7 días y recibe 4 sesiones, el tope de principiante.
  - Nadie recibe 7.
  - Todas las fechas caen en días disponibles y sin repetirse.
- **Bloqueos de N04:**
  - 293 (burpee), 404 (cable pulldown bicep curl) y 1133 (run).
  - Se fijaron tras inspeccionar el plan que el motor elegía sin ellos.
  - No aparecen en ninguno de sus planes.
- **Restricciones:** ningún plan contiene un ejercicio que choque con las banderas declaradas; lo comprueba el filtro del catálogo y V1.

## P-01 en la cohorte (versión definitiva, 30–120 para los cuatro objetivos)

- Los diez perfiles (30–60 min) están dentro de 30–120 y fueron admitidos.
- La generación es **idéntica** con P-01 definitiva: la campaña repetida no cambia ningún plan inicial.
- Prueba negativa separada: **N07_NEG** (GENERAL_FITNESS, 15 min). Se rechazó en el alta, antes de generar: *«La duración por sesión debe estar entre 30 y 120 minutos (objetivo GENERAL_FITNESS); indicaste 15.»* No se creó ningún perfil.

## Regresiones de generación

Se ejecutan en la batería del PR (`mvn verify -Pintegration`), con la generación de los cuatro objetivos y sus referencias:
- `GeneracionPorObjetivoIT` y las de referencia V35–V40;
- V2, V4, V15, V17, V20, V23, V24 y V25;
- 4D v3.2/v3.3 (`Adenda4Dv32IT`);
- P-01 definitiva (`PoliticaP01DuracionMinimaTest`: 15, 29, 30, 60, 119, 120, 121 y 180 × 4 objetivos, más la tolerancia V4; `PoliticasP123IT`);
- las exclusiones por dolor en una generación nueva (`PoliticasP123IT.dolorIdentificadoPersisteCincoSemanas`).

**P-01 definitiva:**
- El hallazgo de la primera versión (INCREASE_STRENGTH de 15 min sin plan, porque V4 rechaza 18 min con un techo de 17) **desaparece**: ya no se admiten 15 min.
- `PoliticasP123IT.cuatroObjetivosEnLosLimites` genera los cuatro objetivos en 30 y en 120: el techo V4 se respeta (34 y 138) y no hay sesiones iniciales bajo el piso de V20.
