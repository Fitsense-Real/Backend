# Corrección del CI

Fecha: 16/11/2026. No se ha hecho ningún merge ni ningún despliegue.

## 1. Qué fallaba

Desde el 16/11/2026, `ReparacionLinajeIT.repararYHeredar` y `ReparacionLinajeRev2IT.repararYHeredar` fallaban:

- en `generation`, en #71 y en #72;
- en local y en GitHub Actions.

El resultado era NO_VALIDA donde se esperaba REPARADA. Causa: la prueba calculaba su semana con `LocalDate.now()`. La
semilla del motor de reglas depende de la fecha, así que la semana elegía un relleno cuya dosis (2x10) no admite el
sustituto seguro (2x13), por V24 y V8C. Es el **caso A**: la prueba dependía de la fecha; la lógica es correcta
(`DIAGNOSTICO_REPARACION_LINAJE.md`).

## 2. Qué se ha cambiado

Solo `src/test/java/.../orchestation/ReparacionLinajeIT.java`. `ReparacionLinajeRev2IT` lo hereda.

| Cambio | Por qué |
|---|---|
| S1 = `RelojPruebas.SEMANA_REFERENCIA` (02/11/2026) | Es la convención del proyecto para las semanas fijas (32 clases). La semana ya no depende del día. |
| La conversión al 1374 elige un relleno de la **misma capacidad** que el sustituto (2x13/90) | La premisa de P-P2 (que exista un sustituto seguro con la misma dosis) pasa a ser explícita. Antes dependía de la semilla. |
| `RelojPruebas.abrirVentanaDeEjecucion` antes de ejecutar cada semana | Con una semana fija en el pasado hay que abrir la ventana de ejecución, como hacen las demás pruebas con semana fija |
| Se quita el apagado de la contención del catálogo | Era una premisa equivocada: el sustituto es el 104 con y sin contención |
| Nueva `repararYHeredarEnCambiosDeMesYAno` | El mismo escenario cruzando a diciembre y a 2027 |
| Nueva `sinSustitutoConLaMismaDosisNoPublicaNada` | El caso NO_VALIDA, que antes solo aparecía cuando la prueba fallaba. No publica nada y conserva la semana progresada. |

**No se ha cambiado ninguna aserción para que el CI quede verde.** Las aserciones de P-P2 y H6 siguen igual. Las
nuevas añaden casos. No se ha tocado:

- el código de producción;
- los validadores (V24, V8C);
- SUST-SEG-1;
- el catálogo;
- la fórmula de adherencia;
- las políticas de progresión y reincorporación.

## 3. Resultados

| Ejecución | Unitarias | Integración | Fallos | Omitidas |
|---|---|---|---|---|
| **`generation` + #73**, local (`mvnw verify -Pintegration`) | 1540 | 759 | **0** | 1 / 57 |
| **PR #73**, GitHub Actions `verify` (16/11, 04:01–04:23 UTC) | — | — | **0** | — |
| **#72 (`8658089`) + la corrección de #73**, local, rama temporal | 1576 | 809 | **0** | 1 / 72 |
| `ReparacionLinajeIT` con la semana de referencia en 23/11/2026, 04/01/2027 y 07/06/2027 | — | 3 + 3 + 3 | **0** | 0 |

Detalle de la combinación #72 + #73 (26 min 26 s):

- `ReparacionLinajeIT` 3/3 y `ReparacionLinajeRev2IT` 3/3.
- `GeneracionAsincronaResilienciaIT` 23/23, con los 20 escenarios de resiliencia.
- `ReanudacionConReplicateSimuladoIT` 11/11 y `MigracionV58aV59IT` 1/1.
- `GeneracionAsincronaIT` (15) se omite, como siempre en CI: es opt-in y necesita `fitsense.pruebas.url`, una
  instancia real. Por eso hay 72 omitidas en vez de 57.

**Comparación con `generation`.** #72 añade 36 unitarias y 50 de integración. Ninguna prueba de `generation` cambia
de resultado.

## 4. Qué falta para que #71 y #72 tengan el CI verde en GitHub

#71 y #72 **no se han modificado**: es una restricción de la orden y la corrección pertenece a otra funcionalidad.
Su CI seguirá rojo hasta que lleven la corrección. El camino es:

1. Fusionar #73 en `generation`.
2. Actualizar #71 con `generation` (merge, sin reescribir la historia) y volver a pasar el CI.
3. Lo mismo con #72.

La ejecución local de #72 + #73 de la tabla anterior es la prueba de que, hecho eso, el CI queda en verde.
