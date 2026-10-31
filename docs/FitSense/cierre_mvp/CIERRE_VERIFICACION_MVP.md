# Cierre de la verificación del MVP

**Fecha:** 31/10/2026.
**Orden:** «ORDEN DE CIERRE DEL MVP: REINCORPORACIÓN CON IA REAL, TRAZABILIDAD Y SIMULADOR».
**Rama:** `cierre-verificacion`, desde `generation` (`f4bac7a`).

| Documento | Contenido |
|---|---|
| `REINCORPORACION_CON_IA_REAL.md` | El escenario completo |
| `HISTORIAL_REINCORPORACION.csv` | Una fila por ciclo |
| `TRAZABILIDAD_EJERCICIO_RETIRADO.md` | El ejercicio retirado, antes, durante y después |
| `VALIDACION_REINCORPORACION_4D.md` | Compatibilidad con 4D v3.2/v3.3 |
| `CORRECCIONES_SIMULADOR.md` | Ids reproducibles, LIVE/REPLAY y validación independiente |
| `ADAPTACIONES_NO_FACTIBLES_API.md` | Qué ve el cliente |
| `evidencia/` | Predicciones reales sin credenciales, CSV y logs |

## 1. Auditoría previa del contrato de reincorporación (fase 1)

Leída en el código vigente (`ProgresionResolver`, `EvidenciaV32`, `DosisLinajeResolver`, `ReduccionAutorizadaResolver`, V8C, V24, V31/V32/V34 y V35), no en las pruebas antiguas.

| # | Pregunta | Respuesta |
|---:|---|---|
| 1 | ¿Qué intervención permite retirar? | **ACORTAR_O_REORGANIZAR** (`permiteReducirDuracion`). SEGURIDAD retira o sustituye el ejercicio que dolió, pero eso **no** es reincorporable |
| 2 | ¿Qué ejercicios? | Una colocación. En GAIN_MUSCLE, solo de relleno (sin slot de 4C). V8C no deja combinar la retirada con cambios de dosis |
| 3 | ¿Cómo se registra? | V32 `applied_sessions` con la intervención ACORTAR, y V31 de la derivada a la medida. `retirado()` exige exactamente un quitado, ningún añadido y ninguna sustitución |
| 4 | ¿Qué permite progresar? | Dos semanas consecutivas contadas desde el último paso, con O→S como copia y una decisión NO_CHANGE. Las dos, al **100 % real**: adherencia, cobertura, todas las sesiones y todos los ejercicios completos. Sin fatiga ni dolor, todos los ejercicios elegibles y el paso ≤ 5 % de la sesión y ≤ techo |
| 5 | ¿Cuándo está completa la etapa de progresión? | Cuando ningún conservado puede dar otro paso dentro de 1,10 × V_base − V_ret (la reserva PENDIENTE) |
| 6 | ¿Cuándo se reincorpora? | Etapa completa, **al menos una PROGRESION después del acortamiento** (1(a)) y el criterio de dos semanas (≥ 80 %), V_actual + V_ret ≤ techo. **Ahora también** que quepa en la M vigente |
| 7 | ¿Con qué dosis? | La inicial exacta de su clase (D-P8, `exacta`), al final de la sesión |
| 8 | ¿Qué la bloquea? | Dolor en el retirado, exclusión de seguridad o que deje de ser elegible (DESCARTADA). V4, colocaciones o presupuesto: la sesión se mantiene sin la reincorporación y la progresión sigue con EN_ESPERA. La M (ver §4), linaje roto o ambiguo, ninguna progresión previa o semanas no cumplidas |
| 9 | ¿Cómo se conserva el linaje? | V31 en cada sesión. V32 una fila por ciclo con un único resultado por sesión. V34 `progressed_sessions` con un paso como máximo. Los pasos van en `conservation_report.pasos` |
| 10 | ¿Qué consta al mantener? | V32 SIN_ORDEN con `progressed_sessions` vacío; los pasos que no pasaron la revalidación, en `pasos_bloqueados` |

## 2. Matriz de verificación final

| Componente | Evidencia | Estado |
|---|---|---|
| Adaptación con IA real | Predicción `4rckyzeapdrmw0d1j02atkdqf4`, aceptada al primer intento (V32 APLICADA) | **Verificado con IA real** |
| Retirada | 480 ausente del jueves publicado (planes 2 a 10) | **Verificado** |
| Persistencia | V31 2 → 4 → … → 20 y V32 por ciclo; consulta de linaje sin violaciones | **Verificado** |
| Ejecución posterior | Las semanas 2 a 10, registradas con los comandos reales (COMPLETED) | **Verificado** |
| Adherencia | Observada 100 % y cobertura 100 % de V32 en cada ciclo (67,5 % en la semana del problema) | **Verificado** |
| Progresión | 7 pasos del backend con 0 llamadas; tres en el jueves (90 → 99) | **Verificado** |
| Reincorporación | 480 con la dosis inicial 2×10 / 90 s, 99 → 119 ≤ 121 | **Verificado** |
| Ejecución reincorporada | Semana 11: 480 COMPLETED (2 series y 20 repeticiones) y mantenimiento en el ciclo 11 | **Verificado** |
| Seguridad | Sin dolor ni exclusiones. Negativas: dolor, no elegible, incumplimiento, sin registro, fatiga y M | **Verificado** (negativas deterministas) |
| Simulador | Doble repetición idéntica (48/48) e ids traducidos por clave lógica | **Verificado** |
| Contabilización | LIVE, REPLAY y SIN_SALIDA separados; V32 sin alterar | **Verificado** |
| No factibilidad | `adaptation.resultado` y `/users/me/plan/adaptations`, con pruebas por el controlador | **Verificado** (deterministas, con IA simulada: la orden no exige IA real aquí) |

