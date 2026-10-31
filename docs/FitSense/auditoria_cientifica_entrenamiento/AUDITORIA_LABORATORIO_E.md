# Auditoría del laboratorio E (`investigacion_reglas_entrenamiento/MATRIZ_EXPERIMENTOS.csv`)

## 1. Inconsistencias señaladas y su causa

| Hallazgo | Comprobado | Causa |
|---|---|---|
| 22 registros de E con duración media > 115 % (13 «válidos», 9 «experimentales»); `L0029` con 2.106 s para 30 min | Sí. Las 22 filas son de E y del grupo BASE: 15 de 30 min, 5 de 45 y 2 de 60 | **Dos implementaciones distintas de V4.** El perfil usa `UserProfile.maxSessionMinutesAllowed = floor(M × 1,15)`: 30 → 34. El validador de planificación usa `PlanningProfile.maxSessionMinutes = Math.round(M × 1,15)`: 30 → **35** (34,5 redondea hacia arriba) y 45 → **52** en vez de 51. Además, V4 compara la duración **declarada**, que el estimador redondea al minuto (`Math.round(s / 60)`). Por eso 2.106 s = 35,1 min se declara como 35 y pasa. A 60 min ambos techos coinciden (69), y el exceso de las 2 filas viene solo de ese redondeo: hasta 4.169 s (69,48 min) se declaran 69. El post-proceso E de entonces llenaba hasta ese mismo techo redondeado; las demás variantes no lo alcanzaban |
| 13 «válidos» que superan el 115 % | Sí | Se clasificaron con el validador real, que efectivamente los acepta por el redondeo anterior. **No era un error de clasificación del laboratorio, sino de la regla.** En sentido estricto (segundos > M × 60 × 1,15) superan la tolerancia de P-01 |
| Promedios por sesión | Sí | `pct_minutos_prom` era una media de las sesiones. Un promedio aceptable puede esconder una sesión que excede. La matriz revisada registra el **máximo por sesión** |
| 713 experimentales en el CSV frente a 722 en el informe | Sí | **Error aritmético del informe.** Se calculó 896 − 174, restando solo las válidas de BASE, cuando las válidas de BASE + RESTRICCIONES eran 174 + 9 = 183. 896 − 183 = **713**. El CSV era correcto |
| Diferencias entre el validador real y el post-proceso | Parcial | El post-proceso B/C/E añadía ejercicios con la dosis base y validaba con el validador vigente. Las clasificaciones son coherentes con ese validador; la discrepancia de fondo es la del punto 1 |

**V4 no se ha modificado** (está prohibido en esta orden). La incoherencia `floor` frente a `round` queda como decisión pendiente en `DECISIONES_QUE_REQUIEREN_APROBACION.md`.

## 2. Correcciones del laboratorio revisado (`LaboratorioModeloEIT`)

1. **El modelo E planifica hasta M**, no hasta M × 1,15 (`ParametrosModeloE.techoTiempoFraccion = 1,0`). La tolerancia de V4 queda solo como holgura de validación.
2. **Nueva columna `v4_estricto_excedido`:** alguna sesión supera M × 60 × 1,15 s, sin redondeos.
3. **Nueva columna `pct_minutos_max_sesion`:** el máximo por sesión, además de la media.
4. **Los grupos «entrenables» se calculan según el enfoque.** Un grupo cuenta solo si algún día de la semana lo admite; en PUSH/PULL/LEGS no se exige pecho el día de piernas.
5. **El objetivo semanal se escala por días** (`min(objetivo, 4 × días)`). Evita las sesiones de 13–21 ejercicios con 1–2 días.
6. **E se implementa como componente** (`ConstructorSemanaModeloE`), no como post-proceso de una prueba. Así se puede probar unitariamente.

## 3. Resultados reconciliados (`MATRIZ_EXPERIMENTOS_REVISADA.csv`, 960 perfiles dentro de P-01)

| | C1 vigente | C2 evolución mínima | C3 modelo E |
|---|---:|---:|---:|
| Válidos con la política del camino | 875 | 950 | 781 |
| Experimentales (solo incumplen el tope de V24 o el presupuesto 4D) | — | — | 178 |
| Inválidos | 85 (V20 79, V15 6) | 10 (V15 6, V20 4) | 1 (V15) |
| GAIN_MUSCLE a 75 min | 0/60 | 56/60 | 60/60 |
| Sesiones por encima de M × 1,15 (V4 estricto) | 0 | 0 | 0 |
| Ejercicios por sesión (máx. / media del máximo) | 8 / 6,1 | 8 / 6,1 | 11 / 5,6 |
| % de M (media de las sesiones) | 78 | 76 | 67 |
| Planes con alguna sesión de > 10 series de un grupo | 14 % | 15 % | 4 % |
| Planes con algún grupo grande entrenable por debajo del objetivo escalado | 57 % | 58 % | 34 % |
| Planes idénticos a C1 | — | 925/960 | — |
| Tiempo medio de cálculo | 5,5 ms | 6,2 ms | 3,3 ms |

**Lectura.**
- Corregido el laboratorio, ninguna variante excede el 115 % real.
- E ya no produce sesiones de 13–21 ejercicios: el máximo es 11.
- **No se acepta «896 de E» como prueba de factibilidad.** La cifra reconciliada es 781 válidas con V20 sustituida y 178 experimentales que siguen necesitando un cambio de V24. Es decir: **con el tope vigente, E solo es factible en 781 de 960**.

## 4. Limitaciones del laboratorio

- Usuarios y semanas sintéticos. Mide factibilidad y propiedades estructurales, no eficacia ni adherencia.
- Series **directas** por `body_part`. No hay datos de músculos secundarios para el conteo fraccional.
- Los objetivos de E (10/4 por semana, escalado 4 × días, techo de 10 por sesión, 20 por semana) son **parámetros de experimento sin aprobar**.
