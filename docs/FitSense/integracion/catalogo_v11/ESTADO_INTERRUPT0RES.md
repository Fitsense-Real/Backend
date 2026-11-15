# Estado de los interruptores en `generation` (`50849d0`)

## Problema

Varias funciones de v1.1 se integraron apagadas. Hay que dejar fijado su estado real en producción y cómo se comprueba.

## Interruptores del catálogo y la seguridad

La columna «Valor efectivo en producción» es el valor por defecto en `application.properties` o en el `@Value` del código. Solo cambia si alguien define la variable o la propiedad.

| Interruptor | Variable / propiedad | Valor efectivo en producción | En las pruebas | Decisión vigente |
|---|---|---|---|---|
| Contención conservadora (INC-CAT-CONS-v1) | `fitsense.catalogo.contencion-conservadora` | **Encendida** (`true`) | Encendida | Aprobada |
| P-DOLOR-PUB: dolor en planes publicados | `fitsense.seguridad.dolor-en-planes-publicados` | **Encendida** (`true`) | Encendida | Aprobada |
| P-03 por colocación | `FITSENSE_P03_POR_COLOCACION` → `fitsense.regeneracion.p03-por-colocacion` | **Apagada** (`false`) | Encendida (`src/test/resources/config/application.properties`; no se empaqueta en el jar) | Se conserva en código, apagada en producción |
| V53 opción A: «no colgarse» excluye el apoyo suspendido | `fitsense.seguridad.apoyo-suspendido-en-no-colgarse` | **Apagada** | Apagada, salvo en sus pruebas aisladas | No activar |
| V53-B: permiso propio de apoyo suspendido | `fitsense.seguridad.permiso-apoyo-suspendido` | **Apagada** | Apagada, salvo en sus pruebas aisladas | Dirección futura preferida. No activar |
| #64: reparación tolerante a la dificultad de publicación | `fitsense.reparacion.tolerar-dificultad-de-publicacion` | **Apagada** | Apagada, salvo `Plan78ContencionReducidaIT#propuestaDificultadDePublicacion`, que la enciende y la restablece en `finally` | No activar |

**Exclusión mutua:** si V53 A y V53-B están encendidas a la vez, `CatalogContextFacade` lanza `IllegalStateException` («Configuración ambigua del apoyo suspendido») y el contexto no arranca.

## Otros interruptores vigentes (sin cambios en esta integración)

| Interruptor | Valor |
|---|---|
| `fitsense.planning.policy` | `PLAN-REV-2.0` |
| `fitsense.ai.adaptation.prompt-version` | `v2.1` |
| Prompt de generación | PR-1.7 (`ConfiguracionEvaluadaTest`) |
| `fitsense.adaptation.pain-localized-removal` | `on` |
| `fitsense.adaptation.v8c.pattern-rule` | `multiconjunto` |
| `fitsense.adaptation.difficulty-guard` | `preferencia` |
| `fitsense.adaptation.progression.enabled` | `true` |
| `fitsense.dose.enforcement` / `fitsense.capability.enforcement` | `ENFORCE` |
| `fitsense.ai.generate.max-per-hour` | 5 |

## Interruptores nuevos fuera de `generation`

| Interruptor | Dónde | Valor |
|---|---|---|
| `fitsense.ai.generacion.contrato-v15` (H2, PR-1.9) | Solo en la rama de #69, sin fusionar | `false` |

## Decisiones de política documentadas (fase 2)

### P-03 por colocación

**Estado:** queda en el código, apagada en producción. Las pruebas de encendido y apagado se conservan.

**Qué no se hizo:**

- no se modificó el algoritmo;
- no se añadió ninguna equivalencia entre repeticiones y segundos.

**Pérdida de factibilidad si se activa** (`premerge_final/VALIDACION_P03_POR_COLOCACION.md` §3, escenario del usuario 17):

1. El lunes, en el puesto del **295**, retirado por dolor y medido a **2×12**, la regeneración coloca el **253** a **3×120 s**, que es su dosis base.
2. La regla lo trata como una SUSTITUCION con más series (2 → 3) y lo bloquea.
3. La semana queda NO_FACTIBLE, cuando en v1 se publicaba (plan 76) con el trabajo total a la baja.

**Precio:** las regeneraciones de nivel 2 publicadas en la regresión bajan de 10 (7 escenarios) a 8 (6). Ninguna NO_FACTIBLE pasa a publicarse.

### #64: reparación tolerante

**Estado:** integrada con el comportamiento opcional apagado.

**Qué no hace, apagada:**

- ninguna reparación automática;
- ningún aumento de dosis;
- ningún cambio retroactivo.

**Lo único que se aplica es V54**, que solo deja constancia: `planned_workouts.system_block_cause` al vencer una sesión bloqueada.

**Caso 494:**

- La reparación del plan 78 sustituye **exactamente** el 1303 (inseguro, INC-CAT-C39) y el 494 (contenido por identidad no determinada, INC-CAT-CONS-v1).
- Lo hace sin más dosis y con ningún sustituto contenido.
- La versión 1 del plan no se toca.
- Si no hay una reparación válida, queda NO_VALIDA sin publicar. Con la semana empezada queda SEMANA_EMPEZADA.

### V53 / #66

**Dirección futura preferida:** la opción B, permiso propio, que el participante confirma.

**Estado:**

- A y B están apagadas y no pueden estar activas a la vez.
- Activar B exige una aprobación aparte y que Flutter pida el permiso.
- En esta fase no se tocó Flutter.

## Cómo comprobarlo en un despliegue

Antes de desplegar, revisar que **no** estén definidas:

- `FITSENSE_P03_POR_COLOCACION`;
- `FITSENSE_AI_GENERACION_CONTRATO_V15`;
- ninguna variable que Spring Boot asocie a las propiedades de V53 A, V53-B y #64. Por la conversión automática de nombres serían, por ejemplo:
  - `FITSENSE_SEGURIDAD_APOYO_SUSPENDIDO_EN_NO_COLGARSE`;
  - `FITSENSE_SEGURIDAD_PERMISO_APOYO_SUSPENDIDO`;
  - `FITSENSE_REPARACION_TOLERAR_DIFICULTAD_DE_PUBLICACION`.

Tampoco deben estar definidas `FITSENSE_CATALOGO_CONTENCION_CONSERVADORA=false` ni `FITSENSE_SEGURIDAD_DOLOR_EN_PLANES_PUBLICADOS=false`.

## Resultado

Lo que está encendido en producción coincide con lo aprobado.

## Pendientes

Cualquier activación necesita una autorización expresa.