## 3. Las 16 preguntas

**1. ¿Se retiró realmente un ejercicio mediante una adaptación de Replicate real?**
Sí: el relleno 480 de la sesión del jueves (GAIN_MUSCLE, 30 min, 2 días), en el ciclo de la semana 1.

**2. ¿Qué predicción produjo la retirada?**
`4rckyzeapdrmw0d1j02atkdqf4` (openai/gpt-5-structured, gpt-5-mini): 54 741 tokens de entrada, 1 738 de salida y 24 s. Está completa en `evidencia/1_ia_real_GAIN_MUSCLE_30min_2dias/predicciones/`.

**3. ¿Se publicó correctamente el plan adaptado?**
Sí. El plan 2 lleva el jueves con 72, 715 y 513 a 3×10 / 90 s, y 18 min calculados por el backend. Pasó V8C y V1–V25 (V20 exento por ACORTAR) y la validación independiente.

**4. ¿La persona ejecutó la sesión adaptada?**
Sí. Las semanas 2 a 10, completas, con los comandos reales de ejecución.

**5. ¿Cuántas semanas necesitó para ser elegible para progresión?**
Dos semanas completas después de la retirada (2 y 3). El jueves fue elegible desde el ciclo 3. Ese paso fue para el lunes, que va primero por fecha cuando ninguna sesión ha progresado, y el jueves dio su primer paso en el ciclo 4.

**6. ¿Qué progresiones aplicó el backend?**
Siete, una por ciclo y alternando sesiones:

| Sesión | Ciclo | Paso |
|---|---|---|
| Lunes | 3 | 144 de 3×10 a 3×11 |
| Lunes | 5 | 144 a 3×12 |
| Lunes | 7 | 125 de 3×10 a 3×11 |
| Lunes | 9 | 125 a 3×12 |
| Jueves | 4 | 72 de 3×10 a 3×11 |
| Jueves | 6 | 72 a 3×12 |
| Jueves | 8 | 715 de 3×10 a 3×11 |

Cada paso fue de +3 de volumen (≤ 5 %), dentro del techo y sin IA.

**7. ¿Cuándo fue elegible para reincorporar?**
En el ciclo 10. Después de tres pasos del jueves, ningún conservado podía dar otro dentro de la reserva (99 + 3 > 121 − 20), y 99 + 20 ≤ 121.

**8. ¿Con qué dosis volvió el ejercicio?**
2×10 / 90 s, su dosis inicial (RELLENO_ESTANDAR de 4D), al final de la sesión.

**9. ¿Se entrenó después de reincorporarse?**
Sí. En la semana 11 el jueves se completó con 480 (2 series, 20 repeticiones, COMPLETED). El ciclo siguiente lo mantuvo, sin duplicado y sin otro paso.

**10. ¿Por qué se bloquearía en casos inseguros?**
Con estas pruebas, todas sin reincorporación en 12 semanas:

| Situación | Por qué se bloquea | Prueba |
|---|---|---|
| Ejercicio retirado por dolor | No es una retirada de ACORTAR | `retiradoPorDolorNoVuelve` |
| Ejercicio no elegible | Reserva DESCARTADA | `retiradoNoElegibleNoVuelve` |
| Sin retirada previa | No hay nada que reincorporar | `sinRetiradaNoSeReincorpora` |
| Adherencia insuficiente, semana sin registro o fatiga | Sin las dos semanas de cumplimiento | `incumplimientoNoReincorpora` ×3 |
| M declarada | EN ESPERA DE TIEMPO | `conMNoSeReincorpora` |
| Sin progresión previa | Falta el paso que exige 1(a) | `ReincorporacionTrasProgresionTest` y `PendientesProgresionIT` 1(a) |
| Techo, colocaciones y simultaneidad | Comprobados en cada ciclo del escenario real | — |

**11. ¿El simulador reproduce correctamente sesiones y ejercicios?**
Sí. Cada usuario tiene su `user_id` original y las sesiones se traducen por (usuario, fecha), con error si la correspondencia no es única. Dos repeticiones dieron 48 de 48 semanas iguales y el mismo registro lógico.

