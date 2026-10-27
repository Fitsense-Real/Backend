# Diseño · Progresión de volumen antes de reincorporar ejercicios

| | |
|---|---|
| Versión | **v0.1 · para revisión.** Solo diseño: sin código, migraciones ni cambios en Flutter |
| Fecha | 27/10/2026 |
| Sustituye | La **restauración automática** de la v0.3 (`DISENO_ADAPTACION_CONTEXTUAL.md` §3, caso 11). El resto de la v0.3 se mantiene. La v0.3, A01 y sus evidencias quedan como antecedentes, sin cambios |
| Relación con el código | La restauración de `b6fd9ae` (V35) está **apagada** por defecto y **no desplegada**. Este diseño propone retirarla o reconvertirla (D-P10) |

## Principio y secuencia obligatoria

> **ADAPTACIÓN MÍNIMA → CUMPLIMIENTO SOSTENIDO → PROGRESIÓN GRADUAL DE VOLUMEN → REINCORPORACIÓN DEL EJERCICIO RETIRADO → MANTENIMIENTO**

- **La prescripción vigente es la adaptada.** Después de una adaptación, la semana adaptada es la nueva base y no se vuelve atrás por calendario ni por una sola sesión completada.
- **Subir volumen es la primera forma de progresar.** Recuperar un ejercicio retirado es una etapa posterior, no el primer ajuste.
- **Cumplir no obliga a subir.** Una adherencia del 100 % es una condición necesaria, no una orden de aumentar. El volumen tiene techo y, una vez alcanzado, se mantiene.
- **Un cambio a la vez.** Como máximo una modificación funcional por ciclo publicado (aprobado en la v0.3), y la adaptación por una causa tiene prioridad.

**Qué cambia respecto de la v0.3.** La v0.3 restauraba **de golpe** la sesión anterior completa tras una sola semana completada. Este diseño la sustituye por pasos graduales con criterios de cumplimiento sostenido, y separa los dos movimientos: primero el volumen de lo que hay, después el ejercicio retirado.

---

## 1. Reglas que ya existen y decisiones nuevas

**Existentes y aprobadas** en fases anteriores, en la configuración activa. Hoy las usaba solo la tabla porcentual `AdjustmentDecisionTable`, que salió del flujo:

| Parámetro (`adjustment`) | Valor | Origen y significado |
|---|---|---|
| `good_threshold_pct` | 80 | Adherencia «buena»; también decide `NO_CHANGE` |
| `progression_required_weeks` | 2 | V15: dos semanas seguidas cumpliendo para progresar (regla «2 por 2» del ACSM 2009, adaptada a la semana) |
| `progression_increase_pct` | 5 | V7: aumento de volumen por paso cuando se progresa **por encima** de la línea base |
| `recovery_step_pct` | 10 | V7: volver **hacia** la línea base en tramos del 10 %, sin exigir el criterio de dos semanas («restituir un volumen que la persona ya sostuvo») |
| `max_cumulative_volume_increase_pct` | 25 | V15: techo del +25 % sobre la línea base. **Sin referencia bibliográfica**: decisión de diseño declarada |
| `max_cumulative_volume_reduction_pct` | 40 | Piso del −40 % |
| `progression_rpe_ceiling` | `null` | V15: desactivado a propósito, porque la escala de RPE no está instrumentada |
| `min_sets_per_exercise` / `min_reps_per_set` | 2 / 6 | Mínimos de dosis |

**Otras reglas vigentes que limitan la progresión:**

| Regla | Efecto |
|---|---|
| `rep_limits` (V16) | Repeticiones de 6 a 30, con mínimos por zona (piernas inferiores 12, cintura 10) |
| `SERIES_MAX_AMPLIACION` = 3 | Las dosis por capacidad admiten subir series hasta 3 (ACSM 2009: 1 a 3 series) |
| V20 | Piso de duración (70 % de los minutos de la sesión) |
| V21 | Descanso máximo de 180 s |
| V24, 4D (`GAIN_MUSCLE`) | Dosis **exacta** por clase (3×10 estructural, 2×10 relleno, +3 repeticiones en zonas pequeñas); tope de 6 o 7 colocaciones; presupuesto del 85 % de la sesión |
| V24 por capacidades (otros objetivos) | Dosis exacta de la capacidad en la generación. En P2-D la validación por capacidades está apagada, así que hoy no se exige |
| Volumen | `equivalentVolume`: repeticiones, más segundos / 30 en los ejercicios de duración. Es la misma medida de las intervenciones |

**Nuevo (pendiente de aprobación):** cómo se aplican esas reglas **a una sesión** y en dos etapas. Cada decisión nueva lleva un código **D-P**.

