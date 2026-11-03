# 04 · Pruebas de H1 en la rama limpia

**Código:** `983e216`, sobre `generation` `0974fb3`.
**Configuración real de la candidata:** regla por defecto, sin propiedades ni variables de entorno.
**Entorno:** PostgreSQL 16 efímero (Testcontainers), IA simulada, sin Replicate, sin Neon y sin Railway.

## 1. Pruebas específicas de H1

| Prueba | Regla | Resultado |
|---|---|---|
| `V8CConservacionPatronH1Test` (32) | las dos | 32/32 |
| `V8CDiagnosticoSustitucionesTest` (11) | `CONJUNTO` explícito (histórica) | 11/11 |
| `ConservacionPatronH1IT` (3) | por defecto | 3/3 |
| `ConservacionPatronH1ConjuntoIT` (3) | `conjunto` (reversión) | 3/3 |

**El caso U04, de extremo a extremo (fase 2.4):**

| Requisito | Dónde | Resultado |
|---|---|---|
| 1. La propuesta incorrecta se aceptaba con `conjunto` | `ConservacionPatronH1ConjuntoIT` | **Publicada** (AI): la distribución cambia |
| 2. Con `multiconjunto` se rechaza | `ConservacionPatronH1IT.distribucionIncorrectaRechazada` | 3 rechazos con el mensaje de H1 |
| 3. La propuesta corregida se acepta | `…distribucionCorrectaPublicada` | Publicada (AI) |
| 4. El resto de las sesiones se conserva | íd. | No señaladas idénticas |
| 5. El plan publicado mantiene las restricciones | íd. | Misma distribución de patrones, sin subir la dosis, pasa V8C/V15/V20/V23/V24 |
| 6. El reintento o la conservación funcionan | `…distribucionIncorrectaRechazada` | 3 llamadas a la IA simulada; semana CONSERVED, copia exacta |

**Las 6 IT corren en CI:** son `*IT`, del perfil `integration`, sin `@EnabledIf…`.

## 2. ¿H1 funciona sin el prompt V2? Sí

La rama limpia **no** contiene `FactibilidadAdaptacion`, V2 ni su propiedad. Las 43 unitarias y las 6 IT de H1 pasan en ella (§1), y la regresión completa también (§4).

## 3. Las 123 sustituciones históricas, revalidadas en la rama limpia

`H1ImpactoHistoricoTest` es un laboratorio *opt-in* de #31 que **no se integra**. Se ejecutó **en la rama limpia** de forma temporal:
- se copiaron la prueba y el CSV del catálogo;
- se lanzó `-Dh1.impacto.dir=<copia de entrada_sesiones.csv>`;
- se borraron después. `git status` quedó limpio.

| Medida | Resultado |
|---|---|
| Salida frente a la de #31 | **Idéntica byte a byte**: sha256 `30c5a3f5…b9ec` en las dos (`evidencia/h1_impacto_hashes.txt`) |
| Sesiones revalidadas | 90: 59 reales (37 HISTORICA_U + 22 HISTORICA_R) y 31 de laboratorio |
| Sesiones reales válidas con las dos reglas | **59 de 59**, que contienen las **123** sustituciones |
| Cambian con `multiconjunto` | **1**, de laboratorio: U04 #12b, el caso que H1 corrige |
| Objetivos | INCREASE_STRENGTH 30, LOSE_WEIGHT 26, GENERAL_FITNESS 21, GAIN_MUSCLE 13 |
| Motivos | TOO_DIFFICULT 50, LACK_OF_MOTIVATION 23, FATIGUE 17 |

**Las sustituciones por dolor** no pasan por la regla nueva, porque la rama de dolor vuelve antes. Lo cubren:
- «V8C: dolor: sustituir o retirar…» (6);
- «Respaldo forzado: con dolor y sustituto…» (3);
- «la excepción de V8C: solo la sustitución obligatoria por seguridad» (8).

Todas pasan (§4).

## 4. Regresión completa

| Bloque | Ejecutadas | Superadas | Fallidas | Errores | Omitidas |
|---|---:|---:|---:|---:|---:|
| Unitarias | **1.086** | 1.086 | **0** | **0** | 0 |
| Integración | **494** | 458 | **0** | **0** | 36 (*opt-in*) |

**Una omitida no es una superada.**
- Las 36 omitidas necesitan IA real, URL de Neon o de lectura, o un laboratorio explícito.
- Son las mismas de `generation`; la lista está en `evidencia/regresion_A_pruebas.txt`.
- **Ninguna valida H1.**

**Frente a `generation`:**
- **Unitarias:** +43 (las de H1). `generation` tiene 1.043.
- **Integración:** +6 (las IT U04). `generation` tiene 488 ejecutadas.

| Área (fase 3 y 7.1) | Pruebas (todas superadas) |
|---|---|
| Generación inicial, cuatro objetivos | «Referencias históricas de generación: intactas» (6), `GeneracionReferencia*IT`, «1 · todas las principales disponibles: semana COMPLETA en los cuatro objetivos» (4), «Validación multisemana… en los cuatro objetivos» (55 de 57; 2 *opt-in*) |
| V8C multiconjunto e histórica | §1 |
| V20 | «V20: el piso es el 70 % exacto…» (16), «V20 exacto: los perfiles que rompía la simulación» (2) |
| V24 y 4D | «V34: V24 + V8C…» (16), «validadores: V23, V24 extendida y V25» (6), «4D v3.1/v3.2/v4» |
| Adherencia y cobertura | `AdherenciaSinPesosTest` (6), `AdherenciaRiesgoIT` (5), «cobertura: COMPLETO, PARCIAL o NO_FACTIBLE» (4), «La cobertura no toca la dosis» (3) |
| Adaptación localizada y ampliada | `AdaptacionCompletaIT` (22), «Adaptación por causas…» (11) |
| Reducciones de dosis | «V8C: la IA no sube series, repeticiones ni segundos…» (40), «V8C: carga: solo la reducción autorizada…» (10) |
| Sesiones no señaladas | `AdaptacionCompletaIT` (cada no señalada idéntica), §1 |
| Dificultad y elegibilidad | `V8CDiagnosticoSustitucionesTest` (dificultad), validadores V1/V23/V25 |
| Dolor | §3 |
| RESPALDO, SIN_EFECTO y NO_FACTIBLE | «Respaldo de reglas: 5 y 6 días…» (4), «Respaldo: el motor de reglas…» (14), «D8: la herencia sigue tras un sin efecto o respaldo idéntico…» (9), «AdaptationOutcome: NO_FACTIBLE estructurado» (9) |
| Progresión y reincorporación | `ProgresionIT` (24), «Progresión al 100 %…» (5), `Reincorporacion4DTest` (5), «1(a): solo una PROGRESIÓN posterior…» (7) |
| Concurrencia e idempotencia | «Ciclo semanal concurrente: bloqueo de sesión por participante» (9), reintento del ciclo en `AdaptacionCompletaIT` |
| Linaje V31/V32 | `LinajeIntegridadIT` (1), linaje V31 en `AdaptacionCompletaIT` |
| Contratos de la API | «Contrato de errores de la API: 401/400/404…» (6), «Contrato IA y V4-C…» (3) |

El resumen del registro de Maven está en `evidencia/regresion_A_resumen.txt` y `evidencia/regresion_A_pruebas.txt`.
