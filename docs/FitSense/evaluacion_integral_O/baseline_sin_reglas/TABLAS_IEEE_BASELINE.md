# Tablas para el short paper · línea base sin reglas frente a FitSense con reglas

**Lectura de las tablas:**
- Cifras con numerador y denominador.
- 19 perfiles originales factibles; O11 se informa aparte y O11-F35 no entra en ningún denominador.
- «Infracción» = infracción única: los hallazgos de varias reglas sobre la misma causa y el mismo objeto cuentan una vez.
- **Etapa comparable** = la primera propuesta de cada condición, evaluada tras el normalizador mecánico del backend, que es como GRA validaba.
- Los planes **publicados** por FitSense se muestran aparte.

## Tabla A. Generación inicial

| Métrica | FitSense con reglas (GRA-R1) | Replicate sin reglas (BASE-R1) | FitSense con reglas (GRA-R2) | Replicate sin reglas (BASE-R2) |
|---|---|---|---|---|
| Perfiles originales evaluados | 20 (19 factibles + O11) | 20 (19 factibles + O11) | 20 (19 factibles + O11) | 20 (19 factibles + O11) |
| Propuestas de generación (todas) | 29 (1.ª + reintentos; O11 sin llamada) | 20 (una por perfil, O11 incluido) | 25 (1.ª + reintentos; O11 sin llamada) | 20 (una por perfil, O11 incluido) |
| Planes con ≥ 1 violación · 1.ª propuesta | 7/19 (36.8 %) | 19/19 (100.0 %) | 6/19 (31.6 %) | 19/19 (100.0 %) |
| Planes sin violaciones · 1.ª propuesta | 12/19 (63.2 %) | 0/19 (0.0 %) | 13/19 (68.4 %) | 0/19 (0.0 %) |
| No evaluables | 0 | 0 | 0 | 0 |
| Total de infracciones · 1.ª propuesta | 22 | 637 | 19 | 659 |
| — Dosis (V24, V8) | 0 | 389 | 0 | 394 |
| — Series y repeticiones (V7, V9, V10, V16, V19) | 0 | 54 | 0 | 53 |
| — Volumen por grupo muscular (V26) | 10 | 41 | 11 | 46 |
| — Patrones obligatorios (V23, V25, V27) | 2 | 57 | 0 | 60 |
| — Duración (V4, V17, V20) | 3 | 2 | 0 | 1 |
| — Otras | 7 | 94 | 8 | 105 |
| Total de infracciones sin normalizador (CRUDO) | 26 | 693 | 19 | 720 |
| Planes publicados con violaciones | 0/19 (los rechazos se corrigieron en el reintento) | no aplica: no se publica | 0/19 (los rechazos se corrigieron en el reintento) | no aplica: no se publica |
| O11 (30 min, inviable para FitSense) | sin llamada; limitación informada | plan generado con 19 infracciones | sin llamada; limitación informada | plan generado con 17 infracciones |

**«Otras» por condición:**
- GRA-R1: distribucion de ejercicios 7.
- BASE-R1: cantidad de ejercicios por sesion 7, carga 48, distribucion de ejercicios 25, distribucion de sesiones 11, frecuencia semanal 3.
- GRA-R2: distribucion de ejercicios 7, distribucion de sesiones 1.
- BASE-R2: cantidad de ejercicios por sesion 5, carga 61, descansos 1, distribucion de ejercicios 22, distribucion de sesiones 13, frecuencia semanal 3.

**Comparación pareada por perfil (1.ª propuesta, 19 perfiles; prueba exacta de McNemar, bilateral):**
- Corrida 1: 12 con violaciones solo sin reglas, 0 solo con reglas, 7 en ambas y 0 en ninguna; p = 0.00049.
- Corrida 2: 13 con violaciones solo sin reglas, 0 solo con reglas, 6 en ambas y 0 en ninguna; p = 0.00024.

## Tabla B. Adaptación

| Métrica | FitSense con reglas (GRA-R1) | Replicate sin reglas (BASE-R1) | FitSense con reglas (GRA-R2) | Replicate sin reglas (BASE-R2) |
|---|---|---|---|---|
| Ciclos evaluados | 10 | 10 | 10 | 10 |
| Propuestas de adaptación evaluadas | 10 (no evaluables: 0) | 10 (no evaluables: 0) | 10 (no evaluables: 0) | 10 (no evaluables: 0) |
| Adaptaciones con cambios no autorizados | 0/10 (0.0 %) | 10/10 (100.0 %) | 0/10 (0.0 %) | 10/10 (100.0 %) |
| Adaptaciones sin cambios no autorizados | 10/10 (100.0 %) | 0/10 (0.0 %) | 10/10 (100.0 %) | 0/10 (0.0 %) |
| Total de cambios no autorizados | 0 | 59 | 0 | 48 |
| — Cambios indebidos de dosis o volumen (C2, C3) | 0 | 20 | 0 | 11 |
| — Cambios indebidos de dificultad (C4) | 0 | 1 | 0 | 3 |
| — Sesiones modificadas indebidamente (C1, C12) | 0 | 22 | 0 | 22 |
| — Otros (C5–C11) | 0 | 16 | 0 | 12 |
| SIN_EFECTO (conservación de la sesión señalada; no se cuenta como cambio) | 0 | 0 | 1 | 0 |

**«Otros» por condición:**
- BASE-R1: C10 9, C11 1, C6 3, C8 3.
- BASE-R2: C10 9, C6 2, C8 1.

**Códigos de cambio:**
- C1: sesión no señalada modificada.
- C2: más trabajo en la sesión.
- C3: subida de dosis de un ejercicio.
- C4: más dificultad.
- C5: restricción, equipamiento o bloqueo.
- C6: ejercicio excluido por dolor que sigue en el plan.
- C7: fuera del catálogo filtrado.
- C8: duración excedida.
- C9: patrón no autorizado.
- C10: cambio que la intervención no permite.
- C11: ante dolor, más añadidos que retirados.
- C12: sesión añadida, eliminada o fuera de fecha.
