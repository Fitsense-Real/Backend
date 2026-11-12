# P2-D: sustitución obligatoria por seguridad (SUST-SEG-1)

**Fecha:** 23/10/2026 · **Estado:** desarrollada y probada en PostgreSQL efímero. **No desplegada sobre datos reales.** No cubre los planes ya publicados (siguiente paso).

## 1. Qué resuelve

Una semana conservada, o la parte no señalada de una adaptación, puede contener un ejercicio que **dejó de ser compatible por seguridad**: el caso de INC-CAT-1374, una restricción declarada después o una corrección de banderas del catálogo. Antes, V1 lo rechazaba y la semana terminaba en `NO_FACTIBLE`. No se publicaba nada inseguro, pero el participante se quedaba sin semana.

Ahora ese ejercicio, y **solo ese**, se sustituye por uno elegible, si existe. Si no existe, el resultado sigue siendo `NO_FACTIBLE`.

## 2. Qué cuenta como «dejó de ser compatible por seguridad»

`SafetyExclusionResolver` lo decide por cada ejercicio **heredado** de la semana medida que hoy no es elegible.

- **Es de seguridad solo con una causa verificada** (`SafetyConflictPolicy`, revisión del 24/10). No basta con que el ID falte del conjunto elegible. La causa sale de un dato comprobable:
  - una **bandera del catálogo** contra una **restricción actual** del perfil: `alto_impacto`, `suelo`, `carga_axial`, `rodillas`, `suspension`, `brazos_arriba`, `inversion`;
  - la retención **`INC-4C-03`**;
  - la contención **`INC-CAT-1374`**.
- **El motivo** registra todas las causas unidas por `+` (p. ej. `INC-CAT-1374`, `suelo+brazos_arriba`). `ExclusionesDeSeguridad` rechaza al construirse cualquier motivo que no esté hecho solo de esas causas. Ya no existe `varias_restricciones_de_seguridad`.
- **No es de seguridad**, y sigue en V1 como siempre:
  - un ejercicio **bloqueado** por el participante (es una preferencia);
  - equipamiento que ya no declara;
  - el filtro funcional;
  - un ejercicio inactivo.
- **Si el resolutor falla** (excepción), no hay exclusiones y el comportamiento es el anterior.

## 3. La regla de sustitución

`SafetySubstitutionPolicy` (dominio puro) solo repara ejercicios que cumplan **las dos** condiciones:

- están excluidos por seguridad (sección 2);
- son **heredados de esa misma sesión** de la semana medida.

Un ejercicio que introduce la IA no se repara: si no es elegible, es un error de la IA y V1 lo rechaza.

**Requisitos del sustituto:**

1. Está en el conjunto elegible actual. Ya respeta equipamiento, dificultad, restricciones de seguridad, bloqueos, la retención y la contención.
2. **Misma clase de slot.** Un estructural sigue en su slot 4C (`LOWER_KNEE`, `LOWER_HIP`, `UPPER_PUSH`, `UPPER_PULL`) y un relleno sigue siendo relleno.
3. Mismo tipo de prescripción (`SETS_REPS` / `DURATION`).
4. Misma condición de complemento (`CatalogEligibilityPolicy`): un complemento de cardio o movilidad no sustituye a trabajo resistido, ni al revés.
5. Zona que el enfoque de la sesión admite, o la misma zona del original.
6. Regla de brazos de V22 en `PUSH`/`PULL`.
7. No está entre los 21 ejercicios en revisión de clasificación.
8. No está ya en la sesión ni en una sesión de un día contiguo.

**Preferencia:** misma zona, luego mismo patrón, luego mismo músculo objetivo. Desempate por id; la elección es determinista.

**Dosis.** Se recalcula para el sustituto; no se copia a ciegas:

- **`GAIN_MUSCLE`:** la dosis 4D de su clase y zona (`MvpDosePolicy`: estructural 3×10, relleno 2×10, +3 repeticiones en zona pequeña, descanso 90).
- **Otros objetivos:** las series y el descanso se conservan, y las repeticiones se ajustan a los límites de la versión de prescripción para su zona y nivel.
- **`DURATION`:** se conservan series, segundos y descanso.
- **Carga sugerida:** `null`, porque no hay registro de carga del sustituto (V11).

**Lo que no cambia:** el día, el enfoque, el nombre, la duración declarada, el orden y el resto de ejercicios y dosis de la sesión. Las demás sesiones quedan intactas.

## 4. La excepción en V8C

V8C prohíbe cualquier cambio en una sesión no señalada. Por eso la excepción es **una lista explícita de sustituciones**, no un permiso sobre la sesión. `ConservationValidator.neutralizarSustituciones` admite cada sustitución solo si se cumplen todas estas condiciones:

1. El quitado estaba en esa sesión de la semana medida.
2. **El quitado tiene una causa de seguridad verificada y el motivo declarado es exactamente esa causa.** V8C recibe las exclusiones, que `SafetyExclusionResolver` calcula aparte desde las banderas, y no se fía de la autorización. Una autorización fabricada para un ejercicio bloqueado, sin el equipamiento o fuera por el filtro funcional se rechaza, aunque su ID falte del conjunto elegible. Esa sesión sigue entonces sin poder cambiarse.
3. El quitado ya **no** es elegible. Si lo sigue siendo, no hay motivo de seguridad y la sustitución se rechaza.
4. El sustituto es elegible.
5. El sustituto no estaba ya en la sesión.
6. Misma clase de slot, comparando el patrón del quitado con el del sustituto.
7. La semana nueva contiene el sustituto y no el quitado.

