# Implementación controlada · progresión y reincorporación condicional (V34)

**Estado:** implementado, **apagado por defecto**. La **V34 está aplicada en Neon** desde el 27/10/2026 (al arrancar la aplicación contra Neon; verificado, §12). Los interruptores siguen apagados.
**Diseño de referencia:** `DISENO_PROGRESION_Y_REINCORPORACION.md` v0.2.1 (§3, §4, §5, §12, §13).

## 1. Interruptores

| Propiedad | Por defecto | Qué activa |
|---|---|---|
| `fitsense.adaptation.progression.enabled` (`FITSENSE_PROGRESSION_ENABLED`) | `false` | El paso de progresión en ciclos `NO_CHANGE`, las dosis autorizadas de V24, el rechazo de subidas de la IA y, por §4.5, la V24 por capacidades en P2-D |
| `fitsense.adaptation.capability-validation.enabled` (`FITSENSE_ADAPTATION_CAPABILITY_VALIDATION`) | `false` | Solo la V24 por capacidades en P2-D (D-P11), **siempre** junto con las reducciones autorizadas |

Con los dos en `false`, la adaptación valida exactamente como antes. `fitsense.adaptation.restoration.enabled` desaparece con la restauración.

## 2. Aislamiento de la generación (requisitos 1 y 2)

- La generación valida con `validate(draft, ctx, divisor)`, que usa `ConservationReport.vacio()`: **no hay ningún permiso**. V24 aplica su regla original.
- Una dosis distinta de la exacta solo se acepta si `ConservationReport.autorizacionDe(fecha, ejercicio)` la autoriza **y** cae en el rango de D-P3 (`ProgresionDosisPolicy`). Esas autorizaciones solo las crea V8C, en P2-D.
- **Prueba:** `GeneracionReferenciaIT` compara los planes iniciales de los 72 perfiles (4 objetivos × 3 niveles × 30/45/60 min × gimnasio/casa) con la referencia capturada **antes** de tocar el código (`evidencia/progresion/generacion_referencia.txt`, commit `1f7d929`): **72/72 idénticos, 0 regresiones.**

## 3. Qué autoriza cada proceso

| Proceso | Autorización para V24 | Quién la crea |
|---|---|---|
| Generación | Ninguna: V24 original | — |
| Adaptación (corrección de la IA) | `REDUCCION`: solo dosis que **bajan** (ninguna dimensión sube, mismo tipo y descanso) en una intervención que permite cambiar la dosis | V8C |
| Progresión | `PASO`: solo la dosis exacta del paso propuesto por el backend, en esa sesión y ese ejercicio | V8C, tras comprobar el paso |
| Copia semanal | `HEREDADA`: solo dosis cuyo origen es un paso o una reducción registrados en V32, con el linaje V31 íntegro (`DosisLinajeResolver`) | V8C |
| Cambio arbitrario de la IA | Ninguna. Una **subida** se rechaza en V8C: «La IA no puede subir dosis» | — |

Rangos (D-P3): `GAIN_MUSCLE` repeticiones de base a base+2, series fijas en estructurales y relleno hasta 3, nunca reducciones (4D intacta); capacidades: progresión hasta +2 repeticiones y series hasta 3, reducción hasta el mínimo de V16 de la zona, 1 serie o 20 s; el descanso nunca cambia. En V25 una colocación autorizada sigue acreditando la capacidad de su dosis base.

## 4. V8C: el paso estrecho

En un ciclo `SIN_ORDEN` con un paso P sobre la sesión S, V8C exige: la derivada **idéntica** a `P.contenido`; respecto de la semana medida, solo el cambio de P (subida de un ejercicio, reincorporación de uno, o vuelta atrás); mismo día y enfoque; minutos que no bajan en un paso que sube; sin dolor ni sustitución por seguridad. Cualquier otra diferencia es un problema. Con una orden de adaptar no se acepta ningún paso (una modificación por ciclo). Antes de escribir V32, el mapeador comprueba que lo publicado es exactamente `P.contenido` (§12.4).

**D-P7:** la exención de V20 continúa a través de un paso solo si es `PROGRESION` o `RECUPERACION`, no baja el volumen ni los minutos y la sesión venía de un acortamiento autorizado (D8). Una reincorporación o una vuelta atrás la cortan.

## 5. El resolutor (`ProgresionResolver`)

