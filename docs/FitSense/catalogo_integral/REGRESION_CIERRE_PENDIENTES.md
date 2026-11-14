# Regresión del cierre de pendientes (orden de 14/11/2026, fase 10)

Todo se ejecutó en local con Testcontainers (Postgres 16, base nueva en cada contexto), sin datos reales y sin Replicate.

> **Actualización (orden de 14/11/2026 sobre las regresiones de V52.1).** Los dos fallos del §1 están **resueltos** sin código de producción. El detalle está en `REGRESIONES_V52_1.md` (commit `b5231cb`).
>
> | Rama | Unitarias | Integración |
> |---|---|---|
> | Integración (`72d6808`) | 1.468 / 0 fallos | 730 / 0 fallos / 0 errores / 51 omitidas |
> | #64 (`f9daccb`) | 1.468 / 0 fallos | 726 / 0 / 0 / 51 |
> | #63 (`eda15de`) | 1.468 / 0 fallos | 725 / 0 / 0 / 51 |
>
> Rejillas de 168 perfiles: 168 de 168 con V53 apagado y encendido. Con V53 apagado, la rejilla es idéntica a la medición anterior; con V53 encendido cambian 16 perfiles.
>
> Procedencia V1–V53: V52.1 toca 7 ejercicios, con 25 cambios de atributo.
>
> CI en verde en #61 (`f42d473`), #63 (`eda15de`) y #64 (`f9daccb`).
>
> Las secciones siguientes reflejan el estado anterior a esa corrección.

## 1. Resultados

