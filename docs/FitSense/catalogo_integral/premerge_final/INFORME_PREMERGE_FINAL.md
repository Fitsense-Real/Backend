# Informe premerge final · FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1

**Orden:** «ORDEN FINAL — FITSENSE: CIERRE DE P-03, CONTROL DE PR Y PREPARACIÓN DEL MERGE».

**Fecha:** 15/11/2026.

**Lo que no se hizo:**

- No se hizo merge ni despliegue.
- No se modificó `generation` (sigue en `623c4fa`).
- No se tocaron Railway, Neon, Flutter ni datos reales.
- No se gastó en Replicate.

## Documentos de esta carpeta

| Documento | Contenido |
|---|---|
| `DECISION_P03.md` | Autorización, diseño y reglas de P-03 por colocación |
| `VALIDACION_P03_POR_COLOCACION.md` | Las 10 pruebas obligatorias, la regresión, el escenario que cambia y los 40 HALL-GM-01 |
| `VALIDACION_DOLOR_Y_PLANES_PUBLICADOS.md` | P-DOLOR-PUB, plan 78/494 y #64 |
| `TRAZABILIDAD_OMISIONES_SISTEMA.md` | Omisiones impuestas, reconstrucción, propuesta V57 y adherencia |
| `ESTADO_PR_55_66.md` | Inventario de PR, dependencias, migraciones, interruptores y CI |
| `ORDEN_MERGE_CONTROLADO.md` | Orden exacto, simulación y conflictos |
| `REGRESION_INTEGRADA_FINAL.md` | Regresión completa y matrices sobre `12c6897` |
| `COMPARACION_V1_V11.md` | Diferencias con v1 |
| `VERSION_CONGELADA_V11.md` | Identificación, huella e interruptores de v1.1 |
| `RIESGOS_RESIDUALES.md` | Riesgos que siguen y los nuevos |
| `LIMITACIONES_PARA_TESIS.md` | Qué se puede y qué no se puede afirmar |
| `evidencia/` | Regresión, rejillas 168 y 312, volcado congelado, HALL-GM-01, P-03 y simulación |

## Las 20 respuestas

**1. Commit final.**

