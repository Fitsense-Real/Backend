# Diseño · Adaptación mínima justificada por la causa

| | |
|---|---|
| Versión | **v0.2 · para revisión.** Sustituye a la v0.1 (`46fb26d`), que queda en el historial |
| Fecha | 27/10/2026 |
| Base | rama `generation`, `46fb26d` |
| Estado | Solo diseño: **sin código, migraciones, endpoints ni cambios en Flutter** hasta su aprobación |

## Principio

> **FitSense ayuda a la persona a mantener y cumplir su rutina personalizada; no le cambia la rutina cada vez que tiene una dificultad.**

**Qué hace cada parte:**
- **La personalización inicial** la hace la IA a partir del perfil: objetivo, nivel, días, tiempo, equipo y restricciones.
- **La adaptación posterior** responde a un problema concreto de ejecución, con el **cambio mínimo** que su causa permite, sobre la sesión afectada. El resto del plan no cambia.

**Cómo se trata cada dato:**
- La omisión es un hecho.
- La causa es información declarada.
- La intervención es una decisión justificada por ambas.

Una omisión **no** implica por sí sola reducir el entrenamiento.

**Estabilidad.** Una rutina estable también es personalizada, y para la evaluación permite atribuir cada cambio a una intervención concreta. Si la rutina cambiara cada semana, los resultados de adherencia serían difíciles de interpretar.

### Qué demuestra el MVP

1. Un plan inicial personalizado, generado con IA.
2. La medición de la adherencia y la detección de cuándo hace falta intervenir.
3. Ajustes mínimos y justificados sobre el plan existente, que preservan su estructura y cuyo resultado se evalúa.

**Fuera del MVP:**
- regenerar automáticamente la semana o la rutina;
- la progresión de carga o dificultad;
- cuestionarios extensos;
- un sistema completo de planificación deportiva.

### Qué se retira de la v0.1

| v0.1 | v0.2 |
|---|---|
| Regenerar la semana si cambian los minutos | **Retirado.** La rutina se conserva; solo se ajusta la sesión afectada, si es factible |
| Pregunta con tres opciones y sugerencia de cambiar el perfil | **Una** pregunta breve, opcional, con dos opciones; el perfil no se toca |
| Alcances puntual, temporal y persistente para todas las causas | Solo se distingue lo que **se restaura**; todo lo demás se conserva |
| Restauraciones encadenadas y una columna de referencia | Restauración de **un paso**, con el linaje V31 que ya existe |

---

## 1. ¿Cuándo adaptar?

**Sin cambios.** Decide el decisor actual (`AdaptationOrderDecider`), con sus umbrales:

| Situación | Decisión |
|---|---|
| Cobertura de registro menor de 50 % | `INSUFFICIENT_DATA` |
| Observada de 80 % o más | `NO_CHANGE` |
| De 50 % a menos de 80 % | `ADAPT_LOCALIZED`: la peor sesión señalada |
| Menos de 50 % | `ADAPT_EXPANDED`: todas las señaladas (pospuesto) |

Se señalan las sesiones con registro que no llegaron al 80 %. La **causa registrada** de cada una elige la intervención.

**Lo único nuevo: una pregunta opcional para la falta de tiempo.** Cuando la persona elige «No tuve tiempo» al omitir, Flutter muestra una pregunta. Si no responde, la omisión queda registrada igual.

> ¿Normalmente tienes el tiempo que indicaste para entrenar?
> - **Sí, fue algo puntual** → `IMPREVISTO`
> - **No, suelo tener menos** → «¿Cuántos minutos sueles tener?» → `MENOS_TIEMPO_HABITUAL` + minutos M

**Reglas de la respuesta:**
- **Los minutos los escribe la persona.** No se infieren de la sesión omitida.
- M debe estar en el rango que ya admite el perfil (15 a 180). No hay umbral nuevo.
- El perfil **no** se modifica: M acota la sesión afectada y nada más.
- Quien pueda entrenar otro día ya tiene su causa propia, «Cambio de horario» (`SCHEDULE_CHANGE`). La pregunta no la duplica.