Una sustitución admitida se descuenta del par añadido/quitado. **Cualquier otra diferencia en la misma sesión sigue rechazada**: otro ejercicio, otra dosis u otro orden. Una autorización para una sesión que no existe es un problema, y una autorización ilegítima deja el cambio sin neutralizar, así que V8C lo rechaza como siempre.

Después, la semana entera pasa por la validación estricta real: V1–V24, la cobertura 4C (V23) y la dosis 4D en `ENFORCE`.

## 5. Evidencia (V32)

`plan_adaptation_attempts.conservation_report` (JSONB, el informe de V8C entero) incluye `sustituciones`, con una entrada por cambio (ejemplo ilustrativo; los nombres van en snake_case):

```json
{"sesion_origen": 101, "fecha": "2026-11-09", "quitado": 1374, "patron_quitado": "TRUNK_FLEXION",
 "puesto": 2001, "motivo": "INC-CAT-1374", "regla": "SUST-SEG-1",
 "dosis_anterior": "2x13 descanso 90", "dosis_nueva": "2x13 descanso 90"}
```

Se registran el ID original, el nuevo, el motivo y la regla. El resultado consta en la misma fila: `status`, `resulting_plan_id`, `generation_source`. Si no hay sustituto:

- `status = NO_FACTIBLE`, sin plan;
- `rejection_reason` empieza por `SEGURIDAD: el ejercicio N de la sesion S (fecha) dejo de ser compatible por seguridad (motivo) y no hay sustituto elegible con la regla SUST-SEG-1 … Requiere intervencion; no se publica una sustitucion insegura.`

Este motivo va antes que los de V8C y V1.

## 6. Pruebas

**Unitarias** (`SustitucionSeguridadTest`, 17, datos sintéticos):

- **Política:**
  - solo se toca el heredado inseguro, y en su posición;
  - un estructural va a su mismo slot;
  - se prefiere la misma zona y nunca un ejercicio en revisión;
  - dosis 4D;
  - sin sustituto, el problema `SEGURIDAD:` y la semana intacta;
  - lo no heredado no se repara;
  - no repite en la sesión ni en un día contiguo;
  - una zona fuera de enfoque no sirve;
  - sin exclusiones, nada cambia.
- **V8C:**
  - admite la autorización legítima;
  - sin autorización, el cambio se rechaza;
  - rechaza un quitado que sigue siendo elegible;
  - rechaza un cambio de clase de slot;
  - otro cambio en la misma sesión sigue rechazado.
- **Productor:**
  - la continuidad con un sustituto se publica `CONSERVED`;
  - sin sustituto, `NO_FACTIBLE` con el motivo de seguridad primero;
  - sin exclusiones, el V1 de antes.

**Integración** (`AdaptacionRegresionIT`, PostgreSQL 16 efímero con V1–V33, flujo `adaptWeeklyPlan` real, validadores reales, IA simulada y **sin llamadas a Replicate**):

| Caso | Resultado |
|---|---|
| 0c · 1374 en la semana medida, sin barra | **Publicada.** El 1374 se sustituye por un relleno elegible con dosis 4D. El resto de la semana es idéntico (fecha, enfoque, duración, ejercicios y dosis). Evidencia con `quitado=1374`, `motivo=INC-CAT-1374`, `regla=SUST-SEG-1`. `ai_calls = 0`, `generation_attempts = 1` |
| 0c · con barra pero brazos arriba prohibidos | Igual; el sustituto no requiere brazos arriba |
| 0c · con barra y sin restricciones | Copia exacta, con el 1374: no se cambia lo que es correcto |
| 1 · un ejercicio pasa a exigir suelo y el participante no lo permite | **Publicada.** Sustituido por uno de su mismo slot, sin suelo y con dosis 4D. `motivo=suelo` |
| 1 · lo mismo en una sesión **no señalada**, con la IA llamada sobre otra sesión (sin propuesta) | Se publica el respaldo con la sustitución; `ai_calls = 1` |
| 1 · **ningún** ejercicio del slot es seguro | `NO_FACTIBLE`, `rejection_reason` que empieza por `SEGURIDAD:`, con `SUST-SEG-1` y `suelo`. Sin plan nuevo y la semana medida intacta |
| 3 · un ejercicio de la semana medida, **bloqueado** | Sigue en `NO_FACTIBLE` por V1: un bloqueo no es seguridad |

Las pruebas que modifican el catálogo efímero lo restauran en `finally`.

**Resultados del 23/10/2026:**

- `mvn test`: 606/606, 0 saltadas.
- `mvn verify -Pintegration`: 37 ejecutadas, 0 fallos, 8 saltadas. Las 8 son las de P2-D antiguas, que exigen `-Dfitsense.pruebas.url`.
- Las 7 de esas clases que no pagan (`InvariantesV33IT`, `PublicacionAtomicaIT`, `DisparadorDiferidoIT`) se ejecutaron aparte contra un PostgreSQL local en Docker con V1–V33: 7/7, sin filas residuales.
- `CorreccionReplicateEvidenciaIT` sigue saltada: requiere una corrida pagada.

## 7. Lo que no cubre

- **Planes ya publicados.** Esto actúa al producir la semana siguiente. Una sesión publicada hoy con el 1374 sigue abierta. Es el siguiente paso autorizado: identificarla, bloquearla en la vista o repararla en una versión nueva sin tocar el histórico.
- **La generación inicial** no lo necesita: ya parte del conjunto elegible.
- **El ejercicio sustituido puede estar entre los 21 en revisión.** Eso solo impide que se elija como sustituto.