---

## 2. Etapa 1 · Progresión del volumen de la prescripción vigente

### 2.1 Qué sesiones entran

Una sesión entra en progresión si en su linaje (V31 + V32) hay una **reducción** aplicada que todavía no se completó:
- una dosis bajada (fatiga o motivación);
- o un complemento retirado (falta de tiempo).

La **referencia** (la sesión anterior a la adaptación) se encuentra con el recorrido ya verificado: copias hasta la adaptación (`LinajeIntegridadIT`). La referencia **no se restaura**: solo define el **objetivo** de la etapa.

### 2.2 ¿Está preparada la persona? (todas las condiciones)

| Condición | Datos que se usan | Regla |
|---|---|---|
| Cumplimiento sostenido de **esa sesión** | `workout_sessions` (la que cuenta) | Completada (A_s ≥ 0,80) en las **2 últimas semanas medidas** | Existente: umbral del 80 % y 2 semanas; aplicarlo **por sesión** es nuevo (D-P1) |
| Adherencia semanal | `weekly_user_metrics` | Observada ≥ 80 % en esas 2 semanas (`NO_CHANGE`) | Existente |
| Tolerancia | Causas declaradas por ejercicio | Ninguna fatiga ni dolor declarados en esa sesión durante las 2 semanas; tampoco dolor en otra sesión que use el mismo ejercicio | Nueva, sin umbral (D-P2) |
| Restricciones vigentes | Perfil, exclusiones de seguridad, catálogo | Ningún ejercicio de la sesión excluido o bloqueado | Existente (V1, SUST-SEG-1) |
| Sin otra modificación en el ciclo | Decisión del ciclo | Solo con `NO_CHANGE` | Existente (v0.3) |
| RPE | — | **No se usa** (D5), pese a que la tabla antigua exigía tener RPE para progresar | Decisión D-P3 |

Si falta cualquiera de las condiciones, la sesión **se mantiene**. La «sin información» no cuenta como cumplimiento.

### 2.3 Cuánto sube y con qué límite

**Un solo incremento discreto por ciclo, en un solo ejercicio de la sesión.** Debe ser el menor posible:
- **+1 serie**, dentro del rango aprobado de su dosis (hasta `SERIES_MAX_AMPLIACION` = 3);
- o **repeticiones**, solo para volver hacia las que tenía en la referencia, sin pasarlas.

**Qué ejercicio:** el primero cuya dosis esté por debajo de la referencia. Si la sesión ya alcanzó la referencia, ninguno: la etapa 1 está completa.

**Límites, todos a la vez:**
1. **Paso del 10 %.** El volumen de la sesión tras el paso no supera el actual × 1,10 (`recovery_step_pct`). Es recuperación hacia la referencia, no sobrecarga.
2. **Techo = referencia.** No supera el volumen de la sesión de referencia, sin el ejercicio retirado (D-P4). La etapa 1 **no** progresa por encima de lo que la persona ya tenía.
3. **Duración.** La duración estimada cabe en los minutos de la sesión y, si la persona declaró su disponibilidad M para ese entrenamiento (v0.3), en M.
4. **Reglas de siempre:** V16 (repeticiones), V21, V24 y V25.

**Si el menor incremento discreto supera el 10 %** (por ejemplo, +1 serie en una sesión pequeña): no se sube ese ciclo, y queda anotado. No se fuerza (D-P5).

**`GAIN_MUSCLE`.** 4D fija la dosis exacta, así que no hay series ni repeticiones que subir. En `GAIN_MUSCLE` la única reducción posible es retirar un complemento, y la etapa 1 consiste en **consolidar**: el cumplimiento sostenido de §2.2 sin cambiar la dosis. Después se evalúa la etapa 2. No se propone tocar 4D.

### 2.4 Qué pasa después de cada paso

| Semana siguiente al paso | Resultado |
|---|---|
| Completada, sin fatiga ni dolor | Cuenta para el criterio de 2 semanas del paso siguiente. El paso nuevo exige otra vez 2 semanas (D-P1) |
| Sin información | Se mantiene; nada avanza |
| Parcial o no completada, sin causa declarada | Se mantiene; no se sube |
| Omitida o parcial **con** causa | La decide el ciclo como siempre: adaptación por esa causa (prioritaria) |
| Fatiga o dolor declarados tras un paso | **Vuelve al paso anterior** el ciclo siguiente: es la reducción mínima, y queda registrada (D-P6) |

**La etapa 1 está completa** cuando la sesión alcanza el volumen objetivo (§2.3, límite 2) y lo sostiene **2 semanas** con las condiciones de §2.2. En `GAIN_MUSCLE`, cuando sostiene 2 semanas la prescripción vigente.

