# Diseño · Progresión de volumen y reincorporación condicional

| | |
|---|---|
| Versión | **v0.2.1 · revisión puntual** de la v0.2 (`7c805d2`): cierra las cuatro observaciones del 27/10 en §12, sin rediseñar. Sustituye a la v0.1 (`cb45457`) |
| Fecha | 27/10/2026 |
| Estado | Solo investigación y diseño: **sin código de producción, migraciones, contratos de Flutter ni cambios de reglas** |
| Evidencia | Simulación con 216 sesiones reales: `docs/FitSense/evidencia/progresion/` (`factibilidad.md`, `factibilidad.csv`), generadas por `ProgresionFactibilidadIT` (solo con `-Dfitsense.factibilidad=true`) |
| Antecedentes | La v0.3 (`DISENO_ADAPTACION_CONTEXTUAL.md`) y A01 se mantienen. **La restauración automática queda descartada**: la de `b6fd9ae` está apagada y no desplegada (§9) |

## 0. Secuencia y prioridades

> **Cumplimiento sostenido → Progresión gradual de volumen → Reincorporación condicional → Mantenimiento**

- **Para todos los objetivos.** La progresión no depende de que haya habido una reducción antes.
- **La adaptación por baja adherencia es un proceso aparte y tiene prioridad.** La progresión solo ocurre en un ciclo `NO_CHANGE`.
- **La reincorporación solo existe si antes se retiró un ejercicio** por una adaptación autorizada. Si no se retiró ninguno, la etapa no existe.
- **Una modificación funcional por ciclo publicado**, por este orden:
  1. seguridad y restricciones;
  2. adaptación por baja adherencia;
  3. progresión de volumen;
  4. reincorporación (nunca en el mismo ciclo que una progresión);
  5. mantenimiento.
- **Cumplir es necesario, pero no basta, y no exige el 100 %.** El umbral es el aprobado: la sesión completada (A_s ≥ 0,80) y una adherencia semanal observada ≥ 80 %, durante **dos semanas consecutivas** (`good_threshold_pct`, `progression_required_weeks`). Cumplirlo no garantiza un aumento: también tienen que pasar las condiciones de seguridad y factibilidad y la validación. Hay techo y, al alcanzarlo, la sesión se mantiene.
- **El 5 % y el 10 % son máximos, no metas.** Se aplica el menor incremento discreto válido (por ejemplo, +1,4 %), nunca un aumento forzado hasta el máximo.

---

## 1. Factibilidad con las reglas vigentes (simulación)

**Método.** Barrido de 72 perfiles: 4 objetivos × 3 niveles × 30, 45 y 60 min × gimnasio o casa, con días [1, 3, 5].
- La semana es la que publica el **motor de reglas real**: 216 sesiones.
- En cada sesión se prueban los **incrementos mínimos de un solo ejercicio**: +1 repetición por serie, +2 repeticiones por serie, +1 serie y +5 s por serie en los de duración.
- Se valida la semana entera con el **validador de producción de la generación** (V1–V25, 4D en ENFORCE y V24 por capacidades en ENFORCE).
- Los problemas que ya traía la semana se descuentan.

**Resultado:**

| Objetivo | Sesiones | Paso mínimo ≤ 5 % del volumen | Algún paso ≤ 10 % **válido hoy** | Válido **si V24 admitiera la dosis** | Mediana del paso mínimo |
|---|---:|---:|---:|---:|---:|
| `LOSE_WEIGHT` | 54 | 100 % | **11 %** | 11 % | 1,4 % |
| `GAIN_MUSCLE` | 54 | 100 % | **0 %** | **100 %** | 1,4 % |
| `INCREASE_STRENGTH` | 54 | 100 % | **33 %** | 33 % | 2,3 % |
| `GENERAL_FITNESS` | 54 | 100 % | **26 %** | 26 % | 0,8 % |

**Qué lo bloquea** (intentos de cada tipo de paso):

| Objetivo | +1 o +2 repeticiones / +5 s | +1 serie |
|---|---|---|
| `GAIN_MUSCLE` | 100 % bloqueados por **V24-dosis (4D exacta)** | 100 % por V24-dosis; además, 257 de 306 superarían las 3 series |
| Otros objetivos | 100 % bloqueados por **V24 por capacidades** (la dosis de la capacidad es exacta) | Válido solo dentro del rango aprobado (hasta 3 series): 216 de 849. El resto ya tiene 3 series, más algún V4 y V25 |

**Conclusiones:**
1. **El límite porcentual no es el problema.** El paso mínimo sube el volumen de la sesión un 0,8–2,3 % (mediana). El 5 % y el 10 % casi nunca lo impiden.
2. **El bloqueo es V24, en los cuatro objetivos**, no solo en `GAIN_MUSCLE`.
   - En `GAIN_MUSCLE`, levantar **solo** la exactitud de la dosis haría factible un paso en el 100 % de las sesiones: el presupuesto, el tope, V4 y V20 no lo impiden.
   - En los demás, solo «+1 serie» dentro del rango aprobado es válido hoy, y solo en el 11–33 % de las sesiones, porque la mayoría ya tiene 3 series.