Solo con `NO_CHANGE` (nunca con `INSUFFICIENT_DATA` ni con una orden). Para cada sesión S (origen O):

1. **Vuelta atrás** si O→S fue un paso y en S consta fatiga (sin dolor: ante dolor manda la seguridad).
2. **Criterio de dos semanas:** S y O completadas; adherencia semanal observada ≥ `good_threshold` (80) en las dos semanas; sin fatiga ni dolor en S ni en O, ni dolor en otro uso de sus ejercicios; la semana que publicó S fue `NO_CHANGE` y O→S es una **copia** (las dos semanas cuentan desde el último paso); todo elegible y sin exclusión.
3. **Recuperación** hacia la dosis anterior a la última reducción de la IA, sin superarla, con paso ≤ `recovery_step_pct` (10 %).
4. **Progresión ordinaria:** primero +1 repetición, +1 serie solo con las repeticiones en su máximo; el de menor cambio relativo en el ejercicio; paso ≤ `progression_increase_pct` (5 %); volumen ≤ 1,10 × V_base (raíz V31).
5. **Reserva R2:** con un relleno retirado por `ACORTAR`, los conservados progresan hasta 1,10 × V_base − V_ret (**PENDIENTE**). Sin margen, se propone la **reincorporación** con la dosis inicial exacta, al final de la sesión, si el total cabe en el techo. Si no se puede publicar, se propone la progresión con el techo entero (**EN_ESPERA_DE_TIEMPO**). Si el retirado deja de ser elegible o seguro, **DESCARTADA**.

Los candidatos se revalidan de uno en uno (V8C + V1–V25) y se publica el primero válido; los bloqueados quedan en `conservation_report.pasos_bloqueados`. Orden: vuelta atrás > recuperación > progresión > reincorporación; dentro de cada clase, la sesión con **menos pasos ya dados** y después la fecha (sin esto, la primera sesión de la semana acaparaba todos los ciclos).

**Evidencia:** `plan_adaptation_attempts.progressed_sessions` (V34) y `conservation_report.pasos` (tipo, ejercicio, dosis antes/después, volúmenes, base, techo, estado de la reserva).

## 6. V34 y bases de datos (requisito 8)

- **Numeración resuelta: V34.** La corrección del 1374 (`migraciones_propuestas/V34__correccion_inc_cat_1374.sql`) no reservaba número: su cabecera dice que se copia «con el siguiente número libre», `MigracionPropuesta1374IT` la ejecuta como script (sin Flyway) y nada en `db/migration` ni en Neon (V33) usa el 34. La progresión es `V34__progresion_de_sesiones.sql`; la del 1374, si se aprueba, entra como V35 o posterior. Así no queda ningún hueco ni hará falta `outOfOrder`.
- `V34__progresion_de_sesiones.sql` sustituye a la V35 de restauración (nunca aplicada en Neon, D-P10 cerrada). Añade `progressed_sessions` con `ck_paa_progresadas_unicas`, `ck_paa_progresadas_disjuntas`, `ck_paa_un_paso`, `ck_paa_paso_sin_orden` y `ck_paa_paso_publicado`, y recrea `fn_intento_coherente`.
- La propuesta V34 no se toca; entrará como V36 o posterior.
- **`fs-p2d` (local) recreada** desde V1 hasta la migración de progresión (mismo esquema con V34), más la marca `fitsense_entorno = PRUEBAS` y el usuario de prueba 1 que exigen las pruebas heredadas. Ya no tiene `restored_sessions`.

## 7. Pruebas