---

## 2. ¿Qué puede adaptarse?

**Solo la sesión señalada, y solo el cambio mínimo que su intervención permite.** Las demás sesiones se conservan idénticas: lo garantiza V8C.

| Causa (y respuesta) | Qué se hace | Qué puede cambiar la IA | Si no es factible | Tras la semana adaptada | Regla |
|---|---|---|---|---|---|
| Falta de tiempo · **imprevisto** | **Conservar**, sin llamar a la IA | Nada | No aplica | Nada que restaurar | Nueva |
| Falta de tiempo · **menos tiempo habitual (M)** | `ACORTAR_O_REORGANIZAR`, con la sesión **≤ M** | Bajar dosis o retirar **un** complemento; conservar los estructurales y la dosis de los que se quedan | `SIN_EFECTO`: la sesión se conserva y consta | **Se conserva**: es una condición declarada, no un imprevisto | Actual + tope M (nuevo) |
| Falta de tiempo · **sin respuesta** | **Conservar**, sin llamar a la IA | Nada | No aplica | Nada que restaurar | **Cambia** la regla actual (hoy acorta) |
| Cambio de horario | `REPROGRAMAR_DIAS` | Solo la fecha, dentro de los días disponibles y de la semana | `SIN_EFECTO` (por ejemplo, sin un día libre) | **Se restaura** el día original | Actual + restauración (nueva) |
| Dolor o molestia | `SEGURIDAD_ANTE_DOLOR` | Retirar o sustituir el ejercicio que dolió, con revisión de seguridad | Sin IA: `NO_FACTIBLE`, no se republica lo que dolió | **Nunca** se restaura el ejercicio | Actual |
| Fatiga | `REVISAR_CARGA_Y_RECUPERACION` | Solo la dosis | `SIN_EFECTO` | **Se restaura** la dosis | Actual + restauración (nueva) |
| Muy difícil | `VARIANTE_MAS_SENCILLA_O_CARGA` | Variante del mismo patrón, o la carga | `SIN_EFECTO` | Se conserva: volver atrás sería **subir** la dificultad | Actual |
| Falta de motivación | `INVESTIGAR_CAMBIO_MINIMO` | Cambios pequeños de dosis | `SIN_EFECTO` | Decisión pendiente **D3** | Actual |
| Otra, o sin causa | `CAUSA_NO_REGISTRADA` | Nada | No aplica | Nada que restaurar | Actual |

**Lo que se mantiene sin cambios:**
- V8C (solo cambia lo señalado y solo lo permitido), V1–V25 con 4D en ENFORCE, y la sustitución por seguridad de lo heredado.
- La exención de V20 solo para la sesión que se acorta.
- La evidencia V32.

**La única validación nueva:** en el caso «menos tiempo habitual», la duración estimada de la sesión ajustada no supera M. M es el dato que declaró la persona, no un umbral del sistema.

**Sin modificación válida** no se fuerza nada. Se registra `SIN_EFECTO` (la IA no encontró un cambio permitido) o `NO_FACTIBLE` (no hay semana publicable), como hoy.

**Sin IA** (solo motor de reglas) sigue siendo `RESPALDO`: la semana se conserva.

---

## 3. ¿Cómo se conserva la rutina?

### 3.1 La prescripción de referencia

La semana nueva se copia de la medida, como hoy. La **referencia** de una sesión adaptada es la versión **anterior** a la adaptación, y ya está en la base: es el origen V31 (`source_planned_workout_id`) de la sesión adaptada. No hace falta copiar datos ni crear una columna, porque la restauración es de **un solo paso** (§3.2).

### 3.2 Restauración

**Qué se restaura.** Solo los cambios marcados en la tabla: el día reprogramado y la dosis bajada por fatiga. Todo lo demás se conserva.

**Cuándo.** Al derivar la semana siguiente a la adaptada, si se cumplen las dos condiciones:
1. esa sesión **no** volvió a señalarse por la misma causa;
2. la referencia supera la revalidación (§3.3).

