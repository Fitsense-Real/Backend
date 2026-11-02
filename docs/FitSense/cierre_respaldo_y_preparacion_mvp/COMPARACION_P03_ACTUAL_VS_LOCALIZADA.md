# P-03 nivel 2 actual frente a una alternativa localizada (D2)

**Solo análisis.** No se ha implementado ni modificado P-03 nivel 2. Las cifras salen de un **prototipo de medición en pruebas** (`AnalisisSustitucion1373IT.prototipoP03Localizado`, *opt-in*) que llama a las mismas piezas de producción:
- el generador determinista de P-03 nivel 2 (`PlanGenerationPipeline.runDeterministic`);
- el validador real (`PlanDraftValidator.validate`, V1–V25).

No publica nada. Código: `generation` 8f112d5. Evidencia: `evidencia/prototipo_localizado.{md,csv}` y `evidencia/analisis_1373.md`.

## 1. Cómo funciona hoy P-03 nivel 2

1. **Cuándo entra:** el ciclo semanal intenta adaptar (IA y, si falla, el respaldo con SUST-SEG-1). Si la semana queda **NO_FACTIBLE** y el motivo es **dolor** (`SEGURIDAD_ANTE_DOLOR`), `PlanningContextFacade.adaptWeeklyPlan` llama a `SafeRegenerationService.regenerar` **en el mismo ciclo**.
2. **Idempotencia:** si esa medición ya se resolvió (`safe_regeneration_attempts`, único por `weekly_metric_id`), devuelve el mismo desenlace. Si ya hay un plan activo para la semana, no duplica.
3. **Qué genera:** prepara la semana **desde cero** (`PlanAdjustment.none()`) y la genera con el **motor de reglas** (`runDeterministic`, sin IA):
   - con el conjunto elegible que ya excluye lo que dolió (P-02);
   - con la semilla de la semana y del usuario;
   - con dosis base de 4D;
   - **sin linaje** hacia la semana medida.
4. **Qué comprueba:**
   - V1–V25;
   - que **ningún excluido** aparezca;
   - que la semana **no suba ni las repeticiones ni los segundos totales** de la semana medida, por separado.
5. **Resultado:**
   - si pasa: publica el plan (`REGENERACION_P03`), registra `safe_regeneration_attempts` con PUBLICADO, y la API devuelve `REGENERACION_SEGURA_REGLAS`;
   - si no: registra NO_FACTIBLE con el motivo y **no publica**.

## 2. Alternativa localizada (propuesta, no implementada)

- Conservar las sesiones válidas de la semana medida (fechas +7, mismos ejercicios y dosis).
- Reconstruir **solo** la sesión con dolor con el motor de reglas.
- Validar la semana entera antes de publicar.

## 3. Medición

**40 semanas con dolor** en el último ejercicio del miércoles: 4 objetivos × 10 participantes, semana 2026-11-02.

En todos estos casos el nivel 1 (SUST-SEG-1) sí resolvió. La medición calcula **qué habría hecho** cada alternativa si hubiera hecho falta.

| Objetivo | N2: sesiones que cambia | N2 dentro del trabajo medido | Localizada: pasa V1–V25 | Localizada: sin excluidos | Localizada: sin más trabajo (sesión y semana) | **Localizada publicable** |
|---|---|---|---|---|---|---|
| LOSE_WEIGHT | **30/30** (12 ejercicios por semana) | 10/10 | 10/10 | 10/10 | 10/10 | **10/10** |
| INCREASE_STRENGTH | 30/30 (12) | 10/10 | 10/10 | 10/10 | 10/10 | **10/10** |
| GAIN_MUSCLE | 30/30 (17–18) | 6/10 | 10/10 | 10/10 | 8/10 | **8/10** |
| GENERAL_FITNESS | 30/30 (16) | 4/10 | 10/10 | 10/10 | 7/10 | **7/10** |
| **Total** | **120/120 sesiones** | **30/40** | **40/40** | **40/40** | **35/40** | **35/40** |

**Lo que muestran los datos:**
- **El nivel 2 actual cambia todas las sesiones, siempre.** También lunes y viernes, que no causaron dolor: entre 12 y 18 ejercicios por semana. Además, en 10 de 40 casos su semana subiría el trabajo y se rechazaría.
- **La variante localizada cambia solo la sesión con dolor** (4–6 ejercicios) y conserva las otras dos idénticas.
  - En 40 de 40 casos la semana mixta **pasa V1–V25**: no se encontraron conflictos semanales, como la cobertura 4C semanal, V15 o V18.
  - En 35 de 40 cumple también la regla de trabajo.
- **Ninguna es mejor en todos los casos.** La localizada no resuelve 5 casos:
  - en 4 tampoco lo hace el nivel 2: GAIN_MUSCLE 51 y GENERAL_FITNESS 72, 79 y 80;
  - en 1 (GAIN_MUSCLE 57) el nivel 2 sí lo resuelve y la localizada no.

  **A la inversa**, en 6 casos el nivel 2 sube el trabajo y la localizada pasa: GAIN_MUSCLE 54, 55 y 59 y GENERAL_FITNESS 76, 77 y 78.
  - **Con las dos en cadena** (localizada y, si no, nivel 2), se resolverían 36 de 40.