3. **Incoherencia detectada.** P2-D, por donde pasaría la progresión, valida con la **V24 por capacidades apagada** (`StrictValidationAdapter` usa `capabilityEnforcement` = OFF). Hoy P2-D aceptaría cualquier dosis en esos objetivos. **La progresión debe validarse con la V24 por capacidades activa** (D-P11).
4. **Ritmo.** Con 2 semanas por paso, un piloto de 12 semanas admite como mucho unos 5 pasos. Con pasos del 1–2 %, el techo acumulado del +25 % no se alcanzaría en el piloto.

---

## 2. Reglas: lo que existe y lo que se propone

**Existentes y configuradas** (V7, V15 y la configuración activa). Hoy solo las usaba la tabla porcentual, que salió del flujo:

| Parámetro | Valor | Uso propuesto |
|---|---|---|
| `good_threshold_pct` | 80 | Sesión y semana «cumplidas» |
| `progression_required_weeks` | 2 | Cumplimiento sostenido antes de cada paso |
| `recovery_step_pct` | 10 | Paso máximo al **recuperar** hacia la referencia |
| `progression_increase_pct` | 5 | Paso máximo en la **progresión ordinaria** |
| `max_cumulative_volume_increase_pct` | 25 | Techo acumulado sobre la línea base. **Sin referencia bibliográfica**; para el MVP, ver D-P5 |
| `progression_rpe_ceiling` | `null` | Desactivado. El RPE no se usa (D5) |
| `min_sets` / `min_reps` | 2 / 6 | Mínimos |
| `rep_limits` (V16) | 6–30, con mínimos por zona | Rango de repeticiones |
| `SERIES_MAX_AMPLIACION` | 3 | Máximo de series ya aprobado para ampliar dosis |
| V4 / V20 | Techo de duración de los minutos del perfil + 15 %; piso del 70 % | Límites de duración |
| V24 (4D) | Dosis exacta; tope de 6 o 7 colocaciones; presupuesto del 85 % | Se mantiene en la generación; extensión solo para la progresión (§4) |

**Nuevo y pendiente de aprobación:** aplicarlo **por sesión**, los dos mecanismos (§3), la extensión de V24 (§4), la interacción con D8 (§6) y el registro (§8). Cada decisión nueva lleva un código **D-P**.

---

## 3. Matriz de decisiones

### 3.1 ¿Puede progresar esta sesión? (todas las condiciones)

| # | Condición | Regla | Fuente |
|---|---|---|---|
| 1 | Ciclo sin otra modificación | Decisión `NO_CHANGE` y ninguna restauración, adaptación ni reincorporación en el ciclo | Existente (v0.3) |
| 2 | Cumplimiento de **esa sesión** | Completada (A_s ≥ 0,80) en las **2 últimas semanas medidas** | Existente (80 % y 2 semanas), aplicado por sesión (D-P1) |
| 3 | Adherencia semanal | Observada ≥ 80 % en esas 2 semanas | Existente |
| 4 | Tolerancia | Sin fatiga ni dolor declarados en esa sesión en esas 2 semanas, ni dolor en otro uso de sus ejercicios | Nueva, sin umbral (D-P2) |
| 5 | Progresiones recientes | Ningún paso en las 2 semanas anteriores: el criterio de 2 semanas se cuenta **desde el último paso** | Nueva (D-P1) |
| 6 | Restricciones vigentes | Ningún ejercicio excluido, bloqueado o con dolor registrado | Existente (V1, SUST-SEG-1) |
| 7 | Techo | El volumen de la sesión está por debajo de su techo (§3.3) | Existente o nueva (D-P5) |
| 8 | RPE | **No se usa** | D5 |
| 9 | Sin información | Se mantiene: no cuenta como cumplimiento | Existente (v0.3) |

### 3.2 Dos mecanismos

| | **Recuperación de volumen** | **Progresión ordinaria** |
|---|---|---|
| Cuándo | La sesión viene de una **reducción de dosis** (fatiga o motivación) que aún no se recuperó | No hay reducción pendiente: nunca la hubo, o ya se recuperó |
| Desde dónde | La prescripción vigente, que es la adaptada | La prescripción vigente |
| Hasta dónde | **La referencia**: la dosis anterior a la reducción. Nunca la supera en esta fase | El **techo** (§3.3) |
| Paso máximo | 10 % del volumen de la sesión (`recovery_step_pct`) | 5 % (`progression_increase_pct`) |
| Qué cambia | Las repeticiones o series de **un** ejercicio **hacia** su dosis de referencia | **Un** ejercicio: +1 serie (hasta 3), o repeticiones dentro del rango autorizado (§4) |
| No es | Una restauración completa: un paso por ciclo | Un aumento indefinido |

**Elección del ejercicio.** En la recuperación, el primero cuya dosis está por debajo de la referencia. En la progresión ordinaria, el de **menor** incremento de volumen que pase la validación; a igualdad, el primero de la sesión. Es una regla de orden, no un umbral.

**Si el incremento mínimo supera el paso permitido**, no se fuerza: esa sesión se mantiene ese ciclo. Según la simulación es raro (el paso mínimo es del 0,8–2,3 %).

**Excepción aprobada (opción 2, 27/10/2026): recuperación imposible dentro del 10 %.** Si la reducción pendiente no admite ningún paso de recuperación ≤ `recovery_step_pct` (típicamente, devolver una serie quitada: 2×10 → 3×10 en una sesión pequeña), el ejercicio reducido **conserva su dosis reducida** y los demás ejercicios de la sesión pueden seguir la progresión ordinaria con sus reglas (≤ 5 %, techo 1,10 × base). La serie no se devuelve de golpe. Prueba: `PendientesProgresionIT.serieNoRecuperableYProgresionDeOtros`.

