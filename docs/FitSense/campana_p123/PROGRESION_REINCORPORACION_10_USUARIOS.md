# Progresión y reincorporación (10 usuarios)

**Condiciones:**
- Progresión encendida **solo** en la base aislada de la campaña (`fitsense.adaptation.progression.enabled=true` en el contexto de prueba). Fuera de las pruebas sigue apagada.
- Sin respuestas de modelo: la progresión es del backend.
- Ningún paso se insertó por SQL.

**Registro completo de cada paso** en `evidencia/PASOS.csv`:
- usuario, semana, sesión y ejercicio;
- dosis antes y después;
- incremento y volumen antes y después;
- volumen base y techo;
- estado de la reserva;
- autorización (informe V32/V35) y validación independiente.

## 1. Las 100 semanas principales

| | |
|---|---|
| Pasos de progresión | **48** (más 1 vuelta atrás: 49 ciclos con paso) |
| Vueltas atrás por fatiga | **1** (N02, S3 → S4) |
| Reincorporaciones | 0 (ninguna retirada elegible; ver §3) |
| Incremento máximo de un paso | 5,00 % del volumen de la sesión (límite: ≤ 5 %) |
| Volumen máximo alcanzado | 1,10 × base, en el techo y nunca por encima |
| Pasos por ciclo | Como máximo uno por usuario |

### Por usuario

| Usuario | Pasos | Observación |
|---|---:|---|
| N01 | 8 | Uno por ciclo desde S3, rotando entre sus dos sesiones |
| N09 | 8 | Igual; 2 más en la extensión (S12 y S14) |
| N06 | 7 | Sin paso en los ciclos de S4 (dolor) y S5 (la sesión sustituida reinicia su racha de dos semanas) |
| N08 | 7 | Tras el dolor de S2, retoma en S4 |
| N02 | 5 + 1 vuelta atrás | El paso de S2 vuelve atrás en S3 por fatiga; retoma en el ciclo de S7 |
| N04 | 5 | Tras las dos omisiones de S2, retoma en S4 |
| N05 | 4 | Una sesión semanal: paso cada dos semanas cumplidas |
| N07 | 4 | La semana sin registros (S3) y la parcial (S4) cortan la racha |
| N03 | 0 | El menor paso posible supera el 5 %: +1 repetición sobre 2×8 son +2 de 25–34 de volumen (5,9–8 %). Limitación ya documentada (`PendientesProgresionIT` 1(a)); no se fuerza |
| N10 | 0 | Sesiones solo de duración (2 ejercicios de 3×120 s): la duración no progresa por segundos y una serie más supera ampliamente el 5 % |

### Fatiga: la vuelta atrás de N02

- En S2, N02 progresó el ejercicio 318 (body-up) de 3×10 a 3×11.
- En S3 declaró fatiga en esa misma sesión.
- El ciclo publicó una **VUELTA_ATRAS** a 3×10: volumen 213 → 210, igual a la base. No hubo un paso nuevo ese ciclo.

### Comprobaciones

- **Prioridad de la adaptación:** ninguna semana con orden de adaptar tuvo paso de progresión.
- **Dolor:**
  - ningún paso tocó un ejercicio excluido (N06: 129; N08: 791 hasta su reactivación);
  - `ProgresionIT.p02ProgresionSinElExcluido` lo prueba aparte, durante seis semanas.
- **Dosis según el objetivo:** pasos de +1 repetición dentro de D-P3 (hasta +2 sobre la exacta) y descanso intacto.
- **Validación independiente:** las 49 semanas con paso son VALIDA_CON_AUTORIZACIONES. El paso, o su herencia, consta en V32 y cada dosis es reconstruible desde V31/V32 en crudo.

## 2. Extensión focalizada de N01 y N09 (S11–S16, fuera de las 100 semanas)

| Usuario | Semanas | Pasos | Retirada | Reincorporación |
|---|---|---:|---|---|
| N01 | S11–S16 | 0 (sus dos sesiones están en el techo o a menos de un paso: 120/121 y 132/132) | Ninguna | No aplica |
| N09 | S11–S16 | 2 | Ninguna | No aplica |

**Condición que lo impide.** En S1, la sesión parcial (la primera, fijada en el guion) **no tenía ningún relleno**: los cuatro ejercicios son de slot 4C. ACORTAR no pudo retirar nada y quedó SIN_EFECTO, como manda «no forzar la retirada». Sin retirada no hay nada que reincorporar. Llegar a S16 no cambia eso.

## 3. Variante declarada a posteriori (N01B y N09B, fuera de la cohorte y de las 100 semanas)

**Por qué existe:** para no dejar sin probar el recorrido retirada → reincorporación, se añadieron después de ver el resultado dos usuarios con el **mismo perfil** que N01 y N09. La sesión parcial de S1 se pone en la **segunda** sesión.

**Qué no es:**
- No sustituye a N01/N09.
- No se cuenta en ningún total.
- Las retiradas las propone la IA SIMULADA, no Replicate.

| | N01B | N09B |
|---|---|---|
| Composición | Distinta de N01: la semilla del motor depende del `user_id`. Sin relleno en la sesión parcial | Con relleno en la sesión parcial |
| S1 | ACORTAR → SIN_EFECTO (sin relleno) | **ACORTAR APLICADA**: retira 416 (cable seated rear lateral raise, 2×10, volumen 20) del domingo |
| Progresión | 8 pasos (S3–S10) | 7 pasos, alternando sesiones. La del domingo respeta la reserva: volumen ≤ techo − vRet = 121 − 20 = 101 (estado PENDIENTE) |
| Reincorporación | — | **Publicada para S11** (ciclo de S10): 416 vuelve al final del domingo con su **dosis base 2×10**. Volumen 99 → 119 ≤ techo 121; sin paso simultáneo |
| Ejecución posterior | — | **Entrenada en S11** (sesión registrada y completada con el reincorporado) |

**Condiciones verificadas en N09B:**
- retirada legítima por ACORTAR, con linaje V31/V32;
- el ejercicio estuvo realmente ausente de S2 a S10;
- progresión posterior exigida (7 pasos) y adherencia del 100 %;
- reserva de volumen y techo;
- sin M (sesión parcial sin M declarada);
- equipamiento y ninguna exclusión por dolor;
- dosis inicial exacta;
- sin duplicados;
- ejecución real mediante los comandos de ejecución.

**La reincorporación no fue automática por tiempo:** llegó cuando las reglas lo autorizaron (reserva, techo y progresión previa). La validación independiente la clasifica como REINCORPORACION.

## 4. Retiradas de toda la campaña

| Usuario | Ejercicio | Cuándo | Motivo | ¿Se reincorporó? |
|---|---|---|---|---|
| N03 | 1117 (cardio de relleno) | S3 → S4 | ACORTAR | No. N03 no tiene progresiones (§1), así que nunca cumple la condición «≥ 1 progresión después de ACORTAR» |
| N09B (variante) | 416 | S1 → S2 | ACORTAR | **Sí, en S11; entrenado en S11** |
| N06 | 129 | S4 → S5 | **Dolor** (sustitución, no retirada reincorporable) | No: excluido por P-02 |
| N08 | 791 | S2 → S3 | **Dolor** (sustitución) | Reactivado en S7, **no reincorporado**: reactivar no es reincorporar |