Si volvió a fallar por la misma causa, no se restaura y la nueva decisión parte de la versión adaptada.

**Cómo.** Se vuelve **exactamente** a la referencia: los mismos ejercicios, dosis y día. No se generan ejercicios nuevos ni combinaciones nuevas. Restaurar **no es progresar**: nunca se prescribe más que la referencia.

### 3.3 Nunca restaurar algo inseguro

La sesión restaurada se valida con el perfil y el catálogo **vigentes**:
- exclusiones de seguridad (SUST-SEG-1);
- ejercicios bloqueados por la persona;
- ningún ejercicio que haya dolido desde la referencia;
- V1–V25.

Si no pasa, **no se restaura**: la sesión queda como estaba y se registra el motivo.

### 3.4 Evitar cambios acumulativos

Cada adaptación toca una sola sesión y lo mínimo que permite su causa:
- Una sesión que se completa no vuelve a cambiar: la decisión es `NO_CHANGE`.
- Un imprevisto o una falta de tiempo sin respuesta no cambian nada.
- Los ajustes de día y de fatiga se restauran.

Solo quedan como cambios estables los que tienen una justificación duradera: la menor disponibilidad declarada, el dolor y la variante más sencilla.

---

## 4. Indicador de resultado por sesión adaptada

**Pregunta:** ¿la persona realizó la sesión concreta que FitSense modificó?

| | |
|---|---|
| Unidad | Cada sesión adaptada publicada: su origen V31 está en `applied_sessions` de V32 |
| Momento | Al cerrar la semana de esa sesión (en el ciclo siguiente, junto a `adherence_after_pct`) |
| Fuente | V32, el linaje V31, `planned_workouts` y `workout_sessions`. **Sin tabla nueva**: una consulta o vista de solo lectura |

**Clasificación** (con los umbrales que ya existen):

| Resultado | Condición |
|---|---|
| Completada | Sesión que cuenta, con A_s de 80 % o más |
| Parcial | Sesión que cuenta, con A_s mayor que 0 y menor que 80 % |
| Omitida | Omitida con causa, o A_s = 0. Se marca si la causa es **la misma** que motivó la adaptación |
| Sin información | Sin sesión y sin omisión declarada |
| No aplica | El intento fue `SIN_EFECTO`, `RESPALDO` o `NO_FACTIBLE`, o la sesión se conservó por su causa |

**Fórmulas** (sin pesos, igual que la adherencia):
- Cumplimiento oficial = Σ A_s / n × 100, sobre **todas** las sesiones adaptadas; sin información cuenta 0.
- Cumplimiento observado = Σ A_s / n_r × 100, solo las que tienen registro.
- Repetición de la dificultad = omitidas por la misma causa / n_r.

**No es una prueba causal.** Que una sesión adaptada se complete es un **resultado observado**, no la demostración de que la IA o la intervención lo produjeron: no hay grupo de control.

---

## 5. El ejemplo de A01 con este diseño

**Viernes del 06/11:** 6 ejercicios, 33 minutos, omitido por falta de tiempo. El perfil dice 45 minutos.

| Caso | Entrada | Decisión | Viernes siguiente | Lunes y miércoles |
|---|---|---|---|---|
| A | «Fue algo puntual» | Conservar, sin IA | Igual: 6 ejercicios, 33 min | Iguales |
| B | «Suelo tener menos», M = 30 | `ACORTAR_O_REORGANIZAR`, ≤ 30 min | Retira el complemento 1256 → 5 ejercicios, 28 min (lo que hizo A01). Si no se pudiera llegar a 30 sin tocar los estructurales: `SIN_EFECTO` | Iguales |
| C | No responde | Conservar, sin IA | Igual | Iguales |
| D | Cambio de horario (perfil con un día libre) | `REPROGRAMAR_DIAS` | Mismo contenido en otro día; la semana siguiente vuelve al viernes | Iguales |

**A01 sigue siendo válida** como prueba técnica histórica, y sus evidencias no cambian. Documenta la regla anterior: falta de tiempo implicaba acortar. Con la v0.2, ese mismo cambio solo ocurre en el caso B.

