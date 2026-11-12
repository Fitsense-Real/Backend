# Informe: corrección de pruebas dependientes de la fecha

**PR:** Fitsense-Real/Backend#19 (`fix/tests-fecha-determinista` → `generation`).

**Commit del arreglo:** c3d6744, creado desde `generation` en bfd5756.

**Estado:**
- CI `verify` **verde**;
- fusionable;
- **sin fusionar**: necesita tu aprobación.

## 1. Causa

**Síntoma:** desde el **lunes 2026-11-02** fallan 8 pruebas de integración en `generation` y en el PR #17, sin ningún cambio de código:

| Prueba | Fallos |
|---|---|
| `GeneracionReferencia*IT` (IT, V35, V36, V37, V39, V40) | 6 |
| `ValidacionMultisemanaIT` | 1 |
| `Usuario31ReproduccionIT` | 1 |

**Mecanismo:**
- El motor de reglas siembra su aleatoriedad con `userId × 1.000.003 + weekStartDate.toEpochDay()` (`RuleBasedTrainingPlanGenerator.seedFor`). Es una semilla estable por usuario y semana, **a propósito**.
- Las tres clases calculaban la semana con `TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1)`.
- Sus referencias (planes esperados, catálogo del usuario 31 y secuencias multisemana) se capturaron con la semana del **2026-11-02**.
- Cada lunes cambiaba la semilla, y con ella la rotación de ejercicios.

**Es un defecto de las pruebas, no del backend.** Ya fallaba en `generation` antes del PR #18 (afef98a).

**Riesgo adicional encontrado:**
- `PoliticaV4CCicloIT` y `C2ReproduccionN03IT` usaban la semana fija 2026-11-02 y **ejecutan sesiones**.
- Producción rechaza abrir un entrenamiento vencido: `expires_at` es a las 23:59 locales del día, comparado con el reloj real.
- En cuanto esa semana pasara, fallarían con «ya venció».
- **Comprobado** con una semana fija ya pasada: 55 errores de vencimiento en `ValidacionMultisemanaIT` sin el arreglo.

## 2. Archivos modificados (solo `src/test`, 7 archivos)

| Archivo | Cambio |
|---|---|
| `RelojPruebas.java` (nuevo) | `SEMANA_REFERENCIA = 2026-11-02` (se comprueba que es lunes) y `abrirVentanaDeEjecucion(jdbc, userId)`, que deja `expires_at` de los entrenamientos del usuario de la prueba al menos una semana en el futuro. Es la misma condición que cuando la semana era futura; no toca ejercicios, dosis, fechas ni estados |
| `GeneracionReferenciaBase.java` | Semana de referencia fija |
| `Usuario31Base.java` | Semana de referencia fija |
| `ValidacionMultisemanaIT.java` | Semana fija; abre la ventana antes de ejecutar; `OverdueWorkoutScheduler` sustituido por un doble (`@MockitoBean`) |
| `C2ReproduccionN03IT.java` | Abre la ventana antes de ejecutar; doble del planificador de vencidos |
| `PoliticaV4CCicloIT.java` | `SEMANA_REFERENCIA`; abre la ventana antes de ejecutar; doble del planificador de vencidos |
| `TrainingWeekTest.java` (nuevo) | 18 pruebas del cálculo de semanas con fechas explícitas |

**Por qué el doble del planificador:** el planificador horario de vencidos (`@Scheduled`, en el minuto :30 de cada hora) también corre en las pruebas. Con fechas fijas pasadas podría marcar sesiones como vencidas en mitad de una prueba. El doble deja el tiempo bajo control de la prueba.

**Lo que no cambia:**
- `src/main`: **0 archivos**;
- ninguna aserción ni ningún valor esperado;
- **ninguna prueba eliminada, omitida ni relajada**.

## 3. Pruebas ejecutadas

PostgreSQL efímero (Testcontainers); 0 llamadas a Replicate.

