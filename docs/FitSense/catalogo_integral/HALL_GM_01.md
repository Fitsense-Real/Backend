# HALL-GM-01 · El sustituto de seguridad en GAIN_MUSCLE deshacía un mantenimiento autorizado (orden de 14/11/2026, fase 1)

**Estado: corrección preparada en una rama independiente (`fix/sustituto-gm-sin-mas-trabajo`), sin fusionar.** No cambia V15, V20, V24, V8C, D8 ni P-03. Solo cambia la dosis que SUST-SEG-1 propone para el sustituto en GAIN_MUSCLE.

## 1. Cómo se encontró

La orden pedía comparar la dosis **por colocación** y no por el máximo semanal de cada ejercicio. Pasos:
1. Se escribió `ComparadorDosisPorColocacion` (pruebas, commit `9dbbb65` en #63), con una correspondencia explícita. No se asume ninguna equivalencia:

| Tipo | Regla | Con qué se compara |
|---|---|---|
| CORRESPONDIDA | Mismo día de la semana y mismo ejercicio; si se repite el mismo día, por orden de aparición | Su colocación medida |
| SUSTITUCIÓN | Mismo día y misma posición que un ejercicio retirado por dolor o seguridad, ausente de la semana publicada | El retirado |
| SIN_CORRESPONDENCIA | El ejercicio estaba en la semana medida, pero otro día | La menor dosis medida de ese ejercicio (criterio conservador) |
| NUEVA | El ejercicio no estaba en la semana medida | Nada por colocación. Su dosis la comprueba V24 y cuenta en el total |

2. Se recorrió el escenario más exigente: PLAN-REV-2.0 (la política por defecto), con un mantenimiento por fatiga autorizado (P-A1: 3x10 → 2x10 en las estructurales), una copia que lo hereda y, después, dolor.
   - Participantes deterministas 9701–9740.
   - IA simulada.
   - Sin Replicate.

## 2. El defecto

En 4 de 40 participantes (9703, 9716, 9719 y 9721) la semana con dolor **se publicaba con más trabajo que la semana medida**:

| Participante | Colocación | Semana medida | Semana publicada | Trabajo total |
|---|---|---|---|---|
| 9703 | Viernes, sustituto 118 del ejercicio que dolió | 2x10 (mantenimiento) | **3x10** | 442 → **452** |
| 9716 | Viernes, sustituto 56 | 2x10 | **3x10** | 472 → **482** |
| 9719 | Viernes, sustituto 74 | 2x10 | **3x10** | 472 → **482** |
| 9721 | Viernes, sustituto 791 | 2x10 | **3x10** | 472 → **482** |

**Causa.** En `SafetySubstitutionPolicy.dosis`, para GAIN_MUSCLE (4D), el sustituto recibía siempre la dosis **base** de su clase (`MvpDosePolicy.dosisPara`), fuera cual fuera la dosis del ejercicio que sustituía. La regla de HALL-PROG-01 («ante dolor nunca se añade trabajo») solo se aplicaba a los objetivos que se prescriben por capacidades, no a GAIN_MUSCLE.

**Por qué no lo detectaba nadie:**
- La comprobación de trabajo de P-03 nivel 1 (`AdaptedWeekProducer.masTrabajoQueLaMedida`) solo mira las sesiones con dolor, y la subida estaba en otra.
- V24 aceptaba la dosis: la base es la dosis exacta de la clase del sustituto.
- El máximo semanal tampoco cambia (2x10 → 3x10 sigue por debajo de otras colocaciones a 3x10).

**Garantías que incumplía:**
- «Ante dolor no se añade trabajo».
- «Ninguna reducción autorizada se deshace sin autorización»: el mantenimiento del viernes volvía a la base.

## 3. La corrección

En GAIN_MUSCLE, si la dosis base del sustituto tiene **más trabajo** (series × repeticiones) que la colocación que sustituye, el sustituto conserva series, repeticiones y descanso del quitado. Es el mismo criterio de tope que HALL-PROG-01.

- **No se transfiere ninguna autorización.** El sustituto es un ejercicio nuevo en esa colocación, sin el MANTENIMIENTO del quitado. Por eso V24 rechaza esa dosis, como debe, y la semana no se publica por esta vía.
- **No se inventa ninguna dosis.**
- **No se relaja V24.**
- Si la base no supera el trabajo del quitado (el caso habitual), nada cambia.

Diff: `src/main/java/.../SafetySubstitutionPolicy.java` (+20 líneas). Ninguna otra clase de producción cambia.

## 4. Resultado medido (40 participantes, mismo escenario)

| | Sin la corrección | Con la corrección |
|---|---|---|
| Publicadas sin ninguna subida por colocación | 36 | 36 (idénticas: mismo desenlace y la misma comparación) |
| Publicadas **con más trabajo** | **4** (9703, 9716, 9719, 9721) | **0** |
| NO_FACTIBLE | 0 | 4: los mismos cuatro |

Desenlace de los cuatro con la corrección, en el orden en que ocurre (registro de 9703):
1. La IA (simulada) propone la semana y V24 la rechaza tres veces: «el ejercicio 118 … lleva 2x10 … y 4D v2 exige 3x10».
2. V32 registra el intento NO_FACTIBLE.
3. P-03 nivel 2 regenera con dosis base y **tampoco puede publicar**: «sube el trabajo de la semana medida (repeticiones 442 → 522)».
4. Desenlace NO_FACTIBLE:
   - no se crea ningún plan;
   - la semana medida no se toca;
   - la exclusión por dolor sigue activa.

**Es el desenlace correcto con las reglas vigentes.** Sin autorización, no hay una semana segura que no añada trabajo. Publicarla exigiría cambiar una política: transferir al sustituto el mantenimiento del quitado. Eso es una decisión del propietario y no se ha hecho.

Evidencia:
- `evidencia/hall_gm_01/rev2_sin_correccion.csv` y `evidencia/hall_gm_01/rev2_con_correccion.csv`;
- `evidencia/hall_gm_01/registro_9703_con_correccion.txt`.

## 5. Pruebas

| Prueba | Tipo | Sin la corrección | Con la corrección |
|---|---|---|---|
| `SustitutoGmSinMasTrabajoTest.mantenimientoNoSeDeshace` | Unitaria: el quitado a base − 1 serie no da al sustituto la base completa | Falla (3x10) | Pasa |
| `SustitutoGmSinMasTrabajoTest.v24RechazaSinAutorizacion` | Unitaria: la dosis resultante no es la base; V24 solo la admitiría con una autorización del propio sustituto | Falla | Pasa |
| `SustitutoGmSinMasTrabajoTest.baseQueCabe` | Control: si la base cabe, se conserva la base | Pasa | Pasa |
| `SustitutoGmSinMasTrabajoTest.nuncaMasTrabajo` | Unitaria: 40 dosis del quitado (1–4 series × 6–15 repeticiones) | Falla (1x6 → 3x10) | Pasa |
| `RegeneracionPorColocacionRev2IT.sustitutoDeMantenimientoSinMasTrabajo` (9703) | Integración: NO_FACTIBLE, sin plan nuevo, V24 en el rechazo, P-03 «sube el trabajo», semana medida intacta | **Falla**: publica la semana 4 con el 118 a 3x10 | Pasa |
| `SustitutoDosisCapacidadTest` (HALL-PROG-01, 9 pruebas), incluido `gainMuscleSinCambios` | Regresión | Pasa | Pasa |
| `RegeneracionPorColocacionRev2IT.dolorTrasMantenimiento` (9701) y `sustitutoQueExisteReducidoOtroDia` (9710) | Regresión: positivos | Pasa | Pasa |

## 6. Qué no cambia

- V15, V20, V24, V8C, D8 y P-03: sin cambios.
- La política de mantenimiento (P-A1), la progresión y la reincorporación: sin cambios.
- Los objetivos distintos de GAIN_MUSCLE siguen con la regla de HALL-PROG-01.
- **No se ha modificado ninguna prueba existente.** Solo hay pruebas nuevas.

## 7. Riesgo residual documentado (no corregido)

P-03 nivel 2 compara el trabajo **total**, no por colocación (`SafeRegenerationService.masTrabajoQueLaMedida`):
- En principio, una semana regenerada podría devolver a su dosis base una colocación reducida, el mismo día, sin que suba el total.
- **No se ha observado:**
  - en REV2, ninguno de los 40 participantes llega a publicar por P-03 nivel 2;
  - en PLAN-HIST-1, `HerenciaD8IT.dolorConRegeneracionSegura` comprueba ahora también la dosis por colocación, y pasa.
- Endurecer P-03 para que compare por colocación cambiaría su política. No se hace sin aprobación: queda como propuesta independiente.