---

## 6. Impacto, mínimo

| Componente | Cambio |
|---|---|
| Flutter | La pregunta opcional tras «No tuve tiempo» y el campo de minutos |
| Backend: contrato | Dos campos opcionales en `POST /api/v1/workouts/{id}/skip`: `lackOfTimeContext` y `availableMinutes` (este, solo con `MENOS_TIEMPO_HABITUAL`, entre 15 y 180) |
| Backend: decisión | La intervención según causa y respuesta (tabla §2); conservar sin llamar a la IA; restaurar día o dosis (§3) |
| PostgreSQL | `planned_workouts`: `lack_of_time_context`, `declared_available_minutes`. V32: registrar las sesiones conservadas por su causa y las restauradas (formato a decidir, **D2**) |
| Prompt | Para la sesión con M: «la sesión debe durar como máximo M minutos». Nada más |
| Validaciones | V8C acepta una restauración solo si es idéntica a la referencia. Nueva: sesión ≤ M. **Sin cambios:** V1–V25 y sus umbrales |
| Analítica | La consulta del indicador (§4) |

**No cambia:** la generación inicial, el decisor, los umbrales, V24, V25 ni el linaje V31.

---

## 7. Casos de aceptación

| # | Entrada | Esperado | Evidencia |
|---|---|---|---|
| 1 | Viernes omitido · imprevisto | Viernes siguiente idéntico; 0 llamadas a la IA | V32 con la sesión conservada por su causa; `ai_calls = 0` |
| 2 | Viernes omitido · menos tiempo, M = 30 (factible) | Viernes ≤ 30 min; solo se retira un complemento; lunes y miércoles idénticos | V8C y V1–V25 aprobadas; duración ≤ 30; V32 `APLICADA` |
| 3 | Igual, con M que solo se alcanza tocando estructurales | `SIN_EFECTO`; viernes conservado | V32 `no_effect_sessions` con el motivo |
| 4 | Viernes omitido · sin respuesta | Viernes idéntico; 0 llamadas | Igual que el caso 1 |
| 5 | Viernes reprogramado por cambio de horario y completado | La semana siguiente vuelve exactamente al viernes | Sesión idéntica a la referencia V31; restauración registrada |
| 6 | Dosis bajada por fatiga; la referencia contiene un ejercicio bloqueado después | No se restaura ese contenido; se registra el motivo | V32 con la restauración bloqueada; ningún ejercicio bloqueado publicado |
| 7 | Sesión adaptada completada / omitida de nuevo por la misma causa | Indicador: completada / omitida con la marca de misma causa | Consulta §4 |

---

## 8. Decisiones pendientes

| # | Decisión | Recomendación |
|---|---|---|
| D1 | Falta de tiempo sin respuesta: conservar (sin IA) | **Conservar**, según el caso C de la revisión del 27/10. Cambia la regla actual |
| D2 | Dónde se registran en V32 las sesiones conservadas por su causa y las restauradas | Dos arreglos con el formato de `applied_sessions`, y el motivo de una restauración bloqueada en `conservation_report` |
| D3 | Falta de motivación: ¿se conserva o se restaura su ajuste de dosis? | Restaurar, igual que la fatiga: es un ajuste para sostener la rutina, no una condición duradera |
| D4 | ¿Preguntar también al cerrar una sesión **parcial** por falta de tiempo? | No en el MVP: solo al omitir |

## 9. Para la tesis

**Qué se puede sostener:** la adaptación es **mínima** (una sesión, un cambio permitido), **justificada** (por la causa y, en la falta de tiempo, por un dato que declara la persona) y **controlada** (V8C, V24, V25 y la trazabilidad V31/V32). La rutina personalizada se conserva.

**Qué no se puede sostener:**
- que una mejor adherencia posterior se deba a la IA;
- que la disponibilidad declarada sea la real.

**Datos:** las adaptaciones con la regla v0.2 deben distinguirse por `rule_version`, para no mezclarlas con las anteriores, A01 incluida.