| Rama | Contenido | Unitarias | Integración | Resultado |
|---|---|---|---|---|
| `cierre/integracion-final` (`0d88a63`) | `generation` + #62 + #61, que incluye #55–#60 y #63 | 1.468 / 0 fallos | 727: **2 fallos** / 0 errores / 50 omitidas (opt-in) | **ROJO** |
| `fix/sustituto-dosis-capacidad` (#62, `1da115a`) | HALL-PROG-01 y V8C | 1.431 / 0 | 617 / 0 / 46 omitidas | **VERDE** |
| `propuesta/reparacion-dificultad-publicada` (#64, `cc1ecf8`) | #63 + plan 78 + V54 | 1.468 / 0 | 723: **2 fallos** (los de #63) / 0 errores / 50 omitidas | **ROJO**, solo por los fallos heredados de #63 |
| CI de #61 | igual que la integración | — | 727: los mismos 2 fallos | **ROJO** |

**Los dos fallos** (aparecen igual en #63, #61, la integración y #64):

| Prueba | Qué pasa | Causa | Estado |
|---|---|---|---|
| `HerenciaD8IT.dolorNoAprovechaD8` | La prueba exige NO_FACTIBLE. Ahora P-03 nivel 2 **publica** una semana regenerada (`excluidos [72, 118, 494, 998, 1044]`) | V52.1 cambia los elegibles de 231, 367 y 1182, y con ellos la semana del participante que elige `acortado()`. Ejecutada sola, pasa (otro participante) | **Sin resolver.** P-03 solo publica si ningún excluido reaparece y no hay más trabajo que en la semana medida (comprobado en `SafeRegenerationService`), así que la salida parece legítima, pero la aserción la prohíbe |
| `AdaptacionRegresionIT.conBarraSeConserva` | V24 rechaza un 1374 con 2x10 | La preparación (`con1374`) le hereda la dosis del relleno al que sustituye. En la semana nueva no hay relleno de cintura y el elegido es de otra capacidad (2x10). La capacidad del 1374 exige 2x13 | **Sin resolver.** V24 actúa correctamente; la premisa de la prueba ya no se cumple |

**No se han modificado esas pruebas.** Se preparó un cambio que comprobaba las garantías reales en lugar del resultado de un catálogo concreto. El control de permisos lo bloqueó, de acuerdo con la restricción de la orden («no modificar pruebas para conseguir resultados verdes»). Decide el propietario (ver #63).

## 2. Pruebas pedidas por la orden (en la integración)

| Ámbito | Prueba | Resultado |
|---|---|---|
| p02 / memoria del dolor | `Auditoria O12 · dolor con la progresión apagada/encendida`, `V8C · dolor`, `Retirada localizada por dolor` | Verde |
| O12 | `O12 · un sustituto de seguridad no puede subir series, repeticiones ni segundos` (7) | Verde |
| HALL-PROG-01 | `HALL-PROG-01 · el sustituto de seguridad recibe una dosis de capacidad válida y nunca más trabajo` (9) | **Verde en la integración** |
| V8C | 40 + 32 + 13 + 10 + 10 + 6 + 5 + 3 + 2 pruebas V8C | Verde |
| V24 | `V34 · V24 + V8C: solo la dosis autorizada y en rango` (18) | Verde |
| Progresión y reincorporación | `1(a) · solo una PROGRESION posterior…` (7), `Reincorporacion4DTest` (5), `D8 · herencia…` (9) | Verde. **Excepción: `HerenciaD8IT.dolorNoAprovechaD8`** (§1) |
| Catálogo | `Catálogo · controles permanentes de calidad` (huella con V52.1), V47–V51, `CatalogoElegibleIT`, `ContencionTextoBanderaIT`, `V53 · interruptor apagado` | Verde |
| Referencias | `GeneracionReferenciaIT` (72 planes válidos, referencia regenerada en #63), V35–V40 | Verde |
| Hipertrofia | `4D v3.4 + C-8 · rejilla de 312 perfiles`: 298 válidos y exactamente 14 NO_FACTIBLE por V20 (775) | Verde |
| Migraciones desde una base nueva | Todas las IT arrancan de cero con V1–V53 y V52.1; `V43 · migración sobre esquema histórico` | Verde |

## 3. Rejillas y procedencia (opt-in, en la integración)

| Medida | Resultado |
|---|---|
| Rejilla de 168, V53 apagado | **168 de 168 con plan** |
| Rejilla de 168, V53 encendido | **168 de 168 con plan.** Cambian los elegibles de 16 perfiles; salen 457, 458, 459, 462, 777, 920, 934, 1184, 1317 y 1335 |
| Frente a la rejilla anterior a V52.1 (apagado) | Elegibles: solo cambia el 1182, que entra en 60 perfiles más (ya no exige cajón). Contenido: 91 planes distintos, por la nueva clasificación de 231 y 367 y por el 1182. Ninguno pierde el plan |
| Procedencia V1–V53 | V52.1: 7 ejercicios, 25 cambios de atributo. Ningún otro cambio imprevisto |

## 4. Propuesta #64 (plan 78 + V54)

- **Primera pasada:** 48 errores de arranque (`Schema validation: missing column [system_block_cause]`). Las pruebas que fijan `spring.flyway.target` en un catálogo anterior aplican las migraciones de solo esquema registradas en `EsquemaVigenteSobreCatalogoAnterior.SOLO_ESQUEMA`, y faltaba V54. Se corrigió registrándola (`cc1ecf8`), igual que V42–V44.2 y V51. No cambia ninguna aserción.
- **Segunda pasada:** 1.468 unitarias en verde; 723 de integración con 0 errores y solo los 2 fallos heredados de #63.
- `Plan78ContencionReducidaIT`: 4 de 4, incluidas la prueba nueva con el interruptor encendido y la constancia de V54.

## 5. Comprobaciones de la orden

| Comprobación | Resultado |
|---|---|
| Nuevas violaciones de seguridad | Ninguna detectada. Las contenciones y banderas solo aumentan |
| Dosis no autorizadas | Ninguna: V24 y V8C en verde. El único rechazo de V24 (`conBarraSeConserva`) es correcto |
| Regresiones sin explicar | Ninguna. Las dos del §1 están explicadas, pero **no resueltas** |
| Cambios de política ocultos | Ninguno: V15, V20, V24, V8C y P-03 sin cambios. La propuesta del plan 78 está apagada |
| Cambios retroactivos | Ninguno. V54 no recalcula nada |
| Pruebas modificadas para ocultar defectos | Ninguna. Hay dos ajustes legítimos y documentados: `CatalogoIntegralTextosIT` lee V52.1, y se regenera la referencia de 72. El cambio de las dos pruebas del §1 **no** se aplicó |
