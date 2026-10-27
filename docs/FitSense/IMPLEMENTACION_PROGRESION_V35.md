# Implementación controlada · progresión y reincorporación condicional (V35)

**Estado:** implementado en el repositorio, **apagado por defecto**, sin desplegar. Neon no se ha tocado.
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

**Evidencia:** `plan_adaptation_attempts.progressed_sessions` (V35) y `conservation_report.pasos` (tipo, ejercicio, dosis antes/después, volúmenes, base, techo, estado de la reserva).

## 6. V35 y bases de datos (requisito 8)

- `V35__progresion_de_sesiones.sql` sustituye a la V35 de restauración (nunca aplicada en Neon, D-P10 cerrada). Añade `progressed_sessions` con `ck_paa_progresadas_unicas`, `ck_paa_progresadas_disjuntas`, `ck_paa_un_paso`, `ck_paa_paso_sin_orden` y `ck_paa_paso_publicado`, y recrea `fn_intento_coherente`.
- La propuesta V34 no se toca; entrará como V36 o posterior.
- **`fs-p2d` (local) recreada** desde V1 hasta la V35 nueva, más la marca `fitsense_entorno = PRUEBAS` y el usuario de prueba 1 que exigen las pruebas heredadas. Ya no tiene `restored_sessions`.

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
| Invariantes V31/V32/V33/V35 (`linaje_v31_v32.sql` + restricciones) | sin violaciones |
| Regresión completa | 710 unitarias y 166 de integración, 0 fallos; 4 omitidas (2 simulaciones opcionales y las 2 corridas pagadas de Replicate, no ejecutadas) |
| Pruebas heredadas de P2-D contra `fs-p2d` recreada | sin fallos |

Volúmenes por sesión en `ProgresionIT` (semanas 1→5):
`LOSE_WEIGHT` [72,60,72]→[75,63,75]; `GAIN_MUSCLE` [179,156,156]→[182,158,158]; `INCREASE_STRENGTH` [84,84,84]→[87,87,87]; `GENERAL_FITNESS` [116,90,144]→[119,93,147].

## 8. Limitaciones y pendientes, declarados

- **Disponibilidad M por entrenamiento** (§5.5): no se modela en el resolutor; el tiempo lo limitan V4 y el presupuesto de 4D en la revalidación.
- **Medición D-P11 con el arnés de §12.5:** no se ha repetido sobre el código real; la prueba integrada cubre un caso de `GENERAL_FITNESS`. Recomendado antes de activar en Neon.
- **Encender la progresión activa también la V24 por capacidades en P2-D** (§4.5). Las semanas conservadas de participantes cuyos planes anteriores no cumplan hoy la V24 por capacidades podrían salir `NO_FACTIBLE`; hay que comprobarlo sobre los planes reales (en solo lectura) antes de encenderla.
- La vuelta atrás tras fatiga está implementada y pasa por V8C, pero en la prueba integrada la fatiga declarada no llegó a probarse con un ciclo `NO_CHANGE` aislado.
- **Despliegue:** requiere aplicar V35 en Neon y autorización explícita. No se ha hecho.
