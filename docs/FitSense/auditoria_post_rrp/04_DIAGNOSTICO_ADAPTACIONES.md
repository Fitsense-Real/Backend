# 04 · Diagnóstico de las adaptaciones: las sesiones no factibles, las conservaciones y la calidad de las 56 adaptaciones

**Fuentes:** solo la evidencia final del PR #41, que no se modifica:
- `docs/FitSense/short_paper_ieee/k20_adaptacion_real/evidencia/final/EVALUACION_DEFINITIVA/` (`SESIONES_FINALES.csv`, `ADAPTACIONES_IA_DETALLE.csv`, `DIFERENCIAS_R_RP.csv`);
- `…/R_U_rep/factibilidad.csv`, `…/R_J/factibilidad.csv`;
- `…/tablas/user_profiles.csv`.

Las cifras se recalculan con los scripts de §5. No se ejecutó ninguna llamada a Replicate.

## 1. Las cinco sesiones no factibles

### Qué tienen en común

| Sesión | Objetivo, nivel y lugar | Intervención | Permisos (sustituir / dosis) | Candidatas probadas por la sonda | Válidas | Rechazos |
|---|---|---|---|---|---|---|
| U03 S3 | GAIN_MUSCLE intermedio, gimnasio, 3×60 | REVISAR_CARGA_Y_RECUPERACION (fatiga) | no / sí | 21 (14 con menos repeticiones, 7 con menos series) | 0 | V20 = 7, V24 = 14 |
| U04 S3 | GAIN_MUSCLE intermedio, gimnasio, 3×60 | INVESTIGAR_CAMBIO_MINIMO (desmotivación) | no / sí | 21 | 0 | V20 = 7, V24 = 14 |
| J01 S4 | GAIN_MUSCLE intermedio, casa, 4×47 | REVISAR_CARGA_Y_RECUPERACION | no / sí | 18 | 0 | V24 = 11, V20 = 4, V9 = 2, V16 = 1 |
| J09 S4 | GAIN_MUSCLE principiante, gimnasio, 4×70 | INVESTIGAR_CAMBIO_MINIMO | no / sí | 27 | 0 | V24 = 15, V20 = 6, V9 = 3, V16 = 3 |
| J13 S3 | GAIN_MUSCLE intermedio, casa, 3×36 | INVESTIGAR_CAMBIO_MINIMO | no / sí | 15 | 0 | V24 = 13, V9 = 2 |

Las cinco son de **GAIN_MUSCLE** y piden una intervención que **solo permite cambiar la dosis** (`TipoIntervencion`: `permiteSustituir = false` y `permiteCambiarDosis = true`). Pero en GAIN_MUSCLE la dosis la fija 4D (3×10/90 s, o 2×10 en relleno). V24 rechaza cualquier dosis distinta, y la validación estricta de la adaptación mantiene 4D en ENFORCE. Además, V20 sigue exigiendo el 70 % en la adaptación, y solo ACORTAR queda exento (D8). Por tanto, **el conjunto de cambios permitidos es vacío por construcción**: ningún modelo, por bueno que sea, podría adaptar esas sesiones.

### Contraste con las sesiones del mismo objetivo que sí permiten sustituir

- En J13 S2, VARIANTE_MAS_SENCILLA_O_CARGA, la sonda encontró **82/82 sustituciones laterales válidas** (mismo patrón y misma dificultad) y 9/9 «más fáciles».
- En U04 S2, encontró 150/150 laterales y 109/109 más fáciles.

El catálogo sí ofrece alternativas. Lo que lo impide es el permiso de la intervención, no la falta de ejercicios.

### Clasificación (las cinco)

