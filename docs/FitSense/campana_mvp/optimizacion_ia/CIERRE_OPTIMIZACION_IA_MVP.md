# Cierre · optimización de la IA real y cobertura del MVP

**Fecha:** 31/10/2026.
**Orden:** «FITSENSE — ORDEN DEFINITIVA: OPTIMIZACIÓN DE IA REAL Y CIERRE DE COBERTURA DEL MVP».
**Rama:** `optimizacion-ia`, sobre `generation`.

**Principio aplicado:**
1. El backend calcula las restricciones y las opciones válidas.
2. La IA elige o propone dentro de esos límites.
3. El backend valida la propuesta completa.
4. Solo entonces se publica.

No cambia ni el modelo ni la arquitectura.

| Documento | Fase |
|---|---|
| `AUDITORIA_48_PREDICCIONES_IA.md`, `RECHAZOS_IA.csv` y `predicciones_48.json` | A |
| `MEJORAS_CONTRATO_BACKEND_IA.md` | B y D |
| `FACTIBILIDAD_PREVIA_IA.md` | C |
| `VERIFICACION_GAIN_MUSCLE_CASA_60MIN.md` y `../../ADENDA_4D_v3_3.md` (con la corrección de `../../ADENDA_4D_v3_2.md`) | E |
| `COMPARACION_IA_ANTES_DESPUES.md` y `evidencia_foco/` | F |
| Este documento | G y cierre |

## Las diez preguntas

### 1. ¿Qué errores eran del modelo?

**39 de los 115 problemas** (causa 3). La IA tenía la información y no la cumplió:
- V15: más de la mitad de la sesión en un grupo.
- V23: patrones obligatorios sin cubrir.
- V25: capacidad principal omitida.
- Parte de V24 (dosis de otra capacidad).
- Parte de V8C (sustituir por otro patrón, o sustituir donde no se permite).

Hubo además 1 fallo del proveedor (un timeout de U06).

**Por predicción:** 7 de las 33 rechazadas eran errores puros del modelo o del proveedor.

Siguen siendo responsabilidad de la IA, y el backend las rechaza igual que antes.

### 2. ¿Cuáles eran del backend o del contexto?

**46 problemas:**

| Causa | Problemas | Qué faltaba o contradecía |
|---|---:|---|
| 1 · Restricción ausente | 3 | La corrección no enviaba las fechas ocupadas ni los días contiguos (V12, V18) |
| 2 · Restricción ambigua | 20 | «Bajar la dosis» no decía que el descanso no se toca ni los rangos admitidos (V24) |
| 5 · Contexto contradictorio | 23 | Los principios 6, 8 y 9 pedían progresar dentro de una corrección (10 V8C); GAIN_MUSCLE recibía candidatos de duración (11 V24); ACORTAR prometía una reducción que GAIN_MUSCLE no tiene |

**Otras causas:**
- **Mixtos (causa 4), 24 problemas:** V17 y V20, aritmética que la IA no debía hacer.
- **Imposibles (causa 7), 5 problemas:** U01 S2 y U04 S5, donde ninguna operación permitida cabía.

**Defectos del validador (causas 6 y 9):** ninguno en la auditoría. Durante la comparación se encontró uno en el cambio nuevo de duración: ver la pregunta 4.

### 3. ¿Cuántos rechazos eran evitables?

**De los 115 problemas:** 62 sí, 29 en parte y 24 no.

**Por predicción:** 8 de las 33 rechazadas eran evitables del todo y 18 en parte.

### 4. ¿Qué cambió en prompts y contratos?

Detalle en `MEJORAS_CONTRATO_BACKEND_IA.md`.

- **B1, dosis:** el snapshot de GAIN_MUSCLE ya no lleva candidatos sin hipertrofia.
- **B2, duración:**
  - El backend fija `expected_duration_minutes` del contenido, en la generación (`PlanDraftNormalizer`) y en la adaptación (`AdaptedWeekProducer`).
  - El prompt da el mínimo de V20 en segundos y los segundos por clase de dosis.
