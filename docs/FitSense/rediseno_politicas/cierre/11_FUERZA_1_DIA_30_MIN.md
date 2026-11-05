# 11 · Fase 2: los cuatro perfiles de fuerza de 1 día y 30 min

**Perfiles:** INCREASE_STRENGTH intermedio y avanzado, en gimnasio y en casa con mancuernas, **1 día de 30 min**.
**Estado:** limitación **legítima**, conservada. **CORREGIDA** su comunicación (alternativa C). Hay **una incoherencia de cálculo demostrada** en V25 cuya corrección necesita una decisión (§3).

## 1. Qué pasa exactamente

| Paso | Dato |
|---|---|
| Dosis de fuerza (4D, CapabilityPrescriptionPolicy) | 3 × 6, 150 s de descanso, 3 s por repetición → **354 s** por ejercicio |
| Calentamiento y transición | 300 s; 60 s entre ejercicios |
| 4 patrones (4C: rodilla, cadera, empuje, tracción) en una sesión | 300 + 4 × 354 + 3 × 60 = **1.896 s (31,6 min)** |
| Límite de V4 | HIST: 30 × 60 × 1,15 = 2.070 s (caben). **REV2: 1.800 s (no caben)** |
| Generador (REV2) | Coloca 3 patrones (1.482 s) y deja el 4.º fuera por tiempo |
| V25 | Exige `min(slots factibles, sesiones × por_sesión)`, con `por_sesión = 3 si M < 30, si no 4`. Con 30 min exige **4**, así que rechaza |
| Casa con peso corporal (mismo objetivo, nivel y tiempo) | Válido con 3 ejercicios: sin tracción elegible, V25 solo exige 3 |
| Antes de `f95e1f7` | La persona recibía un 422 con texto técnico («Ultimos problemas: V25: el slot …») |

## 2. Alternativas medidas

| | Dosis por patrón | Patrones/semana | Duración | Series de fuerza/semana | Descanso | Dificultad | Estímulo y calidad | ¿Requiere autorización? |
|---|---|---|---|---|---|---|---|---|
| **A** · 4 patrones × 2 series | 2 × 6 | 4 | 1.224 s (20,4 min) | 8 | 150 s | Igual | Por debajo de la dosis 4D de fuerza. Una vez por semana con 2 series es una dosis de mantenimiento; la evidencia (`02` §1, N2) describe 1 serie 2–3 veces por semana como eficaz pero subóptima. **Baja la dosis para hacerla caber** | **Sí** (dosis [PRO]) |
| **B** · 3 patrones, 4.º pendiente | 3 × 6 | 3 | 1.482 s (24,7 min) | 9 | 150 s | Igual | Con 1 solo día **no hay otra sesión**: el 4.º patrón no se entrenaría nunca, salvo una rotación semanal que hoy no existe (sería un diseño nuevo). En fuerza general, dejar fijo sin trabajar un patrón principal no es adecuado | **Sí** (cambia V25 / diseño) |
| **C** · Informar y ofrecer disponibilidad | 3 × 6 | 4 (con más tiempo o días) | — | 12 | 150 s | Igual | Mantiene la prescripción completa. Con **32 min** (1 día) o **2 días de 30 min** (3 + 3 colocaciones) el plan es válido con los 4 patrones (`FuerzaSinTiempoIT`) | No: es comunicación |
| **D** · Más tiempo con aceptación explícita | 3 × 6 | 4 | 1.896 s (31,6 min, +5,3 %) | 12 | 150 s | Igual | Prescripción completa, pero supera lo declarado. Requiere un flujo de aceptación explícita que hoy no existe | **Sí** (excede M; flujo nuevo) |
| (E, solo informativa) descanso de 120 s | 3 × 6 | 4 | 1.656 s | 12 | **120 s** | Igual | Cabría, pero cambia el descanso de la dosis de fuerza ([PRO]). **No se propone** | Sí |

## 3. Decisión

**Se adopta C**, implementada en `f95e1f7` sin cambiar V25, la dosis ni V4:

- `CoberturaFuerzaTiempo` calcula, antes de generar, el mínimo de una cota inferior: los ejercicios más cortos de cada slot con la dosis de fuerza, repartidos como exige V25. Nunca marca un caso que sí cabe.
- **Primer plan:** `FuerzaSinTiempoException` (`FUERZA_SIN_TIEMPO`, HTTP 422): «con 1 sesión de 30 minutos no caben los 4 patrones de fuerza de tu objetivo con la dosis de fuerza (se necesitan unos 32 minutos por sesión). No acortamos la dosis ni quitamos patrones para que quepa. Puedes indicar 32 minutos o más por sesión, o más días por semana.»
- **`GET /users/me/plan/limitations`:** el mismo aviso, `TIME / FUERZA_SIN_TIEMPO`, antes de generar.
- **HIST:** no salta nunca (con su tolerancia caben), y el comportamiento histórico es idéntico.
- **Pruebas:** `FuerzaSinTiempoIT`, con HIST y con REV2:
  - los 4 perfiles: REV2 informa, HIST genera;
  - REV2 genera con 32 min, con 35 min y con 2 días de 30 min;
  - REV2 genera en casa con peso corporal.

**Incoherencia de cálculo de V25 (demostrada, sin corregir):** `por_sesión = 3 si M < 30, si no 4` supone la tolerancia histórica de V4. Con REV2, a 30 min solo caben 3 colocaciones de fuerza. Alinear V25 con el límite real de V4 haría válidos estos 4 perfiles **con la alternativa B**, dejando un patrón sin trabajar ninguna semana. Es una decisión deportiva, así que **queda pendiente de tu aprobación (P12)** y no se aplica.

**Recomendación deportiva (pendiente de validación profesional):** C como comportamiento por defecto. Si se quiere ofrecer algo a quien no puede más tiempo, D (con aceptación explícita) es preferible a A o B, porque conserva la prescripción.
