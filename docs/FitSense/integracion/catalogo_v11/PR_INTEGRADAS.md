# PR integradas en `generation` (catálogo v1.1)

## Problema

Hay que dejar constancia de qué entró, en qué orden y qué queda fuera.

## Integradas

| Orden | PR | Contenido | Migración | Interruptor | Commit en `generation` |
|---|---|---|---|---|---|
| 1 | [#62](https://github.com/Fitsense-Real/Backend/pull/62) | HALL-PROG-01: dosis de capacidad del sustituto SUST-SEG-1 | — | — | `17a747a` |
| 2 | [#56](https://github.com/Fitsense-Real/Backend/pull/56) | Catálogo integral 1/4: banderas de seguridad y dificultad conservadora | V47, V48 | — | `5ad814c` |
| 3 | [#57](https://github.com/Fitsense-Real/Backend/pull/57) | Catálogo integral 2/4: textos | V49 | — | `f3075d8` |
| 4 | [#58](https://github.com/Fitsense-Real/Backend/pull/58) | Catálogo integral 3/4: clasificación de equipamiento | V50 | — | `41e61bc` |
| 5 | [#59](https://github.com/Fitsense-Real/Backend/pull/59) | Catálogo integral 4/4: controles permanentes | — | — | `9936a84` |
| 6 | [#55](https://github.com/Fitsense-Real/Backend/pull/55) | Contención INC-CAT-C39 reducida a 8 ejercicios | — | — | `29ed676` |
| 7 | [#60](https://github.com/Fitsense-Real/Backend/pull/60) | Correcciones verificadas, impacto y estiramiento explícitos. Trae el mismo parche de HALL-PROG-01 que #62, que Git aplica una sola vez | V51, V52 | — | `dc4f0e1` |
| 8 | [#63](https://github.com/Fitsense-Real/Backend/pull/63) | Pendientes técnicos del catálogo | V52.1 | — | `ce7c892` |
| 9 | [#65](https://github.com/Fitsense-Real/Backend/pull/65) | HALL-GM-01: el sustituto en GAIN_MUSCLE no supera el trabajo del quitado | — | — | `893cc2f` |
| 10 | [#61](https://github.com/Fitsense-Real/Backend/pull/61) | V53 apoyo suspendido. Solo el dato; la opción A queda apagada | V53 | `fitsense.seguridad.apoyo-suspendido-en-no-colgarse=false` | `a824420` |
| 11 | [#64](https://github.com/Fitsense-Real/Backend/pull/64) | V54: constancia del bloqueo al vencer una sesión. La reparación tolerante del plan 78 queda apagada | V54 | `fitsense.reparacion.tolerar-dificultad-de-publicacion=false` | dentro de `50849d0` |
| 12 | [#66](https://github.com/Fitsense-Real/Backend/pull/66) | V55, V53-B: permiso propio de apoyo suspendido. Solo la columna; queda apagada | V55 | `fitsense.seguridad.permiso-apoyo-suspendido=false` | dentro de `50849d0` |
| 13 | [#67](https://github.com/Fitsense-Real/Backend/pull/67) | Candidata final v1.1: V56 (texto del 853), contención conservadora, P-DOLOR-PUB y P-03 por colocación (apagada en producción) | V56 | `fitsense.regeneracion.p03-por-colocacion=false` | **`50849d0`** |

**Migraciones nuevas en `generation`:** V47, V48, V49, V50, V51, V52, V52.1, V53, V54, V55 y V56.

- Son 11 archivos, en total 59 migraciones desde V1.
- Ninguna reordena ni modifica una anterior.
- `outOfOrder` no se tocó.

## Integrada después del catálogo

| PR | Contenido | Estado |
|---|---|---|
| [#68](https://github.com/Fitsense-Real/Backend/pull/68) | V57: constancia persistente de las omisiones impuestas por el sistema | **Fusionada** → `0d11278` (`INTEGRACION_V57.md`) |

## Pendientes, sin fusionar (por orden)

| PR | Contenido | Por qué no se fusiona |
|---|---|---|
| [#69](https://github.com/Fitsense-Real/Backend/pull/69) | H2: contrato prompt–V15 (PR-1.9) detrás de un interruptor apagado | La orden prohíbe fusionar automáticamente las PR nuevas de H2. Activarla requiere una evaluación pareada pagada (`../../continuacion_desarrollo/CORRECCION_H2.md`) |

## Resultado

Las 13 PR del catálogo están integradas, con 11 migraciones nuevas y sin duplicados.

## Pendientes

Decidir sobre #69.
