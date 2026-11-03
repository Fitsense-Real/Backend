# 12 · Regresión y compatibilidad (fase 8)

**Commit:** `5773f9a`, cabeza del código de la rama H12, que contiene también H1. Después de ese *commit* solo cambian documentos y evidencia, no `src/`.

**Entorno:**
- `mvnw -o -B -ntp verify -Pintegration`;
- PostgreSQL efímero (Testcontainers, `postgres:16-alpine`);
- **sin red y sin Replicate**.

**Evidencia:** `evidencia/regresion/`.

## 1. Con las reglas experimentales APAGADAS (configuración por defecto)

| Bloque | Ejecutadas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| Unitarias (surefire) | 1.119 | 0 | 0 | 1 (*opt-in*) |
| Integración (failsafe) | 489 | 0 | 0 | 37 (*opt-in*) |
| **Comparación de 960 perfiles** (`ComparacionPerfilesIT`) | 1 | 0 | 0 | — |
| **REPLAY de 100 semanas** (`CampanaP123Cohorte02IT`) | 2 | 0 | 0 | 1 (inspección manual, *opt-in*) |

**Las IT cubren:** generación inicial, V1–V25, V8C, V20, V24, adherencia, adaptación localizada y ampliada, RESPALDO, SIN_EFECTO, NO_FACTIBLE, dolor, progresión, reincorporación, concurrencia, idempotencia, seguridad y contratos de la API.

**Unitarias:** las 1.072 del PR #30 más 47 nuevas (32 de H1, 1 *opt-in* de impacto, 10 de H12-A/C y 4 del prompt H12).

**960 perfiles: idénticos byte a byte a la referencia histórica**, los mismos *hashes* que en `51c145b`, `0f17ac7` y `generation`:
- `planes.csv` = `d109a3daa156d6d28bdfbebf109e5b5066bee8213c3663b21ad7b177dd18a859`;
- `colocaciones.csv` = `d227848ba040e73af459b052ab518f67d0b80af55cc731cbe2a455f5abe265ef`.

**REPLAY: igual.** La única diferencia en su evidencia (`docs/FitSense/campana_p123/evidencia/`) son las **marcas de tiempo** de 2 líneas de `exclusiones_dolor.csv` (fecha de ejecución). Se comparó normalizándolas y se descartó con `git checkout`; no se confirmó nada.

**Ningún contexto de Spring activó una regla experimental:** 0 avisos en la fase de integración. Los avisos del registro vienen solo de las pruebas unitarias que construyen el validador explícitamente.

## 2. Con H1 ACTIVADA (`FITSENSE_V8C_PATTERN_RULE=multiconjunto`)

| Bloque | Resultado |
|---|---|
| Unitarias + integración completas (en `fdcd110`, `05` §2.3) | 1.105 + 489 ejecutadas, **0 fallos** (37 *opt-in*); 91 contextos con la regla activa |
| 960 perfiles | **Idénticos** (mismos *hashes*). V8C no participa en la generación |
| REPLAY de 100 semanas | **Igual** (solo marcas de tiempo) |

**Diferencia respecto del comportamiento histórico:** ninguna en las pruebas del repositorio.
- La única documentada es la de `03` §5: añadir 2 de un patrón retirando 1. No la ejercita ninguna prueba de integración ni ninguna corrida real.
- En el laboratorio, U04 #12b (`02` §3.B).

## 3. Con H1 + H12-A ACTIVADAS (prototipo; `…_PATTERN_RULE=multiconjunto`, `FITSENSE_VARIANTE_POLICY=reduccion-demostrable`)

| Bloque | Resultado |
|---|---|
| Unitarias | 1.119, 0 fallos (no dependen de la propiedad) |
| Integración | 489 ejecutadas, **1 fallo esperado**, 37 *opt-in*. 136 contextos con la política activa |
| 960 perfiles | **Idénticos** (mismos *hashes*) |
| REPLAY de 100 semanas | **Igual** (solo marcas de tiempo) |

**El fallo:** `p2d.AdaptacionRegresionIT.sinBloqueoLaVarianteSePublica`.
- Es la **prueba de control** de «un ejercicio bloqueado por el participante».
- Propone una VARIANTE lateral, 919 (KNEE_DOMINANT, dificultad 2) → 124 (KNEE_DOMINANT, dificultad 2), y espera que se publique.
- Con H12-A, V8C la rechaza («no reducen la dificultad del catálogo…») y la semana se conserva.
- **Es exactamente el cambio de comportamiento que define H12-A**, no una regresión.
- Si se aprobara H12-A, esta prueba tendría que elegir un sustituto de menor dificultad, o declarar la política histórica para su control.
- **No se ha modificado** (la regla está apagada y la prueba pasa por defecto).
- Su prueba gemela (el ejercicio bloqueado no entra y se conserva) **pasa** también con H12-A.

**Observación menor:** cuando el añadido no está en el catálogo elegible, H12-A repite en su mensaje un ejercicio que V8C ya rechazaba por «no consta su patrón». Solo afecta al texto (K16 en `14`).

## 4. Comparaciones pedidas (fase 8.2)

| Comprobación | Resultado | Evidencia |
|---|---|---|
| V1 se comporta igual con todo apagado | ✔ IT completas en verde; el prompt V1 es idéntico byte a byte (`SessionFixPromptBuilderTest`, `PromptVarianteH12Test` 1) | §1 |
| V2 conserva su comportamiento documentado | ✔ `SessionFixPromptV2Test` (18) en verde; con la política histórica, el prompt V2 es idéntico byte a byte | §1 |
| Generación inicial idéntica | ✔ 960 perfiles idénticos con las tres configuraciones; `generacion.csv` y el plan inicial, idénticos en las 4 corridas de escenarios (`11` §4) | §1–3 |
| Aw, Aobs y cobertura no cambian | ✔ `adherencia.csv` y las métricas de las semanas 2–3, idénticos en las 4 corridas | `11` §4 |
| Las decisiones semanales no cambian (todo apagado) | ✔ REPLAY igual; 960 iguales | §1 |
| Los validadores históricos siguen funcionando | ✔ 0 fallos con la configuración por defecto | §1 |
| H1 activada: el caso de distribución se rechaza | ✔ | `02`, `05` |
| H1 activada: se mantienen las sustituciones legítimas | ✔ 123/123 y 0 fallos en las IT | `04`, §2 |
| H1 activada: sin violaciones de conservación | ✔ 0 en escenarios y en las IT | `08` §3 |
| H12 activada: sin adaptaciones fuera de alcance ni dosis no autorizadas | ✔ 0 y 0 | `08` §3 |
| H12 activada: sin rotaciones masivas laterales | ✔ 0 laterales con H12-A (42 con la histórica) | `08` §3 |
| H12 activada: impacto en SIN_EFECTO | Registrado: de 11 a 26 filas en escenarios; ~8 sesiones históricas | `08`, `09` |
| H12 activada: restricciones del objetivo | ✔ V24/4D y V20 intactos | `11` |
