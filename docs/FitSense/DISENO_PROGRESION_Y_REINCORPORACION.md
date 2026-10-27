# Diseño · Progresión de volumen y reincorporación condicional

| | |
|---|---|
| Versión | **v0.2 · para revisión.** Sustituye a la v0.1 (`cb45457`), que queda en el historial |
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
- **Cumplir no obliga a subir.** Una adherencia del 100 % es necesaria, pero no basta. Hay techo y, al alcanzarlo, la sesión se mantiene.

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
| 2 | La **progresión de los ejercicios conservados** de esa sesión está completa: en su techo de recuperación, o sin margen dentro de su rango |
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

## 11. Para la tesis

**Qué se puede sostener:**
- La progresión es **gradual, limitada y condicionada** a un cumplimiento sostenido observable.
- Los ejercicios retirados solo vuelven después de consolidar el volumen.
- La generación inicial y su dosis no cambian: la progresión es una excepción trazable.

**Qué no se puede sostener:**
- Que el cumplimiento demuestre adaptación fisiológica.
- Que las referencias del ACSM para la **carga** validen la progresión de **volumen**: FitSense no prescribe kilos (V15).
