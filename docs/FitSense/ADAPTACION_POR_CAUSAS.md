# Adaptación por causas

**Fecha:** 30/10/2026. Implementa `DISENO_ADAPTACION_CONTEXTUAL.md` v0.3:
- D1, D2, D6 y D7 se implementan en V42;
- D4 ya estaba;
- la restauración automática (§3) **no** se reintroduce: la sustituyó `DISENO_PROGRESION_Y_REINCORPORACION.md`, y la progresión sigue apagada.

**Regla de versión:** `adaptation_rule_version` vale ADAPT-1 en las filas V32 existentes y ADAPT-2 en las nuevas.

## 1. Matriz de motivos, subcausas e intervenciones

Columnas de permisos: S = sustituir ejercicio, D = cambiar dosis, R = reprogramar el día, Q = retirar relleno. La comprobación final es siempre V8C.

| Motivo de la omisión | Subcausa | Intervención | IA | S | D | R | Q |
|---|---|---|---|---|---|---|---|
| `LACK_OF_TIME` | sin respuesta | **CONSERVAR** (D1) | no | – | – | – | – |
| | IMPREVISTO | **CONSERVAR** | no | – | – | – | – |
| | SIN_TIEMPO_ESE_DIA | **CONSERVAR** | no | – | – | – | – |
| | MENOS_TIEMPO_HABITUAL con M ≥ minutos de la sesión | **CONSERVAR** (la sesión ya cabe) | no | – | – | – | – |
| | MENOS_TIEMPO_HABITUAL con M < minutos de la sesión | ACORTAR_O_REORGANIZAR, **con techo M** | sí | – | sí | – | sí |
| `LACK_OF_TIME` en una sesión **parcial** | — | ACORTAR_O_REORGANIZAR (D4: hay evidencia de que no cupo) | sí | – | sí | – | sí |
| `SCHEDULE_CHANGE` | — | REPROGRAMAR_DIAS | sí | – | – | sí | – |
| `TOO_DIFFICULT` | — | VARIANTE_MAS_SENCILLA_O_CARGA | sí | sí | sí | – | – |
| `PAIN_OR_DISCOMFORT` | — | SEGURIDAD_ANTE_DOLOR (revisión de seguridad) | sí | sí | sí | – | – |
| `FATIGUE` | — | REVISAR_CARGA_Y_RECUPERACION | sí | – | sí | – | – |
| `LACK_OF_MOTIVATION` | — | INVESTIGAR_CAMBIO_MINIMO | sí | – | sí | – | – |
| `OTHER` o sin motivo | — | CAUSA_NO_REGISTRADA (D6: no se inventa una causa) | no | – | – | – | – |

**API (compatible con los clientes actuales):**
- `POST …/skip` acepta, además de `reason`, dos campos opcionales: `lackOfTimeContext` y `availableMinutes` (de 15 a 180).
- Sin esos campos se comporta como antes, salvo que la falta de tiempo sin subcausa ya no acorta.
- Validaciones:
  - el contexto solo se admite con `LACK_OF_TIME`;
  - los minutos, solo y siempre con MENOS_TIEMPO_HABITUAL.
- Las mismas reglas están como CHECK en V42.

**Techo M:** si la IA devuelve una sesión acortada que sigue durando más de M, V8C la rechaza y no figura como aplicada. Si con M no cabe ninguna sesión válida (V20), el resultado es **RESPALDO**: se publica la sesión original y queda la evidencia en V32.

## 2. Caso A01

| | ADAPT-1 (antes) | ADAPT-2 (ahora) |
|---|---|---|
| El viernes se omite por `LACK_OF_TIME`, sin más datos | ACORTAR_O_REORGANIZAR y llamada a la IA. La sesión se recorta cada semana que se repite | CONSERVAR, sin IA. V32 registra SIN_EFECTO con 0 intentos |
| La misma omisión, declarando «suelo tener 30 min» para una sesión de 43 | igual que la fila anterior | ACORTAR con techo 30. La IA solo puede acortar y V8C comprueba los 30 min |

`A01AdaptacionRealIT` (IA real, de pago) **no se ejecutó**. Esa prueba omite sin subcausa; con ADAPT-2 conservaría la sesión sin llamar a la IA.

## 3. Escenarios de falta de tiempo (`AdaptacionPorCausasIT`)