| Prueba | Resultado |
|---|---|
| `GeneracionReferenciaIT`: 72 perfiles antes/después | 72/72 idénticos |
| `ProgresionV24V8CTest` (16): V24 rechaza lo no autorizado en la generación y en la copia; acepta lo heredado con linaje; paso autorizado sí, arbitrario no; fuera de rango y +1 serie estructural, no; paso de dos ejercicios o distinto de la propuesta, no; paso con orden, no; **la IA no sube dosis**; D-P7 con sesión acortada bajo el piso (sí con D8, no sin D8, no en vuelta atrás) | 16/16 |
| `ProgresionIT` (7, ciclo real + PostgreSQL, IA simulada) | 7/7 |
| · progresión en los cuatro objetivos, cinco semanas | un paso por ciclo, +1 repetición, ≤ 5 %, ≤ techo, sin IA, copia heredada aceptada |
| · D-P11: fatiga → la IA baja 2 repeticiones con la V24 por capacidades activa → copia → recuperación | `APLICADA`, copia publicada, `RECUPERACION` 16→18 (la referencia) |
| · la IA sube una dosis | rechazada; `RESPALDO`, sin subida publicada |
| · `GAIN_MUSCLE`, viernes acortado (30 min, piso 31) | progresa bajo el piso (D-P7) y en la semana 21 reincorpora el relleno 2×13 exacto; total 182 = techo |
| Invariantes V31/V32/V33/V34 (`linaje_v31_v32.sql` + restricciones) | sin violaciones |
| · **fatiga en los cuatro objetivos** (D-P11) | `LOSE_WEIGHT` y `GENERAL_FITNESS`: la reducción se publica y su copia la hereda. `GAIN_MUSCLE`: 4D la rechaza, como antes (sin reducciones autorizadas). `INCREASE_STRENGTH`: en esa sesión no hay reducción válida (todos los ejercicios en el mínimo de V16 y −1 serie la deja en 29 min, bajo el piso de 31 de V20): se conserva (`RESPALDO`); **ningún rechazo viene de V24** (comprobado en los intentos) |
| · **dosis arbitraria sin linaje** (+1 repetición a mano) en los cuatro objetivos | la copia la rechaza V24 → `NO_FACTIBLE` |
| · **fatiga tras un paso** | `NO_CHANGE` → `VUELTA_ATRAS` 19→18, exactamente la dosis anterior al paso |
| · **dolor tras un paso** | sin paso ni vuelta atrás; V8C no republica el ejercicio que dolió (`NO_FACTIBLE`): manda la seguridad |
| Regresión completa | ver §9 |
| Pruebas heredadas de P2-D contra `fs-p2d` recreada | sin fallos |

Volúmenes por sesión en `ProgresionIT` (semanas 1→5):
`LOSE_WEIGHT` [72,60,72]→[75,63,75]; `GAIN_MUSCLE` [179,156,156]→[182,158,158]; `INCREASE_STRENGTH` [84,84,84]→[87,87,87]; `GENERAL_FITNESS` [116,90,144]→[119,93,147].

## 8. Limitaciones y pendientes, declarados

**Atendidos en esta revisión:** numeración (§6), fatiga en los cuatro objetivos, dosis heredadas válidas y arbitrarias (§7), vuelta atrás y dolor tras un paso (§7), y la herramienta de lectura de Neon (§10). La disponibilidad M se explica en §11.


- **Disponibilidad M por entrenamiento** (§5.5): no se modela en el resolutor; el tiempo lo limitan V4 y el presupuesto de 4D en la revalidación.
- **Encender la progresión activa también la V24 por capacidades en P2-D** (§4.5). Una semana publicada hoy con una dosis que no es la exacta ni tiene paso o reducción registrados pasaría a `NO_FACTIBLE` al copiarse (lo demuestra §7). Cuántas hay en Neon lo dice §10; **no se resuelve relajando V24**.
- **Despliegue:** requiere aplicar V34 en Neon y autorización explícita. No se ha hecho.

## 9. Regresión (tras esta revisión)

- **710 unitarias y 178 de integración, 0 fallos.** Seis omitidas, todas opcionales: las dos simulaciones de factibilidad, las dos corridas pagadas de Replicate (no ejecutadas) y las dos de lectura (`EvaluacionLecturaV24IT`, `SemillaLecturaIT`), que solo corren con su URL.
- `GeneracionReferenciaIT`: **72/72 idénticos** con la V34 renumerada.
- `ProgresionIT`: 17/17. `ProgresionV24V8CTest`: 16/16. Pruebas heredadas de P2-D contra `fs-p2d`: sin fallos.

## 10. Evaluación de Neon en solo lectura (`EvaluacionLecturaV24IT`)

**Qué hace.**
1. Abre la fuente con `setReadOnly(true)` y una transacción `REPEATABLE READ, READ ONLY`. Comprueba que `transaction_read_only = on`; si no, aborta sin leer. Solo ejecuta SELECT.
2. Copia todas las tablas (columnas comunes) a un PostgreSQL **efímero local** creado con las migraciones hasta V33; después aplica la V34 **en la copia**.
3. Para la última semana publicada de cada participante simula la copia del ciclo siguiente con **A** (interruptores apagados, la validación de hoy) y **B** (progresión encendida: V24 por capacidades, dosis heredadas con linaje, reducciones autorizadas).
4. Escribe `target/lectura-v24/informe.md`: por participante, el objetivo, A, B y los códigos de regla que solo aparecen con B. Sin datos personales.