### 3.3 Referencias y techo

- **Línea base estable de la sesión:** su volumen en la **generación inicial**, la raíz de su linaje V31. No cambia con adaptaciones ni progresiones. Sirve para calcular el acumulado.
- **Referencia de recuperación:** la sesión justo antes de la reducción (V31 + V32, el recorrido ya verificado).
- **Techo de la progresión ordinaria (D-P5):**
  - *Opción A (recomendada para el MVP):* línea base × 1,10, es decir, como mucho dos pasos del 5 %.
  - *Opción B:* línea base × 1,25, el techo existente sin cambios.

  La opción A cabe en un piloto de 12 semanas y deja margen para medir. La B no se alcanzaría con pasos del 1–2 %, pero no está justificada para este uso.
- **Además, siempre:** V4 (duración ≤ minutos + 15 %), la disponibilidad M declarada para esa sesión si existe (v0.3) y el presupuesto del 85 % de 4D.

### 3.4 Después de cada paso

| La semana siguiente | Resultado |
|---|---|
| Completada, sin fatiga ni dolor | Cuenta para el próximo paso (necesita 2 semanas desde este) |
| Sin información, o parcial sin causa | Se mantiene; nada avanza |
| Omitida o parcial **con** causa | Decide la adaptación por causa (prioridad 2) |
| Fatiga o dolor tras un paso | **Vuelve al paso anterior** en el ciclo siguiente: la reducción mínima, registrada (D-P6) |
| En el techo | **Mantenimiento**: sin más pasos |

---

## 4. Compatibilidad con V24 (propuesta concreta)

**Principio:** la generación inicial **no cambia**. Una dosis distinta de la exacta solo se acepta si es **una progresión autorizada y trazable**.

1. **Rangos de progresión por clase.** Son nuevos y necesitan aprobación (D-P3); se apoyan en reglas ya vigentes:

   | Objetivo | Clase | Dosis inicial (vigente) | Rango de progresión propuesto |
   |---|---|---|---|
   | `GAIN_MUSCLE` | Estructural | 3×10 | Repeticiones de 10 a 12, con 3 series |
   | `GAIN_MUSCLE` | Relleno | 2×10 | Series de 2 a 3 (`SERIES_MAX_AMPLIACION`) y repeticiones de 10 a 12 |
   | `GAIN_MUSCLE` | Zona pequeña | +3 repeticiones | Hasta +3 sobre el rango anterior (13–15) |
   | Capacidades (`INCREASE_STRENGTH`, `LOSE_WEIGHT`, `GENERAL_FITNESS`) | Su dosis de capacidad | Exacta | Series hasta 3 (ya aprobado); repeticiones hasta +2 sobre la dosis de la capacidad, dentro de `rep_limits` y de la zona |

   El 10–12 de `GAIN_MUSCLE` se apoya en la zona de 8 a 12 repeticiones del ACSM 2009 para hipertrofia, ya citada en 4D. El descanso no cambia.
2. **Autorización trazable.** V24 acepta una dosis del rango **solo** en una colocación cuyo linaje (V31 + V32) registre un paso de progresión que llegó a esa dosis.
   - El dato viaja como hoy viaja D8: un conjunto calculado al preparar el contexto, a partir de la evidencia.
   - En cualquier otro caso, V24 exige la dosis exacta, como hoy.
3. **La IA no puede subir dosis.**
   - En la generación: dosis exacta.
   - En una corrección P2-D: V8C solo autoriza mantener o bajar lo permitido por la intervención. Una dosis autorizada se hereda tal cual o se reduce, nunca se sube.
4. **El resto de V24 se mantiene:** el tope de colocaciones, el presupuesto del 85 %, la evidencia de hipertrofia y la capacidad acreditada (la colocación sigue acreditando su capacidad).
5. **P2-D valida la V24 por capacidades** cuando hay progresión (D-P11), para que la regla sea la misma que en la generación.
6. **Prueba de coherencia exigida:** toda semana inicial válida sigue válida tras cada paso autorizado. Se mide con el mismo barrido de §1 una vez implementada.

---

## 5. Reincorporación condicional (solo si se retiró un ejercicio)

| # | Condición |
|---|---|
| 1 | V31 + V32 identifican un complemento retirado por `ACORTAR_O_REORGANIZAR` en el linaje de la sesión. **Nunca** uno retirado por dolor, sustituido por seguridad o cambiado por una variante más sencilla |
| 2 | La **progresión de los ejercicios conservados** de esa sesión está completa: en su techo de recuperación, o sin margen dentro de su rango. **Y, después del acortamiento, esa sesión dio al menos un paso de progresión o de recuperación** (decisión 1(a), 27/10/2026). Si nunca pudo dar ninguno (por ejemplo, el menor paso supera el 5 %), la sesión se mantiene acortada: la reincorporación no sirve para saltarse el límite que bloqueó la progresión |
| 3 | Cumplimiento sostenido: 2 semanas completadas, sin fatiga ni dolor, después de terminar el punto 2 |
| 4 | El ejercicio es elegible y seguro hoy (V1, exclusiones, bloqueos, dolor) |
| 5 | **Tiempo:** cabe en los minutos de la sesión (V4) y en la M declarada para ese entrenamiento. Si se retiró por falta de tiempo con M declarada, solo con una M nueva mayor |
| 6 | Tope de colocaciones y presupuesto de 4D, o el tope por capacidades |
| 7 | El volumen de la sesión con el ejercicio no supera la **referencia completa** (antes de retirarlo) ni el techo (§3.3) |
| 8 | Entra con la **dosis inicial exacta de su clase** (V24, sin extensión), no con la histórica, al final de la sesión |
| 9 | Se valida la sesión completa (V8C, V1–V25) antes de publicar |