### Los casos reales de 1373 (nivel 1 sin sustituto)

| Caso | Nivel 2 actual | Localizada |
|---|---|---|
| C1 (usuario 2) | **Publica:** semana entera regenerada, trabajo igual (416/1.200) | **Publicable:** solo el viernes regenerado; V1–V25 sin problemas, sin el 1373 y trabajo igual (212/360) |
| C2 (usuario 32) | **Rechazada:** segundos 1.200 → 1.260 | **También rechazada:** el viernes regenerado pone un estiramiento de duración (455, 2×30 s) donde había un relleno por repeticiones, y los segundos de la sesión suben 360 → 420 |

**Ambas fallan en C2 por la misma razón:** la regla de trabajo compara repeticiones y segundos **por separado**, y cambiar un ejercicio de repeticiones por uno de duración siempre sube los segundos.

## 4. Ventajas y riesgos

| | Nivel 2 actual | Localizada |
|---|---|---|
| Continuidad | Baja: rehace la semana entera | **Alta:** conserva las sesiones válidas |
| Regla «solo lo necesario» | No la cumple | **La cumple** a nivel de sesión. A nivel de ejercicio, menos que SUST-SEG-1 (cambia 4–6 ejercicios del día, no 1) |
| Factibilidad medida | 30/40 dentro del trabajo | 35/40 |
| Linaje V31 | No hay: plan nuevo | Las sesiones conservadas lo mantienen; la reconstruida necesitaría un nuevo tipo de linaje («reconstruida por seguridad») |
| Evidencia | `safe_regeneration_attempts` + `REGENERACION_SEGURA_REGLAS` | Habría que añadir un resultado nuevo (p. ej. `REGENERACION_SEGURA_SESION`) y la traza de qué sesión se reconstruyó |
| V8C | No aplica (no es continuidad) | **Hace falta una excepción nueva** en `validateContinuidad`: «sesión reconstruida por seguridad», como hoy `SustitucionSeguridad` |
| Dosis y progresiones previas | Vuelve a la dosis base: pierde las progresiones y reducciones autorizadas | Las sesiones conservadas **mantienen** sus dosis. La reconstruida parte de la dosis base: se pierde la progresión **de esa sesión** |
| Complejidad | Ya existe y está probado | Código nuevo en el generador, en V8C, en la evidencia y en la API |
| Riesgo de semana incoherente | Bajo: el motor arma la semana completa | Medio: la sesión se arma sin conocer las otras (repeticiones de ejercicios, V18, slots semanales). En la medición no apareció ninguno, pero el prototipo no lo previene; solo lo detecta |

## 5. Casos de la orden (3.4)

| Caso | Nivel 2 actual | Localizada |
|---|---|---|
| Dolor en un único ejercicio | Funciona (normalmente lo resuelve antes el nivel 1) | Funciona; reconstruye el día |
| Varios ejercicios de la misma sesión | Funciona | Funciona; un solo día reconstruido |
| Dolor en sesiones de distintos días | Funciona | Funciona; varios días reconstruidos, pero se acerca al nivel 2 y aumenta el riesgo de conflictos entre días |
| Falta de equipamiento compatible | Las dos fallan igual (el conjunto elegible es el mismo) | Igual |
| Restricciones de movimiento | Igual: las aplica el conjunto elegible | Igual |
| Sesiones muy cortas | La regla de trabajo es más fácil de romper | Igual, pero solo en el día reconstruido |
| Dosis ya reducidas (fatiga, D8) | **Las deshace** (dosis base), y la regla de trabajo lo rechaza con frecuencia | Las mantiene en los días conservados. El día reconstruido puede superarlas y ser rechazado |
| Exclusiones persistentes | Respetadas | Respetadas |
| Progresiones previas | Se pierden | Se conservan salvo en el día reconstruido |
| Imposible reconstruir | NO_FACTIBLE | NO_FACTIBLE. Además, podría intentarse después el nivel 2: decisión pendiente |
| Idempotencia y linaje | Idempotente; sin linaje | Hay que diseñarlas: misma clave por medición y linaje mixto |
| De repeticiones a duración (C2) | Rechazada | **Rechazada:** la regla de trabajo por magnitudes separadas no se ve afectada por la localización |

## 6. Recomendación

1. **No sustituir el nivel 2 todavía.**
2. Si se implementa, que sea un **nivel intermedio** (nivel 1 → **localizado** → nivel 2 → NO_FACTIBLE), detrás de un indicador apagado y con aprobación funcional, porque:
   - en los datos medidos resuelve **más** casos que el nivel 2 (35 frente a 30 de 40) y siempre con más continuidad;
   - no empeora ninguna garantía de seguridad: mismas exclusiones y mismo V1–V25;
   - el nivel 2 queda como último recurso, tal y como está aprobado.
3. **Antes de implementarlo**, decidir la regla de trabajo para el cambio de repeticiones a duración (D3 c), porque es la que hace fallar las dos alternativas en el caso real de 1373.

El diseño técnico está en `DISENO_P03_LOCALIZADO.md`.