| Prueba | Resultado verificado |
|---|---|
| `faltaDeTiempoSinEvidenciaDeRecorte` (sin respuesta, IMPREVISTO, SIN_TIEMPO_ESE_DIA, y MENOS_TIEMPO_HABITUAL con M ≥ la sesión) | CONSERVAR (SIN_EFECTO, CONSERVED, ADAPT-2): 0 llamadas a la IA, sesiones idénticas y perfil sin cambios |
| `menosTiempoHabitualAcorta` | ACORTAR: la sesión publicada dura como mucho M |
| `menosTiempoHabitualInviable` (M = 15) | RESPALDO tras 3 intentos rechazados. No se publica nada más largo que la original |
| `sinCausaNoLlamaALaIA` (`OTHER`) | CAUSA_NO_REGISTRADA sin IA (D6) |
| `contratoDelSalto` | El contexto y los minutos incoherentes se rechazan |

## 4. Dolor, fatiga, dificultad, horario y motivación

Comprobado en `ValidacionMultisemanaIT`, en los cuatro objetivos:

| Motivo | Decisión | Qué se comprobó |
|---|---|---|
| Dolor | SEGURIDAD_ANTE_DOLOR, APLICADA | El ejercicio que dolió **no vuelve** la semana siguiente; se sustituye por uno elegible del mismo patrón y grupo |
| Fatiga | REVISAR_CARGA_Y_RECUPERACION | No se sube la dosis. SIN_EFECTO o APLICADA según el objetivo; en GAIN_MUSCLE no hay reducción de volumen (V24) |
| Demasiado difícil | VARIANTE_MAS_SENCILLA_O_CARGA | La sustitución pasa V15 y la dificultad |
| Horario | REPROGRAMAR_DIAS | Solo a días disponibles (V3) |
| Desmotivación | INVESTIGAR_CAMBIO_MINIMO | Cambio mínimo; ninguna sesión no señalada cambia |

**Sin reducciones indefinidas:**
- Solo acorta ACORTAR (con evidencia) y ninguna otra intervención puede retirar ejercicios (`TipoIntervencionTextoCoherenteTest`).
- La herencia de una reducción la corta cualquier intervención nueva (D8, `HerenciaD8IT`).
- En las secuencias «alterna» ninguna sesión se recorta sin causa.

## 5. Varios motivos

- **Dolor y falta de tiempo en la misma semana** (`dolorYFaltaDeTiempoEnLaMismaSemana`): cada sesión recibe la intervención de su motivo. La de dolor se resuelve aunque no sea la peor.
- **Completada con dolor** (`cumplidaConDolor`): la adherencia es buena, pero se emite ADAPT_LOCALIZED solo con esa sesión, como SEGURIDAD_ANTE_DOLOR.
- **Prioridad:** si hay dolor, sus sesiones siempre se señalan. El resto sigue la adherencia (la peor en una adaptación localizada; todas las problemáticas en una ampliada).
- Ver la corrección 3 de `CORRECCIONES_IMPLEMENTADAS.md`.

## 6. Resultado posterior por sesión

`consultas/resultado_sesion_adaptada.sql` clasifica cada sesión adaptada publicada:

| Clase | Criterio |
|---|---|
| COMPLETADA | ≥ 0,80 |
| PARCIAL_VALIDA | de 0,30 a 0,80 |
| PARCIAL_NO_VALIDA | menos de 0,30 |
| OMITIDA | omitida con causa, con el indicador `misma_causa` |
| SIN_INFORMACION | sin sesión y sin omisión declarada |

La consulta da también los indicadores de §4 del diseño. Lo prueba `resultadoPorSesionAdaptada`.

**Es un resultado observado, no una prueba de que la intervención ayudara.**

## 7. Implementado frente a pendiente de decisión

| Implementado | Pendiente de decisión |
|---|---|
| D1, D2, D6 y D7 (V42); D4 ya existía | Qué hacer cuando la persona **cambia sus días o su equipamiento** (`VALIDACION_MULTISEMANA.md` §4) |
| CONSERVAR sin IA | Si un RESPALDO por un techo M inviable debe contarse aparte en los indicadores |
| Dolor con adherencia buena | Ninguno más en este bloque |
| Indicador por sesión adaptada (consulta) | Persistirlo como métrica (requeriría una tabla nueva) |

**Lo que necesita el cliente Flutter:** preguntar la subcausa al omitir por falta de tiempo. **Este repositorio no lo cambia.** Sin esa pregunta, toda falta de tiempo se conserva (D1).