**Si alguna falla:** no se reincorpora, la sesión se mantiene y el motivo queda registrado. **No se añade nunca un ejercicio que no se haya retirado antes.**

## 6. Interacción con D8 y V20

**Escenario:**
1. Una sesión se acortó con autorización y quedó bajo el piso de V20.
2. D8 la conserva semana a semana.
3. La persona cumple los criterios y la sesión **sube** un paso (más volumen, misma o mayor duración).
4. La sesión **ya no es una copia idéntica**, así que D8, tal como está, deja de eximirla. Si sigue bajo el piso, V20 rechaza el paso y se publica la copia sin progresar. Esa sesión no podría progresar nunca hasta superar el piso de un salto.

**Propuesta (D-P7):** la herencia de D8 continúa a través de un paso de **progresión autorizada** solo si se cumplen todas estas condiciones:
1. es un paso registrado en V32 sobre esa misma sesión;
2. la duración estimada **no baja** (`duración nueva ≥ duración anterior`);
3. no hay otra intervención, sustitución ni reincorporación en ese paso;
4. el linaje es completo e inequívoco.

**La exención sigue siendo estrecha:** solo V20, solo esa sesión y solo mientras siga bajo el piso. Cuando lo supera, deja de necesitarla. V24, V25, V4 y el resto se aplican siempre. No se convierte en una exención general: una sesión sin un acortamiento autorizado en su linaje nunca la tiene.

---

## 7. Escenarios exigidos

| Escenario | Comportamiento |
|---|---|
| Cumplimiento sostenido, **sin adaptaciones** | Progresión ordinaria hasta el techo; **nunca** se evalúa reincorporar |
| **Dosis reducida** (fatiga o motivación) | Recuperación gradual hacia la referencia (≤ 10 % por paso, 2 semanas cada uno); después, la progresión ordinaria |
| **Ejercicio retirado** | Primero la progresión de los ejercicios conservados; después, si cumple §5, la reincorporación con su dosis inicial |
| Ejercicio retirado **sin** criterios | No se reincorpora; se mantiene |
| **Dolor o fatiga** | No progresa; tras un paso, vuelve al anterior |
| **Volumen máximo** | Mantenimiento |
| `GAIN_MUSCLE` | Progresa **solo** con la extensión autorizada de §4. Sin ella, la simulación da 0 % de pasos válidos |
| Sesión bajo V20 (D8) | Solo con D-P7: un paso que no reduce la duración, sobre un acortado autorizado. Ninguna otra sesión recibe la exención |
| **Sin registros** suficientes | Mantiene la rutina |

## 8. Impacto técnico mínimo

| Pieza | Cambio |
|---|---|
| V35 (no desplegada) | **Rehacerla antes de desplegar** (D-P10): `restored_sessions` → `progressed_sessions` (a lo sumo uno por intento, disjunto, solo con `SIN_ORDEN`). En `conservation_report`: tipo de paso (`RECUPERACION`, `PROGRESION`, `REINCORPORACION`, `VUELTA_ATRAS`), ejercicio, dosis anterior y nueva, volumen antes y después, y línea base |
| Resolutor | Reconvertir `RestauracionResolver` en un resolutor de **paso**: el mismo recorrido V31 + V32, más los criterios de §3 y §5 |
| V8C | Sustituir la autorización «restauración idéntica» por la autorización **estrecha** de un paso: un ejercicio, una dosis dentro del rango o un ejercicio reincorporado, y nada más |
| V24 | La extensión de §4: un conjunto autorizado por el linaje. La generación no cambia |
| P2-D | Validar con la V24 por capacidades cuando hay un paso (D-P11) |
| D8 | La continuación de §6 (D-P7) |
| Configuración | Los parámetros existentes. Si se aprueba la opción A de D-P5, un parámetro nuevo de techo para el MVP en una versión de configuración nueva; los umbrales de adherencia no cambian |
| Flutter | Ninguno |

**Se reutiliza:** V31, V32, `workout_sessions`, `weekly_user_metrics`, la configuración V7/V15, las dosis por capacidad y 4D, el productor P2-D y V8C.

## 9. Pruebas propuestas (al implementar)

- **Positivas:**
  - progresión ordinaria sin adaptaciones;
  - recuperación de una dosis reducida en varios pasos;
  - reincorporación tras completar la progresión;
  - `GAIN_MUSCLE` con la extensión;
  - D-P7: un paso bajo V20 sobre un acortado autorizado.
- **Negativas:**
  - 1 semana completada (no basta);
  - sin información;
  - fatiga o dolor;
  - otra sesión adaptada en el ciclo;
  - progresión y reincorporación en el mismo ciclo (se rechaza);
  - reincorporar un ejercicio retirado por dolor o bloqueado;
  - reincorporar sin tiempo;
  - reincorporar sin retirada previa (no existe);
  - una dosis fuera del rango, o sin autorización en el linaje (V24 la rechaza);
  - la IA subiendo una dosis (V8C la rechaza);
  - una sesión sin acortamiento autorizado bajo V20 (sin exención).
