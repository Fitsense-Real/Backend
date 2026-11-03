# 05 · Pruebas de la corrección candidata H1 (fases 2.5 y 2.6)

## 1. Qué se implementó

**Regla `MULTICONJUNTO` en `ConservationValidator`** (V8C), detrás de una propiedad:

```
# application.properties (valor por defecto)
fitsense.adaptation.v8c.pattern-rule=${FITSENSE_V8C_PATTERN_RULE:conjunto}
```

| Valor | Comportamiento |
|---|---|
| `conjunto` (por defecto) | El histórico, sin ningún cambio |
| `multiconjunto` | Además de lo anterior: para cada patrón, nº de añadidos ≤ nº de retirados (`revisarMulticonjunto`) |
| Cualquier otro valor | La aplicación no arranca (`IllegalArgumentException`) |

**Cambio mínimo, en un solo archivo de `src/main` más la propiedad:**
- un `enum` con su lectura de la propiedad, constructores y un método privado de unas 25 líneas;
- se invoca al final de la rama no dolorosa de `revisarSustitucion`, después de la comprobación por conjunto.

**Lo que no se toca:** V20, V24, dosis, políticas, catálogo y generación inicial.

**Activación:**
- Con `multiconjunto`, cada contexto escribe en el registro: `V8C: regla de patron EXPERIMENTAL activa: MULTICONJUNTO (H1).`
- Sin configurar, la regla es `CONJUNTO`, comprobado por la prueba 0.
- **No se activa en ningún perfil ni entorno.**

## 2. Pruebas antes y después

### 2.1 Unitarias nuevas: `V8CConservacionPatronH1Test` (32 ejecuciones, 0 fallos)

| # | Caso | CONJUNTO | MULTICONJUNTO |
|---|---|---|---|
| 0 | Por defecto e inválidos | CONJUNTO; «pareja» se rechaza | — |
| 1a/1b/1c | **U04 S2** (380 → 56, 389 → 72) / corregido (389 → 74) | acepta / — | **rechaza** / acepta |
| 2 | 8 pares reales del mismo patrón (incluidos los de cambio de músculo y 862 → 74) | acepta | acepta |
| 3 | Dos parejas cruzadas (PUSH → PUSH y PULL → PULL) | acepta | acepta |
| 4 | 1 retirado → 2 añadidos del mismo patrón | acepta | **rechaza** (cambio declarado, `03` §5) |
| 5 | Otro patrón | rechaza (1 mensaje) | rechaza (el mismo mensaje, sin duplicar) |
| 6 | VARIANTE solo con cambio de dosis | acepta y APLICADA | igual |
| 7 | VARIANTE que solo retira | acepta | acepta |
| 8 | Retira 2 y añade 1 del mismo patrón | acepta | acepta |
| 9 | FATIGA e INVESTIGAR con reducción de dosis | acepta | acepta |
| 10 | FATIGA que intenta sustituir | rechaza (permiso) | rechaza (igual) |
| 11 | **Dolor**: sustituye por otro patrón | acepta | acepta |
| 12 | **Dolor**: añade más de lo retirado | rechaza (regla del dolor) | rechaza (igual) |
| 13 | Cambia una sesión no señalada | rechaza | rechaza |

