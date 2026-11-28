# Las dos regresiones de V52.1: causa, cambios en las pruebas y garantías (orden de 14/11/2026)

**Autorización.** El propietario autorizó modificar de forma controlada las dos pruebas, solo tras demostrar que sus escenarios quedaron desactualizados por V52.1. No se ha modificado código de producción.

- Diff exacto: commit `b5231cb` en `cierre/pendientes-seguridad` (#63).
- Pruebas: `HerenciaD8IT` y `AdaptacionRegresionIT`.

## 1. Qué causaba cada fallo

### 1.1 `HerenciaD8IT.dolorNoAprovechaD8`

El motor de reglas siembra su aleatoriedad con `userId × 1.000.003 + inicio de semana`. La prueba creaba a sus participantes con `user_id` autoincremental y calculaba la semana desde `LocalDate.now()`. Por tanto, el escenario cambiaba según el orden de los métodos de la clase y según el día.

**Exploración determinista** (`exploracionDolor`, opcional): misma semana de referencia y 40 participantes fijos (9001–9040), antes de V52.1 (#60) y después (#63).

| | Antes de V52.1 | Después de V52.1 |
|---|---|---|
| Participantes con la premisa (viernes acortado < 31 min) | 35 | 32 |
| P-03 nivel 2 **publica** una semana regenerada segura | **3** (9004, 9008, 9014) | **2** (9004, 9008) |
| P-03 nivel 2 da **NO_FACTIBLE** («sube el trabajo») | 32 | 30 |

**Conclusión:** el NO_FACTIBLE que exigía la prueba **nunca fue una garantía**. Ya antes de V52.1, P-03 publicaba para algunos participantes. La prueba pasaba porque el participante que le tocaba por orden y por fecha era de los NO_FACTIBLE. Con V52.1, el que le tocó dentro de la clase completa fue uno de los que P-03 publica.

**Conjunto elegible del perfil, sin historial, antes y después de V52.1:**
- entra el **1182** (CORE_STABILITY, cintura), porque ya no exige cajón;
- el **367** pasa de ELBOW_EXTENSION (brazos) a SHOULDER_EXTENSION (hombros);
- nada más cambia.

### 1.2 `AdaptacionRegresionIT.conBarraSeConserva`

La preparación `con1374` pone el 1374 en el lugar de un relleno de la semana medida y le **hereda su dosis**. Con V52.1 la misma semilla genera otra semana, porque el pool cambió (1182 y 367).

| | Antes de V52.1 | Después de V52.1 |
|---|---|---|
| Relleno elegido (participante 20) | 502, cintura, 2x13 | 784, pecho, SHOULDER_EXTENSION, 2x10 |
| Dosis del 1374 | 2x13: la de su clase | 2x10: heredada de otra clase |
| V24 | Acepta | **Rechaza, con razón**: «4D v2 exige 2x13» |

El camino «no hay relleno de cintura», que el comentario ya preveía, nunca se había recorrido. **El defecto estaba en la premisa de la prueba, no en el backend.**

## 2. Qué cambió en los escenarios (diff)

### `HerenciaD8IT`

| Cambio | Tipo | Justificación |
|---|---|---|
| `S1 = RelojPruebas.SEMANA_REFERENCIA` (antes, la semana siguiente a hoy) y `RelojPruebas.abrirVentanaDeEjecucion` antes de abrir sesiones | Infraestructura determinista | Es el mecanismo del repositorio para fijar la semilla (ya lo usan O12 y HG9). No toca el backend |
| `participante(long u)` con `user_id` explícito (`OVERRIDING SYSTEM VALUE`), y `acortado(long... candidatos)` | Infraestructura determinista | La semilla ya no depende del orden de la clase |
| Candidatos fijos en `respaldoIdenticoConservaD8` (8101–8110) y `propuestaIdenticaConservaD8` (8201–8210) | Infraestructura | Mismo mecanismo de búsqueda de la premisa, con participantes fijos. **Sin cambios en sus aserciones** |
| `dolorNoAprovechaD8`: participante 9001 | Escenario | **Aserciones idénticas** (NO_FACTIBLE, «sube el trabajo», exclusión activa, sin violaciones de linaje). 9001 da NO_FACTIBLE antes y después de V52.1 (456 → 490 y 456 → 496). Es el caso negativo obligatorio |
| **Nueva** `dolorConRegeneracionSegura` (participante 9004) | Aserciones añadidas | El otro desenlace legítimo, con todas sus garantías comprobadas (§3) |
| **Nueva** `exploracionDolor` (opcional) | Diagnóstico | Desenlace por participante y conjunto elegible. No afirma nada |
| Se elimina `SECUENCIA` (sin uso) | Limpieza | — |

**No se elimina ni se relaja ninguna aserción.**

### `AdaptacionRegresionIT`

| Cambio | Tipo | Justificación |
|---|---|---|
| `con1374`: si el relleno elegido no es de cintura, el 1374 recibe la dosis de **su** clase según `MvpDosePolicy.dosisPara`, la política que aplica V24. Si es de cintura, conserva la del generador | Premisa | El generador nunca habría dado al 1374 la dosis de un relleno de pecho. V24 no se toca y sigue exigiendo 2x13 |
| `zona()` y `dosis1374()` | Auxiliares | — |
| **Nueva** `conBarraDosisInadmisibleSeRechaza` | Prueba negativa | El 1374 a 2x10 se rechaza por V24: no se publica, el motivo nombra al 1374, no hay llamadas a la IA, la semana medida queda intacta y no se crea ningún plan |
| `conBarraSeConserva`, `sinBarraSeSustituye`, `sinBrazosArribaSeSustituye` | — | **Aserciones idénticas**: copia exacta con el 1374, contención INC-CAT-1374 y sustitutos sin brazos arriba con dosis 4D |

## 3. Garantías que siguen protegidas

### `dolorConRegeneracionSegura`

Todas se comprueban de forma independiente del servicio:

1. La IA no publica: su intento sigue NO_FACTIBLE. Publica **solo** P-03 nivel 2, con su fila `PUBLICADO` y los planes medido y resultante correctos.
2. **Ningún ejercicio excluido por dolor reaparece**: el viernes que dolió no vuelve.
3. **No hay más trabajo**: ni en total (repeticiones y segundos, regla de P-03), ni en la dosis de ningún ejercicio común con la semana medida (V8C).
4. **Valida entera sin ninguna autorización** (`ValidacionIndependiente` = `VALIDA`, todas las dosis `BASE`, 0 fechas reducibles):
   - V24 se cumple;
   - no hay progresión, recuperación, reincorporación ni herencia;
   - V20 se cumple **sin la exención de D8**, y el viernes regenerado tiene al menos el piso de 31 min.
5. Sin violaciones de linaje (`linaje_v31_v32.sql`).

### Las demás

- `dolorNoAprovechaD8`: la garantía original (P-03 no publica si regenerar sube el trabajo).
- Contención INC-CAT-1374 y dosis 4D de los sustitutos.
- V24 con el 1374 mal dosificado.

## 4. Las pruebas negativas fallan cuando se introduce la violación

Son mutaciones temporales, revertidas después; no hay ningún cambio de producción en el commit.

| Mutación | Prueba | Resultado |
|---|---|---|
| M1 · P-03 deja de comprobar el trabajo (`SafeRegenerationService`) | `dolorNoAprovechaD8` | **Falla**: «volver a publicar el viernes que dolió no es una solución segura» |
| M2 · V24 deja de comprobar la dosis (`PlanDraftValidator`) | `conBarraDosisInadmisibleSeRechaza` | **Falla**: «se publicó una semana con el 1374 a una dosis inadmisible» |
| M3 · reaparece un ejercicio excluido por dolor en la semana regenerada | `dolorConRegeneracionSegura` | **Falla**: «reaparece lo que dolió» `[337]` |
| M4 · una serie más en un ejercicio | `dolorConRegeneracionSegura` | **Falla**: «más trabajo que la semana medida: 476:0 → 486:0» |
| M5 · dosis no base compensada (+2 y −4 repeticiones; el total no sube) | `dolorConRegeneracionSegura` | **Falla**: `INVALIDA [V24: …]` |

## 5. Confirmaciones de la orden

1. **Las protecciones originales siguen cubiertas.** Las aserciones de las pruebas originales son idénticas. Se añaden dos casos.
2. **Las pruebas ya no dependen de una selección incidental.** La semana y los participantes son fijos, y la dosis del 1374 sale de la política, no del relleno.
3. **Las dos regresiones pasan por razones legítimas.** Pasan por separado y en su clase completa.
4. **No se ha cambiado la lógica de producto.** `git diff` sobre `src/main` está vacío.
5. **V24, V8C, D8 y P-03 conservan sus garantías** (§3 y §4).
6. **Las pruebas negativas fallan con la violación** (§4).