- **Multisemana:** varios pasos hasta el techo y después mantenimiento; vuelta atrás tras fatiga.
- **Invariantes:** V8C, V31, V32 y V33, más V35 rehecha; linaje sin violaciones; historial y adherencia intactos.
- **Regresión:** la suite completa, más el barrido de §1 repetido con la extensión, para medir el porcentaje de sesiones con progresión factible.

## 10. Decisiones que necesitan aprobación antes de implementar

| # | Decisión | Recomendación |
|---|---|---|
| D-P1 | Preparación **por sesión**: completada 2 semanas seguidas, contadas desde el último paso, más la observada semanal ≥ 80 % | Sí |
| D-P2 | Tolerancia: sin fatiga ni dolor declarados (sin RPE) | Sí |
| D-P3 | Rangos de progresión por clase (§4.1) | Aprobar o ajustar los rangos propuestos |
| D-P4 | Pasos: recuperación ≤ 10 %, progresión ordinaria ≤ 5 %, un ejercicio por ciclo | Sí: son los parámetros existentes |
| D-P5 | Techo de la progresión ordinaria: A = línea base × 1,10; B = × 1,25 (existente) | **A** para el MVP |
| D-P6 | Fatiga o dolor tras un paso → volver al paso anterior | Sí |
| D-P7 | La herencia de D8 continúa a través de un paso que no reduce la duración (§6) | Sí, con sus pruebas |
| D-P8 | La reincorporación entra con la dosis inicial exacta de su clase | Sí |
| D-P9 | No activar sobrecarga más allá del techo en el MVP | Sí |
| D-P10 | Rehacer V35 como `progressed_sessions` antes de desplegar, y retirar la restauración automática | Sí; mientras tanto queda apagada |
| **D-P11** | P2-D valida con la **V24 por capacidades** activa al menos cuando hay un paso (hoy está apagada en P2-D) | Sí. Revisar también si debe activarse en toda la adaptación: hoy P2-D aceptaría cualquier dosis en esos objetivos |

## 12. Revisión puntual (v0.2.1): las cuatro observaciones

**Evidencia:** `ProgresionReservaIT`, solo con `-Dfitsense.factibilidad=true`. Usa las mismas 216 sesiones reales del motor de reglas (72 perfiles; **son perfiles simulados, no participantes**). Resultados completos en `docs/FitSense/evidencia/progresion/reserva_y_v24.md`.

**Cómo se simula la extensión de V24.** Un problema de V24 sobre un ejercicio cuya dosis nueva está dentro del rango de D-P3 se trata como autorizado. Todo lo demás lo decide el validador de producción tal cual.

### 12.1 Cumplimiento (observación 1)

Queda fijado en §0: **dos semanas consecutivas** con la sesión completada (≥ 80 %) y la adherencia semanal observada ≥ 80 %, más las condiciones de seguridad y factibilidad. **No se exige el 100 %**, y cumplir el umbral no garantiza un incremento.

### 12.2 Techo y reincorporación (observación 2)

**El problema, demostrado.** Si los ejercicios conservados progresan sin reservar margen y la reincorporación exige no superar la referencia, la reincorporación queda bloqueada en:

| Objetivo | Sesiones con un relleno retirable | Reincorporación bloqueada (política ingenua) |
|---|---:|---:|
| `LOSE_WEIGHT` | 54 | 33 (61 %) |
| `GAIN_MUSCLE` | 24 | 24 (100 %) |
| `GENERAL_FITNESS` | 54 | 53 (98 %) |
| `INCREASE_STRENGTH` | 0 | — (todas sus colocaciones son estructurales; no hay relleno que retirar) |

**Dos políticas de reserva.** Notación: V_base es el volumen de la sesión original y V_ret el del relleno retirado, con su dosis inicial exacta.

| Política | Los conservados pueden progresar hasta | Total tras reincorporar | Pasos medios antes de reincorporar | Reincorporación válida |
|---|---|---|---|---|
| **R1 · reserva estricta** | V_base − V_ret, es decir, **sin margen**: ACORTAR conserva las dosis, así que los conservados ya están ahí | ≤ V_base (la referencia) | 0 | 100 % |
| **R2 · reserva dentro del techo** | 1,10 × V_base − V_ret | ≤ 1,10 × V_base (el techo ordinario aprobado provisionalmente) | `LOSE_WEIGHT` 1,9 · `GAIN_MUSCLE` 4,8 · `GENERAL_FITNESS` 4,6 | 100 % |

**Por qué R2 garantiza la reincorporación:**
1. Mientras la sesión está sin el relleno, su progresión se limita a 1,10 × V_base − V_ret. Ese margen V_ret queda **reservado**.
2. La reincorporación, con la dosis inicial exacta (D-P8), suma exactamente V_ret, así que el total queda ≤ 1,10 × V_base.

El volumen nunca impide la reincorporación. La duración, el tope de colocaciones y el presupuesto de 4D los comprueba la validación: en la simulación, ninguno la impidió.

**R2 no supera automáticamente la referencia.** El techo es el mismo que tiene cualquier participante sin adaptaciones (§3.3). Lo que evita es castigar a quien tuvo que acortar.

