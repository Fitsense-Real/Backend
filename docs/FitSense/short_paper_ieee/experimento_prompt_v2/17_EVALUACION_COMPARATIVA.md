# Evaluación comparativa C1 frente a C2

**Fuentes:** `08`–`13` (generados por `evidencia/analisis.py` a partir de la evidencia en bruto) y `evidencia/analisis_resumen.json`.

**Código:** las 4 corridas evaluaron el mismo código. No hay ningún cambio en `src/` ni en `pom.xml` entre el protocolo congelado (`259bd94`) y la última corrida.

## 1. Métrica primaria: adaptaciones aplicadas / 8

| | r1 | r2 | Media |
|---|---:|---:|---:|
| **C1** (vigente) | 6 | 5 | **5,5** |
| **C2** (factibilidad + prompt V2) | 7 | 7 | **7,0** |
| Máximo teórico (`03`) | 7 | 7 | 7 |

**Referencia histórica:** la etapa C de PR #28, con el mismo C1 en otro día, aplicó **4 de 8**.

## 2. Métricas secundarias

| Métrica | C1 r1 | C1 r2 | C2 r1 | C2 r2 |
|---|---:|---:|---:|---:|
| Sesiones con cambio efectivo / 10 factibles | 7 | 5 | **10** | **10** |
| Propuestas rechazadas | 7 | 8 | **0** | **0** |
| Rechazos por V20 | 6 | 5 | 0 | 0 |
| Rechazos por V8C (sustitución de otro patrón) | 1 | 3 | 0 | 0 |
| SIN_EFECTO (de 8) | 2 | 2 | 1 | 1 |
| RESPALDO (de 8) | 0 | 1 | 0 | 0 |
| Llamadas | 15 | 15 | 8 | 8 |
| Casos resueltos en 1 llamada | 3 | 4 | **8** | **8** |
| Sesiones F3 (U03 S15 y U04 S21) omitidas directamente | 2 de 2 | 2 de 2 | 2 de 2 | 2 de 2 |
| Coste (US$) | 0,222 | 0,231 | 0,121 | 0,119 |
| Tiempo de proveedor (s) | 409 | 481 | 209 | 213 |

## 3. Seguridad (deben ser 0)

| Comprobación | C1 r1 | C1 r2 | C2 r1 | C2 r2 | Cómo se comprobó |
|---|---:|---:|---:|---:|---|
| Planes inválidos publicados | 0 | 0 | 0 | 0 | Todo plan publicado pasó V8C + V1–V25. Además, recálculo independiente de V20 y de la dosis sobre `plan_siguiente` |
| Sesión publicada por debajo del mínimo V20 | 0 | 0 | 0 | 0 | Réplica del estimador sobre cada sesión publicada |
| Cambios en sesiones no señaladas | 0 | 0 | 0 | 0 | `plan_siguiente`: toda sesión cambiada estaba señalada |
| Dosis no autorizada (cualquier subida, o cambio de descanso) | 0 | 0 | 0 | 0 | Comparación de cada ejercicio conservado: 62 cambios de dosis, todos a la baja y con el mismo descanso |
| Ejercicios excluidos o fuera del catálogo elegible | 0 | 0 | 0 | 0 | Todo ejercicio añadido está en `available_exercises` del prompt, que ya excluye lo prohibido |
| Reincorporaciones no permitidas | 0 | 0 | 0 | 0 | No aplica: ningún perfil tiene dolor ni exclusiones, y no se añadió ningún ejercicio retirado antes |
| Violaciones de linaje V31/V32 | 0 | 0 | 0 | 0 | Consulta `linaje_v31_v32.sql` (`resultados_20.csv`) |
| Controles (12) con llamadas o cambios | 0 | 0 | 0 | 0 | 0 llamadas y el mismo resultado que en la etapa C |

**Los validadores siguieron rechazando lo inválido.** En C1 rechazaron 15 propuestas:
- 11 por V20, que retiraban de más;
- 4 por V8C, que sustituían por un cardio de otro patrón.

**Ninguna de las dos condiciones publicó nada inválido.**

## 4. Análisis por caso