---

## 3. Etapa 2 · Reincorporación del ejercicio retirado

**Solo se evalúa con la etapa 1 completa.** Es una sola reincorporación por ciclo, y solo con `NO_CHANGE`.

| Condición | Regla |
|---|---|
| Qué se puede reincorporar | Solo un complemento retirado por `ACORTAR_O_REORGANIZAR`, identificado por V31 + V32 como la diferencia entre la referencia y la adaptada. **Nunca** uno retirado por dolor (`SEGURIDAD_ANTE_DOLOR`) ni sustituido por seguridad, ni uno de una variante más sencilla, sin una reevaluación específica fuera del MVP |
| Cumplimiento sostenido | 2 semanas completadas, sin fatiga ni dolor, después de terminar la etapa 1 |
| Validez | Sigue en el catálogo, elegible y seguro para el perfil vigente (V1, exclusiones, bloqueos, sin dolor registrado) |
| Disponibilidad | La sesión con el ejercicio cabe en sus minutos y en M si se declaró. Si M fue la causa de retirarlo, solo si la persona ya no declara M o declara una M mayor (ADAPT-2) |
| Tope de colocaciones y presupuesto | 4D (6 o 7 colocaciones y el 85 %) en `GAIN_MUSCLE`; el tope por capacidades en los demás |
| Volumen total | La sesión con el ejercicio no supera el volumen de la **referencia** completa (D-P7) |
| Dosis inicial | **La mínima válida de su clase**, no la histórica: en `GAIN_MUSCLE` la de relleno (2×10, o 2×13 en zona pequeña), y en los demás las series base de su dosis por capacidad (D-P8). Si después debe crecer, lo hace por la etapa 1 |
| Posición | Al final de la sesión, como estaba el complemento. El resto de la sesión no cambia |

**Si alguna falla:** no se reincorpora, la sesión se mantiene y el motivo queda en el informe de conservación.

## 4. Mantenimiento

Con la etapa 2 hecha, o sin nada que reincorporar, **la sesión se mantiene**. No hay progresión por encima de la referencia en el MVP.

La regla de sobrecarga existente (+5 % con 2 semanas y techo del +25 %) **no se activa** en este diseño (D-P9). Si se activara, sería una etapa posterior y explícita.

---

## 5. Seguridad, disponibilidad y validación

**Cada paso (subir, reincorporar o volver atrás) es una propuesta determinista del backend.** No interviene la IA y pasa por la cadena ya validada:
1. Sustitución por seguridad de lo heredado (SUST-SEG-1).
2. **V8C**, con una autorización nueva y estrecha. Solo puede cambiar **una** sesión no señalada, y solo de una de estas formas:
   - +1 serie o repeticiones hacia la referencia en **un** ejercicio;
   - o la adición de **un** ejercicio igual al retirado, con su dosis inicial.

   Nada más cambia: ni el día, ni el enfoque, ni los demás ejercicios.
3. **V1–V25**, con el perfil y el catálogo vigentes. V20 sin exención nueva; D8 se mantiene.
4. Si no pasa: no hay cambio, se publica la copia y el motivo queda registrado.

**Sin regenerar.** No se genera ninguna sesión ni semana nueva; todo parte de la copia derivada.

---

## 6. Datos que se reutilizan y cambios mínimos

**Se reutiliza:**
- **Linaje y evidencia:** V31, V32 y el recorrido ya verificado (`ReduccionAutorizadaResolver`, `RestauracionResolver`), para la referencia y el ejercicio retirado.
- **Ejecución:** `workout_sessions` (A_s y estado), causas por ejercicio y `weekly_user_metrics`.
- **Configuración** (§1).
- **Reglas de dosis y validación:** dosis por capacidad (rango de series), 4D, `rep_limits`, el productor P2-D y V8C.

**Cambios mínimos propuestos:**

| Pieza | Cambio |
|---|---|
| V35 (no desplegada) | **Rehacerla antes de desplegar** (D-P10): en lugar de `restored_sessions`, un arreglo `progressed_sessions` (a lo sumo una, mismos invariantes) y el **tipo de paso** en `conservation_report` (`SUBIDA`, `REINCORPORACION` o `VUELTA_ATRAS`), con el ejercicio, la dosis anterior y la nueva |
| Resolutor | Reconvertir `RestauracionResolver`: mismo recorrido, pero devuelve la **etapa** y el **paso propuesto**, no la sesión anterior |
| V8C | La autorización estrecha de §5, que sustituye a la de «restauración idéntica» |
| Productor | Sin cambios de estructura: recibe el paso como hoy recibe la restauración |
| Interruptor | El mismo, apagado por defecto |
| Flutter | Ninguno: la persona ve la sesión actualizada como cualquier publicación |