**Recomendación: R2 (D-P12).** Si se prefiere no superar nunca la referencia en una sesión que tuvo una retirada, R1 es la alternativa segura. Su coste es que, en esas sesiones, no hay progresión antes de reincorporar: la etapa 1 sería solo de consolidación.

**Si la reincorporación no es posible** por otra razón (seguridad, tiempo, colocaciones), el margen reservado **no se libera** y la sesión se mantiene. Así no se invade el espacio del ejercicio retirado.

### 12.3 Impacto por ejercicio (observación 3)

**Regla de elección, sin un porcentaje nuevo:**
- en cada paso se elige el cambio de **menor magnitud relativa en el ejercicio afectado**;
- **primero +1 repetición**; +1 serie solo cuando las repeticiones llegaron al máximo de su rango;
- a igualdad, el de menor incremento en la sesión.

| Objetivo | Sesiones con ≥ 1 paso | Pasos medios hasta parar | Llegan al techo | Mayor cambio en **un ejercicio** | Mayor paso en **la sesión** | Por qué paran |
|---|---:|---:|---:|---:|---:|---|
| `LOSE_WEIGHT` | 36 (67 %) | 2,0 | 24 (44 %) | 16,7 % | 5,0 % | techo 24; paso > 5 % 18; sin margen en el rango 12 |
| `GAIN_MUSCLE` | 54 (100 %) | 5,8 | 30 (56 %) | 10,0 % | 2,6 % | techo 30; paso > 5 % 24 |
| `INCREASE_STRENGTH` | 54 (100 %) | 3,9 | 36 (67 %) | 16,7 % | 4,2 % | techo 36; paso > 5 % 18 |
| `GENERAL_FITNESS` | 53 (98 %) | 4,6 | 36 (67 %) | 16,7 % | 3,9 % | techo 36; paso > 5 % 15; sin margen 3 |

**Qué muestran estos datos:**
- **Ningún paso fue un +1 serie.** El 16,7 % es +1 repetición sobre 6, el mínimo de fuerza. El salto de 2 a 3 series (+50 % en el ejercicio) **no hace falta** con estos rangos, porque siempre hay antes una repetición disponible.
- **Ningún paso superó el 5 % de la sesión.** Cuando el menor cambio lo superaba («paso > 5 %», en sesiones pequeñas), no se aplicó: la regla de «no forzar» funciona.
- **Con la V24 de hoy, en cambio:** `GAIN_MUSCLE` 0 %, y los demás solo con +1 serie (§1).
- **Los rangos propuestos (D-P3) son factibles:** 10–12 repeticiones en `GAIN_MUSCLE` y +2 repeticiones en las dosis por capacidad. La validación completa (V4, V16, V17, V20, V24-presupuesto, V24-tope y V25) no los bloqueó en ninguna sesión.

### 12.4 Autorización de V24 antes de la evidencia V32 (observación 4)

**El problema de la secuencia.** El primer paso se valida antes de que exista su registro en V32. La solución es separar **dos fuentes de autorización** y unirlas en una sola transacción:

1. **Propuesta del ciclo.** El backend calcula **un** paso determinista (sesión de origen, ejercicio, dosis anterior y dosis nueva) **antes** de validar. Lo pasa en el contexto de validación como la única autorización nueva del ciclo. V24 acepta esa dosis **solo** en esa colocación de esa sesión, y **solo** si es exactamente la propuesta y está dentro del rango (D-P3).
2. **Autorizaciones heredadas.** Son las dosis ya publicadas por pasos anteriores, leídas del linaje V31 + V32 al preparar el contexto (como D8). Una copia idéntica las conserva.
3. **Mismo cambio, sin ventanas:**
   - la publicación y la evidencia V32 se escriben **en la misma transacción** (ya probado en `PublicacionAtomicaIT`);
   - antes de escribir, el mapeador comprueba que la dosis publicada es **exactamente** la propuesta (sesión, ejercicio y dosis); si no coincide, no se publica nada;
   - la evidencia guarda la propuesta completa (§8);
   - V35, rehecha, puede añadir un disparador que compruebe que la sesión derivada difiere de su origen **solo** en ese ejercicio.
4. **La IA no puede usar la autorización.** En una corrección P2-D, V8C solo admite mantener o reducir. Una dosis del rango que no sea la base ni la autorizada se rechaza.

### 12.5 V24 por capacidades en P2-D (D-P11): impacto antes de activarla

| Objetivo | Reducción por fatiga (−2 repeticiones) rechazada con V24 por capacidades **activa** | Rechazada hoy en P2-D (V24 por capacidades **apagada**) |
|---|---:|---:|
| `LOSE_WEIGHT` | 39 de 54 (72 %) | 0 % |
| `GAIN_MUSCLE` | 54 de 54 (100 %, por 4D) | 100 % (4D está activa en P2-D) |
| `INCREASE_STRENGTH` | 47 de 54 (87 %) | 0 % |
| `GENERAL_FITNESS` | 54 de 54 (100 %) | 0 % |