**12. ¿Distingue llamadas reales y respuestas reproducidas?**
Sí: columnas LIVE, REPLAY y SIN_SALIDA, y la coherencia `V32 = LIVE + REPLAY + SIN_SALIDA`, sin alterar V32.

**13. ¿Las validaciones independientes reconocen las autorizaciones legítimas?**
Sí, y siguen rechazando lo no autorizado:
- 0 INVALIDA en 48 semanas repetidas (antes, 29 en 64).
- Una dosis puesta a mano da INVALIDA.
- Un linaje ambiguo da NO_CONCLUYENTE.

**14. ¿El backend distingue una adaptación imposible de una aplicada?**
Ahora también hacia el cliente: `adaptation.resultado` en el plan y `GET /users/me/plan/adaptations`. Ningún respaldo ni conservación se presenta como ajuste. El NO_FACTIBLE por seguridad se informa sin publicar la sesión insegura.

**15. ¿Cuánto costó la prueba?**
**5 llamadas reales, ≈ US$ 0,066**, del máximo autorizado de 12 llamadas y US$ 3:

| Escenario | Llamadas | Coste |
|---|---:|---:|
| LOSE_WEIGHT (4 reducciones de dosis válidas, no retiradas) | 4 | ≈ US$ 0,049 |
| GAIN_MUSCLE (la retirada) | 1 | ≈ US$ 0,017 |

El total global de la campaña queda en **58 de 80** llamadas. Todo lo demás (ensayos secos, repeticiones, negativas, 4D y API) fue sin coste.

**16. ¿Qué errores y limitaciones siguen abiertos?**
Ver §5.

## 4. Errores encontrados y corregidos

| # | Hallazgo | Tipo | Corrección | Prueba |
|---:|---|---|---|---|
| 1 | La reincorporación ignoraba la **M declarada** al acortar; el diseño aprobado (§5, fila 5) exige una M nueva mayor | Backend (implementación incompleta) | `ProgresionResolver.mVigente`: sin cabida en M, la reserva pasa a EN ESPERA DE TIEMPO | `CopiaSinIaIT.conMNoSeReincorpora`; `CopiaSinIaIT` y `ProgresionIT` pasan a acortar desde una sesión parcial (premisa justificada) |
| 2 | Ids no reproducibles en la repetición: sesiones ajenas y planes distintos por la semilla | Simulador | `user_id` original y clave lógica (usuario, fecha) | Doble repetición: 48/48 |
| 3 | V32 hacía parecer pagadas las respuestas repetidas | Simulador | LIVE, REPLAY y SIN_SALIDA | Columna `contabilidad_live_replay` |
| 4 | Validación independiente sin autorizaciones (29 falsos INVALIDA) | Simulador | `ValidacionIndependiente` desde V31/V32 en crudo | `ProgresionIT.validacionIndependienteDistingue` |
| 5 | El cliente no podía distinguir una conservación, un respaldo o una imposibilidad de una adaptación | API | `AdaptationOutcome`, el campo `adaptation`, `/plan/adaptations` y el motivo de factibilidad en V32 | `AdaptationOutcomeTest`, `AdaptacionPorCausasIT` y `AdaptacionCompletaIT` |
| 6 | El primer harness desactivaba la adaptación al desactivar la generación (comparten `isEnabled`) | Simulador | La generación se rechaza sin llamada y el motor de reglas publica | Ensayo seco |

**Hallazgo sin cambio de código:** la retirada de 864 en U08 S7 (campaña) se aceptó con una duración **declarada** de 24 min para un contenido de al menos 30.
- Desde el PR #13 el backend calcula la duración, y esa propuesta ya no cabría en M.
- La campaña publicó entonces una sesión unos 6 min por encima de la M declarada. Ya no es posible.

## 5. Limitaciones y pendientes

- **Una sola reincorporación con IA real.** La ruta depende del perfil:
  - con 3 sesiones por semana, los pasos se reparten y la reincorporación no llega en 16 semanas;
  - en los objetivos por capacidades, la IA prefiere reducir dosis antes que retirar (4 de 4), lo que es válido pero no es una retirada.
- **Reincorporar tras un acortamiento con M** exige una M nueva mayor, que hoy solo se declara con otra omisión. Una forma de actualizarla sería una función nueva y no se implementó.
- **`messageShown`** (`/users/me/interventions`) sigue escribiéndose antes del resultado. La app debe usar `adaptation.resultado` (propuesta en `ADAPTACIONES_NO_FACTIBLES_API.md` §6).
- **Validación humana:** no hubo evaluadores humanos; esta es una verificación automática.
- **Sin cambios en:** Neon, Railway, Flutter y `main`.
- **Sin despliegue y sin migraciones nuevas.**
- La progresión sigue apagada fuera de las pruebas aisladas (`fitsense.adaptation.progression.enabled` solo en las IT).

## 6. Regresión

Ver la sección «Pruebas» del PR:
- 884 pruebas unitarias en verde;
- las IT dirigidas;
- CI completo una vez sobre la candidata final.
