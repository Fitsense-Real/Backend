# HALL-GM-01 en la versión candidata: 40 escenarios, protecciones apagadas y encendidas

## 1. Escenario

Participantes 9701–9740 (GAIN_MUSCLE, intermedio, gimnasio, 3 días, 45 min). Cada uno recorre tres semanas:

1. **Semana 1:** fatiga el viernes, que produce el mantenimiento P-A1 (2x10 en las estructurales).
2. **Semana 2:** la copia hereda la reducción.
3. **Semana medida:** dolor el lunes, con adaptación y, si no basta, P-03 nivel 2.

La IA está simulada (sin coste) y aplica el mantenimiento o devuelve la sesión igual. Arnés: `RegeneracionPorColocacionRev2IT#exploracion` (opt-in), sobre el commit candidato.

Se comparan dos modos con el mismo código:

- **Apagadas:** `fitsense.catalogo.contencion-conservadora=false` y `fitsense.seguridad.dolor-en-planes-publicados=false`. Equivale a la PR #65.
- **Encendidas:** los valores por defecto de la candidata.

Evidencia: `evidencia/hall_gm_01/hall_gm_01_40_protecciones_{apagadas,encendidas}.csv`. Las dos ejecuciones se repitieron sobre el commit definitivo `836a1f5` con un resultado idéntico, columna por columna.

## 2. Resultados

| | Apagadas | Encendidas |
|---|---|---|
| Semana con dolor publicada | 36 | 35 |
| NO_FACTIBLE | 4 (9703, 9716, 9719, 9721) | 5 (9717, 9728, 9733, 9734, 9740) |
| Colocaciones que suben en lo publicado | 0 en las 36 | 0 en las 35 |
| Ejercicios que dolieron y reaparecen | 0 | 0 |
| Participantes con trabajo registrado en lo que dolió, tras el dolor (semana medida) | **7** (9703, 9714, 9716, 9719, 9721, 9725, 9730; 9 registros) | **0** |
| Igual a la evidencia corregida de #65 (`evidencia/hall_gm_01/rev2_con_correccion.csv`) | **Sí, 40/40 idénticos** | — |

En los 5 NO_FACTIBLE con las protecciones encendidas:

- la adaptación la rechaza V24, porque el sustituto no puede recibir más trabajo que el quitado;
- P-03 nivel 2 tampoco publica («sube el trabajo»);
- no se publica nada, la semana medida queda intacta y la exclusión por dolor sigue activa.

`#halGm01ConContencion` (9717) lo fija como prueba permanente.

## 3. Por qué cambian los desenlaces

**El cambio entre 4 y 5 NO_FACTIBLE no es un cambio de política.** Viene del conjunto elegible: la contención quita candidatos, el plan inicial de cada participante es distinto, y con él cambian el mantenimiento, el ejercicio que duele y sus posibles sustitutos.

- Con el plan de la candidata, 9703, 9716, 9719 y 9721 encuentran un sustituto seguro que no sube el trabajo y publican.
- 9717, 9728, 9733, 9734 y 9740 no lo encuentran.

En los dos modos, el desenlace cumple la garantía de HALL-GM-01: **ninguna semana publicada tiene más trabajo, total o por colocación, que la semana medida**, y no se publica ninguna semana sin alternativa segura.

**La diferencia en el trabajo registrado sí es efecto de P-DOLOR-PUB.**

- Con la protección apagada, las sesiones posteriores de la semana medida que repetían el ejercicio que dolió lo mostraban sin marca, y el arnés (como haría la persona) lo registraba con trabajo.
- Con la protección encendida, el ejercicio aparece marcado con `dolor_registrado` y solo admite «omitido» (ver `VALIDACION_DOLOR_PUBLICADO.md`).

## 4. Pruebas fijas

| Prueba | Modo | Qué fija |
|---|---|---|
| `#dolorTrasMantenimiento` (9701) | Encendidas | Publica sin restauración ni subida por colocación; trabajo tras el dolor = 0 |
| `#sustitutoQueExisteReducidoOtroDia` (9710) | Contención apagada (su premisa se construyó con el conjunto elegible de #65) | El sustituto ocupa el puesto del retirado |
| `#sustitutoDeMantenimientoSinMasTrabajo` (9703) | Contención apagada (ídem) | HALL-GM-01 original: NO_FACTIBLE por V24 y P-03 |
| `#halGm01ConContencion` (9717) | Encendidas | El mismo mecanismo con el catálogo de la versión conservadora |
| `#dolorMarcadoEnSesionesAbiertas` | Encendidas | Los 10 casos de P-DOLOR-PUB |

9703 y 9710 se ejecutan con la contención apagada porque su premisa depende del conjunto elegible anterior: qué sustituto sale y con qué dosis. El mecanismo que prueban no depende del catálogo, y su equivalente con la contención encendida es 9717.