**Qué significa:**
1. **Hoy, en P2-D, los objetivos por capacidades aceptan cualquier dosis.** Es la incoherencia ya señalada.
2. **Activar la V24 por capacidades tal cual dejaría sin efecto las reducciones de dosis** que permiten `REVISAR_CARGA_Y_RECUPERACION` e `INVESTIGAR_CAMBIO_MINIMO`, en el 72–100 % de las sesiones. Pasarían a `SIN_EFECTO`. Hoy ya ocurre en `GAIN_MUSCLE`.
3. **Por eso, la extensión autorizada de V24 tiene que cubrir también las reducciones.** Una dosis por **debajo** de la base, dentro de `rep_limits` y del mínimo de series, se acepta **solo** si la registra una intervención de reducción aplicada (V32), igual que una progresión. Con eso, activar la V24 por capacidades en P2-D es coherente y no quita intervenciones.
4. **Orden recomendado:** primero la extensión (reducción y progresión autorizadas); después activar la V24 por capacidades en P2-D, y medir otra vez con este arnés. **No activarla antes.**

### 12.6 V35 (D-P10)

- **Dónde está aplicada:**
  - en el repositorio, `V35__restauracion_de_sesiones.sql` (`b6fd9ae`), no desplegada desde este entorno;
  - en la base local de pruebas `fs-p2d`, que **se aplicó a mano** para las pruebas heredadas de P2-D y no tiene historial de Flyway.
- **No se puede comprobar desde aquí:** Neon. Hay que confirmar allí, en solo lectura:
  - `SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;`
  - `SELECT count(*) FROM information_schema.columns WHERE table_name = 'plan_adaptation_attempts' AND column_name = 'restored_sessions';`
- **Si no está aplicada en ningún entorno compartido,** se puede rehacer V35 sin migración de corrección. La base local se recrea.

### 12.7 Decisiones actualizadas

| # | Estado tras la revisión |
|---|---|
| D-P1 | **Aprobada:** 80 %, dos semanas consecutivas, por sesión y por semana |
| D-P2 | **Aprobada** (condiciones observables) |
| D-P3 | **Factible** con la simulación (§12.3); queda condicionada a la autorización trazable (§12.4) y a cubrir también las reducciones (§12.5) |
| D-P4 | **Aprobada** como máximos (no metas) |
| D-P5 | **Aprobada provisionalmente** (techo × 1,10), con la reserva R2 |
| D-P6 | **Aprobada con ajuste:** ante dolor prevalece la seguridad (`SEGURIDAD_ANTE_DOLOR`), no solo el paso atrás |
| D-P7 | **Aprobada conceptualmente**, con pruebas |
| D-P8, D-P9 | **Aprobadas** |
| D-P10 | **Condicionada** a confirmar que V35 no está aplicada en Neon (§12.6) |
| D-P11 | **No activar todavía.** Requiere antes la extensión con reducciones autorizadas (§12.5) |
| **D-P12** (nueva) | Política de reserva: **R2** (recomendada) o R1 |

## 13. Las dos cuestiones previas a implementar (27/10)

### 13.1 ¿Qué pasa con el volumen reservado si el ejercicio retirado no puede volver?

La reserva de R2 (D-P12) solo tiene sentido mientras la reincorporación sea **posible**. Cada ciclo se evalúa el estado del ejercicio retirado, con datos que ya existen y sin umbrales nuevos:

| Estado | Cuándo | Reserva | Reevaluación |
|---|---|---|---|
| **PENDIENTE** | Es elegible, seguro y cabe (tiempo, colocaciones, presupuesto), pero falta completar la etapa 1 o las 2 semanas de cumplimiento | **Se mantiene**: los conservados progresan hasta 1,10 × V_base − V_ret | Cada ciclo |
| **EN ESPERA DE TIEMPO** | Es elegible y seguro, pero **no cabe**: con él, la sesión supera los minutos del perfil + 15 % (V4) o la disponibilidad M declarada para ese entrenamiento, o el tope de colocaciones o el presupuesto de 4D | **Se libera**: la progresión de los conservados sigue hasta el techo de la sesión (1,10 × V_base). La falta de tiempo no bloquea el resto | Cada ciclo. Si vuelve a caber **y** el total queda dentro del techo, se reincorpora; si cabe pero supera el techo, no se reincorpora y la sesión se mantiene |
| **DESCARTADA** | Deja de ser elegible o seguro: fuera del catálogo o de la elegibilidad, bloqueado por la persona, con una exclusión de seguridad o con dolor registrado | **Se libera definitivamente** | No hay reevaluación automática. Volver requiere una reevaluación específica, fuera del MVP |

**Garantías:**
- Nunca se supera el techo (1,10 × V_base), ni la dosis, ni el tiempo.
- La progresión de los conservados no se bloquea indefinidamente: si la reincorporación no es posible, la reserva no la frena.
- El estado y su motivo quedan en `conservation_report` de cada intento, así que es trazable.

**Consecuencia aceptada:** si la reserva se liberó y la persona luego recupera tiempo, el ejercicio solo vuelve si cabe en el techo. Se prefiere mantener lo ya progresado a superar los límites.

### 13.2 V24, V8C y D8 juntos durante un paso

**Un paso** (de recuperación, de progresión o de reincorporación) sobre una sesión **no señalada**, en un ciclo `SIN_ORDEN` con su propuesta P, se valida así. **Todas** las reglas se aplican a la vez:

| Regla | Qué comprueba | Qué no permite |
|---|---|---|
| **V8C** | La sesión difiere de la semana medida **exactamente en P**: un ejercicio con otra dosis, o un ejercicio añadido (el retirado, con su dosis inicial). Mismo día, enfoque y orden de los demás; los minutos son los que estima el estimador | Cualquier otra diferencia. **Hoy V8C rechaza todo cambio en una sesión no señalada** (`revisarNoSenalada`): sin esta autorización estrecha, ningún paso pasaría |
| **V24** | La dosis del ejercicio de P es **exactamente** la de P y está en su rango (D-P3). Cada uno de los demás tiene su dosis base o la **heredada** de un paso o una reducción anteriores registrados en V32. El tope y el presupuesto de 4D se recalculan con la dosis nueva | Una dosis del rango sin propuesta ni herencia; una dosis fuera del rango; una dosis heredada que no coincida exactamente |
| **D8 + D-P7** | Si la sesión venía de un acortamiento autorizado (el recorrido V31 + V32 pasa solo por copias **y pasos de P** que no reducen la duración), la exención de V20 se mantiene **solo** para esa sesión | Eximir de V20 a una sesión sin un acortamiento autorizado en su linaje, o un paso que reduzca la duración |
| **V1–V25 restantes** | Igual que siempre, con el perfil y el catálogo vigentes | — |

**Reducciones autorizadas (para activar D-P11 sin perder intervenciones).** Cuando la IA corrige una sesión **señalada** con `REVISAR_CARGA_Y_RECUPERACION` o `INVESTIGAR_CAMBIO_MINIMO`:
- **V8C**, como hoy, solo admite bajar la dosis en lo que la intervención permite;
- **V24** acepta esa dosis **inferior** a la base solo en esa colocación, solo dentro de `rep_limits` y del mínimo de series, y solo en la corrección que V8C aprobó;
- al publicar, queda en V32 (`applied_sessions` y el informe), y las copias siguientes la heredan.

Una subida en una corrección de la IA la rechaza V8C antes de llegar a V24.

**Dependencia medida de D-P7** (simulación, `reserva_y_v24.md` §2b). Sesiones con un relleno retirado cuya versión acortada queda por debajo del piso de V20:

| Objetivo | Acortadas bajo el piso | Pasos medios **con** D-P7 | Pasos medios **sin** D-P7 |
|---|---:|---:|---:|
| `LOSE_WEIGHT` | 54 de 54 (100 %) | 1,9 | **0** |
| `GAIN_MUSCLE` | 10 de 24 (42 %) | 5,8 | **0** |
| `GENERAL_FITNESS` | 22 de 54 (41 %) | 5,6 | **0** |

Sin D-P7, esas sesiones **no progresan nunca**. Con D-P7, la exención sigue siendo estrecha (solo V20, solo esa sesión, solo con pasos que no reducen la duración), y las demás reglas se aplican igual.

**Qué solo puede probarse con el código reconvertido:**
- la autorización estrecha de V8C;
- el conjunto de dosis autorizadas de V24 (propuesta del ciclo, herencia y reducciones);
- la continuación de D8 a través de un paso.

La simulación filtra esas reglas; la prueba real tiene que ejecutarlas. Son las pruebas multisemana de §9, más:
1. un paso sobre una sesión heredada de D8 que sigue por debajo del piso;
2. una dosis del rango sin propuesta (V24 la rechaza);
3. la IA subiendo una dosis autorizada (V8C la rechaza);
4. una reducción por fatiga con la V24 por capacidades activa (se acepta).

### 13.3 D-P10 y las bases de datos

- **Neon (27/10, en solo lectura):** el historial de Flyway termina en V33 y `restored_sessions` no existe. **V35 no está aplicada: D-P10 queda cerrada** y V35 se puede rehacer.
- **Comprobación adicional recomendada** antes de desplegar, en el esquema correcto:
  - `SELECT current_schema();`
  - `SELECT conname FROM pg_constraint WHERE conname LIKE 'ck_paa_restaur%' OR conname = 'ck_paa_una_restauracion';` debe devolver 0 filas;
  - `SELECT position('restored_sessions' IN pg_get_functiondef('fn_intento_coherente'::regproc)) = 0;` debe devolver `true`.
- **`fs-p2d` (local):** tiene la V35 de restauración aplicada a mano. Se mantiene así mientras el código la usa. Al reconvertir V35 se **recrea** la base desde V1 hasta la V35 nueva (o se revierte V35 a mano y se aplica la nueva), para que coincida con el repositorio.
- **V34** (propuesta, sin aplicar): sigue pendiente la decisión de orden frente a V35 (renumerar o usar `outOfOrder`).

### 13.4 Estado de las decisiones

| # | Estado |
|---|---|
| D-P10 | **Cerrada:** V35 no está en Neon; se rehace como `progressed_sessions` |
| D-P12 | **Aprobada (R2)**, con los estados de §13.1 |
| Incrementos por ejercicio | **Aprobados** (primero +1 repetición) |
| D-P3 | **Aprobada conceptualmente**, sujeta a pruebas integradas |
| D-P11 | Solo **junto con** la extensión de V24 (reducciones y progresiones autorizadas) |
| D-P7 | **Necesaria** según la simulación; falta la prueba integrada |

## 14. Para la tesis

**Qué se puede sostener:**
- La progresión es **gradual, limitada y condicionada** a un cumplimiento sostenido observable.
- Los ejercicios retirados solo vuelven después de consolidar el volumen.
- La generación inicial y su dosis no cambian: la progresión es una excepción trazable.

**Qué no se puede sostener:**
- Que el cumplimiento demuestre adaptación fisiológica.
- Que las referencias del ACSM para la **carga** validen la progresión de **volumen**: FitSense no prescribe kilos (V15).