- **B3, correcciones:**
  - No se progresa, el descanso no se toca, y hay una línea DOSIS por sesión (en GAIN_MUSCLE, solo retirar un relleno).
  - Además, DURACIÓN calculada, FECHAS YA OCUPADAS y SUSTITUTOS ADMITIDOS por ejercicio.
  - CONSERVAR sigue sin ir a la IA.
- **B4, reprogramar:** FECHAS LIBRES VÁLIDAS. El perfil no se toca.
- **C, factibilidad previa:** `FactibilidadPreviaIA` evita la llamada solo si demuestra que ninguna propuesta cabe.
- **D, reintentos:** siguen siendo 3. Cada motivo apunta al dato que lo resuelve (`PistasDeReintento`).
- **Defecto encontrado con la IA real y corregido:** la duración calculada no se aplicaba si la IA repetía los minutos de antes con un contenido nuevo.
  - Caso: U02 S3, 23 min declarados para 16 de contenido.
  - Prueba: `AdaptedWeekProducerTest.minutosRepetidosConContenidoNuevo`.

### 5. ¿Cuántas llamadas se evitan?

**En los casos de la campaña:**
- **7 llamadas** evitadas por factibilidad previa: U01 S2 (2), U04 S5 (3) y U07 S2 (2). Ninguna podía terminar en una adaptación válida.
- **4 llamadas** menos en los dos casos que sí llaman (U02 S3 y S5): de 6 a 2, sin reintentos.

**En total:** en los cinco casos problemáticos, de 13 llamadas a 2.

CONSERVAR sigue sin llamada (0 cambios, 0 llamadas).

### 6. ¿Mejoró la proporción de propuestas válidas?

**En los casos medidos, sí:**
- Aceptación al primer intento: 0 de 5 operaciones antes, **2 de 2** después.
- Rechazos por reglas: de 47 problemas a 0.
- Adaptaciones aplicadas: de 0 a 2.

**Pero no se puede estimar una tasa general:** son dos casos con llamada, con un modelo no determinista. La afirmación honesta es que **los rechazos evitables desaparecieron donde se midieron**.

**La mejora no viene de aceptar más:**
- Al repetir 21 respuestas antiguas con el backend actual (`evidencia_foco/0_repeticion_sin_llamadas`), las 9 propuestas de generación que antes se rechazaban **se siguen rechazando**.
- Todas las semanas llegan con el mismo origen.

### 7. ¿Qué pasa con las 8 configuraciones de casa?

**Resueltas: 8/8**, con plan válido en V1–V25 y sin relajar V15 ni V20. En total, 24/24 de GAIN_MUSCLE de 5–6 días a 60 min.

**Cómo:**
- Se verificó primero que no existía una composición válida con las dosis anteriores. El día PULL de casa no pasa de 2430 s, y el mínimo es 2520.
- Después se aplicó la extensión condicionada, 4D v3.3: el bíceps de relleno estándar puede pasar de 2×10 a 3×10, solo en los días PULL de GAIN_MUSCLE y solo si sin esa serie la sesión no llega al mínimo.
- No es general para RELLENO_ESTANDAR.
- La adenda 4D v3.2 se corrigió para reflejar el orden real del código.

### 8. ¿Se mantienen la seguridad y las políticas?

**Sí.**
- **Ninguna validación se relajó:** V8C, V15, V18, V20, V23, V24 y V25 siguen igual. La repetición sin llamadas lo confirma.
- **Las adaptaciones aplicadas con IA real:**
  - no suben dosis, no cambian descansos, no cambian de patrón y no añaden trabajo;
  - pasaron el backend completo y la revalidación independiente.
- **Los imposibles** publican la semana conservada.
- **La progresión** sigue apagada fuera de las pruebas aisladas. Ni la fórmula de adherencia ni el número de reintentos cambian.
- **Sin cambios en:** Flutter, Neon, Railway, `main` y las migraciones (ninguna nueva).
- **Sin restauración automática.**
- **La validación es solo automática.** No se declara validación humana.