`V8CDiagnosticoSustitucionesTest` (PR #30, 11 pruebas) **no se modifica** y sigue aprobada con la regla por defecto.

### 2.2 Barrido de todas las unitarias con MULTICONJUNTO como regla por defecto

**Cómo se hizo:**
- Cambio **temporal, no confirmado**: el constructor sin argumentos devolvió MULTICONJUNTO.
- Se ejecutaron las **1.104** pruebas unitarias.
- Evidencia: `evidencia/regresion/unitarias_multiconjunto_por_defecto.txt`.

| Resultado | Pruebas |
|---|---|
| Aprobadas | **1.101** |
| Fallidas | **3, todas esperadas**: `V8CDiagnostico…distribucionNoSeConserva` y `…numeroNoSeLimita` (documentan el hueco con la regla histórica) y `V8CConservacionPatronH1Test.porDefecto` (comprueba que el valor por defecto sea CONJUNTO) |

**Entre las aprobadas** están todas las demás clases que usan V8C: `ConservationValidatorTest`, `SustitucionSeguridadTest`, `AdaptedWeekProducerTest`, `ProgresionV24V8CTest`, `HerenciaReduccionV20Test`, `ConservationValidatorQuitarRellenoTest`, `CargaV8CTest`, `DolorSinMasTrabajoV8CTest`, `DosisIaV8CTest`, `SemanaAcortadaPublicableTest`, `AdaptacionAcortaIntegradaTest` y `TipoIntervencionTextoCoherenteTest`.

### 2.3 Regresión completa de integración con la regla ACTIVADA

**Configuración:** `FITSENSE_V8C_PATTERN_RULE=multiconjunto mvnw verify -Pintegration` sobre `fdcd110`, PostgreSQL efímero y sin red. Evidencia: `evidencia/regresion/regresion_completa_multiconjunto.txt`.

| Bloque | Ejecutadas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| Unitarias (surefire) | 1.105 | 0 | 0 | 1 (`H1ImpactoHistoricoTest`, *opt-in*) |
| Integración (failsafe): generación, V1–V25, adaptación localizada y ampliada, RESPALDO, SIN_EFECTO, NO_FACTIBLE, dolor, progresión, reincorporación, concurrencia, idempotencia, seguridad y API | 489 | **0** | **0** | 37 (*opt-in*) |

**Se comprobó que la regla estaba activa:** **91** contextos de Spring registraron el aviso de regla experimental.

### 2.4 Impacto histórico, con el V8C real (`04`)

`H1ImpactoHistoricoTest` volvió a validar con las dos reglas las **90 sesiones cambiadas y publicadas** de las corridas conservadas.

| Grupo | Sesiones | Sustituciones | Aceptadas con CONJUNTO | Aceptadas con MULTICONJUNTO | Cambian |
|---|---:|---:|---:|---:|---:|
| Histórico U (PR #28 etapa C y PR #29 C1/C2) | 37 | 57 | 37 | 37 | 0 |
| Histórico R (PR #30 RC1_r1/RC2_r1, 2 semanas) | 22 | 66 | 22 | 22 | 0 |
| Laboratorio (guiones de PR #30) | 31 | — | 31 | 30 | **1: U04 #12b** |

**Las 123 sustituciones reales siguen siendo válidas**, en los cuatro objetivos:

| Objetivo | Sustituciones | Válidas con MULTICONJUNTO | Rechazadas | Con excepción |
|---|---:|---:|---:|---:|
| GAIN_MUSCLE | 56 | 56 | 0 | 0 |
| LOSE_WEIGHT | 32 | 32 | 0 | 0 |
| INCREASE_STRENGTH | 26 | 26 | 0 | 0 |
| GENERAL_FITNESS | 9 | 9 | 0 | 0 |

**Ninguna necesita excepción:**
- en las 59 sesiones reales, cada retirado tiene su reemplazo del mismo patrón (123 retirados y 123 añadidos);
- ninguna sesión real añadió más ejercicios de un patrón que los que retiró.

### 2.5 De extremo a extremo (`02` §3.B)

| Ejecución | Resultado en U04 |
|---|---|
| `H1_COMBINADOS_conjunto` | Se publica la sesión con 3 HPUSH y 1 VPULL |
| `H1_COMBINADOS_multiconjunto` | El intento 1 se rechaza por V8C; el reintento con un VERTICAL_PULL se publica |

Los otros 19 usuarios quedan **idénticos byte a byte** en las dos semanas.

### 2.6 Con la regla apagada

La regresión por defecto (regla `conjunto`) de la rama completa, con los 960 perfiles y la REPLAY, está en `12_REGRESION_HISTORICA.md`.

## 3. Criterios de aceptación (fase 2.6)

| # | Criterio | Resultado | Evidencia |
|---|---|---|---|
| 1 | U04 ya no se publica con la distribución incorrecta | ✔ | 1b; §2.5 |
| 2 | Las sustituciones legítimas del mismo patrón se aceptan | ✔ 123 de 123 reales y 8 pares de prueba | §2.4; prueba 2 |
| 3 | Las intervenciones que no sustituyen quedan intactas | ✔ | Pruebas 6, 7, 9 y 10; §2.2 |
| 4 | Las excepciones de seguridad conservan su comportamiento | ✔ | Pruebas 11 y 12; `SustitucionSeguridadTest` y `DolorSinMasTrabajoV8CTest` en el barrido; IT de dolor en §2.3 |
| 5 | No se modifican sesiones no señaladas | ✔ | Prueba 13; §2.5 (los demás usuarios, idénticos) |
| 6 | No aparecen regresiones | ✔ 0 fallos con la regla activa (1.105 unitarias + 489 IT). El único cambio de comportamiento es el declarado (prueba 4) | §2.2 y §2.3 |
| 7 | Reproducible | ✔ Scripts, entradas y salidas en `evidencia/h1_impacto`; laboratorio *opt-in* con `-Dfitsense.experimento.kora20.v8c-patron` | `evidencia/` |

**H1 queda técnicamente satisfactoria, aislada, revisable y sin activación automática.** Activarla es decisión tuya (`15`).
