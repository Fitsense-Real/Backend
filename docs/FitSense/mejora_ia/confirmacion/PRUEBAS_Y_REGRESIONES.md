# Pruebas y regresiones · CONF-IA

Cifras reales de cada ejecución. Ninguna suite se da por verde si tiene fallos.

## 1. Corrección de V8C (fase 1)

| Ejecución | Commit | Resultado |
|---|---|---|
| `V8CRetiradasYDescansoTest` sobre el validador **sin corregir** | `0878f85` + pruebas nuevas | **25 pruebas, 9 fallos**: las 4 negativas de H-A1 con VARIANTE y las 5 negativas de H-A2. El defecto queda demostrado |
| `V8CRetiradasYDescansoTest` + `DolorSinMasTrabajoV8CTest` con la corrección | `5bea428` | 31 pruebas, 0 fallos |
| Unitarias completas, 1.ª pasada | `5bea428` | 1341 pruebas, **5 fallos**: tres pruebas existentes fijaban el comportamiento defectuoso (H1 7 y 8 con las dos reglas, y C4 «menos descanso») |
| Unitarias completas, tras invertir esas tres pruebas | `5bea428` | **1341 pruebas, 0 fallos, 1 omitida** |
| Integración completa (`verify -Pintegration`) | `5bea428` | **Interrumpida**: el contenedor se reinició a mitad. Hasta entonces (287 clases) solo fallaban las 3 dependientes de la fecha |
| Integración completa | `ab04e67` (V8C + pruebas de fecha) | **593 pruebas, 0 fallos, 0 errores, 44 omitidas** |

**Qué cubre la integración:**
- V1–V27, V24 y V25 en la generación inicial de los cuatro objetivos (referencias de generación y rejilla);
- V8C con adaptaciones localizadas y expandidas;
- progresión (`ProgresionIT`, `ProgresionRev2IT`);
- recuperación y reincorporación;
- conservación de sesiones;
- PLAN-REV-2.0;
- catálogo V36/V37.

V24 de la generación inicial no cambia: la generación no pasa por V8C. Las referencias de generación de la integración siguen idénticas.

## 2. Pruebas dependientes de la fecha (commit aparte, `ab04e67`)

| Prueba | Antes (09/11/2026) | Causa | Corrección (solo código de prueba) | Ahora |
|---|---|---|---|---|
| `CatalogoV36IT.semanaSiguiente` | FALLO: el plan de 872 sale NO_FACTIBLE por 1296 | La semana se calculaba con `LocalDate.now()`. El motor de reglas siembra su aleatoriedad con la semana, así que otra semana elige otros ejercicios (1296, desactivado en V36) | Semana fija `RelojPruebas.SEMANA_REFERENCIA + 1` (la semana con la que pasaba el 05/11/2026) y apertura de la ventana de ejecución antes de cumplir las sesiones | Pasa |
| `ProgresionIT.p02ProgresionSinElExcluido` | ERROR (`NoSuchElement`) | La misma | La misma (`ejecutar` abre la ventana de ejecución) | Pasa |
| `ProgresionRev2IT.p02ProgresionSinElExcluido` | ERROR | Hereda de `ProgresionIT` | — | Pasa |

No cambian la lógica productiva, las políticas, las fechas de negocio ni ningún resultado experimental anterior.

## 3. V8C corregido en las corridas reales (Replicate)

| Corridas | Adaptaciones con IA | Rechazos de V8C | Cambios no autorizados que pasaron |
|---|---|---|---|
| D1–D5, V2.1 (10 corridas) | 60 sesiones (50 predicciones) | 0 | 0 |
| D1–D5, v2.2a (10 corridas) | 60 sesiones | 1: D3, O19, intento 1 (ACORTAR que retira un ejercicio **y además** baja la dosis de otro). La regla de acortar ya lo prohibía antes de esta corrección. El reintento se aplicó | 0 |
| Flujo conjunto I1 | 12 sesiones | 0 | 0 |

- Ninguna propuesta real retiró ejercicios sin autorización (H-A1) ni cambió descansos (H-A2), ni antes ni después de V8C.
- El oráculo independiente lo confirma: sin cambios no autorizados por identidad en las 132 sesiones adaptadas.
- Las pruebas del §1 demuestran que, si una propuesta lo intenta, ahora se rechaza.
- Después de la congelación (`957a08d`) no se tocó código productivo. Las corridas usan `ab04e67`.