### 9. ¿Qué resultados y costes dio la IA real?

**Fase F:** **5 llamadas, ≈ US$ 0,044 y 160 s**, de un máximo autorizado de 20 llamadas y US$ 5.
- 3 en la primera ejecución, la que descubrió el defecto de duración.
- 2 en la repetición de U02 con el código final.

**Total global:** 53 de 80 llamadas, ≈ US$ 1,05 acumulados entre la campaña, U06 y esta fase.

**Resultado con el código final:**

| Caso | Resultado | Llamadas |
|---|---|---:|
| U02 S3 | APLICADA (variante con banda, mismo patrón y misma dosis) | 1 |
| U02 S5 | APLICADA (cardio de 90 a 80 s, reducción autorizada) | 1 |
| U01 S2, U04 S5 y U07 S2 | RESPALDO con la misma semana | 0 |

### 10. ¿Qué riesgos quedan abiertos?

| Riesgo | Por qué sigue abierto |
|---|---|
| **Errores puros del modelo** (causa 3: V15, V23, V25) | No dependen del contrato. Siguen gastando reintentos, y su efecto en la generación inicial no se midió aquí con IA real |
| **Tamaño de la muestra** | Dos operaciones con llamada no estiman una tasa. Hace falta medirla con el estudio real o con otra campaña autorizada |
| **ACORTAR en GAIN_MUSCLE con poco tiempo** | Seguirá en RESPALDO, ahora sin coste. Mientras GAIN_MUSCLE no tenga reducciones autorizadas, un aviso a la persona es una decisión de producto, no de este backend |
| **La factibilidad previa es conservadora** | Si los datos están incompletos, llama. Las dos causas de imposibilidad más caras de demostrar (sin sustituto y capacidades obligatorias) no se comprueban: se deja decidir a la IA |
| **Simulador** | La repetición depende de los identificadores, y la fila V32 cuenta los intentos repetidos. Está documentado y no afecta a la producción |
| **Validación independiente del simulador** | Sigue sin conocer los pasos autorizados de progresión y reducción (las 29 INVALIDA explicadas en la campaña) |

## Regresión (fase G)

Todas en verde antes de integrar.

| Área | Pruebas |
|---|---|
| Generación de los cuatro objetivos y referencias históricas | `GeneracionPorObjetivoIT`, `GeneracionReferenciaIT`, `Plan60HipertrofiaIT`, `V20ComposicionCasosIT` y `ComposicionE1E2IT` |
| V8C y V1–V25 | Las pruebas unitarias del validador (877 en total) y `LimitacionesPlanIT` |
| ADAPT-2, tiempo con y sin contexto, causas múltiples | `AdaptacionPorCausasIT` (11, una premisa actualizada: RESPALDO sin llamada) y `AdaptacionCompletaIT` |
| Dolor y fatiga | `AdaptacionPorCausasIT`, `DolorSinMasTrabajoV8CTest` y `SustitucionSeguridadTest` |
| Reprogramación | `SessionFixPromptBuilderTest` y `FactibilidadPreviaIATest` |
| Copia semanal y herencia | `CopiaSinIaIT` y `HerenciaD8IT` |
| Progresión, reincorporación y sin restauración automática | `ProgresionIT`, `ProgresionCienPorCienIT`, `ProgresionV24V8CTest` y `ValidacionMultisemanaIT` |
| Trazabilidad V31–V35 y más | `CorreccionesReplicate{Apagado,Encendido}IT` y la campaña en modo foco |
| Compatibilidad 4D v3.2/v3.3 | `Adenda4Dv32IT`, `RespaldoSesentaMinutosIT` (24/24 y 72/72) y `DuracionForzadaIT` |

**No hizo falta repetir** las simulaciones largas de referencia: el motor de reglas no cambió en esta fase y sus pruebas de referencia pasan sin cambios.