**Lo que importa:** la columna «A publica, B no». Cada fila es una semana que encender la progresión dejaría `NO_FACTIBLE`.

**Cómo ejecutarla** (con credenciales de solo lectura de Neon, y en tu equipo):
```
mvn -o verify -Pintegration -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=EvaluacionLecturaV24IT \
  -Dfitsense.lectura.url='jdbc:postgresql://HOST/DB?sslmode=require' \
  -Dfitsense.lectura.usuario=USUARIO -Dfitsense.lectura.clave=CLAVE
```
Recomendable además: un rol de Neon con solo `SELECT`.

**Ensayo previo (local).** `SemillaLecturaIT` sembró una base local con el ciclo real y la progresión apagada (solo acepta URLs de `127.0.0.1`/`localhost`); después se le quitó la columna de V34 y su fila de Flyway para que se pareciera a Neon. Resultado:

| Participantes | A y B publican | **A publica, B no** | A no, B sí | Ninguno |
|---:|---:|---:|---:|---:|
| 7 | 5 | **1** | 0 | 1 |

- Los cuatro objetivos, semanas normales: A y B publican.
- `GENERAL_FITNESS` con reducción por fatiga aplicada **sin** la V24 por capacidades: B la reconoce como dosis heredada con linaje y publica.
- `GENERAL_FITNESS` con una dosis puesta a mano: A publica, **B no** (V24). Es el caso que hay que contar en Neon.
- `GAIN_MUSCLE` con una dosis puesta a mano: A y B la rechazan (4D, como hoy).
- La fuente no cambió (mismas filas antes y después), y una escritura en esa sesión la rechaza PostgreSQL: `cannot execute UPDATE in a read-only transaction`.

**Sin Docker: `EvaluacionDirectaV24IT`.** Misma evaluación, directa contra Neon (que ya tiene la V34), con la conexión en solo lectura impuesta por el servidor (`options=-c default_transaction_read_only=on`), `readOnlyMode=always` y Hikari `read-only`; Flyway apagado y las tareas programadas sustituidas por simulacros. Antes de evaluar comprueba `transaction_read_only = on`. Ensayada sobre la misma base sembrada: resultado idéntico (7 / 5 / **1** / 0 / 1); con esa opción PostgreSQL rechaza las escrituras (`cannot execute UPDATE in a read-only transaction`).
```
mvnw verify -Pintegration -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=EvaluacionDirectaV24IT \
  -Dfitsense.lectura.directa.url='jdbc:postgresql://HOST/DB?sslmode=require' \
  -Dfitsense.lectura.usuario=USUARIO -Dfitsense.lectura.clave=CLAVE
```

**Si Neon muestra filas «A publica, B no»,** las opciones se deciden con los datos delante, sin relajar V24: dejar la progresión apagada para esos participantes o regenerar su semana, por ejemplo. No se ha implementado ninguna.

## 11. Disponibilidad M por entrenamiento

**Hallazgo:** M **no existe en el backend.** Solo está descrita en el diseño contextual v0.3 (`DISENO_ADAPTACION_CONTEXTUAL.md` §1; `lackOfTimeContext` y `availableMinutes` en la omisión), que no se ha implementado: no hay columna, ni campo en `POST /workouts/{id}/skip`, ni uso en la adaptación (`forcedSessionMinutes` existe, pero las órdenes de P2-D no lo fijan). Por tanto **hoy ninguna sesión tiene una M declarada**, y la condición «no reincorporar por encima de M» no puede darse.

**Qué limita hoy el tiempo de un paso o de una reincorporación:** V4 (minutos del perfil + 15 %) y, en `GAIN_MUSCLE`, el presupuesto de 4D, en la revalidación completa. En `ProgresionIT`, el viernes reincorporado dura 34 min con un perfil de 45.

**Propuesta para cuando se implemente M** (no hecha, requiere aprobación): el resolutor tomará como límite de cada sesión su M; una reincorporación cuya duración estimada supere M quedará EN ESPERA DE TIEMPO (sin publicarse), y si la retirada se debió a falta de tiempo con M, solo volverá con una M nueva mayor (§5.5 del diseño).