**Sin cambios:** los umbrales de adherencia, el decisor, la generación inicial, D8, la adaptación por causa (v0.3) y los parámetros de configuración existentes.

## 7. Pruebas que lo demostrarían

Todas con el ciclo de producción y PostgreSQL real, multisemana, sin IA en los pasos de progresión.

| # | Escenario | Esperado |
|---|---|---|
| 1 | Fatiga: dosis bajada; 1 semana completada | **No** sube (necesita 2) |
| 2 | 2 semanas completadas sin fatiga | Sube **un** paso en **un** ejercicio (≤ +10 %, ≤ referencia); `SIN_ORDEN`, `progressed_sessions` = esa sesión, 0 llamadas a la IA |
| 3 | Sigue cumpliendo | Siguen los pasos (2 semanas cada uno) hasta la referencia, y para ahí |
| 4 | Fatiga declarada después de un paso | Vuelve al paso anterior el ciclo siguiente |
| 5 | Sin información, o parcial sin causa | Se mantiene |
| 6 | Otra sesión señalada en el ciclo | No hay paso (una modificación por ciclo) |
| 7 | Falta de tiempo: complemento retirado (`GAIN_MUSCLE`) | Etapa 1 = consolidar 2 semanas; después, reincorporación con dosis de relleno, dentro del tope de 4D y del presupuesto |
| 8 | Reincorporar con la etapa 1 incompleta | No se reincorpora |
| 9 | Ejercicio retirado por dolor, o bloqueado después | Nunca se reincorpora |
| 10 | Reincorporar sin tiempo (duración o M) | No se reincorpora; el motivo consta |
| 11 | Después de reincorporar | Mantenimiento: ningún paso más |
| 12 | Invariantes | V8C, V31, V32 y V33; linaje sin violaciones; historial y adherencia intactos |
| 13 | Contraprueba | Con el interruptor apagado, ningún paso |

---

## 8. Decisiones pendientes de aprobación

| # | Decisión | Recomendación |
|---|---|---|
| D-P1 | Criterio de preparación: la **sesión** completada 2 semanas seguidas (≥ 80 %) más una observada semanal ≥ 80 %, otra vez por cada paso | Sí: aplica por sesión los valores existentes (80 % y 2 semanas) |
| D-P2 | Tolerancia: sin fatiga ni dolor declarados en la sesión durante esas 2 semanas | Sí: sin umbral nuevo |
| D-P3 | RPE: la tabla antigua exigía tener RPE para progresar; D5 lo descartó para restaurar | No usarlo, por coherencia con D5 y V15 (escala no instrumentada). Se sigue recogiendo como dato descriptivo |
| D-P4 | Techo de la etapa 1 = volumen de la referencia, sin el ejercicio retirado | Sí: recuperar lo que ya sostuvo, sin sobrecarga |
| D-P5 | Paso máximo = `recovery_step_pct` (10 %) del volumen de la sesión; si el menor incremento discreto lo supera, no se sube | Sí: reutiliza el 10 % aprobado. Alternativa: permitir siempre +1 serie aunque supere el 10 % |
| D-P6 | Fatiga o dolor después de un paso → volver al paso anterior | Sí |
| D-P7 | Volumen total tras reincorporar ≤ referencia completa | Sí |
| D-P8 | Dosis inicial del reincorporado = la mínima válida de su clase | Sí |
| D-P9 | No activar en el MVP la sobrecarga (+5 % sobre la línea base, techo +25 %) | No activarla: fuera del MVP |
| D-P10 | Qué hacer con la restauración de `b6fd9ae` (apagada, no desplegada): rehacer V35 como `progressed_sessions` y reconvertir el resolutor y V8C, o quitarla del todo hasta implementar este diseño | Rehacer V35 **antes** de desplegar y reconvertir el código cuando se apruebe; mientras tanto, sigue apagada |

## 9. Para la tesis

**Qué se puede sostener:**
- La adaptación es mínima.
- La recuperación es **gradual**, con **cumplimiento sostenido** y techos explícitos.
- Los ejercicios retirados vuelven **después** de consolidar el volumen, con una dosis controlada.
- Los parámetros de progresión son los ya aprobados (V7 y V15), aplicados por sesión. Su limitación ya está documentada: el +25 % no tiene referencia y la escala de RPE no está instrumentada.

**Qué no se puede sostener:**
- Que completar las sesiones demuestre tolerancia fisiológica: es una condición operativa observable.
- Que las referencias del ACSM, pensadas para la **carga**, validen la progresión de **volumen**: FitSense no prescribe kilos (V15 ya lo declara).