- Código: **`12c6897028fa1af183596d973961b8c39768a981`**, en la rama `cierre/catalogo-premerge-final`.
- Los commits posteriores (`79401e4`, `048b5de` y el de este informe) solo añaden documentos y evidencias: `git diff 12c6897 HEAD -- src` está vacío.
- PR final: [Fitsense-Real/Backend#67](https://github.com/Fitsense-Real/Backend/pull/67), borrador, base `generation`.

**2. ¿P-03 por colocación queda activada en la configuración candidata?**

- **Sí, en la candidata y en todas sus pruebas:** `src/test/resources/config/application.properties` → `true`.
- **En producción, no:** `fitsense.regeneracion.p03-por-colocacion=${FITSENSE_P03_POR_COLOCACION:false}`. Activarla requiere una autorización posterior.

**3. ¿Se reprodujo el contraejemplo y quedó bloqueado?**

Sí, en `P03ComparacionPorColocacionIT#contraejemplo` (usuario 9850): una colocación reducida de 3x10 a 2x10 vuelve a 3x10 mientras otra baja, y el total no sube.

- Con la regla apagada se acepta: el defecto se reproduce.
- Con la regla encendida se rechaza: «P-03 nivel 2 (por colocación, MANTENIDA)…».
- El cambio de día también se bloquea (`#cambioDeDia`, 9851, SIN_CORRESPONDENCIA).

**4. ¿Siguen funcionando las progresiones legítimas?**

Sí:

- `ProgresionIT` (24), `ProgresionRev2IT` (24), `ProgresionCienPorCienIT` y `PendientesProgresion*IT`;
- `Reincorporacion4DTest` y `ReincorporacionTrasProgresionTest`;
- `ComparacionDosisPorColocacionTest#progresionAutorizada`.

Todo pasa con la regla encendida. La única diferencia está en la respuesta 5.

**5. ¿Cuántos escenarios cambiaron con P-03?**

| Conjunto | Escenarios que cambian |
|---|---|
| 40 HALL-GM-01 | **0** |
| Rejillas 168 y 312, y referencia de 72 | **0** (la generación no pasa por P-03) |
| Regresión completa | **1** |

El escenario que cambia es el del usuario 17, en `ProgresionIT#p03Nivel2TrasProgresion` y su copia REV2:

- en v1 **se publicaba**: menos trabajo total y las mismas dosis en los ejercicios comunes;
- en v1.1 queda **NO_FACTIBLE**: el 253 (3x120 s) ocupa el puesto del 295 retirado (2x12), con más series.

Es el comportamiento conservador documentado y no es un defecto de seguridad (`VALIDACION_P03_POR_COLOCACION.md` §3). Las regeneraciones publicadas bajan de 10 ejecuciones (7 escenarios) a 8 (6).

**6. ¿Cuántos continúan NO_FACTIBLE?**

- **HALL-GM-01:** los mismos **5** que en v1 (9717, 9728, 9733, 9734 y 9740).
- **Regeneraciones P-03 de la regresión:** las **15** ejecuciones NO_FACTIBLE de v1 siguen así, y se suma la del usuario 17 (17 en total, contando HIST y REV2). Ninguna pasa a publicarse.
- **Generación:** los **14** NO_FACTIBLE legítimos de la rejilla de 312 (775, peso corporal) siguen igual.

**7. ¿Se mantuvieron V24, V8C y D8?**

Sí, sin cambios en el código. Siguen pasando:

- V24: `ProgresionV24V8CTest`, `SustitutoDosisCapacidadTest` y `SustitutoGmSinMasTrabajoTest`;
- V8C: `V8C*Test` y `DolorSinMasTrabajoV8CTest`;
- D8: `HerenciaD8IT`.

V15, V20 y V25 tampoco cambian.

**8. ¿Se mantuvo la protección ante dolor?**

Sí:

- **P-DOLOR-PUB** sigue encendida: `ProteccionPlanesPublicadosIT` (12), `PublishedPlanSafetyServiceDolorTest` (7) y `RegeneracionPorColocacionRev2IT#dolorMarcadoEnSesionesAbiertas`.
- En los 40 HALL-GM-01, `trabajo_tras_dolor = 0`.
- En el caso NO_FACTIBLE no se publica nada y la exclusión se conserva.

**9. ¿Qué ocurre con el plan 78 y el 494?**

Con el criterio aprobado, la versión reparada sustituye **exactamente** el 1303 y el 494 (contenido por identidad, INC-CAT-CONS-v1):

- sin más dosis;
- sin ningún sustituto contenido;
- con la versión 1 intacta.

Si no hay reparación válida, queda NO_VALIDA sin publicar. Con la semana empezada, SEMANA_EMPEZADA. #64 sigue apagada y solo se prueba de forma aislada.

**10. ¿Se modificó alguna sesión histórica?**

No:

- Las sesiones terminadas no se marcan y lo registrado no cambia.
- La versión publicada del plan 78 no se toca.
- `user_safety_exclusions` nunca se borra.

**11. ¿Cómo quedan registradas las omisiones impuestas por el sistema?**

Hoy:

- la sesión vencida por bloqueo guarda su causa (V54, `system_block_cause` en `planned_workouts`);
- el **ejercicio** omitido por seguridad queda como una omisión normal.

La causa de dolor se puede reconstruir con la ventana de `user_safety_exclusions`, suponiendo que P-DOLOR-PUB estaba encendida. Las demás causas no se recuperan.

Propuesta mínima, **no incluida**: V57, en [Fitsense-Real/Backend#68](https://github.com/Fitsense-Real/Backend/pull/68).

**12. Limitación de adherencia para la tesis.**

El indicador oficial, que no cambia, cuenta como no hechas las omisiones que impuso el sistema:

- **v1.1 es válida para generar y adaptar rutinas.**
- **Para medir la adherencia**, hay que declarar la limitación o integrar V57 antes de recoger datos reales (`LIMITACIONES_PARA_TESIS.md`).

**13. Interruptores.**

| Estado | Interruptores |
|---|---|
| **Encendidos** | Contención conservadora; P-DOLOR-PUB |
| **Encendido solo en pruebas** | P-03 por colocación |
| **Apagados** | V53 A; V53-B; #64 |

**14. V53 y V53-B.**

- **V53:** el dato `requires_suspended_support` (15 ejercicios). La opción A está **apagada**.
- **V53-B (#66):** permiso propio, V55. Está **apagada**; nadie tiene el permiso confirmado.
- Las dos a la vez se rechazan con `IllegalStateException`.
- La contención mantiene fuera a los 15.

**15. PR #64.**

Su V54 se aplica porque solo deja constancia. La tolerancia a la dificultad de publicación está **apagada**:

- se prueba encendida solo en `Plan78ContencionReducidaIT#propuestaDificultadDePublicacion`, que la restablece en `finally`;
- **activarla requiere una aprobación posterior.**

**16. Orden exacto del merge.**

1. #62
2. #56
3. #57
4. #58
5. #59
6. #55
7. #60
8. #63
9. #65
10. #61
11. #64
12. #66
13. #67 (final)

Cada paso es `--no-ff`. Opcional, solo si se aprueba: #68 después de #67.

**17. Conflictos resueltos.**

Hay dos, en la simulación: #64 y #66. Los dos están en `EsquemaVigenteSobreCatalogoAnterior.SOLO_ESQUEMA`.

- Se resuelven conservando todas las líneas: la 51 (V51 y V53), la 54 y la 55.
- No hay conflictos en el código de producción ni en Flyway.
- El parche HALL-PROG-01 (#60/#62) se aplica **una vez**: mismo `patch-id`.
- El árbol resultante es idéntico al probado (`475703a0…`).

**18. ¿Pasó la regresión integrada completa?**

**Sí**, sobre `12c6897`, sin interrupciones:

- 1540 unitarias y 754 de integración, 0 fallos;
- migraciones V1…V56 desde un PostgreSQL vacío;
- rejillas 168/312 y huella idénticas a v1;
- HALL-GM-01 idéntico.

**19. ¿v1.1 es reconstruible y conserva el catálogo conservador?**

**Sí.** Dos reconstrucciones independientes dan la misma huella normalizada (REPRODUCIBLE), y es idéntica a la de v1:

- `exercises` `20d6a795…`;
- equipamiento `a23c6843…`;
- aprobaciones `f79df277…`;
- Flyway `57c239d3…`, 59 migraciones.

A/B/C/D siguen en 919/273/27/157.

**20. ¿Puede aprobarse el merge sin defectos de seguridad conocidos ni cambios no autorizados?**

**Sí:**

- no queda ningún defecto de seguridad conocido sin corregir;
- el único cambio de comportamiento, P-03 por colocación, está autorizado para la candidata y apagado en producción;
- su efecto medido (1 escenario de NO_FACTIBLE adicional) está explicado y es conservador.

Lo que sigue abierto son limitaciones declaradas, no defectos (`RIESGOS_RESIDUALES.md`):

- validación profesional y 312 decisiones;
- adherencia;
- perfiles sintéticos.

## Veredicto

> **LISTO PARA AUTORIZACIÓN DE MERGE**

**Condiciones** (`ORDEN_MERGE_CONTROLADO.md`):

1. Fusionar en el orden de la respuesta 16.
2. Si `generation` avanza antes del merge, repetir la simulación, comparar el árbol y, si difiere, repetir la regresión.
3. No activar en producción P-03 por colocación, V53 A, V53-B ni #64 sin una autorización expresa. Activar P-03 implica aceptar su coste en factibilidad (respuesta 5, R14).
4. Antes de recoger datos reales de adherencia, decidir sobre V57 (#68).

**Pendiente del propietario:** la autorización del merge. **No se ha ejecutado ningún merge ni despliegue.**