| Sesión | Clasificación | Motivo |
|---|---|---|
| U03 S3, J01 S4 (fatiga) | **Conflicto generación-adaptación + decisión profesional** | Ante la fatiga, la única acción permitida es reducir la dosis, y 4D y V24 lo prohíben en GM. La evidencia revisada (documento 02, B: Bickel 2011, Spiering 2021, Bell 2023) **no respalda** prohibir una reducción temporal. Corregirlo exige cambiar el contrato de V24 (una «dosis de mantenimiento» autorizada por V8C) y la exención de V20. **No se aplica sin aprobación** (propuesta P-A1 del documento 06). |
| U04 S3, J09 S4, J13 S3 (desmotivación) | **Restricción excesiva + conflicto generación-adaptación** | INVESTIGAR_CAMBIO_MINIMO prohíbe sustituir. En GM, «un cambio pequeño de dosis» es, por definición, una dosis distinta de la de 4D, así que tampoco se puede. Una sola sustitución lateral del mismo patrón sería un cambio mínimo factible: la sonda de J13 S2 lo demuestra con 82/82. Exige cambiar la matriz de permisos de `TipoIntervencion`, que es una política aprobada (propuesta P-A2). |

**Ninguna** de las cinco es una imposibilidad justificada por el perfil o por el catálogo. **Ninguna** es un fallo del modelo de IA: la sonda demuestra que no había ninguna respuesta válida que pudiera dar.

## 2. Las conservaciones U07 S3 y U12 S3

| Sesión | Objetivo | Intervención | Resultado |
|---|---|---|---|
| U07 S3 | INCREASE_STRENGTH intermedio, gimnasio | CAUSA_NO_REGISTRADA | Se conserva sin llamar a la IA (`SIN_EFECTO_SIN_IA`) |
| U12 S3 | LOSE_WEIGHT principiante, casa | CAUSA_NO_REGISTRADA | Igual |

- **Clasificación:** heurística de planificación justificada.
- Sin causa registrada, cualquier cambio sería arbitrario. La decisión D1/D6 de conservar es coherente con el principio de cambio mínimo y no esconde ningún fallo.
- **Riesgo:** una sesión con cumplimiento bajo sin causa se repite igual. Una pregunta de seguimiento al usuario («¿qué te impidió…?») sería una mejora de producto, no una corrección del motor.

## 3. Calidad de las 56 adaptaciones efectivas (R + R′, U y J)

### Medidas (todas de origen IA y categoría ACC)

| Intervención | Sesiones | Sustituciones por sesión | Reducciones de dosis por sesión | Reducción del trabajo estimado |
|---|---|---|---|---|
| REVISAR_CARGA_Y_RECUPERACION (fatiga) | 10 | 0 | 1–6 (moda 2) | 4,5 % – 21,4 %, mediana 8,2 % |
| VARIANTE_MAS_SENCILLA_O_CARGA (difícil) | 28 | 0–3 | 0–6 | 0,0 % – 13,2 %, mediana 3,0 % |
| INVESTIGAR_CAMBIO_MINIMO (desmotivación) | 18 | 0 | siempre 1 | 0,6 % – 5,9 %, mediana 2,5 % |

### Lectura técnica (lo que el sistema comprueba)

- Las 56 cumplen los validadores, el oráculo v2, V8C y el límite de dificultad: **0 incumplimientos de seguridad**.
- Ante la desmotivación, siempre hay 1 cambio. Ninguna sesión supera los 2 cambios, así que B-4 se cumple.

### Lectura deportiva (pertinencia; NO validada por un profesional)

