# 06 · Correcciones de V8C (H-A1 y H-A2) y de las pruebas dependientes de la fecha

**Estado:** corregido y fusionado en `generation`.
- PR #44: merge `543932a`, 10/11/2026.
- PR #43: merge `d8c925f`, 10/11/2026.

## 1. Qué es V8C

V8C es el validador de **conservación de las adaptaciones** (`ConservationValidator`).
- En una sesión señalada para adaptar, comprueba que la IA solo cambió lo que permite su tipo de intervención (`TipoIntervencion`).
- En las sesiones no señaladas, comprueba que se conservan.
- La **generación inicial no pasa por V8C**: allí deciden V1–V27, con V24 y V25.

## 2. Diagnóstico

Los dos huecos los encontró la auditoría de reglas de MEJ ronda 2 (`docs/FitSense/mejora_ia/ronda2/AUDITORIA_REGLAS_FITSENSE.md`, rama `experimento/mejora-ia`, tabla de hallazgos).

| Hallazgo | Comportamiento anterior | Política que debía cumplirse | Causa técnica |
|---|---|---|---|
| **H-A1** · retiradas no autorizadas | En `VARIANTE_MAS_SENCILLA_O_CARGA`, V8C aceptaba propuestas que **solo retiraban** ejercicios, una o varias y estructurales incluidas. Con más retiradas que añadidos, tampoco las rechazaba | `TipoIntervencion`: VARIANTE autoriza «una variante más sencilla del mismo patrón, o ajustar la carga», no retirar. Retirar sin reemplazo solo lo autoriza `SEGURIDAD_ANTE_DOLOR`. `ACORTAR_O_REORGANIZAR` autoriza **una** colocación de relleno | `revisarSustitucion` terminaba con `if (d.anadidos().isEmpty()) return;` y no comprobaba nada más. El emparejamiento multiconjunto solo miraba que cada añadido tuviera un retirado de su patrón, no al revés |
| **H-A2** · descanso no autorizado | Con permiso de dosis, se aceptaba un cambio **solo de `rest_seconds`** de un ejercicio que se queda. También una bajada válida de series que llevaba otro descanso | Instrucción de corrección (`SessionFixPromptBuilder`): «El descanso (rest_seconds) de un ejercicio que se queda no se cambia nunca». V24: bajar la dosis son series, repeticiones o segundos, nunca el descanso | Un cambio solo del descanso no es una subida de dosis y entraba en el permiso de dosis. El descanso solo se miraba para no emitir la reducción hacia V24 |

**Impacto observado antes de corregir:**
- En las campañas con IA real, ninguna propuesta llegó a explotar estos huecos.
- Los análisis de MEJ R2 y CONF-IA los vigilaron por identidad con `analisis_mej_r2.py`, independiente de V8C, y no encontraron cambios no autorizados.
- El defecto era del validador. No había ningún incidente en los resultados.

## 3. Corrección

La corrección aplica la política existente. No añade reglas ni validadores, no cambia `TipoIntervencion` y no toca descansos ni dosis de capacidades o clases.

- **H-A1** (`revisarSustitucion`):
  - fuera de `SEGURIDAD_ANTE_DOLOR`, si los retirados (sin contar los que dolieron) superan a los añadidos, se rechaza con un motivo explícito («cada retirado necesita su reemplazo»);
  - ACORTAR sigue por su rama propia (una de relleno);
  - el multiconjunto, el límite de dificultad y el impacto no cambian.
- **H-A2** (`revisarDosisDeLaIa`):
  - si un ejercicio que se queda vuelve con otro descanso, se rechaza;
  - un sustituto (sin descanso previo) no se compara: lleva la prescripción de su clase, que juzgan V16, V17 y V24;
  - si la semana medida no registra descanso (`null`), no se compara, igual que antes.

### Historia de la corrección

| Paso | Rama y commit | Fecha |
|---|---|---|
| Corrección y pruebas | `experimento/mejora-ia` `5bea428` | 10/11/2026 |
| Evaluación con IA real (CONF-IA) | `experimento/mejora-ia` `ab04e67` (= `5bea428` + pruebas de fecha) | 10/11/2026 |
| Traslado limpio a `generation` (cherry-pick, 4 archivos) | `fix/v8c-retiradas-descanso` `5b709dd`; base actualizada con `4ca5cd2` | 10/11/2026 |
| **Fusión** | **PR #44 → `generation` `543932a`** | 10/11/2026 |

**Archivos de la PR #44:**
- `ConservationValidator.java`: +33 / −1;
- `V8CRetiradasYDescansoTest.java`: nuevo, 25 pruebas;
- `V8CConservacionPatronH1Test.java`: casos 7 y 8 invertidos;
- `AdaptedWeekProducerTest.java`: caso C4 invertido.

## 4. Pruebas

### Nuevas: `V8CRetiradasYDescansoTest` (25)