| Ejecución | Contabilizadas | Ejecutadas | Fallos | Omitidas |
|---|---|---|---|---|
| **Sin el arreglo** (`generation` bfd5756), las 12 clases afectadas | 71 | 68 | **8** (los mismos del CI) | 3 |
| **Con el arreglo**, las mismas 12 clases | 71 | 68 | **0** | 3 |
| Con el arreglo y la referencia movida a una semana **ya vencida** (2026-10-26; experimento local, no publicado) | 61 | 59 | 2 (de semilla, ver §5); **0 de vencimiento** (antes: 55) | 2 |
| **Regresión completa** `mvn -o verify -Pintegration` (c3d6744): unitarias | 1.028 | 1.028 | 0 | 0 |
| **Regresión completa**: integración | 466 | 433 | 0 | **33** |
| **CI de GitHub** `verify` (c3d6744) | — | — | **success** | — |

**Las 12 clases afectadas:**
- `GeneracionReferenciaIT`, `V35IT`, `V36IT`, `V37IT`, `V39IT`, `V40IT` y `CandidataIT`;
- `ValidacionMultisemanaIT`;
- `Usuario31ReproduccionIT` y `Usuario31V37IT`;
- `C2ReproduccionN03IT`;
- `PoliticaV4CCicloIT`.

**Comparación con `generation`:**
- Unitarias: 1.028 frente a 1.010; la diferencia son las 18 de `TrainingWeekTest`.
- Integración: 466 / 433 / 33, idéntica a la de `generation` cuando aún pasaba.

**`TrainingWeekTest` (18 pruebas) comprueba:**
- cada día de la semana de referencia;
- el domingo anterior y el lunes siguiente;
- los cambios de mes;
- los cambios de año: 2026-12-31 → semana del 2026-12-28; 2027-01-04 → lunes; 2028-12-31 y 2029-01-01;
- los bisiestos: 2024-02-29 y 2028-02-29;
- `previous` y `next` durante 60 semanas;
- que la fórmula anterior daba exactamente el 2026-11-02 entre el 26 de octubre y el 1 de noviembre, y otra semana desde el 2 de noviembre.

## 4. Pruebas omitidas

**Ninguna se omitió por este cambio.**

| Ejecución | Omitidas | Cuáles son |
|---|---|---|
| Integración, regresión completa | 33 | Laboratorios opt-in (por propiedad) y pruebas que exigen IA real. Las mismas de antes. No se cuentan como aprobadas |
| 12 clases afectadas | 3 | Las mismas con y sin el arreglo: `GeneracionReferenciaCandidataIT` (opt-in) y 2 de `ValidacionMultisemanaIT` |

## 5. Observaciones (sin cambio)

**1. La aserción «RESPALDO ⇒ semana idéntica»** de `ValidacionMultisemanaIT`, en los escenarios con dolor, depende de la semilla:
- falla con las semanas del 2026-10-26 y del 2026-11-09;
- pasa con la del 2026-11-02, la fijada.

Con la fecha fija la prueba es determinista. Revisar si la aserción es demasiado estricta queda para otra decisión.

**2. Tres pruebas opt-in, fuera del CI,** también usan la semana fija y ejecutan sesiones:
- `CampanaP123Cohorte02IT` (REPLAY);
- `CampanaMvp8UsuariosIT`;
- `ReincorporacionIaRealIT`.

No se han tocado. Si se vuelven a lanzar después de esa semana, necesitarán `RelojPruebas.abrirVentanaDeEjecucion`.

## 6. Próximos pasos

1. **Tu aprobación** para fusionar el PR #19.
2. Después:
   - actualizar el PR #17 desde `generation` con *merge*;
   - repetir su regresión;
   - comprobar que su CI queda verde;
   - mantenerlo en borrador.

**Sin despliegue, sin migraciones remotas, sin activar políticas y sin tocar Flutter, el catálogo ni Replicate.**