- **Fatiga.** La reducción mediana es del 8 %. Es pequeña frente a un *deload* típico (reducir series, repeticiones o ambas; Bell 2023, consenso Delphi). La causa es estructural: V20 mantiene el piso del 70 % también en la adaptación, y la IA no puede bajar de ahí. En dos casos, la reducción recayó en estiramientos de movilidad, que poco alivian la fatiga. **Pertinencia técnica sí; pertinencia deportiva dudosa** cuando la reducción es mínima.
- **Demasiado difícil.**
  - Las sustituciones van a variantes del mismo patrón con dificultad menor o igual (el límite de dificultad estaba activo en K20, en modo `preferencia`).
  - En varias sesiones cambian 5–8 ejercicios a la vez (sustituciones más reducciones). Es coherente con «la sesión entera era difícil», pero se aleja del cambio mínimo.
  - **El catálogo no codifica el impacto.** Caso J07 S3 en R′: la IA cambió «Ciclo cross trainer» (532, dificultad 2, sin impacto) por «Saltos con las piernas separadas» (53, dificultad 1, **de impacto**). Por la escala de dificultad del catálogo es «más fácil», pero para una persona a quien la sesión le resultó difícil no es claramente más sencillo. Es una limitación del catálogo: la dificultad no recoge el impacto ni la carga articular. La sustitución es **técnicamente válida y deportivamente discutible**.
- **Desmotivación.** Casi siempre se quita 1 repetición o 1 serie a un ejercicio, con una reducción mediana del 2,5 %. Cumple «cambio mínimo», pero **es improbable que sirva contra la desmotivación**, porque el contenido no cambia. La matriz de permisos no deja hacer lo único que cambiaría la experiencia: variar un ejercicio. Ver P-A2.

### Etiqueta del catálogo «Pase de banda» (ID 125)

- El ejercicio original es *band pull through*: glúteo e isquios, patrón HIP_HINGE. **La clasificación funcional es correcta.**
- La traducción «Pase de banda» es **ambigua**: sugiere un ejercicio de hombro o pecho.
- Se propone la etiqueta «Tirón de banda entre las piernas (bisagra de cadera)», que es un cambio de texto del catálogo (P-C3). No afecta a la seguridad ni a la validación.

### Las 13 diferencias entre R y R′

| Medida | Resultado |
|---|---|
| Pares de sesiones comparables | 35 |
| Misma categoría en R y R′ | 35 de 35 (todas ACC) |
| Mismo cambio exacto en R y R′ | 22 de 35 |
| Cambio distinto en R y R′ | 13 de 35: 9 por dificultad, 2 por fatiga y 2 por desmotivación |

En las 13, el modelo eligió otra combinación de sustituciones o reducciones, igualmente válida. La variabilidad del modelo no afecta a la seguridad ni a la categoría, pero **sí a la experiencia**: dos ejecuciones iguales no producen el mismo plan. J07 S3 es el caso más visible: R solo reduce una serie, y R′ además sustituye un cardio y acorta otro.

## 4. Pertinencia técnica frente a pertinencia profesional

| Dimensión | Estado |
|---|---|
| Cumple las reglas, la seguridad y el cambio mínimo | **COMPROBADO** (56/56) |
| Reduce de verdad la carga ante la fatiga | **PENDIENTE DE VALIDACIÓN PROFESIONAL**: la reducción es pequeña por V20 |
| Una alternativa «más sencilla» lo es para la persona | **PENDIENTE DE VALIDACIÓN PROFESIONAL**: el catálogo ignora el impacto (J07) |
| La intervención por desmotivación es útil | **NO DEMOSTRADO**: −1 repetición casi siempre |
| GAIN_MUSCLE adaptable ante fatiga o desmotivación | **NO CUMPLE** por política (§1). Requiere decisión |

## 5. Reproducibilidad

Desde `docs/FitSense/short_paper_ieee/k20_adaptacion_real/evidencia/final/EVALUACION_DEFINITIVA/`:

```
python3 -I - SESIONES_FINALES.csv   # agrupa las filas de origen IA por intervención: reducción del trabajo (1 − después/antes) y número de sustituciones y reducciones
python3 -I - DIFERENCIAS_R_RP.csv   # cuenta igual_categoria e igual_cambio
grep -E "^(U03|U04|J01|J09|J13)/" ../*/factibilidad.csv   # salida de la sonda
```

Las órdenes exactas usadas están en `evidencia/adaptaciones/ordenes.txt`.