| Grupo | Casos | Resultado |
|---|---|---|
| H-A1 negativas (7) | VARIANTE retira un estructural · retira varios · sustituye y además retira · retira el relleno · FATIGA retira · ACORTAR retira un estructural · ACORTAR retira dos | Todas rechazadas |
| H-A1 positivas (8) | Sustitución del mismo patrón (una y dos) · sustitución y bajada de dosis · patrón distinto sigue rechazado · ACORTAR retira una de relleno · DOLOR retira sin reemplazo · DOLOR sustituye · VARIANTE sin cambios = sin efecto | Lo autorizado sigue funcionando |
| H-A2 negativas (5) | Solo descanso en ACORTAR, FATIGA, VARIANTE y MOTIVACIÓN · bajada válida con descanso cambiado | Todas rechazadas |
| H-A2 positivas (5) | Bajada de series o repeticiones con el mismo descanso · sustituto con su propio descanso (VARIANTE y DOLOR) · sesión no señalada sigue sin poder cambiar el descanso | Sin cambios |

**El defecto queda demostrado:**
- con el validador **sin corregir**, las mismas 25 pruebas dan **9 fallos**: las 4 negativas de H-A1 con VARIANTE y las 5 de H-A2;
- se reprodujo dos veces: sobre `0878f85` en la rama experimental y sobre `generation` `9ba78cc`;
- con la corrección pasan las 25.

### Pruebas existentes que fijaban el defecto (se invierten)

| Prueba | Antes | Ahora |
|---|---|---|
| `V8CConservacionPatronH1Test` 7 «VARIANTE que retira sin añadir» | aceptada («la regla no prohíbe retiradas») | rechazada (H-A1) |
| `V8CConservacionPatronH1Test` 8 «retira 2 de un patrón y añade 1» | aceptada | rechazada (una retirada sin reemplazo) |
| `AdaptedWeekProducerTest` C4 «menos descanso con permiso de dosis» | APLICADA | rechazada (H-A2) |

Las tres describían el comportamiento del código, no una política. Sus casos vecinos («menos series», «menos repeticiones») siguen comprobando que una bajada real cuenta como APLICADA.

### Regresión

| Ejecución | Commit | Resultado |
|---|---|---|
| Unitarias completas | `5b709dd` (rama de la PR) | 1331 pruebas, 0 fallos, 1 omitida (DEV-R, opcional con Replicate) |
| Integración completa, antes de #43 | `5b709dd` | 593 pruebas, 1 fallo y 2 errores, 44 omitidas. Los 3 son las pruebas de fecha (§5), que fallaban igual en `generation` `9ba78cc` |
| `verify -Pintegration` completo tras actualizar la base | `4ca5cd2` | **1331 unitarias, 0 fallos, 1 omitida · 593 de integración, 0 fallos, 44 omitidas** |
| CI de la PR #44 | `4ca5cd2` | Verde: 593 de integración, 0 fallos |
| **CI de `generation` tras la fusión** (run 218) | **`543932a`** | **Verde: 593 de integración, 0 fallos, 44 omitidas** |

### En las corridas reales (CONF-IA, con V8C corregido)
- 132 sesiones adaptadas con IA real sin ningún cambio no autorizado.
- V8C solo rechazó una propuesta (D3, O19, v2.2a), por una regla de ACORTAR que ya existía. El reintento se aplicó.
- Ver `03_ADAPTACION_PLANES.md`.

## 5. Pruebas dependientes de la fecha (PR #43)

| Prueba | Fallo desde el 09/11/2026 | Causa | Corrección (solo código de prueba) |
|---|---|---|---|
| `CatalogoV36IT.semanaSiguiente` | `872 … NO_FACTIBLE … V1: el ejercicio 1296 no esta en available_exercises` | La semana se calculaba con `LocalDate.now()`. El motor de reglas siembra su aleatoriedad con la semana, así que cada lunes elige otros ejercicios (el 1296 está desactivado en V36) | `S1 = RelojPruebas.SEMANA_REFERENCIA.plusWeeks(1)` y `RelojPruebas.abrirVentanaDeEjecucion` antes de cumplir las sesiones |
| `ProgresionIT.p02ProgresionSinElExcluido` | `NoSuchElementException` | La misma | La misma |
| `ProgresionRev2IT.p02ProgresionSinElExcluido` | Igual, por herencia | — | — |

**Historia:**
- Primero se corrigió en `experimento/mejora-ia` (`ab04e67`).
- Se trasladó a `generation` con `test/semana-fija-catalogo-progresion` `685161a` → **PR #43, merge `d8c925f`**.
- CI de la PR verde. Las tres clases dan 55 pruebas y 0 fallos, frente a 1 fallo y 2 errores en `9ba78cc`.
- CI de `generation` tras la fusión: verde (run 215).

No cambian el código productivo, las políticas ni las fechas de negocio. `RelojPruebas` ya existía en `generation` (`docs/FitSense/correccion_pruebas_fecha/`).

## 6. Referencias

- **Corrección y pruebas:** `docs/FitSense/mejora_ia/confirmacion/CORRECCION_V8C_HA1_HA2.md` y `PRUEBAS_Y_REGRESIONES.md` (rama `experimento/mejora-ia` `1d5c28f`).
- **PR:** https://github.com/Fitsense-Real/Backend/pull/43 y https://github.com/Fitsense-Real/Backend/pull/44.
- **Código vigente:** `src/main/java/main/web/services/fitsense/planning/domain/services/ConservationValidator.java` en `generation` `543932a`.