## 12. V34 en Neon (27/10/2026)

Flyway aplicó la V34 al arrancar la aplicación local contra Neon (`Current version 33` → `now at version v34`). Comprobaciones en solo lectura, hechas por la persona responsable:

| Comprobación | Esperado | Resultado |
|---|---|---|
| Las cinco restricciones `ck_paa_*` de V34 | 5 filas | 5 |
| Columna `restored_sessions` | 0 | 0 |
| `fn_intento_coherente` sin `restored_sessions` | true | true |
| `fn_intento_coherente` con `progressed_sessions` | true | true |
| Intentos con `progressed_sessions` no vacío | 0 | 0 |

Con los interruptores apagados, el comportamiento no cambia. Sigue pendiente `EvaluacionLecturaV24IT` (§10) antes de encender nada.

## 13. Neon: datos sintéticos de desarrollo (27/10/2026)

`EvaluacionDirectaV24IT` sobre Neon (solo lectura, Flyway 34): 26 perfiles. A publica y B no en 5 de ellos (7, 10, 19, 22 y 25). Todos son **sintéticos**, generados con versiones anteriores de las reglas (planes del 12/10, V25 endurecida el 26/10). Decisión: **no se modifica V24 ni V25** para hacer compatibles esos datos; queda en suspenso la propuesta de separar V25 del interruptor. Lo que importa es si una rutina generada hoy funciona (§14).

## 14. Evaluación limpia: rutinas generadas hoy, 10 semanas (`EvaluacionLimpiaProgresionIT`)

Base efímera, progresión encendida (con ella la V24 por capacidades y V25 en la adaptación), IA simulada que solo hace lo que permite cada intervención. Los 72 perfiles de referencia, con este guion: semanas 1–3 todo hecho; 4, viernes omitido por fatiga; 5, todo hecho; 6, viernes omitido por falta de tiempo; 7–10, todo hecho. Evidencia completa: `evidencia/progresion/evaluacion_limpia.md`.

| Objetivo | Perfiles | Inicial válida (V24+V25) | Semanas publicadas | Sin publicar | Progresión | Recuperación | Reincorporación | Adaptación aplicada | Respaldo | Sin efecto |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| LOSE_WEIGHT | 18 | 18 | 180 | 0 | 56 | 0 | 6 | 18 | 14 | 4 |
| GAIN_MUSCLE | 18 | 18 | 180 | 0 | 90 | 0 | 0 | 12 | 18 | 6 |
| INCREASE_STRENGTH | 18 | 18 | 180 | 0 | 88 | 2 | 0 | 4 | 14 | 18 |
| GENERAL_FITNESS | 18 | 18 | 180 | 0 | 78 | 12 | 0 | 31 | 5 | 0 |

**Respuesta:** sí. Las 72 rutinas generadas hoy pasan V24 y V25 al generarse, y las **720 semanas** siguientes se publicaron todas: ninguna `NO_FACTIBLE` ni rechazada. Cada una pasó V24, la evidencia de hipertrofia y V25 en la validación de la adaptación.

**Lectura:**
- `GAIN_MUSCLE`: la reducción por fatiga la rechaza 4D (respaldo), como siempre.
- `INCREASE_STRENGTH`: no tiene rellenos, así que acortar sale sin efecto.
- Las reincorporaciones en 10 semanas solo llegan en `LOSE_WEIGHT`; en los demás objetivos la reserva R2 necesita más pasos (en `ProgresionIT`, `GAIN_MUSCLE` reincorpora en la semana 21).
- `LOSE_WEIGHT` de 30 minutos no progresa en 10 semanas: el menor paso supera el 5 % de una sesión pequeña («no se fuerza»).

**Hallazgo (desviación del diseño §3.2):** cuando la IA redujo una **serie** (por ejemplo 3×10 → 2×10 en un ejercicio que ya está en su mínimo de repeticiones), devolverla supera el 10 % del paso de recuperación. El resolutor no la recupera y **sigue con la progresión ordinaria de otros ejercicios** de esa sesión. El diseño dice que la progresión ordinaria solo ocurre «sin reducción pendiente». Pasa en `LOSE_WEIGHT` (0 recuperaciones) y en parte de `INCREASE_STRENGTH`. No viola V24 ni V25. Queda pendiente de decisión.