| Caso | Clase (`03`) | C1 | C2 | Lectura |
|---|---|---|---|---|
| U03 | F3 | Omite en las 2 | Omite en las 2 | Sin solución posible. **C1 también omitió aquí**, a diferencia de la etapa C, donde devolvió la sesión idéntica |
| U04 | S20 VARIANTE, S21 F3 | S2 sustituida en las 2 | Ídem | Igual en las dos condiciones: 7 sustituciones del mismo patrón con la misma dosis |
| U08 | F1 | Aplicada (r1 tras 1 rechazo V20) | Aplicada en 1 intento | C2: 3×10 → 3×9 (r1) o 3×8 (r2) en 323 y 487 (18–36 s) |
| U09 | F1, margen 36 | Solo una de las dos sesiones, tras 2 rechazos V20 | **Las dos**, en 1 intento | C2 combina sustituciones del mismo patrón con 3×10 → 3×9 |
| U13 | margen 162 | Aplicada | Aplicada | Igual |
| U14 | margen 162 | Solo S2, tras 1 rechazo V20 y 1 V8C | **S2 y S3**, en 1 intento | C2 recorta 10 s de cardio. En C1 r1 y r2 se intentó un cardio por un ejercicio de fuerza (V8C) |
| U18 | **F1** | **SIN_EFECTO en las 2** (V20 y luego omite) | **Aplicada en las 2** (una serie menos de cardio, exactamente 150 s = el margen) | Es el caso que mejor refleja el diagnóstico (`02`): C1 vuelve a fallar como en la etapa C |
| U19 | F1 | r1: aplicada tras V20. r2: **RESPALDO** (V20 y 2 × V8C) | Aplicada en las 2 | C2: una serie menos de cardio, o reducciones repartidas |

## 5. Calidad técnica de las modificaciones

Fuente: `13_CAMBIOS_Y_RESTRICCIONES.csv`.

| Criterio | C1 (2 corridas) | C2 (2 corridas) |
|---|---|---|
| Solo sesiones señaladas | Sí | Sí |
| Sustituciones con el mismo patrón | 23 de 23 | 21 de 21 |
| Mismo músculo objetivo | 21 de 23 | 17 de 21 |
| Dificultad ≤ que la sustituida | 23 de 23 | 21 de 21 |
| Sustitución con la misma dosis | 23 de 23 | 20 de 21 (1117 → 253: 3×90 s → 3×80 s, reducción permitida) |
| Equipamiento | Dentro del perfil (catálogo filtrado) | Ídem |
| Cambios de dosis | 20, todos a la baja | 42, todos a la baja |
| Descanso | Sin cambios | Sin cambios |
| Duración publicada | ≥ mínimo V20; 2 sesiones exactamente en el mínimo (U08, 2520 s) | ≥ mínimo V20; 5 sesiones exactamente en el mínimo (U18 y U19 a 1890 s; U08 r2 a 2520 s) |
| Linaje | 0 violaciones | 0 violaciones |

**Observaciones** (ninguna incumple una regla; no se cambia el catálogo ni las reglas):

1. **Cambios de músculo dentro del mismo patrón.** Hay 6, en las dos condiciones:
   - 580 → 313 o 129 (pectorales → tríceps);
   - 775 → 313 (dorsal alto → tríceps);
   - 780 → 119 (deltoides → dorsal alto);
   - 933 → 318 (pectorales → tríceps).

   V8C exige el mismo patrón, no el mismo músculo. Es coherente con la observación H10b de PR #28 y la propuesta P4, pendiente de tu decisión.
2. **La magnitud de C2 es pequeña:**
   - en GM y FU, 1–2 repeticiones menos en 2 ejercicios (18–36 s);
   - en PP y AG, una serie de cardio o 10 s por serie (60–150 s).

   Es lo que permiten los márgenes (`03`). **No se valora su efecto clínico ni conductual.**
3. **C2 lleva más sesiones exactamente al mínimo:** 5 de 20 sesiones cambiadas, frente a 2 de 12 en C1. Es válido por la regla (igualdad admitida), pero deja la sesión sin margen para la semana siguiente. Es un dato para revisar con la política de duración, que no se toca aquí.

## 6. Clasificación (regla congelada en `07` §8)

- a = (6, 5), media(a) = 5,5;
- b = (7, 7), media(b) = 7,0;
- 0 violaciones de seguridad;
- min(b) = 7 > max(a) = 6;
- media(b) − media(a) = **1,5**.

| Condición | ¿Se cumple? |
|---|---|
| Desfavorable (violación o media(b) < media(a)) | No |
| Favorable (min(b) > max(a) y diferencia ≥ 2) | **No:** la diferencia es 1,5 < 2 |
| **Parcial** (diferencia ≥ 1) | **Sí** |

**Resultado: PARCIAL.**

**Nota sobre el techo:** el máximo posible es 7 de 8, porque U03 es F3. Con C1 en 5 y 6, una diferencia ≥ 2 exigía que C1 tuviera de media 5 o menos. **C2 alcanzó el techo en las dos corridas** (7 de 8 y 10 de 10 sesiones factibles), y la diferencia no llegó al umbral Favorable porque C1 rindió por encima de la etapa C (4). El umbral se fijó antes de ver los datos y **no se reinterpreta**.

**La evidencia es exploratoria:**
- 8 casos × 2 corridas por condición;
- un modelo y una semana;
- intervención combinada (cálculo + texto).
